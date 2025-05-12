/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.dataset.service.jpa.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.attribute.AttributeNotFoundException;
import org.qubership.atp.dataset.exception.datasetlist.DataSetListNotFoundException;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.DataSetListDependencyNode;
import org.qubership.atp.dataset.service.jpa.model.copy.DataSetListCopyData;
import org.qubership.atp.dataset.service.jpa.model.dscontext.CycleChecker;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.dsllazyload.dsl.DataSetListFlat;
import org.qubership.atp.dataset.service.jpa.model.dsllazyload.referencedcontext.RefDataSetListFlat;
import org.qubership.atp.dataset.service.rest.server.CopyDataSetListsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JpaDataSetListServiceImpl implements JpaDataSetListService {
    @Autowired
    protected DataSetListContextService dataSetListContextService;
    @Autowired
    protected ModelsProvider modelsProvider;
    @Autowired
    protected DataSetParameterProvider parameterProvider;
    @Autowired
    protected DataSetListAsyncService dataSetListAsyncService;

    /**
     * For top level DSL.
     */
    @Override
    @Transactional(readOnly = true)
    public DataSetListFlat getDataSetListFlat(UUID dataSetListId) {
        try {
            DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
            checkNotNull(dataSetListId, dataSetList);
            return new DataSetListFlat(dataSetList, parameterProvider);
        } finally {
            dataSetListContextService.dropLocalThreadCache();
        }
    }

    /**
     * Copy DSLs. If updateReferences == true -
     * updates attributes DSL references with new DSL ids from copy scope.
     */
    @Override
    @Transactional
    public List<CopyDataSetListsResponse> copyDataSetLists(List<UUID> dataSetListIds, boolean updateReferences,
                                                           String postfix, String prevNamePattern) {
        return copyDataSetLists(dataSetListIds, updateReferences, null, postfix, prevNamePattern, null);
    }

    /**
     * Copy DSLs. If updateReferences == true -
     * updates attributes DSL references with new DSL ids from copy scope.
     */
    @Override
    @Transactional
    public List<CopyDataSetListsResponse> copyDataSetLists(List<UUID> dataSetListIds, boolean updateReferences,
                                                           UUID targetVisibilityAreaId, String postfix,
                                                           String prevNamePattern, @Nullable UUID sagaSessionId) {
        List<CopyDataSetListsResponse> result = new LinkedList<>();
        Map<UUID, UUID> dataSetMap = new LinkedHashMap<>();
        List<DataSetList> copies = new LinkedList<>();
        Map<UUID, DataSetListCopyData> copiesData = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        long duration;
        try {
            copyDataSetLists(dataSetListIds, copies, copiesData, dataSetMap, result, postfix, prevNamePattern,
                    sagaSessionId);
            if (updateReferences) {
                for (DataSetList dataSetList : copies) {
                    log.debug("UpdateReferences dataSetList copies id: {}", dataSetList.getId());
                    dataSetList.updateDslReferences(copiesData, dataSetMap);
                }
            }
            duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("Finish copy Dataset Lists, time sec: {}", duration);
        } catch (Exception e) {
            log.error("Cannot copy Data Set Lists or update Dsl References exception:", e);
        }
        log.info("Copy all DSL step 1 - Finish");

        startTime = System.currentTimeMillis();
        List<Future<?>> overlapFutures = new LinkedList<>();
        for (DataSetList dataSetList : copies) {
            try {
                overlapFutures.add(dataSetListAsyncService.updateOverlaps(dataSetList, copiesData));
            } catch (Exception e) {
                log.error("Cannot update overlaps overlapFutures.add: ", e);
            }
        }
        for (Future<?> future : overlapFutures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Cannot update overlaps future.get: ", e);
            }
        }
        if (targetVisibilityAreaId != null) {
            for (DataSetList dataSetList : copies) {
                dataSetList.setVisibilityArea(targetVisibilityAreaId);
            }
        }
        duration = (System.currentTimeMillis() - startTime) / 1000;
        log.info("Finish copy Dataset Lists, time sec: {}", duration);
        log.info("Update overlaps step 2 - Finish");
        return result;
    }

    private void copyDataSetLists(List<UUID> dataSetListIds, List<DataSetList> copies,
                                  Map<UUID, DataSetListCopyData> copiesData, Map<UUID, UUID> dataSetMap,
                                  List<CopyDataSetListsResponse> result, @Nullable String postfix,
                                  String prevNamePattern, @Nullable UUID sagaSessionId) {
        Map<UUID, Future<DataSetListCopyData>> copyDslFutures = new LinkedHashMap<>();
        for (UUID dataSetListId : dataSetListIds) {
            copyDslFutures.put(dataSetListId,
                    dataSetListAsyncService.duplicate(dataSetListId, postfix, prevNamePattern, sagaSessionId));
        }
        for (UUID dataSetListId : copyDslFutures.keySet()) {
            try {
                DataSetListCopyData duplicateData = copyDslFutures.get(dataSetListId).get();
                Preconditions.checkNotNull(duplicateData, "Duplicate data is null for DataSetList with id "
                        + dataSetListId);
                copies.add(duplicateData.getDataSetListCopy());
                copiesData.put(dataSetListId, duplicateData);
                dataSetMap.putAll(duplicateData.getDataSetsMap());

                CopyDataSetListsResponse dslResult = new CopyDataSetListsResponse();
                dslResult.setOriginalId(dataSetListId);
                dslResult.setCopyId(duplicateData.getCopyId());
                dslResult.setDatasets(duplicateData.getDataSetsMap());
                result.add(dslResult);
            } catch (Exception e) {
                log.error("Cannot duplicate DataSetList with id {}.", dataSetListId, e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DataSetList getById(UUID dataSetListId) {
        return modelsProvider.getDataSetListById(dataSetListId);
    }

    @Override
    @Transactional
    public void remove(UUID dataSetListId) {
        DataSetList object = modelsProvider.getDataSetListById(dataSetListId);
        if (object != null) {
            object.remove();
        }
    }

    @Override
    @Transactional
    public DataSetList create(String name, UUID visibilityAreaId) {
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(visibilityAreaId);
        return visibilityArea.createDataSetList(name);
    }

    @Override
    @Transactional
    public DataSetList replicate(UUID id, String name, UUID visibilityAreaId, UUID sourceId, UUID createdBy,
                                 Timestamp createdWhen, UUID modifiedBy, Timestamp modifiedWhen)
            throws DataSetServiceException {
        return modelsProvider.replicateDataSetList(id, name, visibilityAreaId, sourceId, createdBy, createdWhen,
                modifiedBy, modifiedWhen);
    }

    /**
     * For referenced DSL.
     */
    @Override
    @Transactional(readOnly = true)
    public RefDataSetListFlat getReferencedDataSetListFlat(UUID dataSetListId,
                                                           UUID dataSetListAttributeId,
                                                           String attributePath,
                                                           @Nullable List<UUID> dataSetIds,
                                                           Pageable pageable) {
        try {
            DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
            checkNotNull(dataSetListId, dataSetList);
            List<Integer> columnsToLoad = getColumnsToLoad(dataSetList, dataSetListAttributeId, dataSetIds);
            boolean isLastPage = checkLastPage(columnsToLoad, pageable);
            columnsToLoad = getSubListByPage(columnsToLoad, pageable);

            List<UUID> path = getPath(attributePath);
            DataSetListContext dataSetListContext = dataSetListContextService.getDataSetListContext(
                    dataSetListId, columnsToLoad, Arrays.asList(AttributeTypeName.values()), path, pageable
            );
            return new RefDataSetListFlat(dataSetListContext, dataSetList, path, parameterProvider, isLastPage);
        } finally {
            dataSetListContextService.dropLocalThreadCache();
        }
    }

    /**
     * For referenced DSL.
     */
    @Override
    @Transactional(readOnly = true)
    public RefDataSetListFlat getReferencedDataSetListFlatRows(UUID dataSetListId,
                                                               UUID dataSetListAttributeId,
                                                               String attributePath,
                                                               @Nullable List<UUID> dataSetIds,
                                                               Pageable pageable) {
        try {
            DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
            checkNotNull(dataSetListId, dataSetList);
            List<Integer> columnsToLoad = getColumnsToLoad(dataSetList, dataSetListAttributeId, dataSetIds);
            List<UUID> path = getPath(attributePath);
            DataSetListContext dataSetListContext = dataSetListContextService.getDataSetListContext(
                    dataSetListId, columnsToLoad, Arrays.asList(AttributeTypeName.values()), path, pageable
            );
            return new RefDataSetListFlat(dataSetListContext, dataSetList, path, parameterProvider);
        } finally {
            dataSetListContextService.dropLocalThreadCache();
        }
    }

    private List<Integer> getColumnsToLoad(DataSetList dataSetList,
                                           UUID dataSetListAttributeId,
                                           @Nullable List<UUID> dataSetIds) {
        Attribute attribute = modelsProvider.getAttributeById(dataSetListAttributeId);
        if (attribute == null) {
            log.error("Attribute {} not found", dataSetListAttributeId);
            throw new AttributeNotFoundException();
        }
        List<Integer> columnsToLoad = new LinkedList<>();
        if (CollectionUtils.isEmpty(dataSetIds)) {
            Integer dataSetsCount = dataSetList.getDataSetsCount();
            for (int i = 0; i < dataSetsCount; i++) {
                columnsToLoad.add(i);
            }
        } else {
            columnsToLoad = dataSetList.getDataSetColumnsByIds(dataSetIds);
        }
        return columnsToLoad;
    }

    private void checkNotNull(UUID dataSetListId, DataSetList dataSetList) {
        if (dataSetList == null) {
            log.error("Data Set List {} not found", dataSetListId);
            throw new DataSetListNotFoundException();
        }
    }

    private boolean checkLastPage(List list, Pageable pageable) {
        int startIdx = pageable.getPageNumber() * pageable.getPageSize();
        return startIdx >= (list.size() - pageable.getPageSize());
    }

    private <T> List<T> getSubListByPage(List<T> list, Pageable pageable) {
        int startIdx = pageable.getPageNumber() * pageable.getPageSize();
        int endIdx = startIdx + pageable.getPageSize();
        if (startIdx >= list.size()) {
            return Collections.emptyList();
        }
        return list.subList(startIdx, Math.min(endIdx, list.size()));
    }

    /**
     * Path as list of UUIDs.
     */
    public List<UUID> getPath(String key) {
        List<UUID> result = new LinkedList<>();
        String[] splitResult = key.split(",");
        for (String splitPart : splitResult) {
            result.add(UUID.fromString(splitPart));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public DataSetListContext getDataSetListContext(UUID dataSetListId, List<UUID> dataSetsIds, Pageable pageable) {
        AttributeTypeName[] attributesToLoad = {
                AttributeTypeName.TEXT,
                AttributeTypeName.ENCRYPTED,
                AttributeTypeName.LIST,
                AttributeTypeName.FILE,
                AttributeTypeName.CHANGE};
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        checkNotNull(dataSetListId, dataSetList);
        List<Integer> columnsToLoad = dataSetList.getDataSetsColumns(dataSetsIds);
        return dataSetListContextService.getDataSetListContext(
                dataSetListId, columnsToLoad, Arrays.asList(attributesToLoad), null, pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DataSetListContext getDataSetListContext(UUID dataSetListId, List<UUID> dataSetsIds) {
        AttributeTypeName[] attributesToLoad = {
                AttributeTypeName.TEXT,
                AttributeTypeName.ENCRYPTED,
                AttributeTypeName.LIST,
                AttributeTypeName.FILE,
                AttributeTypeName.CHANGE};
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        checkNotNull(dataSetListId, dataSetList);
        List<Integer> columnsToLoad = dataSetList.getDataSetsColumns(dataSetsIds);
        return dataSetListContextService.getDataSetListContext(
                dataSetListId, columnsToLoad, Arrays.asList(attributesToLoad), null, null
        );
    }

    @Override
    @Transactional
    public void save(DataSetList dataSetList) {
        dataSetList.save();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSetListDependencyNode> getDependencies(List<UUID> dataSetListIds) {
        List<DataSetListDependencyNode> result = new LinkedList<>();
        for (UUID dataSetListId : dataSetListIds) {
            result.add(getDependencies(dataSetListId));
        }
        return result;
    }

    /**
     * Returns DSL dependency tree.
     */
    public DataSetListDependencyNode getDependencies(UUID dataSetListId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        checkNotNull(dataSetListId, dataSetList);
        return DataSetListDependencyNode.getDependencyTree(dataSetList);
    }

    @Override
    public List<DataSetListDependencyNode> getDependenciesRecursive(List<UUID> dataSetListIds) {
        Set<UUID> dataSetListIdsResult = new LinkedHashSet<>();
        for (UUID dataSetListId : dataSetListIds) {
            DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
            if (dataSetList != null) {
                CycleChecker cycleChecker = new CycleChecker();
                getAllDslRecursively(dataSetList, dataSetListIdsResult, cycleChecker);
            }
        }
        return getDependencies(new LinkedList<>(dataSetListIdsResult));
    }

    @Override
    public List<DataSetList> getByNameAndVisibilityAreaId(String name, UUID visibilityArea) {
        return modelsProvider.getDataSetListByNameAndVisibilityAreaId(name, visibilityArea);
    }

    @Override
    public List<DataSetList> getByVisibilityAreaId(UUID visibilityAreaId) {
        return modelsProvider.getByVisibilityAreaId(visibilityAreaId);
    }

    @Override
    public List<DataSetList> getBySourceIdAndVisibilityAreaId(UUID sourceId, UUID visibilityAreaId) {
        return modelsProvider.getDataSetListBySourceIdAndVisibilityAreaId(sourceId, visibilityAreaId);
    }

    private Set<UUID> getAllDslRecursively(DataSetList dataSetList, Set<UUID> dataSetListIds,
                                           CycleChecker cycleChecker) {
        dataSetListIds.add(dataSetList.getId());
        cycleChecker.openNode(dataSetList.getId());
        List<Attribute> referencedAttributes = dataSetList.getAttributesByTypes(
                Collections.singletonList(AttributeTypeName.DSL)
        );
        for (Attribute referencedAttribute : referencedAttributes) {
            DataSetList typeDataSetList = referencedAttribute.getTypeDataSetList();
            getAllDslRecursively(typeDataSetList, dataSetListIds, cycleChecker);
        }
        cycleChecker.closeNode(dataSetList.getId());
        return dataSetListIds;
    }

    @Transactional(readOnly = true)
    @Override
    public Timestamp getModifiedWhen(UUID dataSetListId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        if (dataSetList == null) {
            return Timestamp.from(Instant.EPOCH);
        }
        return dataSetList.getModifiedWhen();
    }

    @Override
    public LinkedList<UUID> getDataSetsIdsByDataSetListId(UUID dataSetListId) {
        return modelsProvider.getDataSetsIdsByDataSetListId(dataSetListId);
    }

    @Transactional
    @Override
    public void checkDslNames(UUID visibilityArea) {
        List<String> names = modelsProvider.getNotUniqueDslNames(visibilityArea);
        if (!names.isEmpty()) {
            throw new RuntimeException(String.format("There are duplicated DSL names: %s", names));
        }
    }
}

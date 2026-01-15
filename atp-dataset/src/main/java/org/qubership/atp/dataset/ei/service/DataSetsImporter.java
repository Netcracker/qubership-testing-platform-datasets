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

package org.qubership.atp.dataset.ei.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.dataset.ei.Constants;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSetsImporter {

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final JpaDataSetService dsService;
    private final JpaDataSetListService dslService;
    private final DuplicateNameChecker duplicateNameChecker;
    private final EntityManagerController entityManagerController;
    private final ClearCacheService clearCacheService;

    /**
     * Import data sets.
     *
     * @param workDir the work dir
     */
    public void importDataSets(Path workDir, ExportImportData importData) {
        log.info("start importDataSets(workDir: {})", workDir);

        Map<UUID, Long> maxOrdering = new HashMap<>();

        Map<UUID, Path> list = objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSet.class);
        log.debug("importDataSets list: {}", list);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(list.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                try {
                    importDataSets(id, list.get(id), maxOrdering, importData);
                } catch (RuntimeException e) {
                    log.error("Cannot import dataset {}", id, e);
                    throw e;
                }
            });
            entityManagerController.flushAndClear();
            log.info("Chunk with size {} completed. Total size {}.", chunk.size(), list.size());
        }

        log.info("end importDataSets()");
    }

    private void importDataSets(UUID id, Path path, Map<UUID, Long> maxOrdering, ExportImportData importData) {
        log.debug("importDataSets start import id: {}", id);

        org.qubership.atp.dataset.ei.model.DataSet object;
        if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
            Map<UUID, UUID> map = new HashMap<>(importData.getReplacementMap());
            object = objectLoaderFromDiskService
                    .loadFileAsObjectWithReplacementMap(path, org.qubership.atp.dataset.ei.model.DataSet.class, map);

        } else {
            object = objectLoaderFromDiskService
                    .loadFileAsObject(path, org.qubership.atp.dataset.ei.model.DataSet.class);
        }
        log.debug("import object: {}", object);
        if (object == null) {
            String message = String.format("Cannot load file by path %s", path.toString());
            log.error(message);
            throw new RuntimeException(message);
        }

        DataSet dataSet = dsService.getById(object.getId());
        Long order = calculateOrder(maxOrdering, object);
        if (dataSet == null) {
            object.setSourceId(id);
            createWithCheckName(object, order);
        } else {
            updateWithCheckName(dataSet, object, order);
        }

        duplicateNameChecker.addToCache(object.getDataSetList(), object);
        clearCacheService.evictDatasetListContextCache(id);
    }

    private Long calculateOrder(Map<UUID, Long> maxOrdering, org.qubership.atp.dataset.ei.model.DataSet object) {
        try {
            if (!maxOrdering.containsKey(object.getDataSetList())) {
                DataSetList dsl =
                        dslService.getById(object.getDataSetList());
                maxOrdering.put(object.getDataSetList(), dsl.getLastDataSetsOrderNumber());
            }
            return maxOrdering.get(object.getDataSetList()) + object.getOrdering();
        } catch (Exception e) {
            log.error("Unable calculate order for dataset = [{}], maxOrdering = [{}]",
                    object, maxOrdering, e);
            throw e;
        }
    }

    private void updateWithCheckName(DataSet dataSet,
                                     org.qubership.atp.dataset.ei.model.DataSet object, Long order) {
        checkAndCorrectName(object);
        dataSet.setName(object.getName());
        dataSet.setDataSetList(object.getDataSetList());
        dataSet.setOrdering(order);
        dataSet.setLocked(isDataSetLocked(object));
        log.debug("[importDataSets][save DataSet] dataSet: {}", dataSet);
        dsService.save(dataSet);
    }

    private void createWithCheckName(org.qubership.atp.dataset.ei.model.DataSet object, Long order) {
        checkAndCorrectName(object);
        try {
        dsService.replicate(object.getId(), object.getName(), object.getDataSetList(),
                            order, object.getSourceId(), isDataSetLocked(object));
        } catch (DataSetServiceException e) {
            String message = String.format("Cannot create new data set by import object %s", object);
            throw new ExportException(message, e);
        }
    }

    private boolean isDataSetLocked(org.qubership.atp.dataset.ei.model.DataSet dataSet) {
        return dataSet.getIsLocked() != null && dataSet.getIsLocked();
    }

    public void checkAndCorrectName(org.qubership.atp.dataset.ei.model.DataSet object) {
        initCache(object.getDataSetList());
        duplicateNameChecker.checkAndCorrectName(object.getDataSetList(), object);
    }

    private boolean isNameUsed(org.qubership.atp.dataset.ei.model.DataSet object, UUID dsListId) {
        initCache(dsListId);
        return duplicateNameChecker.isNameUsed(dsListId, object);
    }

    private void initCache(UUID dsListId) {
        if (!duplicateNameChecker.isInitialized(org.qubership.atp.dataset.ei.model.DataSet.class, dsListId)) {
            Multimap<String, UUID> initMap = HashMultimap.create();
            dsService.getByDataSetListId(dsListId)
                    .forEach(entity -> initMap.put(entity.getName(), entity.getId()));
            duplicateNameChecker.init(org.qubership.atp.dataset.ei.model.DataSet.class, dsListId, initMap);
        }
    }

    /**
     * Gets DS ids.
     *
     * @param workDir the work dir
     * @return the object ids
     */
    public List<UUID> getDsIds(Path workDir) {
        return new ArrayList<>(objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSet.class)
                .keySet());
    }

    /**
     * Validate data sets collection.
     *
     * @param workDir the work dir
     * @return the collection
     */
    public Collection<? extends String> validateDataSets(Path workDir, Map<UUID, UUID> repMap,
                                                         boolean isInterProjectImport) {
        Set<String> result = new HashSet<>();

        Map<UUID, Path> list = objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSet.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(list.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> validateDataSets(id, list.get(id), repMap, isInterProjectImport, result));
        }

        return new ArrayList<>(result);
    }

    private void validateDataSets(UUID id, Path path, Map<UUID, UUID> repMap,
                                  boolean isInterProjectImport,
                                  Set<String> result) {
        log.debug("validateDataSets start import id: {}", id);

        org.qubership.atp.dataset.ei.model.DataSet object;
        if (isInterProjectImport) {
            object = objectLoaderFromDiskService
                    .loadFileAsObjectWithReplacementMap(path, org.qubership.atp.dataset.ei.model.DataSet.class, repMap);
        } else {
            object = objectLoaderFromDiskService
                    .loadFileAsObject(path, org.qubership.atp.dataset.ei.model.DataSet.class);
        }
        log.debug("validate object: {}", object);
        if (object == null) {
            log.error("Cannot load file by path {}", path.toString());
            result.add("Some file cannot be loaded from import archive.");
            return;
        }

        if (isNameUsed(object, object.getDataSetList())) {
            DataSetList dataSetList =
                    dslService.getById(object.getDataSetList());
            result.add(String.format(
                    "Data Set with name '%s' already exists in the data set list '%s'. "
                            + "Imported one will be renamed to '%s Copy'.",
                    object.getName(), dataSetList.getName(), object.getName()));
        }
    }

    /**
     * Fills Replacement map with dataset source-target values.
     *
     * @param replacementMap the replacement map
     * @param workDir        the work dir
     */
    public void fillRepMapWithSourceTargetValues(Map<UUID, UUID> replacementMap,
                                                 Path workDir) {
        Map<UUID, Path> objectsToImport = objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSet.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(objectsToImport.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                if (!replacementMap.containsKey(id)) {
                    org.qubership.atp.dataset.ei.model.DataSet object
                            = objectLoaderFromDiskService.loadFileAsObject(objectsToImport.get(id),
                            org.qubership.atp.dataset.ei.model.DataSet.class);
                    List<DataSet> existingObject =
                            dsService.getBySourceAndDataSetListId(id,
                                    replacementMap.get(object.getDataSetList()));
                    if (CollectionUtils.isEmpty(existingObject)) {
                        replacementMap.put(id, null);
                    } else {
                        replacementMap.put(id, existingObject.get(0).getId());
                    }
                }
            });
        }
    }
}

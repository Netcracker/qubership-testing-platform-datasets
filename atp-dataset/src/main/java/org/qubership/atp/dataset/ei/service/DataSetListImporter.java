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

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.dataset.ei.Constants;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaVisibilityAreaService;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;
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
public class DataSetListImporter {

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final JpaDataSetListService dslService;
    private final JpaVisibilityAreaService vaService;
    private final DuplicateNameChecker duplicateNameChecker;
    private final EntityManagerController entityManagerController;

    /**
     * Import data set lists list.
     *
     * @param workDir the work dir
     * @return the list
     */
    public List<UUID> importDataSetLists(Path workDir, ExportImportData importData) {
        log.info("start importDataSetLists(workDir: {})", workDir);

        List<UUID> dslImportedIds = new ArrayList<>();
        Map<UUID, Path> list = objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSetList.class);
        log.debug("importDataSetLists list: {}", list);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(list.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                try {
                    dslImportedIds.add(importDataSetList(id, list.get(id), importData));
                } catch (RuntimeException e) {
                    log.error("Cannot import dataset list {}", id, e);
                    throw e;
                }
            });
            entityManagerController.flushAndClear();
            log.info("Chunk with size {} completed. Total size {}.", chunk.size(), list.size());
        }

        log.info("end importDataSetLists(list: {})", list);
        return dslImportedIds;
    }

    private UUID importDataSetList(UUID id, Path path, ExportImportData importData) {
        log.debug("importDataSetLists start import id: {}", id);

        org.qubership.atp.dataset.ei.model.DataSetList object;
        if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
            Map<UUID, UUID> map = new HashMap<>(importData.getReplacementMap());
            object = objectLoaderFromDiskService
                    .loadFileAsObjectWithReplacementMap(
                            path, org.qubership.atp.dataset.ei.model.DataSetList.class, map);
        } else {
            object = objectLoaderFromDiskService
                    .loadFileAsObject(path, org.qubership.atp.dataset.ei.model.DataSetList.class);
        }
        log.debug("Import object: {} in path: {}", object, path);
        if (object == null) {
            String message = String.format("Cannot load file by path %s", path.toString());
            log.error(message);
            throw new RuntimeException(message);
        }
        UUID vaId = object.getVisibilityArea();
        createVisibilityAreaIfAbsent(vaId);

        DataSetList dataSetList = dslService.getById(object.getId());
        if (dataSetList == null) {
            object.setSourceId(id);
            createWithCheckName(object);
        } else {
            updateWithCheckName(dataSetList, object);
        }
        duplicateNameChecker.addToCache(object.getVisibilityArea(), object);
        return object.getId();
    }

    /**
     * Gets DSL ids.
     *
     * @param workDir the work dir
     * @return the object ids
     */
    public List<UUID> getDslIds(Path workDir) {
        return new ArrayList<>(objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSetList.class)
                .keySet());
    }

    private void updateWithCheckName(DataSetList dataSetList,
                                     org.qubership.atp.dataset.ei.model.DataSetList object) {
        checkAndCorrectName(object);

        dataSetList.setName(object.getName());
        dataSetList.setVisibilityArea(object.getVisibilityArea());
        log.debug("[ImportDataSetList][save] import object: {} datasetList: {} ", object, dataSetList);
        dslService.save(dataSetList);
    }

    private void createWithCheckName(org.qubership.atp.dataset.ei.model.DataSetList object) {
        checkAndCorrectName(object);
        try {
        dslService.replicate(object.getId(), object.getName(), object.getVisibilityArea(),
                             object.getSourceId(), object.getCreatedBy(), object.getCreatedWhen(),
                             object.getModifiedBy(), object.getModifiedWhen());
        } catch (DataSetServiceException e) {
            String message = String.format("Cannot create new data set list by import object %s", object);
            log.error(message);
            throw new ExportException(message, e);
        }
    }

    /**
     * Check and correct name.
     *
     * @param object the object
     */
    public void checkAndCorrectName(org.qubership.atp.dataset.ei.model.DataSetList object) {
        initCache(object.getVisibilityArea());
        duplicateNameChecker.checkAndCorrectName(object.getVisibilityArea(), object);
    }

    private boolean isNameUsed(org.qubership.atp.dataset.ei.model.DataSetList object, UUID visibilityAreaId) {
        initCache(visibilityAreaId);
        return duplicateNameChecker.isNameUsed(visibilityAreaId, object);
    }

    private void initCache(UUID visibilityAreaId) {
        if (!duplicateNameChecker
                .isInitialized(org.qubership.atp.dataset.ei.model.DataSetList.class, visibilityAreaId)) {
            Multimap<String, UUID> initMap = HashMultimap.create();
            dslService.getByVisibilityAreaId(visibilityAreaId)
                    .forEach(entity -> initMap.put(entity.getName(), entity.getId()));
            duplicateNameChecker.init(org.qubership.atp.dataset.ei.model.DataSetList.class, visibilityAreaId, initMap);
        }
    }

    private void createVisibilityAreaIfAbsent(UUID vaId) {
        try {
            VisibilityArea visibilityArea = vaService.getById(vaId);
            if (visibilityArea == null) {
                log.info("waitForCreatingOfVisibilityAreaOrCreateNewByTimeout visibilityArea: {}", visibilityArea);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyyHH:mm:ss");
                vaService.replicate(vaId, "Exported VA " + simpleDateFormat.format(new Date()));
                // there is no such important a name of VA as its' id.
            }
        } catch (DataSetServiceException e) {
            String message =
                    String.format("Cannot create Visibility Area %s", vaId);
            throw new ExportException(message, e);
        }
    }

    /**
     * Validate data set lists list.
     *
     * @param workDir the work dir
     * @return the list
     */
    public List<String> validateDataSetLists(Path workDir, Map<UUID, UUID> repMap, boolean isInterProjectImport) {
        Set<String> result = new HashSet<>();

        Map<UUID, Path> list = objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSetList.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(list.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> validateDataSetLists(id, list.get(id), repMap, isInterProjectImport, result));
        }

        return new ArrayList<>(result);
    }

    private void validateDataSetLists(UUID id, Path path, Map<UUID, UUID> repMap,
                                      boolean isInterProjectImport,
                                      Set<String> result) {
        log.debug("validateDataSetLists start import id: {}", id);

        org.qubership.atp.dataset.ei.model.DataSetList object;
        if (isInterProjectImport) {
            object = objectLoaderFromDiskService
                    .loadFileAsObjectWithReplacementMap(
                            path, org.qubership.atp.dataset.ei.model.DataSetList.class, repMap);
        } else {
            object = objectLoaderFromDiskService
                    .loadFileAsObject(path, org.qubership.atp.dataset.ei.model.DataSetList.class);
        }
        log.debug("validate object: {}", object);
        if (object == null) {
            log.error("Cannot load file by path {}", path.toString());
            result.add("Some file cannot be loaded from import archive.");
            return;
        }
        if (isNameUsed(object, object.getVisibilityArea())) {
            result.add(String.format(
                    "Data Set List with name '%s' already exists in the project. "
                            + "Imported one will be renamed to '%s Copy'.",
                    object.getName(), object.getName()));
        }
    }

    /**
     * Fills Replacement map with datasetlist source-target values.
     *
     * @param replacementMap the replacement map
     * @param workDir        the work dir
     */
    public void fillRepMapWithSourceTargetValues(Map<UUID, UUID> replacementMap,
                                                 Path workDir) {
        Map<UUID, Path> objectsToImport = objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSetList.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(objectsToImport.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                if (!replacementMap.containsKey(id)) {
                    org.qubership.atp.dataset.ei.model.DataSetList object
                            = objectLoaderFromDiskService.loadFileAsObject(objectsToImport.get(id),
                            org.qubership.atp.dataset.ei.model.DataSetList.class);
                    List<DataSetList> existingObject =
                            dslService.getBySourceIdAndVisibilityAreaId(id,
                                    replacementMap.get(object.getVisibilityArea()));
                    if (CollectionUtils.isEmpty(existingObject)) {
                        replacementMap.put(id, null);
                    } else {
                        replacementMap.put(id, existingObject.get(0).getId());
                    }
                }
            });
        }
    }

    public void clearDuplicateNamesCache() {
        duplicateNameChecker.clearCache();
    }

}

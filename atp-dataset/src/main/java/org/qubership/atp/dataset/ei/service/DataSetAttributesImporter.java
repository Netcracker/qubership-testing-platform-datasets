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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.qubership.atp.dataset.ei.Constants;
import org.qubership.atp.dataset.ei.model.AttributeKeyIdsDbUpdate;
import org.qubership.atp.dataset.ei.model.DataSetAttribute;
import org.qubership.atp.dataset.ei.model.DataSetAttributeKey;
import org.qubership.atp.dataset.ei.model.DataSetListValue;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
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
public class DataSetAttributesImporter {

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final JpaAttributeService attrService;
    private final JpaDataSetListService dslService;
    private final DuplicateNameChecker duplicateNameChecker;
    private final EntityManagerController entityManagerController;

    /**
     * Import data set attributes.
     *
     * @param workDir      the work dir
     * @param dataSetLists the data set lists
     */
    public void importDataSetAttributes(Path workDir, List<UUID> dataSetLists, ExportImportData importData) {
        log.info("start importDataSetAttributes(workDir: {}, dataSetLists: {})", workDir, dataSetLists);
        Map<UUID, Path> list = objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttribute.class);
        log.debug("importDataSetAttributes list: {}", list);
        List<UUID> createdAttributes = new ArrayList<>();
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(list.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> importDataSetAttributes(id, list.get(id), dataSetLists, createdAttributes, importData));
            entityManagerController.flushAndClear();
            log.info("Chunk with size {} completed. Total size {}.", chunk.size(), list.size());
        }

        Map<UUID, Path> listValues =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetListValue.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(listValues.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> importDataSetListValues(id, listValues.get(id), createdAttributes, importData));
            entityManagerController.flushAndClear();
            log.info("Chunk with size {} completed. Total size {}.", chunk.size(), list.size());
        }
        log.info("end importDataSetAttributes()");
    }

    private void importDataSetAttributes(UUID id, Path path, List<UUID> dataSetLists,
                                         List<UUID> createdAttributes, ExportImportData importData) {
        log.debug("importDataSetAttributes start import id: {}", id);

        DataSetAttribute object;
        if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
            Map<UUID, UUID> map = new HashMap<>(importData.getReplacementMap());
            object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path, DataSetAttribute.class, map);
        } else {
            object = objectLoaderFromDiskService.loadFileAsObject(path, DataSetAttribute.class);
        }
        log.debug("importDataSetAttributes import object: {}", object);
        if (object == null) {
            String message = String.format("Cannot load file by path %s", path.toString());
            log.error(message);
            throw new RuntimeException(message);
        }

        if (!dataSetLists.contains(object.getDataSetList())) {
            // do not import attributes which DSL is out-of-scope of import
            // this attribute relates to attribute key from exported DSL
            return;
        }
        if (object.getTypeDataSetList() != null) {
            DataSetList dataSetList =
                                                                    dslService.getById(object.getTypeDataSetList());
            log.debug("dataSetList: {}", dataSetList);
            if (dataSetList == null && !dataSetLists.contains(object.getTypeDataSetList())) {
                return; // do not import attribute (DSL type) if DSL is absent
            }
        }
        AttributeTypeName type = AttributeTypeName.getTypeById(object.getAttributeType());
        Attribute attribute = attrService.getById(object.getId());
        if (attribute == null) {
            object.setSourceId(id);
            createWithCheckName(object, type);
        } else {
            updateWithCheckName(attribute, object, type);
        }
        duplicateNameChecker.addToCache(object.getDataSetList(), object);
        createdAttributes.add(object.getId());
    }

    private void importDataSetListValues(UUID id, Path path, List<UUID> createdAttributes,
                                         ExportImportData importData) {

        DataSetListValue object;
        if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
            Map<UUID, UUID> map = new HashMap<>(importData.getReplacementMap());
            object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path, DataSetListValue.class, map);
        } else {
            object = objectLoaderFromDiskService.loadFileAsObject(path, DataSetListValue.class);
        }
        log.debug("import object: {}", object);
        if (object == null) {
            String message = String.format("Cannot load file by path %s", path.toString());
            log.error(message);
            throw new RuntimeException(message);
        }

        if (!createdAttributes.contains(object.getAttribute())) {
            return; // do not import list values which Attribute is out-of-scope of import
        }

        ListValue listValue = attrService.getListValueById(object.getId());
        if (listValue == null) {
            object.setSourceId(id);
            try {
                attrService.replicateListValue(object.getId(), object.getText(),
                        object.getAttribute(), object.getSourceId());
            } catch (DataSetServiceException e) {
                String message = String.format("Cannot create list value by import object %s", object);
                log.error(message);
                throw new ExportException(message, e);
            }
        } else {
            listValue.setText(object.getText());
            listValue.setAttribute(object.getAttribute());
            attrService.save(listValue);
        }
    }

    private void updateWithCheckName(Attribute attribute, DataSetAttribute object, AttributeTypeName type) {
        checkAndCorrectName(object);

        attribute.setName(object.getName());
        attribute.setAttributeType(type);
        attribute.setDataSetList(object.getDataSetList());
        attribute.setTypeDataSetListId(object.getTypeDataSetList());
        attrService.save(attribute);
    }

    private void createWithCheckName(DataSetAttribute object, AttributeTypeName type) {
        checkAndCorrectName(object);
        try {
            Attribute attribute = attrService.replicate(object.getId(), object.getName(), type, object.getDataSetList(),
                            object.getSourceId());
            attribute.setTypeDataSetListId(object.getTypeDataSetList());
            attrService.save(attribute);
        } catch (DataSetServiceException e) {
            String message = String.format("Cannot create new attribute by import object %s", object);
            log.error(message);
            throw new ExportException(message, e);
        }
    }

    /**
     * Check and correct name.
     *
     * @param object the object
     */
    public void checkAndCorrectName(DataSetAttribute object) {
        initCache(object.getDataSetList());
        duplicateNameChecker.checkAndCorrectName(object.getDataSetList(), object);
    }

    private boolean isNameUsed(DataSetAttribute object, UUID dsListId) {
        initCache(dsListId);
        return duplicateNameChecker.isNameUsed(dsListId, object);
    }

    private void initCache(UUID dsListId) {
        if (!duplicateNameChecker.isInitialized(DataSetAttribute.class, dsListId)) {
            Multimap<String, UUID> initMap = HashMultimap.create();
            attrService.getByDataSetListId(dsListId)
                    .forEach(entity -> initMap.put(entity.getName(), entity.getId()));
            duplicateNameChecker.init(DataSetAttribute.class, dsListId, initMap);
        }
    }

    /**
     * Import data set attribute keys.
     *
     * @param workDir the work dir
     */
    public void importDataSetAttributeKeys(Path workDir, ExportImportData importData) throws IOException {
        log.info("start importDataSetAttributeKeys(workDir: {})", workDir);

        Map<UUID, Path> list =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttributeKey.class);
        log.debug("importDataSetAttributeKeys list: {}", list);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(list.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> importDataSetAttributeKeys(id, list.get(id), importData));
            entityManagerController.flushAndClear();
            log.debug("Chunk with size {} completed. Total size {}.", chunk.size(), list.size());
        }
        log.info("end importDataSetAttributeKeys()");
    }

    private void importDataSetAttributeKeys(UUID id, Path path, ExportImportData importData) {
        log.debug("importDataSetAttributeKeys start import id: {}", id);

        DataSetAttributeKey object;
        if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
            Map<UUID, UUID> map = new HashMap<>(importData.getReplacementMap());
            object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path,
                    DataSetAttributeKey.class, map);
            object.setKey(getKeyWithReplacement(object, map));
        } else {
            object = objectLoaderFromDiskService.loadFileAsObject(path, DataSetAttributeKey.class);
        }
        log.debug("import object: {}", object);
        if (object == null) {
            String message = String.format("Cannot load file by path %s", path.toString());
            log.error(message);
            throw new RuntimeException(message);
        }
        Attribute attribute = attrService.getById(object.getAttribute());
        log.debug("attribute: {}", attribute);
        if (attribute == null) {
            return; // no such attribute on server and we are not importing it
        }

        AttributeKey attrKey = getAttributeKey(object);
        if (attrKey == null) {
            object.setSourceId(id);
            try {
            attrService.replicateAttributeKey(object.getId(), object.getKey(), object.getAttribute(),
                                              object.getDataSet(), object.getDataSetList(), object.getSourceId());
            } catch (DataSetServiceException e) {
                String message =
                        String.format("Cannot create new attribute key by import object %s", object);
                log.error(message);
                throw new ExportException(message, e);
            }
        } else {
            attrKey.setKey(object.getKey());
            attrKey.setAttribute(object.getAttribute());
            attrKey.setDataSet(object.getDataSet());
            attrKey.setDataSetList(object.getDataSetList());
            attrService.save(attrKey);
        }
    }

    private AttributeKey getAttributeKey(DataSetAttributeKey object) {
        AttributeKey attrKey = attrService.getAttributeKeyById(object.getId());
        if (Objects.isNull(attrKey)) {
            attrKey = attrService.getAttributeKeyByKeyAndDataSetListIdAndDataSetIdAndAttributeId(object.getKey(),
                    object.getDataSetList(),object.getDataSet(),object.getAttribute());
            if (Objects.nonNull(attrKey)) {
                attrService.getFoundedAttributeKeyIdAndDatasetIdUpdate().put(object.getId(),
                        new AttributeKeyIdsDbUpdate(attrKey.getId(), attrKey.getDataSetId()));
            }
        }
        return attrKey;
    }

    private String getKeyWithReplacement(DataSetAttributeKey object, Map<UUID, UUID> replacementMap) {

        List<UUID> keyIds = new ArrayList<>();
        String[] splitResult = object.getKey().split("_");
        for (String splitPart : splitResult) {
            keyIds.add(replacementMap.get(UUID.fromString(splitPart)));
        }
        return StringUtils.join(keyIds, "_");
    }

    /**
     * Gets Dataset attribute ids.
     *
     * @param workDir the work dir
     * @return the object ids
     */
    public List<UUID> getDsAttributeIds(Path workDir) {
        return new ArrayList<>(objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttribute.class).keySet());
    }

    /**
     * Gets Dataset attribute keys ids.
     *
     * @param workDir the work dir
     * @return the object ids
     */
    public List<UUID> getDsAttributeKeysIds(Path workDir) {
        return new ArrayList<>(
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttributeKey.class).keySet());
    }

    /**
     * Gets Dataset list values ids.
     *
     * @param workDir the work dir
     * @return the object ids
     */
    public List<UUID> getDsListValuesIds(Path workDir) {
        return new ArrayList<>(objectLoaderFromDiskService.getListOfObjects(workDir, DataSetListValue.class).keySet());
    }

    /**
     * Validate data set attributes list.
     *
     * @param workDir the work dir
     * @return the list
     */
    public List<String> validateDataSetAttributes(Path workDir,
                                                  Map<UUID, UUID> repMap,
                                                  boolean isInterProjectImport) {
        log.info("start validateDataSetAttributes(workDir: {})", workDir);
        Set<String> result = new HashSet<>();
        Map<UUID, Path> dataSetLists = objectLoaderFromDiskService
                .getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSetList.class);
        Set<UUID> dslIdsWithReplacement = isInterProjectImport
                ? dataSetLists.keySet().stream().map(repMap::get).collect(Collectors.toSet()) : dataSetLists.keySet();

        Map<UUID, Path> dataSetAttributes =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttribute.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(dataSetAttributes.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                try {
                    validateDataSetAttributes(id, dataSetAttributes.get(id), dslIdsWithReplacement,
                            repMap, isInterProjectImport, result);
                } catch (RuntimeException e) {
                    log.error("Cannot import attribute {}", id, e);
                    throw e;
                }
            });
        }
        Map<UUID, Path> dataSetAttributeKeys =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttributeKey.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(dataSetAttributeKeys.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                try {
                    validateDataSetAttributeKeys(id, dataSetAttributeKeys.get(id), dataSetAttributes,
                            repMap, isInterProjectImport, result);
                } catch (RuntimeException e) {
                    log.error("Cannot import attribute key {}", id, e);
                    throw e;
                }
            });
        }
        log.info("end validateDataSetAttributes(result: {})", result);
        return new ArrayList<>(result);
    }

    private void validateDataSetAttributes(UUID id, Path path,
                                           Set<UUID> dataSetLists,
                                           Map<UUID, UUID> repMap,
                                           boolean isInterProjectImport,
                                           Set<String> result) {
        log.debug("dataSetAttributes start validate id: {}", id);

        DataSetAttribute object;
        if (isInterProjectImport) {
            object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path,
                    DataSetAttribute.class, repMap);
        } else {
            object = objectLoaderFromDiskService.loadFileAsObject(path, DataSetAttribute.class);
        }

        log.debug("dataSetAttributes validate object: {}", object);
        if (object == null) {
            log.error("Cannot load file by path {}", path.toString());
            result.add("Some file cannot be loaded from import archive.");
            return;
        }

        if (isNameUsed(object, object.getDataSetList())) {
            DataSetList dataSetList;
            dataSetList = dslService.getById(object.getDataSetList());
            result.add(String.format(
                    "Attribute with name '%s' already exists in Data Set List '%s'. "
                            + "Imported one will be renamed to '%s Copy'.",
                    object.getName(), dataSetList.getName(), object.getName()));
        }

        if (object.getTypeDataSetList() != null) {
            DataSetList dataSetList;
            dataSetList = dslService.getById(object.getTypeDataSetList());
            log.debug("dataSetList: {}", dataSetList);
            if (dataSetList == null && !dataSetLists.contains(object.getTypeDataSetList())) {
                log.error(
                        "Attribute {} has link to data set list {}, "
                                + "but this data set list is absent in server and is not being imported",
                        object, object.getTypeDataSetList());
                result.add(
                        String.format(
                                "There is an attribute that has the link to data set list %s, "
                                        + "but this data set list is absent in server and is not being imported.",
                                object.getName()));
            }
        }
    }

    private void validateDataSetAttributeKeys(UUID id, Path path,
                                              Map<UUID, Path> dataSetAttributes,
                                              Map<UUID, UUID> repMap,
                                              boolean isInterProjectImport,
                                              Set<String> result) {
        log.debug("dataSetAttributeKeys start validate id: {}", id);

        UUID attributeIdWithoutReplacement;
        DataSetAttributeKey object;
        if (isInterProjectImport) {
            object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path,
                    DataSetAttributeKey.class, repMap);
            DataSetAttributeKey objectWithoutReplacementMap =
                    objectLoaderFromDiskService.loadFileAsObject(path, DataSetAttributeKey.class);
            attributeIdWithoutReplacement = objectWithoutReplacementMap.getAttribute();
        } else {
            object = objectLoaderFromDiskService.loadFileAsObject(path, DataSetAttributeKey.class);
            attributeIdWithoutReplacement = object.getAttribute();
        }
        log.debug("dataSetAttributeKeys validate object: {}", object);
        if (object == null) {
            log.error("Cannot load file by path {}", path.toString());
            result.add("Some file cannot be loaded from import archive.");
            return;
        }

        Attribute attribute = attrService.getById(object.getAttribute());
        log.debug("attribute: {}", attribute);
        if (attribute == null) {
            DataSetAttribute linkToDsAttribute = objectLoaderFromDiskService
                    .loadFileAsObject(dataSetAttributes.get(attributeIdWithoutReplacement),
                            DataSetAttribute.class);
            if (linkToDsAttribute == null) {
                log.error("Cannot load file by path {}", path.toString());
                result.add("Some file cannot be loaded from import archive.");
                return;
            }

            DataSetAttribute absentAttribute = objectLoaderFromDiskService
                    .loadFileAsObject(dataSetAttributes.get(attributeIdWithoutReplacement),
                            DataSetAttribute.class);
            if (absentAttribute == null) {
                log.error("Attribute key (Link to Data Set Attribute) {} refers to absent attribute {}", object,
                        object.getAttribute());
                result.add(String.format("There is a Link to Data Set Attribute "
                        + "that refers to absent attribute on server."));
            }
        }
    }

    /**
     * Fills Replacement map with attributes, attributes keys and list source-target values.
     *
     * @param replacementMap the replacement map
     * @param workDir        the work dir
     */
    public void fillRepMapWithSourceTargetValues(Map<UUID, UUID> replacementMap,
                                                 Path workDir) {
        fillAttributesSourceTargetMap(replacementMap, workDir);
        fillAttrKeysSourceTargetMap(replacementMap, workDir);
        fillListValuesSourceTargetMap(replacementMap, workDir);
    }

    private void fillAttributesSourceTargetMap(Map<UUID, UUID> replacementMap,
                                               Path workDir) {
        Map<UUID, Path> objectsToImport = objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttribute.class);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(objectsToImport.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                if (!replacementMap.containsKey(id)) {
                    DataSetAttribute object = objectLoaderFromDiskService.loadFileAsObject(objectsToImport.get(id),
                            DataSetAttribute.class);
                    List<Attribute> existingObject =
                            attrService.getBySourceIdAndDataSetListId(id,
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

    private void fillAttrKeysSourceTargetMap(Map<UUID, UUID> replacementMap, Path workDir) {
        Map<UUID, Path> objectsToImport =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttributeKey.class);

        for (List<UUID> chunk : Lists.partition(new ArrayList<>(objectsToImport.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                if (!replacementMap.containsKey(id)) {
                    DataSetAttributeKey object = objectLoaderFromDiskService.loadFileAsObject(objectsToImport.get(id),
                            DataSetAttributeKey.class);
                    List<AttributeKey> existingObject = attrService.getAttrKeyBySourceIdAndDataSetListId(id,
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

    private void fillListValuesSourceTargetMap(Map<UUID, UUID> replacementMap,
                                               Path workDir) {
        Map<UUID, Path> objectsToImport =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetListValue.class);

        for (List<UUID> chunk : Lists.partition(new ArrayList<>(objectsToImport.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                if (!replacementMap.containsKey(id)) {
                    DataSetListValue object = objectLoaderFromDiskService.loadFileAsObject(objectsToImport.get(id),
                            DataSetListValue.class);
                    List<ListValue> existingObject = attrService.getListValueBySourceIdAndAttrId(id,
                            replacementMap.get(object.getAttribute()));
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

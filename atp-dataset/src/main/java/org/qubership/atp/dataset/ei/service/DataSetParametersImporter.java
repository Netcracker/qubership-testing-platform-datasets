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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.dataset.ei.Constants;
import org.qubership.atp.dataset.ei.model.AttributeKeyIdsDbUpdate;
import org.qubership.atp.dataset.ei.model.DataSetAttribute;
import org.qubership.atp.dataset.ei.model.DataSetParameter;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.exceptions.ExportException;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSetParametersImporter {

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final JpaParameterService paramService;
    private final JpaAttributeService attrService;
    private final JpaDataSetService dsService;
    private final EntityManagerController entityManagerController;
    private final ClearCacheService clearCacheService;

    public static final String UUID_STRING =
            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

    /**
     * Import data set parameters.
     *
     * @param workDir the work dir
     */
    public void importDataSetParameters(Path workDir, ExportImportData importData) {
        log.info("start importDataSetParameters(workDir: {})", workDir);
        Map<UUID, Path> list = objectLoaderFromDiskService.getListOfObjects(workDir, DataSetParameter.class);
        log.debug("importDataSetParameters list: {}", list);
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(list.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                try {
                    importDataSetParameters(id, list.get(id), importData);
                } catch (RuntimeException e) {
                    log.error("Cannot import parameter {}", id, e);
                    throw e;
                }
            });
            entityManagerController.flushAndClear();
            log.info("Chunk with size {} completed. Total size {}.", chunk.size(), list.size());
        }
        log.info("end importDataSetParameters()");
    }

    private void importDataSetParameters(UUID id, Path path, ExportImportData importData) {
        log.debug("importDataSetParameters start import id: {}", id);

        DataSetParameter object;
        if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
            Map<UUID, UUID> map = importData.getReplacementMap();
            object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path, DataSetParameter.class, map);

        } else {
            object = objectLoaderFromDiskService.loadFileAsObject(path, DataSetParameter.class);
        }

        log.debug("importDataSetParameters import object: {}", object);
        if (object == null) {
            String message = String.format("Cannot load file by path %s", path.toString());
            log.error(message);
            throw new RuntimeException(message);
        }

        UUID objAttributeId = object.getAttribute();
        AttributeKeyIdsDbUpdate attributeKeyIdFounded =
                attrService.getFoundedAttributeKeyIdAndDatasetIdUpdate().get(objAttributeId);
        UUID resultImportAttributeId = Objects.isNull(attributeKeyIdFounded)
                ? objAttributeId
                : attributeKeyIdFounded.getId();

        Attribute attribute = attrService.getById(resultImportAttributeId);
        log.debug("attribute: {}", attribute);
        if (attribute == null) {
            AttributeKey attributeKey = attrService.getAttributeKeyById(resultImportAttributeId);
            log.debug("attributeKey: {}", attributeKey);
            if (attributeKey == null) {
                return;
            }
        }

        Parameter parameter = getParameter(object, attributeKeyIdFounded);
        if (parameter == null) {
            object.setSourceId(id);
            try {
                parameter = paramService.replicate(object.getId(), object.getDataSet(),
                        object.getAttribute(), object.getSourceId());
                Map<UUID, UUID> repMap = importData.getReplacementMap();

                replaceUuidsInMacrosStringInObject(object, repMap);
                updateParameter(parameter, object);
                paramService.save(parameter);
            } catch (DataSetServiceException e) {
                String message = String.format("Cannot create new parameter by import object %s", object);
                log.error(message, e);
                throw new ExportException(message, e);
            }
        } else {
            parameter.setDataSet(object.getDataSet());
            parameter.setAttribute(object.getAttribute());
            updateParameter(parameter, object);
            paramService.save(parameter);
        }
        clearCacheService.evictParameterCache(id);
    }

    private Parameter getParameter(DataSetParameter object, AttributeKeyIdsDbUpdate attributeKeyIdFounded) {
        Parameter parameter;
        if (Objects.nonNull(attributeKeyIdFounded)) {
            parameter = paramService.getParameterByAttributeIdAndDataSetId(attributeKeyIdFounded.getId(),
                    attributeKeyIdFounded.getDataSetId());
            object.setDataSet(attributeKeyIdFounded.getDataSetId());
            object.setAttribute(attributeKeyIdFounded.getId());
        } else {
            parameter = paramService.getById(object.getId());
            if (Objects.isNull(parameter)) {
                parameter = paramService.getParameterByAttributeIdAndDataSetId(object.getAttribute(),
                        object.getDataSet());
            }
        }
        return parameter;
    }

    private void replaceUuidsInMacrosStringInObject(DataSetParameter object, Map<UUID, UUID> repMap) {
        if (Objects.nonNull(repMap) && !repMap.isEmpty()) {
            String parameterValue = object.getStringValue();
            if (!Strings.isNullOrEmpty(parameterValue) && parameterValue.contains("#REF")) {
                List<UUID> uuidsFromMacros = new ArrayList<>();
                Pattern pattern = Pattern.compile(UUID_STRING);
                Matcher matcher = pattern.matcher(parameterValue);
                while (matcher.find()) {
                    uuidsFromMacros.add(UUID.fromString(matcher.group()));
                }
                for (UUID uuidMacros : uuidsFromMacros) {
                    UUID uuidMapValue = repMap.get(uuidMacros);
                    if (Objects.nonNull(uuidMapValue)) {
                        parameterValue = parameterValue.replace(uuidMacros.toString(), uuidMapValue.toString());
                    }
                }
                object.setStringValue(parameterValue);
            }
        }
    }

    private void updateParameter(Parameter parameter, DataSetParameter object) {
        parameter.setStringValue(object.getStringValue());
        parameter.setListValueId(object.getListValue());
        parameter.setFileValueId(object.getFileValueId());

        if (object.getDataSetReferenceValue() != null) {
            DataSet dataSet;
            dataSet = dsService.getById(object.getDataSetReferenceValue());
            if (dataSet != null) {
                parameter.setDataSetReferenceId(object.getDataSetReferenceValue());
            }
        }
    }

    /**
     * Gets Dataset attribute keys ids.
     *
     * @param workDir the work dir
     * @return the object ids
     */
    public List<UUID> getDsParameterIds(Path workDir) {
        return new ArrayList<>(objectLoaderFromDiskService.getListOfObjects(workDir, DataSetParameter.class).keySet());
    }

    /**
     * Validate data set parameters list.
     *
     * @param workDir the work dir
     * @return the list
     */
    public List<String> validateDataSetParameters(Path workDir, Map<UUID, UUID> repMap,
                                                  boolean isInterProjectImport) {
        log.info("start validateDataSetParameters(workDir: {})", workDir);
        Set<String> result = new HashSet<>();
        Map<UUID, Path> dataSetParameters =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetParameter.class);
        Map<UUID, Path> dataSetAttributesWithoutReplacement =
                objectLoaderFromDiskService.getListOfObjects(workDir, DataSetAttribute.class);
        Map<UUID, Path> dsIdsWithoutReplacement =
                objectLoaderFromDiskService.getListOfObjects(workDir, org.qubership.atp.dataset.ei.model.DataSet.class);
        Set<UUID> dsIdsWithReplacement = isInterProjectImport
                ? dsIdsWithoutReplacement.keySet().stream().map(repMap::get).collect(Collectors.toSet())
                : dsIdsWithoutReplacement.keySet();
        for (List<UUID> chunk : Lists.partition(new ArrayList<>(dataSetParameters.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> validateDataSetParameters(id, dataSetParameters.get(id), dsIdsWithReplacement,
                    dataSetAttributesWithoutReplacement, repMap, isInterProjectImport, result));
        }

        log.info("end validateDataSetParameters(result: {})", result);
        return new ArrayList<>(result);
    }

    private void validateDataSetParameters(UUID id, Path path,
                                           Set<UUID> dsIdsWithReplacement,
                                           Map<UUID, Path> dataSetAttributesWithoutReplacement,
                                           Map<UUID, UUID> repMap,
                                           boolean isInterProjectImport,
                                           Set<String> result) {
        log.debug("validateDataSetParameters start validate id: {}", id);

        UUID attributeIdWithoutReplacement;
        DataSetParameter object;
        if (isInterProjectImport) {
            object = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path, DataSetParameter.class,
                    repMap);
            DataSetParameter objectWithoutReplacement = objectLoaderFromDiskService.loadFileAsObject(path,
                    DataSetParameter.class);
            attributeIdWithoutReplacement = objectWithoutReplacement.getAttribute();
        } else {
            object = objectLoaderFromDiskService.loadFileAsObject(path, DataSetParameter.class);
            attributeIdWithoutReplacement = object.getAttribute();
        }

        log.debug("validateDataSetParameters validate object: {}", object);
        if (object == null) {
            log.error("Cannot load file by path {}", path.toString());
            result.add("Some file cannot be loaded from import archive.");
            return;
        }

        if (object.getDataSetReferenceValue() != null) {
            DataSet dataSet;
            dataSet = dsService.getById(object.getDataSetReferenceValue());
            if (dataSet == null && !dsIdsWithReplacement.contains(object.getDataSetReferenceValue())) {
                DataSetAttribute attribute = objectLoaderFromDiskService
                        .loadFileAsObject(dataSetAttributesWithoutReplacement.get(attributeIdWithoutReplacement),
                                DataSetAttribute.class);
                if (attribute == null) {
                    String message = String.format("Cannot load file by path %s", path.toString());
                    log.error(message);
                    result.add(message);
                    return;
                }
                log.error("Parameter {} refers to absent Data Set {}", object, object.getDataSetReferenceValue());
                result.add("Some Link to Data Set Attribute refers to absent Data Set.");
            }
        }
    }

    /**
     * Fills Replacement map with parameters source-target values.
     *
     * @param replacementMap the replacement map
     * @param workDir the work dir
     */
    public void fillRepMapWithSourceTargetValues(Map<UUID, UUID> replacementMap,
                                                 Path workDir) {
        Map<UUID, Path> objectsToImport = objectLoaderFromDiskService.getListOfObjects(workDir, DataSetParameter.class);

        for (List<UUID> chunk : Lists.partition(new ArrayList<>(objectsToImport.keySet()), Constants.CHUNK_SIZE)) {
            chunk.forEach(id -> {
                if (!replacementMap.containsKey(id)) {
                    DataSetParameter object = objectLoaderFromDiskService.loadFileAsObject(objectsToImport.get(id),
                            DataSetParameter.class);
                    List<Parameter> existingObject =
                            paramService.getBySourceIdAndDataSetId(id, replacementMap.get(object.getDataSet()));
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

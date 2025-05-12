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

package org.qubership.atp.dataset.versioning.service.impl;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.versioning.exception.RestoredReferenceInvalidException;
import org.qubership.atp.dataset.versioning.model.domain.AttributeKeySnapshot;
import org.qubership.atp.dataset.versioning.model.domain.AttributeSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.DataSetSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.ListValueSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.ParameterSnapshot;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.qubership.atp.dataset.versioning.service.RestoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class RestoreServiceImpl implements RestoreService {

    private final Provider<UserInfo> userInfoProvider;
    private final DataSetListSnapshotService snapshotService;
    private final ModelsProvider modelsProvider;
    private final ClearCacheService clearCacheService;

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(UUID id, Integer revisionId) {
        log.debug("Starting restore DSL with id = {} and revision = {}", id, revisionId);
        DataSetListSnapshot dataSetListJ = snapshotService.findDataSetListSnapshot(id, revisionId);
        validateDataSetListIsLockedAnyDataSet(dataSetListJ);
        validateDataSetList(dataSetListJ);
        restoreDataSetList(dataSetListJ);
        snapshotService.findAndCommitRestored(id, revisionId);
        log.debug("Restored DLS with id = {} and revision = {}", id, revisionId);
    }

    private void validateDataSetListIsLockedAnyDataSet(DataSetListSnapshot dataSetListSnapshot) {
        UUID dslId = dataSetListSnapshot.getId();
        List<DataSet> dataSets = modelsProvider.getDataSetByDataSetListId(dslId);
        if (!dataSets.isEmpty()) {
            for (DataSet dataSet : dataSets) {
                Preconditions.checkArgument(!dataSet.isLocked(),
                        "Can not restore DSL '%s' with id : %s , because dataSet '%s' with id : %s is locked",
                        dataSetListSnapshot.getName(), dataSetListSnapshot.getId(), dataSet.getName(), dataSet.getId());
            }
        }
    }

    private void validateDataSetList(DataSetListSnapshot dataSetListSnapshot) {
        List<String> violations = new LinkedList<>();
        for (AttributeSnapshot attribute : dataSetListSnapshot.getAttributes()) {
            if (AttributeTypeName.DSL.equals(attribute.getType())) {
                DataSetList dataSetListById = modelsProvider.getDataSetListById(attribute.getDataSetListReference());
                if (dataSetListById == null) {
                    violations.add(
                            String.format("Data Set List reference '%s' not found", attribute.getDataSetListReference())
                    );
                } else {
                    for (ParameterSnapshot parameter : attribute.getParameters()) {
                        UUID parameterId = parameter.getDataSetReference();
                        if (Objects.nonNull(parameterId)) {
                            DataSet dataSetById = modelsProvider.getDataSetById(parameterId);
                            if (dataSetById == null) {
                                violations.add(
                                        String.format(
                                                "Data Set List reference '%s' not found",
                                                attribute.getDataSetListReference()
                                        )
                                );
                            }
                        }
                    }
                }
            }
            if (AttributeTypeName.CHANGE.equals(attribute.getType())) {
                DataSetList dataSetListById = modelsProvider.getDataSetListById(attribute.getDataSetListReference());
                if (dataSetListById == null) {
                    violations.add(
                            String.format("Data Set List reference '%s' not found", attribute.getDataSetListReference())
                    );
                } else {
                    for (ParameterSnapshot parameter : attribute.getParameters()) {
                        String textValue = parameter.getText();
                        List<UUID> dataSetUuids = extractDataSetsUuids(textValue);
                        for (UUID dataSetId : dataSetUuids) {
                            DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
                            if (dataSet == null) {
                                violations.add(String.format("Data Set reference '%s' not found", dataSetId));
                            }
                        }
                    }
                }
            }
        }
        for (AttributeKeySnapshot overlap : dataSetListSnapshot.getOverlaps()) {
            for (UUID attributeId : overlap.getAttributePath().subList(1, overlap.getAttributePath().size())) {
                if (modelsProvider.getAttributeById(attributeId) == null) {
                    violations.add(String.format("Attribute '%s' not found", attributeId));
                }
            }
            UUID attributeId = overlap.getAttributeId();
            if (modelsProvider.getAttributeById(attributeId) == null) {
                violations.add(String.format("Attribute '%s' not found", attributeId));
            }
        }
        if (!violations.isEmpty()) {
            log.error("Validation failed for DataSetListJ: {}. Violations: {}", dataSetListSnapshot, violations);
            throw new RestoredReferenceInvalidException(violations);
        }
    }

    private void restoreDataSetList(DataSetListSnapshot targetSnapshot) {
        UUID dataSetListId = targetSnapshot.getId();
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        UUID userId = userInfoProvider.get().getId();
        Timestamp modified = new Timestamp(Instant.now(Clock.system(UTC)).toEpochMilli());
        DataSetListSnapshot currentSnapshot;
        if (dataSetList == null) {
            insertDataSetList(targetSnapshot, userId, modified);
            dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        } else {
            updateSimpleDataSetListFields(targetSnapshot, dataSetList, userId, modified);
        }
        currentSnapshot = new DataSetListSnapshot(dataSetList);
        updateDataSets(currentSnapshot, targetSnapshot);
        updateAttributes(currentSnapshot, targetSnapshot);
        updateOverlaps(currentSnapshot, targetSnapshot);
    }

    private void updateOverlaps(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        updateExistingOverlaps(currentSnapshot, targetSnapshot);
        createDeletedOverlaps(currentSnapshot, targetSnapshot);
        deleteCreatedOverlaps(currentSnapshot, targetSnapshot);
    }

    private void createDeletedOverlaps(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        Set<UUID> missingOverlaps = getMissing(currentSnapshot.getOverlapsIds(), targetSnapshot.getOverlapsIds());
        for (UUID missingOverlapId : missingOverlaps) {
            AttributeKeySnapshot missingOverlap = targetSnapshot.getOverlapById(missingOverlapId);
            insertOverlap(currentSnapshot.getId(), missingOverlap);
        }
    }

    private void deleteCreatedOverlaps(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        Set<UUID> overlapsToDelete = getExcessive(currentSnapshot.getOverlapsIds(), targetSnapshot.getOverlapsIds());
        for (UUID overlapToDelete : overlapsToDelete) {
            modelsProvider.getAttributeKeyById(overlapToDelete).remove();
        }
    }

    private void updateExistingOverlaps(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        Set<UUID> commonOverlaps = getIntersection(currentSnapshot.getOverlapsIds(), targetSnapshot.getOverlapsIds());
        for (UUID commonOverlapId : commonOverlaps) {
            AttributeKeySnapshot currentOverlap = currentSnapshot.getOverlapById(commonOverlapId);
            AttributeKeySnapshot targetOverlap = targetSnapshot.getOverlapById(commonOverlapId);
            ParameterSnapshot targetParameter = targetOverlap.getParameter();
            if (!currentOverlap.getParameter().equals(targetParameter)) {
                Parameter parameter = modelsProvider.getParameterById(currentOverlap.getParameter().getId());
                parameter.setId(targetParameter.getId());
                parameter.setStringValue(targetParameter.getText());
                parameter.setListValueId(targetParameter.getListValueId());
                parameter.setDataSetReferenceId(targetParameter.getDataSetReference());
                clearCacheService.evictParameterCache(parameter.getId());
            }
        }
    }

    private void updateDataSets(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        updateExistingDataSets(currentSnapshot, targetSnapshot);
        deleteCreatedDataSets(currentSnapshot, targetSnapshot);
        createDeletedDataSets(currentSnapshot, targetSnapshot);
    }

    private void updateAttributes(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        updateExistingAttributes(currentSnapshot, targetSnapshot);
        deleteCreatedAttributes(currentSnapshot, targetSnapshot);
        createDeletedAttributes(currentSnapshot, targetSnapshot);
    }

    private void createDeletedDataSets(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        Set<UUID> missingDataSets = getMissing(currentSnapshot.getDataSetsIds(), targetSnapshot.getDataSetsIds());
        for (UUID dataSetToCreate : missingDataSets) {
            DataSetSnapshot dataSetSnapshot = targetSnapshot.getDataSetById(dataSetToCreate);
            insertDataSet(targetSnapshot.getId(), dataSetSnapshot);
        }
    }

    private void deleteCreatedDataSets(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        Set<UUID> dataSetsToDelete = getExcessive(currentSnapshot.getDataSetsIds(), targetSnapshot.getDataSetsIds());
        for (UUID dataSetToDelete : dataSetsToDelete) {
            modelsProvider.getDataSetById(dataSetToDelete).remove();
        }
    }

    private void updateExistingDataSets(DataSetListSnapshot currentSnapshot, DataSetListSnapshot targetSnapshot) {
        Set<UUID> commonDataSets = getIntersection(currentSnapshot.getDataSetsIds(), targetSnapshot.getDataSetsIds());
        currentSnapshot.getDataSetsIds().forEach(clearCacheService::evictDatasetListContextCache);
        for (UUID commonDataSetId : commonDataSets) {
            DataSetSnapshot currentDataSet = currentSnapshot.getDataSetById(commonDataSetId);
            DataSetSnapshot targetDataSet = targetSnapshot.getDataSetById(commonDataSetId);
            if (!currentDataSet.equals(targetDataSet)) {
                DataSet dataSet = modelsProvider.getDataSetById(commonDataSetId);
                dataSet.setName(targetDataSet.getName());
                dataSet.setOrdering(targetDataSet.getOrdering());
                dataSet.setLocked(targetDataSet.getLocked());
            }
        }
    }

    private Set<UUID> getIntersection(Set<UUID> current, Set<UUID> target) {
        Set<UUID> result = new HashSet<>(target);
        result.retainAll(current);
        return result;
    }

    private Set<UUID> getExcessive(Set<UUID> current, Set<UUID> target) {
        Set<UUID> result = new HashSet<>(current);
        result.removeAll(target);
        return result;
    }

    private Set<UUID> getMissing(Set<UUID> current, Set<UUID> target) {
        Set<UUID> result = new HashSet<>(target);
        result.removeAll(current);
        return result;
    }

    private void updateExistingAttributes(DataSetListSnapshot currentVersion, DataSetListSnapshot targetVersion) {
        Set<UUID> commonAttributes = getIntersection(currentVersion.getAttributeIds(), targetVersion.getAttributeIds());
        for (UUID commonAttributeId : commonAttributes) {
            AttributeSnapshot currentAttribute = currentVersion.getAttributeById(commonAttributeId);
            AttributeSnapshot targetAttribute = targetVersion.getAttributeById(commonAttributeId);
            Attribute attribute = modelsProvider.getAttributeById(currentAttribute.getId());
            attribute.setName(targetAttribute.getName());
            attribute.setTypeDataSetListId(targetAttribute.getDataSetListReference());
            updateListValues(currentAttribute, targetAttribute);
            updateParameters(currentAttribute, targetAttribute);
        }
    }

    private void updateParameters(AttributeSnapshot currentAttribute, AttributeSnapshot targetAttribute) {
        currentAttribute.getParametersIds().forEach(clearCacheService::evictParameterCache);
        updateExistingParameters(currentAttribute, targetAttribute);
        deleteCreatedParameters(currentAttribute, targetAttribute);
        createDeletedParameters(currentAttribute, targetAttribute);
    }

    private void updateExistingParameters(AttributeSnapshot currentAttribute, AttributeSnapshot targetAttribute) {
        Set<UUID> commonParameters = getIntersection(
                currentAttribute.getParametersIds(),
                targetAttribute.getParametersIds()
        );
        for (UUID commonParameter : commonParameters) {
            ParameterSnapshot currentParameter = currentAttribute.getParameterById(commonParameter);
            ParameterSnapshot targetParameter = targetAttribute.getParameterById(commonParameter);
            if (!currentParameter.equals(targetParameter)) {
                Parameter parameter = modelsProvider.getParameterById(commonParameter);
                setValueByType(parameter, targetParameter, targetAttribute.getType());
            }
        }
    }

    private void setValueByType(Parameter parameter, ParameterSnapshot targetParameter, AttributeTypeName typeName) {
        switch (typeName) {
            case CHANGE:
            case ENCRYPTED:
            case TEXT:
                parameter.setStringValue(targetParameter.getText());
                break;
            case LIST:
                parameter.setListValueId(targetParameter.getListValueId());
                break;
            case DSL:
                parameter.setDataSetReferenceId(targetParameter.getDataSetReference());
                break;
            case FILE:
            default:
                break;
        }
    }

    private void createDeletedParameters(AttributeSnapshot currentAttribute, AttributeSnapshot targetAttribute) {
        Set<UUID> missingParameters = getMissing(
                currentAttribute.getParametersIds(), targetAttribute.getParametersIds()
        );
        for (UUID targetParameter : missingParameters) {
            ParameterSnapshot parameter = targetAttribute.getParameterById(targetParameter);
            insertParameter(parameter, targetAttribute.getId());
        }
    }

    private void deleteCreatedParameters(AttributeSnapshot currentAttribute, AttributeSnapshot targetAttribute) {
        Set<UUID> parametersToDelete = getExcessive(
                currentAttribute.getParametersIds(), targetAttribute.getParametersIds()
        );
        for (UUID parameterToDelete : parametersToDelete) {
            modelsProvider.getParameterById(parameterToDelete).remove();
        }
    }

    private void updateListValues(
            AttributeSnapshot currentAttribute,
            AttributeSnapshot targetAttribute
    ) {
        updateExistingListValues(currentAttribute, targetAttribute);
        deleteCreatedListValues(currentAttribute, targetAttribute);
        createDeletedListValues(currentAttribute, targetAttribute);
    }

    private void createDeletedListValues(AttributeSnapshot currentAttribute, AttributeSnapshot targetAttribute) {
        Set<UUID> missingListValues = getMissing(
                currentAttribute.getListValuesIds(),
                targetAttribute.getListValuesIds()
        );
        Attribute attribute = modelsProvider.getAttributeById(targetAttribute.getId());
        for (UUID missingListValueId : missingListValues) {
            ListValueSnapshot missingListValue = targetAttribute.getListValueById(missingListValueId);
            attribute.insertListValue(missingListValue.getId(), missingListValue.getName());
        }
    }

    private void deleteCreatedListValues(AttributeSnapshot currentAttribute, AttributeSnapshot targetAttribute) {
        Set<UUID> listValuesToDelete = getExcessive(
                currentAttribute.getListValuesIds(),
                targetAttribute.getListValuesIds()
        );
        for (UUID listValueToDelete : listValuesToDelete) {
            modelsProvider.getListValueById(listValueToDelete).remove();
        }
    }

    private void updateExistingListValues(AttributeSnapshot currentAttribute, AttributeSnapshot targetAttribute) {
        Set<UUID> commonListValues = getIntersection(
                currentAttribute.getListValuesIds(),
                targetAttribute.getListValuesIds()
        );
        for (UUID commonListValueId : commonListValues) {
            ListValueSnapshot currentListValue = currentAttribute.getListValueById(commonListValueId);
            ListValueSnapshot targetListValue = targetAttribute.getListValueById(commonListValueId);
            if (!currentListValue.equals(targetListValue)) {
                ListValue listValue = modelsProvider.getListValueById(commonListValueId);
                listValue.setText(targetListValue.getName());
            }
        }
    }

    private void createDeletedAttributes(DataSetListSnapshot currentVersion, DataSetListSnapshot targetVersion) {
        Set<UUID> missingAttributes = getMissing(currentVersion.getAttributeIds(), targetVersion.getAttributeIds());
        for (UUID missingAttributeId : missingAttributes) {
            AttributeSnapshot attribute = targetVersion.getAttributeById(missingAttributeId);
            insertAttribute(attribute, currentVersion.getId());
            for (ParameterSnapshot parameter : attribute.getParameters()) {
                insertParameter(parameter, attribute.getId());
            }
        }
    }

    private void deleteCreatedAttributes(DataSetListSnapshot currentVersion, DataSetListSnapshot targetVersion) {
        Set<UUID> attributesToDelete = getExcessive(
                currentVersion.getAttributeIds(),
                targetVersion.getAttributeIds()
        );
        for (UUID currentAttribute : attributesToDelete) {
            modelsProvider.getAttributeById(currentAttribute).remove();
        }
    }

    private void insertDataSetList(DataSetListSnapshot snapshot, UUID userId, Timestamp modified) {
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(snapshot.getVisibilityAreaId());
        DataSetList dataSetList = visibilityArea.insertDataSetList(snapshot.getId(), snapshot.getName());
        dataSetList.setModifiedBy(userId);
        dataSetList.setModifiedWhen(new Timestamp(modified.getTime()));
    }

    private void updateSimpleDataSetListFields(
            DataSetListSnapshot dataSetListJ,
            DataSetList dataSetList,
            UUID userId,
            Timestamp modified
    ) {
        dataSetList.setModifiedBy(userId);
        dataSetList.setModifiedWhen(new Timestamp(modified.getTime()));
        dataSetList.setName(dataSetListJ.getName());
        dataSetList.save();
    }

    private void insertAttribute(AttributeSnapshot attributeSnapshot, UUID dataSetListId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        Attribute attribute = dataSetList.insertAttribute(
                attributeSnapshot.getId(),
                attributeSnapshot.getName(),
                attributeSnapshot.getType(),
                attributeSnapshot.getOrdering()
        );
        attribute.setTypeDataSetListId(attributeSnapshot.getDataSetListReference());
        for (ListValueSnapshot listValue : attributeSnapshot.getListValues()) {
            attribute.insertListValue(listValue.getId(), listValue.getName());
        }
    }

    private void insertParameter(ParameterSnapshot parameterSnapshot, UUID attributeId) {
        DataSet dataSet = modelsProvider.getDataSetById(parameterSnapshot.getDataSetId());
        Parameter parameter = dataSet.insertParameter(parameterSnapshot.getId(), attributeId);
        parameter.setStringValue(parameterSnapshot.getText());
        if (Objects.nonNull(parameterSnapshot.getDataSetReference())) {
            parameter.setStringValue(null);
            parameter.setDataSetReferenceId(parameterSnapshot.getDataSetReference());
        }
        parameter.setListValueId(parameterSnapshot.getListValueId());
    }

    private void insertDataSet(UUID dataSetListId, DataSetSnapshot dataSetSnapshot) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        DataSet dataSet = dataSetList.insertDataSet(dataSetSnapshot.getId(), dataSetSnapshot.getName());
        dataSet.setOrdering(dataSetSnapshot.getOrdering());
    }

    private void insertOverlap(UUID dataSetListId, AttributeKeySnapshot missingOverlap) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        ParameterSnapshot targetParameter = missingOverlap.getParameter();
        Parameter parameter = dataSetList.insertOverlap(
                missingOverlap.getDataSetId(),
                missingOverlap.getId(),
                missingOverlap.getAttributeId(),
                missingOverlap.getAttributePath(),
                targetParameter.getId()
        );
        parameter.setStringValue(targetParameter.getText());
        parameter.setListValueId(targetParameter.getListValueId());
        parameter.setDataSetReferenceId(targetParameter.getDataSetReference());
    }

    private List<UUID> extractDataSetsUuids(String textValue) {
        if (Strings.isEmpty(textValue)) {
            return Collections.emptyList();
        }
        try {
            String[] split = textValue.replaceFirst("MULTIPLY ", "").split(" ");
            List<UUID> result = new LinkedList<>();
            for (String uuidString : split) {
                result.add(UUID.fromString(uuidString));
            }
            return result;
        } catch (Exception e) {
            log.info("Error parsing multiple parameter '{}'", textValue, e);
            return Collections.emptyList();
        }
    }
}

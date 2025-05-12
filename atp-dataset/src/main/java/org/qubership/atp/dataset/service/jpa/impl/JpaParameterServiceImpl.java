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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.direct.DateAuditorService;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JpaParameterServiceImpl implements JpaParameterService {

    @Autowired
    protected ModelsProvider modelsProvider;
    @Autowired
    protected DataSetListSnapshotService dataSetListSnapshotService;
    @Autowired
    protected DateAuditorService dateAuditorService;
    @Autowired
    protected ClearCacheService clearCacheService;

    @Override
    @Transactional
    public Parameter createParameter(UUID dataSetId, UUID attributeId) {
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
        return dataSet.createParameter(attributeId);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheEnum.Constants.DATASET_LIST_CONTEXT_CACHE, key = "#dataSetId")
    public Parameter createParameter(
            UUID dataSetId,
            UUID attributeId,
            String value,
            UUID dataSetReference,
            UUID listValueReference
    ) {
        Parameter parameter = createParameterWithoutCommitting(dataSetId, attributeId, value,
                dataSetReference, listValueReference);
        dataSetListSnapshotService.commitEntity(parameter.getDataSet().getDataSetList().getId());
        return parameter;
    }

    /**
     * Creates parameter without committing.
     */
    @Transactional
    public Parameter createParameterWithoutCommitting(UUID dataSetId, UUID attributeId, String value,
                                                      UUID dataSetReference, UUID listValueReference) {
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
        Preconditions.checkArgument(!modelsProvider.isDsLocked(dataSetId), "Can not create"
                        + " parameter with attribute id %s and dataSet name: '%s', id: %s because dataset locked",
                attribute.getId(), dataSet.getName(), dataSet.getId());
        Parameter parameter = dataSet.createParameter(attributeId);
        AttributeTypeName attributeType = attribute.getAttributeType();
        setParameterValueByType(parameter, attributeType, value, dataSetReference, listValueReference);
        return parameter;
    }

    /**
     * Creates overlap parameter without committing.
     */
    @Transactional
    public Parameter createParameterOverlapWithoutCommitting(UUID attributeId, List<UUID> attributePath,
                                                             UUID dataSetId, String value, UUID dataSetReference,
                                                             UUID listValueReference) {
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);

        Preconditions.checkArgument(!modelsProvider.isDsLocked(dataSetId), "Can not create"
                        + " parameter with attribute in dataSet name: '%s', id: %s because dataset locked",
                dataSet.getName(), dataSet.getId());

        Parameter parameter = dataSet.createOverlap(attributeId, attributePath);
        AttributeTypeName attributeType = parameter.getAttributeKey().getAttributeType();
        setParameterValueByType(parameter, attributeType, value, dataSetReference, listValueReference);
        return parameter;
    }

    private boolean setParameterValueByType(
            Parameter parameter,
            AttributeTypeName attributeType,
            String stringValue,
            UUID dataSetReference,
            UUID listValueReference
    ) {
        switch (attributeType) {
            case CHANGE:
            case ENCRYPTED:
            case TEXT:
                parameter.setStringValue(stringValue);
                return true;
            case DSL:
                parameter.setDataSetReferenceId(dataSetReference);
                return true;
            case LIST:
                parameter.setListValueId(listValueReference);
                return true;
            default:
                return false;
        }
    }

    @Override
    @Transactional
    public Parameter updateParameter(UUID parameterId, String value, UUID dataSetReference, UUID listValueReference) {
        Parameter parameter = updateParameterWithoutCommitting(parameterId, value, dataSetReference,
                listValueReference);
        dataSetListSnapshotService.commitEntity(parameter.getDataSet().getDataSetList().getId());
        return parameter;
    }

    /**
     * Updates parameter without committing.
     */
    @Transactional
    public Parameter updateParameterWithoutCommitting(UUID parameterId, String value,
                                                      UUID dataSetReference, UUID listValueReference) {
        Parameter parameter = modelsProvider.getParameterById(parameterId);
        Attribute attribute = parameter.getAttribute();
        setParameterValueByType(parameter, attribute.getAttributeType(), value, dataSetReference, listValueReference);
        return parameter;
    }

    /**
     * Updates overlap parameter without committing.
     */
    @Transactional
    public void updateOverlapParameterWithoutCommitting(Parameter parameter, String value,
                                                             UUID dataSetReference, UUID listValueReference) {
        AttributeTypeName attributeType = parameter.getAttributeKey().getAttributeType();
        setParameterValueByType(parameter, attributeType, value, dataSetReference, listValueReference);
    }

    @Override
    public List<Parameter> getBySourceIdAndDataSetId(UUID sourceId, UUID dataSetId) {
        return modelsProvider.getParameterBySourceIdAndDataSetId(sourceId, dataSetId);
    }

    @Override
    @Transactional
    public boolean bulkUpdateValues(
            String stringValue,
            UUID dataSetReference,
            UUID listValueReference,
            UUID dataSetListId,
            UUID dataSetId,
            List<UUID> listIdsParametersToChange) {

        List<UUID> parameterIds = dataSetId == null
                ? listIdsParametersToChange
                : modelsProvider.getParametersIdByDsId(dataSetId);
        parameterIds.forEach(paramIds -> {
            DataSet dataSet = modelsProvider.getParameterById(paramIds).getDataSet();
            Preconditions.checkArgument(!dataSet.isLocked(), "Can not change list"
                            + " value(s) in parameter id: '%s', dataSet name: '%s', id: %s because dataset is locked",
                    paramIds, dataSet.getName(), dataSet.getId());
        });

        boolean isUpdated = false;
        for (UUID parameterId : parameterIds) {
            Parameter parameter = modelsProvider.getParameterById(parameterId);
            Attribute attribute = parameter.getAttribute();
            if (attribute != null) {
                AttributeTypeName attributeType = attribute.getAttributeType();
                isUpdated = isUpdated | setParameterValueByType(
                        parameter,
                        attributeType,
                        stringValue,
                        dataSetReference,
                        listValueReference
                );
                if (isUpdated) {
                    clearCacheService.evictParameterCache(parameterId);
                    clearCacheService.evictDatasetListContextCache(parameter.getDataSetId());
                }
            } else {
                log.info("Orphan parameter '{}' detected!", parameterId);
            }
        }
        if (isUpdated && dataSetListId != null) {
            dateAuditorService.updateModifiedFields(dataSetListId);
            dataSetListSnapshotService.commitEntity(dataSetListId);
        }
        return isUpdated;
    }

    @Override
    @Transactional
    public Parameter replicate(UUID id, UUID dataSetId, UUID attributeId, UUID sourceId)
            throws DataSetServiceException {
        return modelsProvider.replicateParameter(id, dataSetId, attributeId, sourceId);
    }

    @Override
    @Transactional
    public void remove(UUID id) {
        Parameter parameter = modelsProvider.getParameterById(id);
        if (parameter != null) {
            parameter.remove();
            dataSetListSnapshotService.commitEntity(parameter.getAttribute().getDataSetList().getId());
        }
    }

    /**
     * Remove parameter without committing.
     */
    @Transactional
    public void removeWithoutCommitting(UUID id) {
        Parameter parameter = modelsProvider.getParameterById(id);
        if (parameter != null) {
            parameter.remove();
        }
    }

    @Override
    @Transactional
    public void save(Parameter parameter) {
        parameter.save();
    }

    @Override
    @Transactional
    public Parameter getById(UUID id) {
        return modelsProvider.getParameterById(id);
    }

    @Override
    public Parameter getParameterByAttributeIdAndDataSetId(UUID attributeId, UUID dataSetId) {
        return modelsProvider.getParameterByAttributeIdAndDataSetId(attributeId, dataSetId);
    }
}

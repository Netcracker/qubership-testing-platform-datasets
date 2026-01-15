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

package org.qubership.atp.dataset.service.jpa.delegates;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.LabelEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.exception.file.FileDsCopyException;
import org.qubership.atp.dataset.exception.file.FileDsNotFoundToCopyException;
import org.qubership.atp.dataset.model.impl.DataSetShort;
import org.qubership.atp.dataset.service.jpa.model.copy.AttributeCopyData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataSet extends AbstractObjectWrapper<DataSetEntity> {

    public DataSet(DataSetEntity entity) {
        super(entity);
    }

    public UUID getId() {
        return entity.getId();
    }

    public String getName() {
        return entity.getName();
    }

    public Long ordering() {
        return entity.getOrdering();
    }

    public boolean isLocked() {
        return entity.isLocked();
    }

    public void setLocked(boolean locked) {
        entity.setLocked(locked);
    }

    public DataSetList getDataSetList() {
        return modelsProvider.getDataSetList(entity.getDataSetList());
    }

    /**
     * All parameters.
     */
    public List<Parameter> getParameters() {
        List<Parameter> result = new LinkedList<>();
        List<ParameterEntity> parameters = entity.getParameters();
        for (ParameterEntity parameter : parameters) {
            result.add(modelsProvider.getParameter(parameter));
        }
        return result;
    }

    /**
     * Parameter by ID.
     */
    public Parameter getParameterByAttributeId(UUID attributeId) {
        List<ParameterEntity> parameters = entity.getParameters();
        for (ParameterEntity parameter : parameters) {
            if (parameter.getAttribute().getId().equals(attributeId)) {
                return modelsProvider.getParameter(parameter);
            }
        }
        return null;
    }

    /**
     * Associated labels list.
     */
    public List<Label> getLabels() {
        List<Label> result = new LinkedList<>();
        for (LabelEntity labelEntity : entity.getLabels()) {
            result.add(modelsProvider.getLabel(labelEntity));
        }
        return result;
    }

    @Override
    public void beforeRemove() {
        List<Parameter> parameters = getParameters();
        for (Parameter parameter : parameters) {
            parameter.remove();
        }
        List<AttributeKey> overlaps = getOverlaps();
        for (AttributeKey overlap : overlaps) {
            overlap.remove();
        }
        List<DataSetEntity> dataSets = getDataSetList().getEntity().getDataSets();
        DataSetEntity dataSetToRemove = null;
        for (DataSetEntity dataSet : dataSets) {
            if (dataSet.getId().equals(getId())) {
                dataSetToRemove = dataSet;
                break;
            }
        }
        if (dataSetToRemove != null) {
            dataSets.remove(dataSetToRemove);
        }
    }

    /**
     * All overlaps under this dataset.
     */
    public List<AttributeKey> getOverlaps() {
        List<AttributeKey> result = new LinkedList<>();
        String nativeQuery = "select * from attribute_key where dataSet_id = :ds_id";
        entityManager
                .createNativeQuery(nativeQuery, AttributeKeyEntity.class)
                .setParameter("ds_id", getId())
                .getResultList().forEach(o -> result.add(new AttributeKey((AttributeKeyEntity) o)));
        return result;
    }

    /**
     * Create new parameter, returns new delegate.
     */
    public Parameter insertParameter(UUID parameterId, UUID attributeId) {
        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setDataSet(entity);
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        parameterEntity.setAttribute(attribute.getEntity());
        parameterEntity.setDataSet(entity);
        attribute.getEntity().getParameters().add(parameterEntity);
        insert(parameterEntity, parameterId);
        return modelsProvider.getParameter(parameterEntity);
    }

    /**
     * Create new parameter, returns new delegate.
     */
    public Parameter createParameter(UUID attributeId) {
        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setDataSet(entity);
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        parameterEntity.setAttribute(attribute.getEntity());
        parameterEntity.setDataSet(entity);
        attribute.getEntity().getParameters().add(parameterEntity);
        save(parameterEntity);
        return modelsProvider.getParameter(parameterEntity);
    }

    /**
     * Create new parameter overlap for path. Returns new delegate.
     */
    public Parameter createOverlap(UUID attributeId, List<UUID> attributePath) {
        AttributeKeyEntity attributeKeyEntity = new AttributeKeyEntity();
        attributeKeyEntity.setDataSetList(getDataSetList().getEntity());
        attributeKeyEntity.setDataSet(entity);

        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        attributeKeyEntity.setAttribute(attribute.getEntity());
        attributeKeyEntity.setKey(StringUtils.join(attributePath, "_"));
        save(attributeKeyEntity);

        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setAttribute(attributeKeyEntity);
        parameterEntity.setDataSet(entity);
        save(parameterEntity);

        return modelsProvider.getParameter(parameterEntity);
    }

    public void setName(String name) {
        entity.setName(name);
    }

    public void setOrdering(Long ordering) {
        entity.setOrdering(ordering);
    }

    /**
     * Sets data set list.
     *
     * @param dataSetListId the data set list id
     */
    public void setDataSetList(UUID dataSetListId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        if (dataSetList == null) {
            throw new Error("Can't find Data Set List by id " + dataSetListId);
        }
        entity.setDataSetList(dataSetList.getEntity());
    }

    /**
     * Copy parameters to another dataset. Needed attributes map.
     */
    public void copyParametersTo(DataSet anotherDataSet, Map<UUID, AttributeCopyData> attributesMap) {
        for (Parameter parameter : getParameters()) {
            Attribute oldAttribute = parameter.getAttribute();
            if (oldAttribute == null) {
                //It's attribute key
                continue;
            }
            if (attributesMap.containsKey(oldAttribute.getId())) {
                AttributeCopyData newAttribute = attributesMap.get(oldAttribute.getId());
                Parameter copyParameter = anotherDataSet.createParameter(newAttribute.getCopyId());
                switch (oldAttribute.getAttributeType()) {
                    case CHANGE:
                    case ENCRYPTED:
                    case TEXT:
                        copyParameter.setStringValue(parameter.getStringValue());
                        break;
                    case DSL:
                        copyParameter.setDataSetReferenceId(parameter.getDataSetReferenceId());
                        break;
                    case FILE:
                        try {
                            gridFsService.copy(parameter.getId(), copyParameter.getId(), false);
                        } catch (IllegalStateException | FileDsNotFoundToCopyException | FileDsCopyException e) {
                            log.error("File " + parameter.getId() + " not found. Continue copying.", e);
                        }
                        break;
                    case LIST:
                        if (parameter.getListValue() != null) {
                            UUID newListValueId = newAttribute.getListValuesMap().get(parameter.getListValue().getId());
                            copyParameter.setListValueId(newListValueId);
                        }
                        break;
                    default:
                }
            } else {
                log.error("Copy of old attribute with id = [{}] is not exist, old dslId: {}", oldAttribute.getId(),
                        oldAttribute.getDataSetList().getId());
            }
        }
    }

    public DataSetShort getShortInfo() {
        return new DataSetShort(this.getDataSetList().getId(), this.getId(), this.getName());
    }
}

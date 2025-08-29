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
import java.util.UUID;

import javax.persistence.NoResultException;

import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttributeKey extends AbstractObjectWrapper<AttributeKeyEntity> {
    private static final long serialVersionUID = 3769738483534840381L;
    private Parameter cachedParameter = null;

    public AttributeKey(AttributeKeyEntity entity) {
        super(entity);
    }

    public UUID getId() {
        return entity.getId();
    }

    public DataSetList getDataSetList() {
        return modelsProvider.getDataSetList(entity.getDataSetList());
    }

    public String getKey() {
        return entity.getKey();
    }

    public DataSet getDataSet() {
        return modelsProvider.getDataSet(entity.getDataSet());
    }

    public UUID getDataSetId() {
        return entity.getDataSet().getId();
    }

    public Attribute getAttribute() {
        return modelsProvider.getAttribute(entity.getAttribute());
    }

    public UUID getAttributeId() {
        return entity.getAttribute().getId();
    }

    public String getName() {
        return entity.getName();
    }

    public AttributeTypeName getAttributeType() {
        return getAttribute().getAttributeType();
    }

    /**
     * Get referenced parameter.
     */
    public Parameter getParameter() {
        if (cachedParameter == null) {
            try {
                ParameterEntity singleResult = (ParameterEntity) entityManager
                        .createNativeQuery("SELECT * from parameter p where p.attribute_id = ?1", ParameterEntity.class)
                        .setParameter(1, getId())
                        .getSingleResult();
                cachedParameter = modelsProvider.getParameter(singleResult);
            } catch (NoResultException e) {
                return null;
            }
        }
        return cachedParameter;
    }

    /**
     * Get path as list of UUIDs.
     */
    public List<UUID> getPath() {
        List<UUID> result = new LinkedList<>();
        String[] splitResult = getKey().split("_");
        for (String splitPart : splitResult) {
            result.add(UUID.fromString(splitPart));
        }
        return result;
    }

    /**
     * Get string with path attributes names.
     * */
    public String getPathNames() {
        List<String> names = new LinkedList<>();
        for (UUID attributeId : getPath()) {
            names.add(modelsProvider.getAttributeById(attributeId).getName());
        }
        return String.join(".", names);
    }

    public void setKey(String key) {
        entity.setKey(key);
    }

    /**
     * Sets attribute.
     *
     * @param attributeId the attribute id
     */
    public void setAttribute(UUID attributeId) {
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        if (attribute == null) {
            throw new Error("Can't find copy of Attribute by id: " + attributeId + ", old AttrId: " + getAttributeId());
        }
        entity.setAttribute(attribute.getEntity());
    }

    /**
     * Sets data set.
     *
     * @param dataSetId the data set id
     */
    public void setDataSet(UUID dataSetId) {
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
        if (dataSet == null) {
            throw new Error("Can't find Data Set by id " + dataSetId);
        }
        entity.setDataSet(dataSet.getEntity());
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

    @Override
    public void beforeRemove() {
        getParameter().remove();
    }
}

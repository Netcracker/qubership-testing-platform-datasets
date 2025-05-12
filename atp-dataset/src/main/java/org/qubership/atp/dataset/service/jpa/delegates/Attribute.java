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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Attribute extends AbstractObjectWrapper<AttributeEntity> {
    private static final long serialVersionUID = -55774378413986230L;
    private static final Logger LOG = LoggerFactory.getLogger(Attribute.class);

    public Attribute(AttributeEntity entity) {
        super(entity);
    }

    public UUID getId() {
        return entity.getId();
    }

    public DataSetList getDataSetList() {
        return modelsProvider.getDataSetList(entity.getDataSetList());
    }

    public String getName() {
        return entity.getName();
    }

    public Integer getOrdering() {
        return entity.getOrdering();
    }

    public AttributeTypeName getAttributeType() {
        return AttributeTypeName.getTypeById(entity.getAttributeTypeId());
    }

    /**
     * For DSL attributes. Returns referenced DSL.
     */
    public DataSetList getTypeDataSetList() {
        DataSetListEntity dataSetListEntity = entityManager.find(
                DataSetListEntity.class, entity.getTypeDataSetListId()
        );
        return modelsProvider.getDataSetList(dataSetListEntity);
    }

    /**
     * For DSL attributes. Returns referenced DSL.
     */
    public UUID getTypeDataSetListId() {
        return entity.getTypeDataSetListId();
    }

    /**
     * For DSL attributes. Sets referenced DSL.
     */
    public void setTypeDataSetListId(UUID dataSetListId) {
        entity.setTypeDataSetListId(dataSetListId);
        save(entity);
    }

    /**
     * Gets list values.
     *
     * @return the list values
     */
    public List<ListValue> getListValues() {
        List<ListValue> result = new LinkedList<>();
        entity.getListValues().forEach(listValue -> result.add(modelsProvider.getListValue(listValue)));
        return result;
    }


    /**
     * Get parameters.
     */
    public List<Parameter> getParameters() {
        List<Parameter> parameters = new LinkedList<>();
        entity.getParameters().forEach(
                parameterEntity -> parameters.add(
                        modelsProvider.getParameter(parameterEntity)
                )
        );
        return parameters;
    }

    @Override
    public void beforeRemove() {
        List<ListValue> listValues = getListValues();
        for (ListValue listValue : listValues) {
            listValue.remove();
        }
        List<Parameter> parameters = getParameters();
        for (Parameter parameter : parameters) {
            parameter.remove();
        }
        List<AttributeKey> overlaps = getOverlaps();
        for (AttributeKey overlap : overlaps) {
            overlap.remove();
        }
    }

    /**
     * Returns all attribute keys, overlapping current attribute.
     */
    public List<AttributeKey> getOverlaps() {
        List<AttributeKey> result = new LinkedList<>();
        String nativeQuery = "select * from attribute_key where attribute_id = :attr_id";
        List<AttributeKeyEntity> keys = entityManager
                .createNativeQuery(nativeQuery, AttributeKeyEntity.class)
                .setParameter("attr_id", getId())
                .getResultList();
        for (AttributeKeyEntity key : keys) {
            result.add(new AttributeKey(key));
        }
        return result;
    }


    /**
     * For list attributes. Create new List Value.
     */
    public ListValue createListValue(String text) {
        ListValueEntity listValueEntity = new ListValueEntity();
        listValueEntity.setAttribute(entity);
        listValueEntity.setText(text);
        save(listValueEntity);
        entity.getListValues().add(listValueEntity);
        return modelsProvider.getListValue(listValueEntity);
    }

    /**
     * For list attributes. Create new List Value with name and id.
     */
    public ListValue insertListValue(UUID id, String text) {
        ListValueEntity listValueEntity = new ListValueEntity();
        listValueEntity.setAttribute(entity);
        listValueEntity.setText(text);
        insert(listValueEntity, id);
        entity.getListValues().add(listValueEntity);
        return modelsProvider.getListValue(listValueEntity);
    }

    public void setName(String name) {
        entity.setName(name);
    }

    public void setAttributeType(AttributeTypeName type) {
        entity.setAttributeTypeId(type.getId());
    }

    public void setDataSetList(UUID dataSetListId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        entity.setDataSetList(dataSetList.getEntity());
    }

    /**
     * Copy attribute list values to another attribute.
     * */
    public Map<UUID,UUID> copyListValuesTo(Attribute attributeCopy) {
        Map<UUID,UUID> result = new LinkedHashMap<>();
        List<ListValue> listValues = getListValues();
        for (ListValue listValue : listValues) {
            ListValueEntity copiedListValue = new ListValueEntity();
            copiedListValue.setText(listValue.getText());
            copiedListValue.setAttribute(attributeCopy.getEntity());
            attributeCopy.getEntity().getListValues().add(copiedListValue);
            save(copiedListValue);
            result.put(listValue.getId(), copiedListValue.getId());
        }
        return result;
    }

    public void setOrdering(Integer ordering) {
        entity.setOrdering(ordering);
    }
}

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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.db.jpa.repositories.JpaListValueRepository;
import org.qubership.atp.dataset.ei.model.AttributeKeyIdsDbUpdate;
import org.qubership.atp.dataset.exception.attribute.AttributeExistsException;
import org.qubership.atp.dataset.exception.attribute.AttributeNotFoundException;
import org.qubership.atp.dataset.exception.attribute.AttributePositionException;
import org.qubership.atp.dataset.service.direct.DateAuditorService;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.delegates.AbstractObjectWrapper;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JpaAttributeServiceImpl implements JpaAttributeService {

    @Autowired
    protected ModelsProvider modelsProvider;
    @Autowired
    protected JpaListValueRepository listValueRepository;
    @Autowired
    protected DataSetListCheckService checkService;
    @Autowired
    protected DataSetListSnapshotService dataSetListSnapshotService;
    @Autowired
    protected DateAuditorService dateAuditorService;

    private final Map<UUID, AttributeKeyIdsDbUpdate> foundedAttributeKeyIdAndDatasetIdUpdate = new HashMap<>();

    @Override
    @Transactional
    public Attribute create(String name, AttributeTypeName type, UUID dataSetListId) {
        Attribute attribute = createWithoutCommitting(name, type, dataSetListId);
        dataSetListSnapshotService.commitEntity(attribute.getDataSetList().getId());
        return attribute;
    }

    @Override
    @Transactional(rollbackFor = {AttributeExistsException.class})
    @CacheEvict(value = CacheEnum.Constants.DATASET_LIST_CONTEXT_CACHE, key = "#dataSetListId")
    public Attribute create(String name,
                            AttributeTypeName type,
                            UUID dataSetListId,
                            UUID typeDataSetListId,
                            List<String> listValues) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        for (Attribute attribute : dataSetList.getAttributes()) {
            if (attribute.getName().equals(name)) {
                log.error("Attribute with name '" + name + "' already exists");
                throw new AttributeExistsException(name);
            }
        }
        Attribute attribute = dataSetList.createAttribute(name, type);
        if (type == AttributeTypeName.CHANGE) {
            attribute.setTypeDataSetListId(typeDataSetListId);
        }
        if (type == AttributeTypeName.DSL) {
            attribute.setTypeDataSetListId(typeDataSetListId);
            checkService.checkOnCyclesThrowException(dataSetList);
        }
        if (type == AttributeTypeName.LIST) {
            for (String listValue : listValues) {
                attribute.createListValue(listValue);
            }
        }
        dateAuditorService.updateModifiedFields(dataSetListId);
        dataSetListSnapshotService.commitEntity(dataSetListId);
        return attribute;
    }

    /**
     * Creates attribute without committing.
     */
    @Transactional
    public Attribute createWithoutCommitting(String name, AttributeTypeName type, UUID dataSetListId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        Attribute attribute = dataSetList.createAttribute(name, type);
        return attribute;
    }

    @Transactional
    public ListValue createListValueWithoutCommitting(String text, UUID attributeId) {
        return modelsProvider.getAttributeById(attributeId).createListValue(text);
    }

    @Override
    @Transactional
    public Attribute replicate(UUID id, String name, AttributeTypeName type, UUID dataSetListId, UUID sourceId)
            throws DataSetServiceException {
        return modelsProvider.replicateAttribute(id, name, type, dataSetListId, sourceId);
    }

    @Override
    @Transactional
    public void remove(UUID id) {
        Attribute attribute = modelsProvider.getAttributeById(id);
        if (attribute != null) {
            attribute.remove();
        }
    }

    @Override
    @Transactional
    public void save(AbstractObjectWrapper attribute) {
        attribute.save();
    }

    @Override
    @Transactional
    public void removeAttributeKey(UUID id) {
        AttributeKey attributeKey = modelsProvider.getAttributeKeyById(id);
        if (attributeKey != null) {
            attributeKey.remove();
        }
    }

    @Override
    @Transactional
    public AttributeKey replicateAttributeKey(UUID id, String key, UUID attribute, UUID dataSet, UUID dataSetList,
                                              UUID sourceId) throws DataSetServiceException {
        return modelsProvider.replicateAttributeKey(id, key, attribute, dataSet, dataSetList, sourceId);
    }

    @Override
    public Attribute getById(UUID id) {
        return modelsProvider.getAttributeById(id);
    }

    @Override
    public AttributeKey getAttributeKeyById(UUID id) {
        return modelsProvider.getAttributeKeyById(id);
    }

    @Override
    public List<AttributeKey> getAttributeKeysByDatasetListId(UUID datasetListId) {
        return modelsProvider.getByDataSetListId(datasetListId);
    }

    @Override
    public void removeListValuesByAttributeId(UUID id) {
        List<ListValueEntity> lists = listValueRepository.getByAttributeId(id);
        listValueRepository.deleteAll(lists);
    }

    @Override
    @Transactional
    public ListValue getListValueById(UUID id) {
        Optional<ListValueEntity> value = listValueRepository.findById(id);
        return value.map(listValueEntity -> modelsProvider.getListValue(listValueEntity)).orElse(null);
    }

    @Override
    public ListValue getListValueByAttributeIdAndValue(UUID attributeId, String value) {
        return modelsProvider.getByAttributeIdAndText(attributeId, value);
    }

    @Override
    public List<ListValue> getListValuesByAttributeId(UUID attributeId) {
        return modelsProvider.getListValuesByAttributeId(attributeId);
    }

    @Override
    public List<Attribute> getByNameAndDataSetListId(String name, UUID dataSetListId) {
        return modelsProvider.getAttributeByNameAndDataSetListId(name, dataSetListId);
    }

    @Override
    public List<Attribute> getByDataSetListId(UUID dataSetListId) {
        return modelsProvider.getAttributeByDataSetListId(dataSetListId);
    }

    @Override
    public List<Attribute> getByDataSetListIdIn(Collection<UUID> dataSetListIds) {
        return modelsProvider.getAttributesByDataSetListIdIn(dataSetListIds);
    }

    public List<Attribute> getBySourceIdAndDataSetListId(UUID sourceId, UUID dataSetListId) {
        return modelsProvider.getAttributeBySourceAndDataSetListId(sourceId, dataSetListId);
    }

    public List<AttributeKey> getAttrKeyBySourceIdAndDataSetListId(UUID sourceId, UUID dataSetListId) {
        return modelsProvider.getAttributeKeyBySourceAndDataSetListId(sourceId, dataSetListId);
    }

    @Override
    public AttributeKey getAttributeKeyByKeyAndDataSetListIdAndDataSetIdAndAttributeId(String key,
                                                                                       UUID dataSetListId,
                                                                                       UUID datasetId,
                                                                                       UUID attributeId) {
        return modelsProvider.getAttributeKeyByKeyAndDataSetListIdAndDataSetIdAndAttributeId(key, dataSetListId,
                datasetId, attributeId);
    }

    public List<ListValue> getListValueBySourceIdAndAttrId(UUID sourceId, UUID attributeId) {
        return modelsProvider.getListValueBySourceIdAndAttrId(sourceId, attributeId);
    }

    @Override
    @Transactional
    public void setPosition(UUID attributeId, Integer position) {
        if (position == null || position < 0) {
            log.error("Attribute '" + attributeId + "' position can't be null or negative");
            throw new AttributePositionException();
        }
        Attribute attributeToMove = modelsProvider.getAttributeById(attributeId);
        if (attributeToMove == null) {
            log.error("Attribute '" + attributeId + "' not found");
            throw new AttributeNotFoundException();
        }
        DataSetList dataSetList = attributeToMove.getDataSetList();
        List<Attribute> attributes = dataSetList.getAttributes();
        if (position > attributes.size() - 1) {
            log.error("Position out of range [0-" + (attributes.size() - 1) + "]");
            throw new AttributePositionException();
        }
        attributes.remove(attributeToMove);
        attributes.add(position, attributeToMove);
        int orderCounter = 0;
        for (Attribute attribute : attributes) {
            attribute.setOrdering(orderCounter);
            orderCounter++;
        }
    }

    @Override
    @Transactional
    public ListValue createListValue(String text, UUID attributeId) {
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        ListValue listValue = attribute.createListValue(text);
        dataSetListSnapshotService.commitEntity(attribute.getDataSetList().getId());
        return listValue;
    }

    @Override
    @Transactional
    public ListValue replicateListValue(UUID id, String text, UUID attributeId, UUID sourceId)
            throws DataSetServiceException {
        return modelsProvider.replicateListValue(id, text, attributeId, sourceId);
    }

    @Override
    @Transactional
    public void setReferencedDataSetList(UUID attributeId, UUID dataSetListId) {
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        attribute.setTypeDataSetListId(dataSetListId);
    }

    @Override
    public Map<UUID, AttributeKeyIdsDbUpdate> getFoundedAttributeKeyIdAndDatasetIdUpdate() {
        return this.foundedAttributeKeyIdAndDatasetIdUpdate;
    }

    /**
     * Merge values from the source ListValue to the target ListValue.
     *
     * @param sourceAttrId source attribute id
     * @param targetAttrId target attribute id
     * @param sourceListValue value in source ListValue
     * @return Returns the Id of the ListValue from the result list
     *         whose value matches the sourceListValue. If sourceListValue is null return null
     */
    @Nullable
    public UUID mergeListValuesAndGetListValueReference(UUID sourceAttrId, UUID targetAttrId,
                                                        @Nullable ListValueEntity sourceListValue) {
        log.debug("mergeListValuesAndGetListValueReference (sourceAttrId: {}, targetAttrId: {}, sourceListValue: {})",
                sourceAttrId, targetAttrId, sourceListValue);
        mergeListValues(sourceAttrId, targetAttrId);
        if (Objects.nonNull(sourceListValue)) {
            return getListValueByAttributeIdAndValue(targetAttrId, sourceListValue.getText()).getId();
        }
        return null;
    }

    /**
     * Merge values from the source ListValue to the right ListValue.
     *
     * @param sourceAttributeId source Attribute Id
     * @param targetAttributeId target Attribute Id
     */
    @Transactional
    public void mergeListValues(UUID sourceAttributeId, UUID targetAttributeId) {
        log.debug("mergeListValues (sourceAttrId: {}, targetAttrId: {})", sourceAttributeId, targetAttributeId);
        List<ListValue> sourceValues = getListValuesByAttributeId(sourceAttributeId);
        Map<String, ListValue> targetValues = getListValuesByAttributeId(targetAttributeId)
                .stream().collect(Collectors.toMap(ListValue::getText, Function.identity()));
        for (ListValue sourceValue : sourceValues) {
            if (!targetValues.containsKey(sourceValue.getText())) {
                log.info("Add new value \"{}\" to attribute (id: {})", sourceValue.getText(), targetAttributeId);
                createListValueWithoutCommitting(sourceValue.getText(), targetAttributeId);
            }
        }
    }
}

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

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.LabelEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.db.jpa.entities.TestPlanEntity;
import org.qubership.atp.dataset.db.jpa.entities.VisibilityAreaEntity;
import org.qubership.atp.dataset.exception.file.FileDsCopyException;
import org.qubership.atp.dataset.exception.file.FileDsNotFoundToCopyException;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.copy.AttributeCopyData;
import org.qubership.atp.dataset.service.jpa.model.copy.DataSetListCopyData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataSetList extends AbstractObjectWrapper<DataSetListEntity> {

    public DataSetList(DataSetListEntity entity) {
        super(entity);
    }

    public UUID getId() {
        return entity.getId();
    }

    public String getName() {
        return entity.getName();
    }

    public VisibilityArea getVisibilityArea() {
        return modelsProvider.getVisibilityArea(entity.getVisibilityArea());
    }

    public TestPlanEntity getTestPlan() {
        return entity.getTestPlan();
    }

    /**
     * Get overlaps.
     */
    public List<AttributeKey> getAttributeKeys() {
        List<AttributeKey> result = new LinkedList<>();
        String nativeQuery = "select * from attribute_key where datasetlist_id = :dsl_id";
        entityManager
                .createNativeQuery(nativeQuery, AttributeKeyEntity.class)
                .setParameter("dsl_id", getId())
                .getResultList().forEach(o -> result.add(new AttributeKey((AttributeKeyEntity) o)));
        return result;
    }

    /**
     * Get parameters without empty attributes.
     */
    public List<Parameter> getOverLapParameters() {
        List<Parameter> result = new LinkedList<>();
        String nativeQuery = "select p.* from attribute_key ak, parameter p where ak.datasetlist_id = :dsl_id and "
                + "p.attribute_id = ak.id";
        entityManager
                .createNativeQuery(nativeQuery, ParameterEntity.class)
                .setParameter("dsl_id", getId())
                .getResultList().forEach(o -> result.add(new Parameter((ParameterEntity) o)));
        return result;
    }

    /**
     * Get parameters without empty attributes.
     */
    public Map<AttributeKey, Parameter> getOverLapParametersMapping() {
        Map<AttributeKey, Parameter> result = new HashMap<>();
        List<AttributeKey> attributeKeys = getAttributeKeys();
        List<Parameter> overLapParameters = getOverLapParameters();
        for (AttributeKey attributeKey : attributeKeys) {
            for (Parameter parameter : overLapParameters) {
                if (parameter.getAttributeId().equals(attributeKey.getId())) {
                    result.put(attributeKey, parameter);
                }
            }
        }
        return result;
    }

    /**
     * Overlaps by datasets.
     */
    public List<AttributeKey> getAttributeKeysByDataSet(UUID dataSetId) {
        List<AttributeKey> result = new LinkedList<>();
        String nativeQuery = "select * from attribute_key where datasetlist_id = :dsl_id and dataset_id = :ds_id";
        entityManager
                .createNativeQuery(nativeQuery, AttributeKeyEntity.class)
                .setParameter("dsl_id", getId())
                .setParameter("ds_id", dataSetId)
                .getResultList().forEach(o -> result.add(new AttributeKey((AttributeKeyEntity) o)));
        return result;
    }

    /**
     * Get attributes.
     */
    public List<Attribute> getAttributes() {
        List<Attribute> result = new LinkedList<>();
        entity.getAttributes().forEach(attributeEntity -> result.add(modelsProvider.getAttribute(attributeEntity)));
        return result;
    }

    /**
     * Attributes count.
     */
    public Integer getAttributesCount() {
        String nativeQuery = "select count(1) from attribute where datasetlist_id = :dsl_id";
        return ((BigInteger) entityManager.createNativeQuery(nativeQuery)
                .setParameter("dsl_id", getId())
                .getSingleResult()).intValue();
    }

    /**
     * Get DSl attribute by id.
     */
    public Attribute getAttributeById(UUID id) {
        String nativeQuery = "select * from attribute where datasetlist_id = :dsl_id and id = :attr_id";
        List<AttributeEntity> resultList = entityManager.createNativeQuery(nativeQuery, AttributeEntity.class)
                .setParameter("dsl_id", getId())
                .setParameter("attr_id", id)
                .getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return modelsProvider.getAttribute(resultList.iterator().next());
    }

    /**
     * Takes Data Sets Ids list, returns their columns.
     */
    public List<Integer> getDataSetsColumns(List<UUID> dataSetIds) {
        List<Integer> result = new LinkedList<>();
        String nativeQuery =
                "select num from ("
                        + "Select id, row_number() over(ORDER BY ordering) num"
                        + " from Dataset ds"
                        + " where ds.datasetlist_id = :list_id"
                        + " order by ordering"
                        + ") ds_ordered"
                        + " where id in (:ds_ids)";
        List<BigInteger> queryResult = entityManager.createNativeQuery(nativeQuery)
                .setParameter("list_id", getId())
                .setParameter("ds_ids", dataSetIds)
                .getResultList();
        queryResult.forEach(bigInteger -> result.add(bigInteger.intValue() - 1));
        return result;
    }

    /**
     * Gets DS name, returns its column.
     */
    public Integer getDataSetColumnByName(String dataSetName) {
        String nativeQuery =
                "select num from ("
                        + "Select name, row_number() over(ORDER BY ordering) num"
                        + " from Dataset ds"
                        + " where ds.datasetlist_id = :list_id"
                        + " order by ordering"
                        + ") ds_ordered"
                        + " where name = :ds_name";
        return ((BigInteger) entityManager
                .createNativeQuery(nativeQuery)
                .setParameter("list_id", getId())
                .setParameter("ds_name", dataSetName)
                .getSingleResult()).intValue() - 1;
    }

    /**
     * Gets DS id, returns its column.
     */
    public Integer getDataSetColumnById(UUID dataSetId) {
        String nativeQuery =
                "select num from ("
                        + "Select id, row_number() over(ORDER BY ordering) num"
                        + " from Dataset ds"
                        + " where ds.datasetlist_id = :list_id"
                        + " order by ordering"
                        + ") ds_ordered"
                        + " where id = :ds_id";
        return ((BigInteger) entityManager
                .createNativeQuery(nativeQuery)
                .setParameter("list_id", getId())
                .setParameter("ds_id", dataSetId)
                .getSingleResult()).intValue() - 1;
    }

    /**
     * Get columns by dataset ids.
     */
    public List<Integer> getDataSetColumnsByIds(List<UUID> dataSetIds) {
        String nativeQuery =
                "select num from ("
                        + "Select id, row_number() over(ORDER BY ordering) num"
                        + " from Dataset ds"
                        + " where ds.datasetlist_id = :list_id"
                        + " order by ordering"
                        + ") ds_ordered"
                        + " where id in :ds_ids";
        List untypedResult = entityManager
                .createNativeQuery(nativeQuery)
                .setParameter("list_id", getId())
                .setParameter("ds_ids", dataSetIds)
                .getResultList();
        List<Integer> result = new ArrayList<>();
        for (Object o : untypedResult) {
            result.add(((BigInteger) o).intValue() - 1);
        }
        return result;
    }

    /**
     * All DSs.
     */
    public List<DataSet> getDataSets() {
        List<DataSet> result = new LinkedList<>();
        for (DataSetEntity dataSet : entity.getDataSets()) {
            result.add(modelsProvider.getDataSet(dataSet));
        }
        return result;
    }

    /**
     * Gets DS column, returns DS.
     */
    public DataSet getDataSetByColumn(int columnNumber) {
        String nativeQuery =
                "select * from ("
                        + "Select *, row_number() over(ORDER BY ordering) num"
                        + " from Dataset ds"
                        + " where ds.datasetlist_id = :list_id"
                        + " order by ordering"
                        + ") ds_ordered"
                        + " where num  = :row_num";
        DataSetEntity result = (DataSetEntity) entityManager
                .createNativeQuery(nativeQuery, DataSetEntity.class)
                .setParameter("list_id", getId())
                .setParameter("row_num", columnNumber + 1)
                .getSingleResult();
        return modelsProvider.getDataSet(result);
    }

    /**
     * Gets DS name, returns DS.
     */
    public DataSet getDataSetByName(String dataSetName) {
        String nativeQuery = "select * from dataSet where datasetlist_id = :dsl_id and name = :ds_name";
        DataSetEntity result = (DataSetEntity) entityManager
                .createNativeQuery(nativeQuery, DataSetEntity.class)
                .setParameter("dsl_id", getId())
                .setParameter("ds_name", dataSetName)
                .getSingleResult();
        return modelsProvider.getDataSet(result);
    }

    /**
     * Gets DS id, returns DS.
     */
    public DataSet getDataSetById(UUID dataSetId) {
        String nativeQuery = "select * from dataSet where datasetlist_id = :dsl_id and id = :ds_id";
        DataSetEntity result = (DataSetEntity) entityManager
                .createNativeQuery(nativeQuery, DataSetEntity.class)
                .setParameter("dsl_id", getId())
                .setParameter("ds_id", dataSetId)
                .getSingleResult();
        return modelsProvider.getDataSet(result);
    }

    /**
     * How many DSs this DSL has.
     */
    public Integer getDataSetsCount() {
        String nativeQuery = "select count(1) from dataset where datasetlist_id = :list_id";
        return ((BigInteger) entityManager
                .createNativeQuery(nativeQuery)
                .setParameter("list_id", getId())
                .getSingleResult()).intValue();
    }

    public List<LabelEntity> getLabels() {
        return entity.getLabels();
    }

    /**
     * Returns attributes filtered by type.
     */
    public List<Attribute> getAttributesByTypes(List<AttributeTypeName> attributeTypeNames) {
        List<Attribute> result = new LinkedList<>();
        for (AttributeEntity attribute : entity.getAttributes()) {
            AttributeTypeName type = AttributeTypeName.getTypeById(attribute.getAttributeTypeId());
            if (attributeTypeNames.contains(type)) {
                result.add(modelsProvider.getAttribute(attribute));
            }
        }
        return result;
    }

    /**
     * Get page of attributes.
     *
     * @param uuid               dsl id
     * @param attributeTypeNames types
     * @param pageable           page
     * @return page of attributes.
     */
    public Page<AttributeEntity> getAttributesByTypesPageable(UUID uuid, List<AttributeTypeName> attributeTypeNames,
                                                              Pageable pageable) {
        return modelsProvider.getAttributesPageableByDslId(uuid, attributeTypeNames, pageable);
    }

    /**
     * Extract attributes of page.
     *
     * @param entities           attribute entities
     * @param attributeTypeNames types
     * @return list of attributes
     */
    public List<Attribute> getAttributesOfPage(Page<AttributeEntity> entities,
                                               List<AttributeTypeName> attributeTypeNames) {
        List<Attribute> result = new LinkedList<>();
        for (AttributeEntity attribute : entities.toList()) {
            AttributeTypeName type = AttributeTypeName.getTypeById(attribute.getAttributeTypeId());
            if (attributeTypeNames.contains(type)) {
                result.add(modelsProvider.getAttribute(attribute));
            }
        }
        return result;
    }

    /**
     * Returns DSL dependencies.
     */
    public List<DataSetList> getDependencies() {
        List<Attribute> referenceAttributes = getAttributesByTypes(
                Collections.singletonList(AttributeTypeName.DSL)
        );
        Set<UUID> dependedDataSetLists = new LinkedHashSet<>();
        for (Attribute attribute : referenceAttributes) {
            UUID typeDataSetListId = attribute.getTypeDataSetListId();
            if (typeDataSetListId != null) {
                dependedDataSetLists.add(typeDataSetListId);
            }
        }
        List<DataSetList> result = new LinkedList<>();
        for (UUID dependedDataSetList : dependedDataSetLists) {
            result.add(modelsProvider.getDataSetListById(dependedDataSetList));
        }
        return result;
    }

    /**
     * Set DSL reference attributes.
     */
    public void setDataSetListAttributes(List<AttributeEntity> entityList) {
        entity.setAttributes(entityList);
    }

    /**
     * Returns DSL reference attributes.
     */
    public List<Attribute> getDataSetListReferences() {
        return getAttributesByTypes(Collections.singletonList(AttributeTypeName.DSL));
    }

    /**
     * Create with name.
     */
    public DataSet createDataSet(String name) {
        return createDataSet(name, getLastDataSetsOrderNumber() + 1);
    }

    /**
     * Create with name and order.
     */
    public DataSet createDataSet(String name, long order) {
        DataSetEntity dataSetEntity = new DataSetEntity();
        dataSetEntity.setName(name);
        dataSetEntity.setDataSetList(entity);
        dataSetEntity.setOrdering(order);
        save(dataSetEntity);
        return modelsProvider.getDataSet(dataSetEntity);
    }

    /**
     * Create with name and id.
     */
    public DataSet insertDataSet(UUID id, String name) {
        DataSetEntity dataSetEntity = new DataSetEntity();
        dataSetEntity.setName(name);
        dataSetEntity.setDataSetList(entity);
        dataSetEntity.setOrdering(getLastDataSetsOrderNumber() + 1);
        insert(dataSetEntity, id);
        return modelsProvider.getDataSet(dataSetEntity);
    }

    @Override
    public void beforeRemove() {
        List<AttributeKey> attributeKeys = getAttributeKeys();
        for (AttributeKey attributeKey : attributeKeys) {
            attributeKey.remove();
        }
        List<DataSet> dataSets = getDataSets();
        for (DataSet dataSet : dataSets) {
            dataSet.remove();
        }
        List<Attribute> attributes = getAttributes();
        for (Attribute attribute : attributes) {
            attribute.remove();
        }
    }

    /**
     * Creates new attribute with name and type.
     */
    public Attribute createAttribute(String name, AttributeTypeName type) {
        return createAttribute(name, type, getLastAttributeOrderNumber() + 1);
    }

    /**
     * Creates new attribute with name, type and ordering.
     */
    public Attribute createAttribute(String name, AttributeTypeName type, Integer ordering) {
        AttributeEntity attributeEntity = new AttributeEntity();
        attributeEntity.setName(name);
        attributeEntity.setDataSetList(entity);
        attributeEntity.setAttributeTypeId(type.getId());
        attributeEntity.setOrdering(ordering);
        save(attributeEntity);
        entity.getAttributes().add(attributeEntity);
        log.debug("createAttribute() - new attributeEntity id: {}, name: {}", attributeEntity.getId(), name);
        return modelsProvider.getAttribute(attributeEntity);
    }

    /**
     * Creates new attribute with name and type.
     */
    public Attribute insertAttribute(UUID id, String name, AttributeTypeName type, int ordering) {
        AttributeEntity attributeEntity = new AttributeEntity();
        attributeEntity.setId(id);
        attributeEntity.setName(name);
        attributeEntity.setDataSetList(entity);
        attributeEntity.setAttributeTypeId(type.getId());
        attributeEntity.setOrdering(ordering);
        insert(attributeEntity, id);
        entity.getAttributes().add(attributeEntity);
        return modelsProvider.getAttribute(attributeEntity);
    }

    /**
     * Returns maximum attribute order number.
     */
    public int getLastAttributeOrderNumber() {
        String nativeQuery =
                "select COALESCE(max(ordering), 1) max_order_number "
                        + " from attribute "
                        + " where datasetlist_id = :list_id";
        return ((Integer) entityManager
                .createNativeQuery(nativeQuery)
                .setParameter("list_id", getId())
                .getSingleResult());
    }

    /**
     * Returns maximum data set order number.
     */
    public long getLastDataSetsOrderNumber() {
        String nativeQuery = "select max(ordering) max_order_number from dataset where datasetlist_id = ?";
        BigInteger result = ((BigInteger) entityManager
                .createNativeQuery(nativeQuery)
                .setParameter(1, getId())
                .getSingleResult());
        return result != null ? result.longValue() : 0L;
    }

    public void setName(String name) {
        entity.setName(name);
    }

    /**
     * Sets visibility area.
     *
     * @param visibilityAreaId the visibility area id
     */
    public void setVisibilityArea(UUID visibilityAreaId) {
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(visibilityAreaId);
        if (visibilityArea == null) {
            throw new Error("Can't find Visibility Area by id " + visibilityAreaId);
        }
        entity.setVisibilityArea(visibilityArea.getEntity());
    }

    /**
     * Copies a DSL with the same name using postfix or, if postfix is not specified, uses (N) postfix
     * where N - is copy number.
     */
    public DataSetListCopyData duplicate(@Nullable String postfix, String prevNamePattern,
                                         @Nullable UUID sagaSessionId) {
        final String copyPostfix = StringUtils.isEmpty(postfix) ? "Copy" : postfix;
        final String copyName = getNextCopyName(copyPostfix, prevNamePattern);

        return copy(entity.getVisibilityArea(), copyName, sagaSessionId);
    }

    /**
     * Copy DSL to selected VA with selected name.
     */
    private DataSetListCopyData copy(VisibilityAreaEntity visibilityArea, String name, @Nullable UUID sagaSessionId) {
        DataSetListEntity dataSetListCopy = new DataSetListEntity();
        Timestamp dslCopyTime = Timestamp.from(Instant.now());
        dataSetListCopy.setVisibilityArea(visibilityArea);
        dataSetListCopy.setName(name);
        dataSetListCopy.setCreatedWhen(dslCopyTime);
        dataSetListCopy.setModifiedWhen(dslCopyTime);
        dataSetListCopy.setSagaSessionId(sagaSessionId);
        save(dataSetListCopy);
        DataSetList dataSetList = modelsProvider.getDataSetList(dataSetListCopy);
        DataSetListCopyData dataSetListCopyData = new DataSetListCopyData(dataSetList);
        Map<UUID, AttributeCopyData> attributesMap = copyAttributesTo(dataSetList);
        dataSetListCopyData.setAttributesMap(attributesMap);
        Map<UUID, UUID> dataSetsMap = copyDataSetsTo(dataSetList, attributesMap);
        dataSetListCopyData.setDataSetsMap(dataSetsMap);
        copyOverlapsToListNoPathUpdate(dataSetListCopyData);
        return dataSetListCopyData;
    }

    /**
     * Copy overlaps keys. Path stays the same (technically invalid).
     */
    private void copyOverlapsToListNoPathUpdate(
            DataSetListCopyData dataSetListCopyData
    ) {
        DataSetList dataSetListCopy = dataSetListCopyData.getDataSetListCopy();
        log.info("Start copy overlaps keys for Dataset List with id {} and name {}", dataSetListCopy.getId(),
                dataSetListCopy.getName());
        Map<UUID, UUID> dataSetsMap = dataSetListCopyData.getDataSetsMap();
        Map<UUID, AttributeCopyData> attributesMap = dataSetListCopyData.getAttributesMap();
        for (AttributeKey attributeKey : getAttributeKeys()) {
            AttributeCopyData attributeCopyData = attributesMap.get(attributeKey.getAttribute().getId());
            Attribute newAttribute;
            if (attributeCopyData != null) {
                newAttribute = attributeCopyData.getAttributeCopy();
            } else {
                newAttribute = attributeKey.getAttribute();
            }
            List<UUID> path = attributeKey.getPath();
            UUID newDataSetId = dataSetsMap.get(attributeKey.getDataSet().getId());
            DataSet newDataSet = modelsProvider.getDataSetById(newDataSetId);
            Parameter newOverlap = newDataSet.createOverlap(newAttribute.getId(), path);
            log.debug("CreateOverlap - newAttributeId: {},  oldAttributeKey: {}, newAttributeKey: {}",
                    newAttribute.getId(), attributeKey.getId(), newOverlap.getAttributeKey().getId());
            Parameter oldOverlap = attributeKey.getParameter();
            switch (newAttribute.getAttributeType()) {
                case CHANGE:
                case ENCRYPTED:
                case TEXT:
                    newOverlap.setStringValue(oldOverlap.getStringValue());
                    break;
                case LIST:
                    if (oldOverlap != null && oldOverlap.getListValue() != null) {
                        newOverlap.setListValueId(oldOverlap.getListValue().getId());
                    }
                    break;
                case FILE:
                    try {
                        gridFsService.copy(oldOverlap.getId(), newOverlap.getId(), true);
                    } catch (IllegalStateException | FileDsNotFoundToCopyException | FileDsCopyException e) {
                        log.error("File " + oldOverlap.getId() + " not found. Continue copying.", e);
                    }
                    break;
                case DSL:
                    UUID originalDataSetReference = oldOverlap.getDataSetReferenceId();
                    UUID newDataSetReferenceId = dataSetsMap.get(originalDataSetReference);
                    if (newDataSetReferenceId != null) {
                        newOverlap.setDataSetReferenceId(newDataSetReferenceId);
                    } else {
                        newOverlap.setDataSetReferenceId(originalDataSetReference);
                    }
                    break;
                default:
            }
        }
        log.info("Finish copy overlaps keys for Dataset List with id {} and name {}", dataSetListCopy.getId(),
                dataSetListCopy.getName());
    }

    /**
     * Updates path by chain.
     */
    private List<UUID> getUpdatedPath(List<UUID> oldPath, Map<UUID, AttributeCopyData> attributesMap) {
        List<UUID> newPath = new LinkedList<>();
        boolean chainBroken = false;
        for (UUID oldPathStep : oldPath) {
            if (chainBroken) {
                newPath.add(oldPathStep);
            } else {
                AttributeCopyData attributeCopyData = attributesMap.get(oldPathStep);
                if (attributeCopyData != null) {
                    newPath.add(attributeCopyData.getCopyId());
                } else {
                    chainBroken = true;
                    newPath.add(oldPathStep);
                }
            }
        }
        return newPath;
    }

    /**
     * Copy DSL attributes to another DSL.
     */
    private Map<UUID, AttributeCopyData> copyAttributesTo(DataSetList anotherDataSetList) {
        Map<UUID, AttributeCopyData> originalToCopy = new LinkedHashMap<>();
        for (Attribute attribute : getAttributes()) {
            Attribute copyAttribute = anotherDataSetList.createAttribute(
                    attribute.getName(),
                    attribute.getAttributeType(),
                    attribute.getOrdering()
            );
            copyAttribute.setTypeDataSetListId(attribute.getTypeDataSetListId());
            copyAttribute.setOrdering(attribute.getOrdering());
            Map<UUID, UUID> listValuesMap = attribute.copyListValuesTo(copyAttribute);
            originalToCopy.put(
                    attribute.getId(),
                    new AttributeCopyData(copyAttribute, listValuesMap)
            );
        }
        log.debug("copyAttributesTo() - " + originalToCopy.entrySet());
        return originalToCopy;
    }

    /**
     * Copy DSL datasets to another DSL. Needs attributes map.
     */
    private Map<UUID, UUID> copyDataSetsTo(
            DataSetList anotherDataSetList,
            Map<UUID, AttributeCopyData> attributesMap) {
        Map<UUID, UUID> dataSetMap = new LinkedHashMap<>();
        List<DataSet> dataSets = getDataSets();
        for (DataSet dataSet : dataSets) {
            DataSet copyDataSet =
                    anotherDataSetList.createDataSet(dataSet.getName(), dataSet.getEntity().getOrdering());
            dataSet.copyParametersTo(copyDataSet, attributesMap);
            dataSetMap.put(dataSet.getId(), copyDataSet.getId());
        }
        return dataSetMap;
    }

    private String getNextCopyName(String postfix, String prevNamePattern) {
        final Set<String> dataSetListNames = new HashSet<>(getVisibilityArea().getDataSetListsNames());
        final String name = getName();

        String newName;
        if (!StringUtils.isEmpty(prevNamePattern) && name.contains(prevNamePattern)) {
            newName = name.replaceAll(prevNamePattern, postfix);
        } else {
            newName = String.format("%s %s", name, postfix);
        }

        while (dataSetListNames.contains(newName)) {
            newName = String.format("%s %s", newName, "Copy");
        }

        return newName;
    }

    /**
     * Changes attribute DSL reference to the new one according to the map.
     */
    public void updateDslReferences(Map<UUID, DataSetListCopyData> copiesData, Map<UUID, UUID> dataSetsMap) {
        long startTime = System.currentTimeMillis();
        List<Attribute> referencedAttributes = getAttributesByTypes(
                Collections.singletonList(AttributeTypeName.DSL)
        );
        for (Attribute referencedAttribute : referencedAttributes) {
            DataSetListCopyData copyData = copiesData.get(referencedAttribute.getTypeDataSetListId());
            if (copyData != null) {
                referencedAttribute.setTypeDataSetListId(copyData.getCopyId());
                for (Parameter referencedParameter : referencedAttribute.getParameters()) {
                    UUID newId = dataSetsMap.get(referencedParameter.getDataSetReferenceId());
                    referencedParameter.setDataSetReferenceId(newId);
                }
            }
        }
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        log.debug("UpdateDslReferences, time sec: {}", duration);
    }

    /**
     * Updates overlaps keys.
     */
    public void updateOverlaps(Map<UUID, DataSetListCopyData> copiesData) {
        Map<UUID, AttributeCopyData> allAttributesMap = new LinkedHashMap<>();
        Map<UUID, UUID> allDataSetsMap = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        try {
            for (DataSetListCopyData values : copiesData.values()) {
                allAttributesMap.putAll(values.getAttributesMap());
                allDataSetsMap.putAll(values.getDataSetsMap());
            }
            List<AttributeKey> attributeKeys = getAttributeKeys();
            int count = 0;
            for (AttributeKey attributeKey : attributeKeys) {
                List<UUID> oldPath = attributeKey.getPath();
                List<UUID> newPath = getUpdatedPath(oldPath, allAttributesMap);
                attributeKey.setKey(StringUtils.join(newPath, "_"));
                UUID lastOld = oldPath.get(oldPath.size() - 1);
                UUID lastNew = newPath.get(newPath.size() - 1);
                if (lastOld != lastNew) {
                    AttributeCopyData attributeCopyData = allAttributesMap.get(attributeKey.getAttribute().getId());
                    if (attributeCopyData != null) {
                        count++;
                        attributeKey.setAttribute(attributeCopyData.getCopyId());
                        ;
                        if (attributeKey.getAttributeType().equals(AttributeTypeName.DSL)) {
                            Parameter parameter = attributeKey.getParameter();
                            if (parameter != null) {
                                UUID dataSetReferenceId = parameter.getDataSetReferenceId();
                                UUID newReferencedDataset = allDataSetsMap.get(dataSetReferenceId);
                                if (newReferencedDataset != null) {
                                    parameter.setDataSetReferenceId(newReferencedDataset);
                                }
                            }
                        }
                    }
                }
            }
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log.info("finish update overlaps - getAttributeKeys() - dsl_id: {}, attributeKeys.size(): {}, total count"
                    + " attrKey: {}, time sec: {}", getId(), attributeKeys.size(), count, duration);
        } catch (Throwable e) {
            log.error("Update overlaps - dsl_id: {}", getId(), e);
        }
    }

    public void setModifiedBy(UUID id) {
        entity.setModifiedBy(id);
        save(entity);
    }

    public void setModifiedWhen(Timestamp timestamp) {
        entity.setModifiedWhen(timestamp);
        save(entity);
    }

    /**
     * Insert overlap with id and parameter for data set.
     */
    public Parameter insertOverlap(
            UUID dataSetId, UUID attributeKeyId, UUID attributeId, List<UUID> path, UUID parameterId
    ) {
        DataSet dataSet = getDataSetById(dataSetId);
        AttributeKeyEntity attributeKeyEntity = new AttributeKeyEntity();
        attributeKeyEntity.setDataSetList(entity);
        attributeKeyEntity.setDataSet(dataSet.getEntity());

        attributeKeyEntity.setAttribute(modelsProvider.getAttributeById(attributeId).getEntity());
        attributeKeyEntity.setKey(StringUtils.join(path, "_"));
        insert(attributeKeyEntity, attributeKeyId);

        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setId(parameterId);
        parameterEntity.setAttribute(attributeKeyEntity);
        parameterEntity.setDataSet(dataSet.getEntity());

        insert(parameterEntity, parameterId);

        return modelsProvider.getParameter(parameterEntity);
    }

    public Timestamp getModifiedWhen() {
        return entity.getModifiedWhen();
    }

    public UUID getSagaSessionId() {
        return entity.getSagaSessionId();
    }
}

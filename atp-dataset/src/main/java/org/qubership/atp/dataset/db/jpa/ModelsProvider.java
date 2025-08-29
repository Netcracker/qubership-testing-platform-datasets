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

package org.qubership.atp.dataset.db.jpa;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.db.jpa.entities.AbstractAttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AbstractUuidBasedEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.LabelEntity;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.db.jpa.entities.VisibilityAreaEntity;
import org.qubership.atp.dataset.db.jpa.repositories.JpaAttributeKeyRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaAttributeRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaAttributeTypeRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaDataSetListRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaDataSetRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaListValueRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaParameterRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaVisibilityAreaRepository;
import org.qubership.atp.dataset.exception.dataset.DataSetNotFoundException;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Label;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.rest.PaginationResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.AbstractEntityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ModelsProvider {

    @Autowired
    protected JpaDataSetListRepository dataSetListRepository;
    @Autowired
    protected JpaDataSetRepository dataSetRepository;
    @Autowired
    protected JpaAttributeRepository attributeRepository;
    @Autowired
    protected JpaAttributeKeyRepository attributeKeyRepository;
    @Autowired
    protected JpaVisibilityAreaRepository visibilityAreaRepository;
    @Autowired
    protected JpaParameterRepository parameterRepository;
    @Autowired
    protected JpaListValueRepository listValueRepository;
    @Autowired
    protected JpaAttributeTypeRepository attributeTypeRepository;

    public DataSetList getDataSetListById(UUID uuid) {
        Optional<DataSetListEntity> dataSetListOptional = dataSetListRepository.findById(uuid);
        return dataSetListOptional.map(this::getDataSetList).orElse(null);
    }

    /**
     * Get pageable attributes.
     *
     * @param uuid                 dsl id
     * @param attributeTypesToLoad types
     * @param pageable             page
     * @return page of attribute entities.
     */
    public Page<AttributeEntity> getAttributesPageableByDslId(UUID uuid, List<AttributeTypeName> attributeTypesToLoad,
                                                              Pageable pageable) {
        List<Long> typeIds = attributeTypesToLoad.stream().map(AttributeTypeName::getId).collect(Collectors.toList());
        return attributeRepository.findByEntityAndTypeIds(uuid, typeIds, pageable);
    }

    /**
     * Visibility areas without order.
     */
    public List<VisibilityArea> getAllVisibilityAreas() {
        List<VisibilityArea> result = new LinkedList<>();
        visibilityAreaRepository.findAll().forEach(
                visibilityAreaEntity -> result.add(getVisibilityArea(visibilityAreaEntity))
        );
        return result;
    }

    /**
     * Visibility areas ordered by name.
     */
    public List<VisibilityArea> getAllVisibilityAreasOrderedByNameAsc() {
        List<VisibilityArea> result = new LinkedList<>();
        visibilityAreaRepository.findAllByOrderByNameAsc().forEach(
                visibilityAreaEntity -> result.add(getVisibilityArea(visibilityAreaEntity))
        );
        return result;
    }

    public Attribute getAttributeById(UUID uuid) {
        Optional<AttributeEntity> dataSetListOptional = attributeRepository.findById(uuid);
        return dataSetListOptional.map(this::getAttribute).orElse(null);
    }

    @Cacheable(value = CacheEnum.Constants.PARAMETER_CACHE, key = "#uuid")
    public Parameter getParameterById(UUID uuid) {
        Optional<ParameterEntity> dataSetListOptional = parameterRepository.findById(uuid);
        return dataSetListOptional.map(this::getParameter).orElse(null);
    }

    /**
     * Get Parameter by constraint fields sourceId and dataSetId.
     */
    public Parameter getParameterByAttributeIdAndDataSetId(UUID attributeId, UUID dataSetId) {
        ParameterEntity parameterEntity = parameterRepository.getByAttributeIdAndDataSet_Id(attributeId, dataSetId);
        return this.getParameter(parameterEntity);
    }

    public DataSet getDataSetById(UUID uuid) {
        Optional<DataSetEntity> dataSetOptional = dataSetRepository.findById(uuid);
        return dataSetOptional.map(this::getDataSet).orElse(null);
    }

    public List<DataSet> getDataSetByNameAndDataSetListId(String name, UUID uuid) {
        List<DataSetEntity> dataSetOptional = dataSetRepository.findByNameAndDataSetListId(name, uuid);
        return dataSetOptional.stream().map(this::getDataSet).collect(Collectors.toList());
    }

    public List<DataSet> getDataSetByDataSetListId(UUID uuid) {
        List<DataSetEntity> dataSetOptional = dataSetRepository.findByDataSetListId(uuid);
        return dataSetOptional.stream().map(this::getDataSet).collect(Collectors.toList());
    }

    public List<DataSet> getDatasetsByDataSetListId(UUID dataSetListId) {
        List<DataSetEntity> dataSetEntities = dataSetRepository.findByDataSetListId(dataSetListId);
        return dataSetEntities.stream().map(this::getDataSet).collect(Collectors.toList());
    }

    public List<Attribute> getAttributesByDataSetListId(UUID dataSetListId) {
        List<AttributeEntity> attributeEntities = attributeRepository.getByDataSetListId(dataSetListId);
        return attributeEntities.stream().map(this::getAttribute).collect(Collectors.toList());
    }

    public List<Attribute> getAttributesByDataSetListIdIn(Collection<UUID> dataSetListIds) {
        List<AttributeEntity> attributeEntities = attributeRepository.getByDataSetListIdIn(dataSetListIds);
        return attributeEntities.stream().map(this::getAttribute).collect(Collectors.toList());
    }

    public List<DataSet> getDataSetBySourceAndDataSetListId(UUID sourceId, UUID datasetId) {
        List<DataSetEntity> dataSetOptional = dataSetRepository.findBySourceIdAndDataSetListId(sourceId, datasetId);
        return dataSetOptional.stream().map(this::getDataSet).collect(Collectors.toList());
    }

    public VisibilityArea getVisibilityAreaById(UUID uuid) {
        Optional<VisibilityAreaEntity> visibilityAreaOptional = visibilityAreaRepository.findById(uuid);
        return visibilityAreaOptional.map(this::getVisibilityArea).orElse(null);
    }

    public VisibilityArea getVisibilityAreaByName(String name) {
        VisibilityAreaEntity visibilityAreaEntity = visibilityAreaRepository.findByName(name);
        return getVisibilityArea(visibilityAreaEntity);
    }

    /**
     * New visibility area.
     */
    public VisibilityArea createVisibilityArea(String name) {
        VisibilityAreaEntity newArea = new VisibilityAreaEntity();
        newArea.setName(name);
        VisibilityAreaEntity saved = visibilityAreaRepository.save(newArea);
        return getVisibilityArea(saved);
    }

    /**
     * New visibility area.
     */
    public VisibilityArea replicate(UUID id, String name) {
        VisibilityAreaEntity newArea = new VisibilityAreaEntity();
        newArea.setName(name);
        newArea.setId(id);
        VisibilityArea area = getVisibilityArea(newArea);
        area.replicate();
        return getVisibilityAreaById(id);
    }

    /**
     * Get or create delegate by entity.
     */
    public Attribute getAttribute(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new Attribute((AttributeEntity) entity);
    }

    /**
     * Get or create delegate by entity.
     */
    public AttributeKey getAttributeKey(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new AttributeKey((AttributeKeyEntity) entity);
    }

    /**
     * Gets attribute key by id.
     *
     * @param id the id
     * @return the attribute key by id
     */
    public AttributeKey getAttributeKeyById(UUID id) {
        Optional<AttributeKeyEntity> attributeKey = attributeKeyRepository.findById(id);
        return getAttributeKey(attributeKey.orElse(null));
    }

    /**
     * Gets attribute keys by DSL id.
     *
     * @param dataSetListId the DSL id
     * @return the attribute key by id
     */
    public List<AttributeKey> getByDataSetListId(UUID dataSetListId) {
        List<AttributeKeyEntity> attributeKeys = attributeKeyRepository.getByDataSetListId(dataSetListId);
        if (CollectionUtils.isEmpty(attributeKeys)) {
            return Collections.emptyList();
        }
        return attributeKeys.stream().map(this::getAttributeKey).collect(Collectors.toList());
    }

    /**
     * Get or create delegate by entity.
     */
    public DataSet getDataSet(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new DataSet((DataSetEntity) entity);
    }

    /**
     * Get or create delegate by entity.
     */
    public DataSetList getDataSetList(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new DataSetList((DataSetListEntity) entity);
    }

    /**
     * Get or create delegate by entity.
     */
    public Label getLabel(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new Label((LabelEntity) entity);
    }

    /**
     * Get or create delegate by entity.
     */
    public Parameter getParameter(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new Parameter((ParameterEntity) entity);
    }

    /**
     * Get or create delegate by entity.
     */
    public VisibilityArea getVisibilityArea(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new VisibilityArea((VisibilityAreaEntity) entity);
    }

    /**
     * Get or create delegate by entity.
     */
    public ListValue getListValue(AbstractUuidBasedEntity entity) {
        return entity == null ? null : new ListValue((ListValueEntity) entity);
    }

    /**
     * Get all DS (id, name) for visibility area and filter by name.
     *
     * @param name             for filtering DS
     * @param visibilityAreaId for filtering DS
     * @param pageable         page request
     * @return datasets
     */
    public Page<AbstractEntityResponse> getDatasetsIdNamesPageByNameAndVaId(String name,
                                                                            UUID visibilityAreaId,
                                                                            Pageable pageable) {
        VisibilityAreaEntity visibilityAreaEntity = new VisibilityAreaEntity();
        visibilityAreaEntity.setId(visibilityAreaId);
        List<UUID> dataSetListsId =
                dataSetListRepository.findByVisibilityArea(visibilityAreaEntity)
                        .stream().map(DataSetListEntity::getId)
                        .collect(Collectors.toList());
        return dataSetRepository.findAllByNameContainsAndDslIn(name, dataSetListsId, pageable)
                .orElse(new PageImpl<>(Collections.emptyList(), pageable, 0))
                .map(dataSetEntity -> new AbstractEntityResponse(dataSetEntity.getId(), dataSetEntity.getName()));
    }

    /**
     * Get or create delegate by entity.
     */
    public ListValue getListValueById(UUID id) {
        if (id == null) {
            return null;
        }
        ListValueEntity entity = listValueRepository.getById(id);
        return new ListValue(entity);
    }

    public Page<DataSetEntity> getAllNamesByName(String name, Pageable pageable) {
        return dataSetRepository.findAllByNameContains(name, pageable);
    }

    /**
     * Create data set list with requested id data set list.
     *
     * @param id               the id
     * @param name             the name
     * @param visibilityAreaId the visibility area id
     * @param sourceId         the source id
     * @return the data set list
     * @throws DataSetServiceException the data set service exception
     */
    public DataSetList replicateDataSetList(UUID id, String name, UUID visibilityAreaId, UUID sourceId, UUID createdBy,
                                            Timestamp createdWhen, UUID modifiedBy, Timestamp modifiedWhen)
            throws DataSetServiceException {
        DataSetListEntity dataSetListEntity = new DataSetListEntity();
        dataSetListEntity.setName(name);
        dataSetListEntity.setId(id);
        dataSetListEntity.setSourceId(sourceId);
        dataSetListEntity.setCreatedBy(createdBy);
        dataSetListEntity.setCreatedWhen(createdWhen);
        dataSetListEntity.setModifiedBy(modifiedBy);
        dataSetListEntity.setModifiedWhen(modifiedWhen);
        Optional<VisibilityAreaEntity> visibilityArea = visibilityAreaRepository.findById(visibilityAreaId);
        if (!visibilityArea.isPresent()) {
            throw new DataSetServiceException("Visibility Area with id " + visibilityAreaId + " not found");
        }
        dataSetListEntity.setVisibilityArea(visibilityArea.get());
        DataSetList area = getDataSetList(dataSetListEntity);
        area.replicate();
        return area;
    }

    /**
     * Create data set with requested id data set.
     *
     * @param id            the id
     * @param name          the name
     * @param dataSetListId the data set list id
     * @param sourceId      the source id
     * @return the data set
     * @throws DataSetServiceException the data set service exception
     */
    public DataSet replicateDataSet(UUID id, String name, UUID dataSetListId, Long ordering,
                                    UUID sourceId, Boolean isLocked) throws DataSetServiceException {
        DataSetEntity dataSetEntity = new DataSetEntity();
        dataSetEntity.setName(name);
        dataSetEntity.setId(id);
        Optional<DataSetListEntity> dataSetListEntity = dataSetListRepository.findById(dataSetListId);
        if (!dataSetListEntity.isPresent()) {
            throw new DataSetServiceException("Data Set with id " + dataSetListEntity + " not found");
        }
        dataSetEntity.setDataSetList(dataSetListEntity.get());
        dataSetEntity.setOrdering(ordering);
        dataSetEntity.setSourceId(sourceId);
        dataSetEntity.setLocked(isLocked);
        DataSet dataSet = getDataSet(dataSetEntity);
        dataSet.replicate();
        return dataSet;
    }

    /**
     * Replicate attribute attribute.
     *
     * @param id            the id
     * @param name          the name
     * @param type          the type
     * @param dataSetListId the data set list id
     * @param sourceId      the source id
     * @return the attribute
     * @throws DataSetServiceException the data set service exception
     */
    public Attribute replicateAttribute(UUID id, String name, AttributeTypeName type, UUID dataSetListId, UUID sourceId)
            throws DataSetServiceException {
        AttributeEntity attributeEntity = new AttributeEntity();
        attributeEntity.setName(name);
        attributeEntity.setId(id);
        Optional<DataSetListEntity> dataSetListEntity = dataSetListRepository.findById(dataSetListId);
        if (!dataSetListEntity.isPresent()) {
            throw new DataSetServiceException("Data Set with id " + dataSetListEntity + " not found");
        }
        attributeEntity.setDataSetList(dataSetListEntity.get());
        attributeEntity.setOrdering(getDataSetList(dataSetListEntity.get()).getLastAttributeOrderNumber() + 1);
        attributeEntity.setAttributeTypeId(type.getId());
        attributeEntity.setSourceId(sourceId);
        Attribute attribute = getAttribute(attributeEntity);
        attribute.replicate();
        return attribute;
    }

    /**
     * Replicate parameter parameter.
     *
     * @param id          the id
     * @param dataSetId   the data set id
     * @param attributeId the attribute id
     * @param sourceId    the source id
     * @return the parameter
     * @throws DataSetServiceException the data set service exception
     */
    public Parameter replicateParameter(UUID id, UUID dataSetId, UUID attributeId, UUID sourceId)
            throws DataSetServiceException {
        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setId(id);
        parameterEntity.setSourceId(sourceId);

        Optional<DataSetEntity> dataSetEntity = dataSetRepository.findById(dataSetId);
        if (!dataSetEntity.isPresent()) {
            throw new DataSetServiceException("Data Set with id " + dataSetEntity + " not found");
        }
        parameterEntity.setDataSet(dataSetEntity.get());

        AbstractAttributeEntity attributeEntity = attributeRepository.getById(attributeId);
        if (attributeEntity == null) {
            attributeEntity = attributeKeyRepository.getById(attributeId);
        }
        if (attributeEntity == null) {
            throw new DataSetServiceException("Attribute with id " + attributeId + " not found");
        }
        parameterEntity.setAttribute(attributeEntity);

        Parameter parameter = getParameter(parameterEntity);
        parameter.replicate();
        return parameter;
    }

    /**
     * Replicate list value list value.
     *
     * @param id          the id
     * @param text        the text
     * @param attributeId the attribute id
     * @param sourceId    the sourceId
     * @return the list value
     * @throws DataSetServiceException the data set service exception
     */
    public ListValue replicateListValue(UUID id, String text, UUID attributeId, UUID sourceId)
            throws DataSetServiceException {
        ListValueEntity listValueEntity = new ListValueEntity();
        listValueEntity.setId(id);
        listValueEntity.setText(text);
        listValueEntity.setSourceId(sourceId);

        Attribute attribute = getAttributeById(attributeId);
        if (attribute == null) {
            throw new DataSetServiceException("Attribute with id " + attributeId + " not found");
        }
        listValueEntity.setAttribute(attribute.getEntity());
        ListValue listValue = getListValue(listValueEntity);
        listValue.replicate();
        return listValue;
    }

    /**
     * Replicate attribute key.
     *
     * @param id            the id
     * @param key           the key
     * @param attributeId   the attribute id
     * @param dataSetId     the data set id
     * @param dataSetListId the data set list id
     * @param sourceId      the source id
     * @return the attribute key
     * @throws DataSetServiceException the data set service exception
     */
    public AttributeKey replicateAttributeKey(UUID id, String key, UUID attributeId, UUID dataSetId, UUID dataSetListId,
                                              UUID sourceId)
            throws DataSetServiceException {
        AttributeKeyEntity attributeKeyEntity = new AttributeKeyEntity();
        attributeKeyEntity.setId(id);
        attributeKeyEntity.setKey(key);
        attributeKeyEntity.setSourceId(sourceId);
        Attribute attribute = getAttributeById(attributeId);
        if (attribute == null) {
            throw new DataSetServiceException("Attribute with id " + attributeId + " not found");
        }
        attributeKeyEntity.setAttribute(attribute.getEntity());

        DataSet dataSet = getDataSetById(dataSetId);
        if (dataSet == null) {
            throw new DataSetServiceException("Data set with id " + dataSetId + " not found");
        }
        attributeKeyEntity.setDataSet(dataSet.getEntity());

        DataSetList dataSetList = getDataSetListById(dataSetListId);
        if (dataSetList == null) {
            throw new DataSetServiceException("Data set list with id " + dataSetListId + " not found");
        }
        attributeKeyEntity.setDataSetList(dataSetList.getEntity());

        AttributeKey attributeKey = getAttributeKey(attributeKeyEntity);
        attributeKey.replicate();
        return attributeKey;
    }

    /**
     * Get an attribute with selected name and data set list id.
     *
     * @param name          the name
     * @param dataSetListId the data set list id
     * @return the by name and data set list id
     */
    public List<Attribute> getAttributeByNameAndDataSetListId(String name, UUID dataSetListId) {
        List<AttributeEntity> attributeEntities = attributeRepository.getByNameAndDataSetListId(name,
                dataSetListId);
        if (CollectionUtils.isEmpty(attributeEntities)) {
            return Collections.emptyList();
        }
        return attributeEntities.stream().map(this::getAttribute).collect(Collectors.toList());
    }

    /**
     * Get an attribute by data set list id.
     *
     * @param dataSetListId the data set list id
     * @return the by data set list id
     */
    public List<Attribute> getAttributeByDataSetListId(UUID dataSetListId) {
        List<AttributeEntity> attributeEntities = attributeRepository.getByDataSetListId(dataSetListId);
        if (CollectionUtils.isEmpty(attributeEntities)) {
            return Collections.emptyList();
        }
        return attributeEntities.stream().map(this::getAttribute).collect(Collectors.toList());
    }

    /**
     * Get an attribute with selected name and data set list id.
     *
     * @param sourceId      the sourceId
     * @param dataSetListId the data set list id
     * @return the by name and data set list id
     */
    public List<Attribute> getAttributeBySourceAndDataSetListId(UUID sourceId, UUID dataSetListId) {
        List<AttributeEntity> attributeEntities =
                attributeRepository.getBySourceIdAndDataSetListId(sourceId, dataSetListId);
        if (CollectionUtils.isEmpty(attributeEntities)) {
            return Collections.emptyList();
        }
        return attributeEntities.stream().map(this::getAttribute).collect(Collectors.toList());
    }

    /**
     * Get an attribute with selected name and data set list id.
     *
     * @param sourceId      the sourceId
     * @param dataSetListId the data set list id
     * @return the by name and data set list id
     */
    public List<AttributeKey> getAttributeKeyBySourceAndDataSetListId(UUID sourceId, UUID dataSetListId) {
        List<AttributeKeyEntity> attributeEntities =
                attributeKeyRepository.getBySourceIdAndDataSetListId(sourceId, dataSetListId);
        if (CollectionUtils.isEmpty(attributeEntities)) {
            return Collections.emptyList();
        }
        return attributeEntities.stream().map(this::getAttributeKey).collect(Collectors.toList());
    }

    /**
     * Get an attribute with selected key and data set list id and data set id and attribute id.
     *
     * @param key           the key
     * @param dataSetListId the data set list id
     * @param datasetId     the data set id
     * @param attributeId   the attribute id
     * @return {@link AttributeKey}
     */
    public AttributeKey getAttributeKeyByKeyAndDataSetListIdAndDataSetIdAndAttributeId(String key,
                                                                                       UUID dataSetListId,
                                                                                       UUID datasetId,
                                                                                       UUID attributeId) {
        AttributeKeyEntity attributeKeyEntity = attributeKeyRepository
                .findFirstByKeyAndDataSetListIdAndDataSetIdAndAttributeId(key, dataSetListId, datasetId, attributeId);
        return this.getAttributeKey(attributeKeyEntity);
    }

    /**
     * Gets data set list by name and visibility area id.
     *
     * @param name           the name
     * @param visibilityArea the visibility area
     * @return collection of data set list by name and data set list id
     */
    public List<DataSetList> getDataSetListByNameAndVisibilityAreaId(String name, UUID visibilityArea) {
        List<DataSetListEntity> attributeEntities = dataSetListRepository.getByNameAndVisibilityAreaId(name,
                visibilityArea);
        if (CollectionUtils.isEmpty(attributeEntities)) {
            return Collections.emptyList();
        }
        return attributeEntities.stream().map(this::getDataSetList).collect(Collectors.toList());
    }

    /**
     * Gets data set list visibility are id.
     *
     * @param visibilityArea the visibility area
     * @return collection of data set list by name and data set list id
     */
    public List<DataSetList> getDataSetListByVisibilityAreaId(UUID visibilityArea) {
        List<DataSetListEntity> entities = dataSetListRepository.findByVisibilityAreaId(visibilityArea);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::getDataSetList).collect(Collectors.toList());
    }

    /**
     * Get attributes by type dataset list identifier.
     *
     * @param dataSetListId dataset list identifier
     * @param pageable      page request
     * @return result response
     */
    public PaginationResponse<TableResponse> getAttributesByTypeDatasetListId(UUID dataSetListId, Pageable pageable) {
        Page<AttributeEntity> page = attributeRepository.getByTypeDataSetListId(dataSetListId, pageable);
        return toPaginationResponse(page, TableResponse::fromAttributeEntity);
    }

    /**
     * Get parameters by type dataset identifier.
     *
     * @param dataSetId dataset identifier
     * @param pageable  page request
     * @return result response
     */
    public PaginationResponse<TableResponse> getParametersByDatasetId(UUID dataSetId, Pageable pageable) {
        Page<ParameterEntity> page = parameterRepository.getByDataSetReferenceId(dataSetId, pageable);
        return toPaginationResponse(page, TableResponse::fromParameterEntity);
    }

    private <T> PaginationResponse<TableResponse> toPaginationResponse(Page<T> page,
                                                                       Function<T, TableResponse> mapFunc) {
        PaginationResponse<TableResponse> response = new PaginationResponse<>();
        response.setEntities(page.get()
                .map(mapFunc)
                .collect(Collectors.toList()));
        response.setTotalCount(page.getTotalElements());
        return response;
    }

    /**
     * Gets data set list by source id and visibility area id .
     *
     * @param sourceId       the source id
     * @param visibilityArea the visibility area
     * @return collection of data set list by name and data set list id
     */
    public List<DataSetList> getDataSetListBySourceIdAndVisibilityAreaId(UUID sourceId, UUID visibilityArea) {
        List<DataSetListEntity> attributeEntities = dataSetListRepository.getBySourceIdAndVisibilityAreaId(sourceId,
                visibilityArea);
        if (CollectionUtils.isEmpty(attributeEntities)) {
            return Collections.emptyList();
        }
        return attributeEntities.stream().map(this::getDataSetList).collect(Collectors.toList());
    }

    /**
     * Gets data set lists by saga session id and visibility area id.
     * <br>Used in saga transactions.
     *
     * @param sagaSessionId    the saga session id
     * @param visibilityAreaId the visibility area id
     * @return collection of data set list
     */
    public Set<UUID> getDataSetListIdsBySagaSessionIdIdAndVisibilityAreaId(UUID sagaSessionId,
                                                                           UUID visibilityAreaId) {
        return dataSetListRepository.findAllIdsBySagaSessionIdAndVisibilityAreaId(sagaSessionId, visibilityAreaId);
    }

    /**
     * Gets parameter by source id and dataset id.
     *
     * @param sourceId  the source id
     * @param datasetId the visibility area
     * @return collection of parameters
     */
    public List<Parameter> getParameterBySourceIdAndDataSetId(UUID sourceId, UUID datasetId) {
        List<ParameterEntity> attributeEntities = parameterRepository.getBySourceIdAndDataSetId(sourceId,
                datasetId);
        if (CollectionUtils.isEmpty(attributeEntities)) {
            return Collections.emptyList();
        }
        return attributeEntities.stream().map(this::getParameter).collect(Collectors.toList());
    }

    public List<Parameter> getSortedParameterByDataSetId(UUID datasetId) {
        List<ParameterEntity> attributeEntities = parameterRepository.getByDataSetIdSorted(datasetId);
        return attributeEntities.stream().map(this::getParameter).collect(Collectors.toList());
    }

    public List<Parameter> getSortedOverlapParametersByDataSetId(UUID datasetId) {
        List<ParameterEntity> overlaps = parameterRepository.getOverlapByDataSetIdSorted(datasetId);
        return overlaps.stream().map(this::getParameter).collect(Collectors.toList());
    }

    /**
     * Gets list value by source id and dataset id.
     *
     * @param sourceId    the source id
     * @param attributeId the attribute id
     * @return collection of list values
     */
    public List<ListValue> getListValueBySourceIdAndAttrId(UUID sourceId, UUID attributeId) {
        List<ListValueEntity> lvEntities = listValueRepository.getByAttributeIdAndSourceId(attributeId, sourceId);
        if (CollectionUtils.isEmpty(lvEntities)) {
            return Collections.emptyList();
        }
        return lvEntities.stream().map(this::getListValue).collect(Collectors.toList());
    }

    public ListValue getByAttributeIdAndText(UUID attributeId, String value) {
        return getListValue(listValueRepository.getByAttributeIdAndText(attributeId, value));
    }

    public List<ListValue> getListValuesByAttributeId(UUID attributeId) {
        List<ListValueEntity> listValueEntities = listValueRepository.getByAttributeId(attributeId);
        return listValueEntities.stream().map(this::getListValue).collect(Collectors.toList());
    }

    public List<DataSetList> getByVisibilityAreaId(UUID visibilityAreaId) {
        List<DataSetListEntity> visibilityAreas = dataSetListRepository.findAllByVisibilityAreaId(visibilityAreaId);
        return visibilityAreas.stream().map(this::getDataSetList).collect(Collectors.toList());
    }

    public List<DataSet> getByDataSetListIdIn(Collection<UUID> dataSetListIds) {
        List<DataSetEntity> datasets = dataSetRepository.findByDataSetListIdIn(dataSetListIds);
        return datasets.stream().map(this::getDataSet).collect(Collectors.toList());
    }

    public List<DataSet> getByIds(Collection<UUID> ids) {
        List<DataSetEntity> datasets = dataSetRepository.findAllById(ids);
        return datasets.stream().map(this::getDataSet).collect(Collectors.toList());
    }

    public List<UUID> getParametersIdByDsId(UUID dsId) {
        return parameterRepository.getParametersIdByDataSetReferenceId(dsId);
    }

    public List<DataSet> getLockedDatasets(UUID dataSetListId) {
        List<DataSetEntity> dataSets = dataSetRepository.findByDataSetListIdAndLocked(dataSetListId, true);
        return dataSets.stream().map(this::getDataSet).collect(Collectors.toList());
    }

    public List<String> getNotUniqueDslNames(UUID visibilityArea) {
        return dataSetListRepository.getNotUniqueDslNames(visibilityArea);
    }

    public List<String> getDsNamesForDsl(UUID datasetListId) {
        return dataSetRepository.getDsNames(datasetListId);
    }

    public UUID getDatasetListIdByDatasetId(UUID dsId) {
        return dataSetRepository.getDslId(dsId).orElseThrow(DataSetNotFoundException::new);
    }

    public UUID getDataSetsListIdByDataSetId(UUID dsId) {
        return dataSetRepository.getDataSetsListIdByDataSetId(dsId);
    }

    public LinkedList<UUID> getDataSetsIdsByDataSetListId(UUID dataSetListId) {
        return dataSetListRepository.getDataSetsIdsByDataSetListId(dataSetListId);
    }

    public LinkedList<UUID> getAffectedDataSetsListIdsByDataSetListId(List<UUID> dataSetListId) {
        return dataSetListRepository.getAffectedDataSetListIdsByDataSetListId(dataSetListId);
    }

    public Set<UUID> getAffectedDataSetsIdsByDataSetListId(Set<UUID> dataSetListId) {
        return dataSetListRepository.getAffectedDataSetIdsByDataSetListId(dataSetListId);
    }

    public int countAttributes(UUID datasetId) {
        return attributeRepository.countAttributesByDataset(datasetId);
    }

    public List<Attribute> getNotUsedAttributesByDatasetId(UUID datasetId) {
        List<AttributeEntity> attributeEntities = attributeRepository.getNotUsedByDatasetId(datasetId);
        return attributeEntities.stream().map(this::getAttribute).collect(Collectors.toList());
    }

    public AttributeTypeName getAttributeTypeByAttributeId(UUID attributeId) {
        return attributeTypeRepository.findByAttributeId(attributeId).getEnum();
    }

    public List<Attribute> getAttributesByDataSetId(UUID dataSetId) {
        List<AttributeEntity> attributeEntities = attributeRepository.getByDatasetId(dataSetId);
        return attributeEntities.stream().map(this::getAttribute).collect(Collectors.toList());
    }

    public List<AttributeKey> getAttributesKeyByDataSetId(UUID dataSetId) {
        List<AttributeKeyEntity> attributeKeyEntities = attributeKeyRepository.findByDataSetId(dataSetId);
        return attributeKeyEntities.stream().map(this::getAttributeKey).collect(Collectors.toList());
    }

    public boolean isDsLocked(UUID dataSetId) {
        return dataSetRepository.isLocked(dataSetId);
    }

    public boolean isDslDifferentAttributes(UUID leftAttrId, UUID rightAttrId) {
        return attributeRepository.isDifferentDslAttributes(leftAttrId, rightAttrId);
    }

    public Set<UUID> getUniqueDataSetIdsByReferenceDataSetId(UUID dataSetId) {
        return parameterRepository.getUniqueDataSetIdsByDataSetReferenceId(dataSetId);
    }
}

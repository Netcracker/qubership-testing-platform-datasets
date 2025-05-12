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

package org.qubership.atp.dataset.service.direct.impl;

import static org.qubership.atp.dataset.model.AttributeType.CHANGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.javers.common.string.Strings;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.dataset.db.AttributeRepository;
import org.qubership.atp.dataset.db.ListValueRepository;
import org.qubership.atp.dataset.db.ParameterRepository;
import org.qubership.atp.dataset.db.jpa.entities.AttributesSortType;
import org.qubership.atp.dataset.db.jpa.entities.UserSettingsEntity;
import org.qubership.atp.dataset.db.jpa.repositories.JpaAttributesSortEnabledRepository;
import org.qubership.atp.dataset.exception.attribute.AttributeTypeException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DateAuditorService;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSet;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManParameter;
import org.qubership.atp.dataset.service.ws.entities.Pair;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AttributeServiceImpl implements AttributeService {

    private static final String multiplyPrefix = "MULTIPLY";

    private final ListValueRepository listValueRepository;
    private final AttributeRepository attributeRepository;
    private final ParameterRepository parameterRepository;
    private final Provider<DataSetListService> dslServiceProvider;
    private final ObjectMapper mapper;
    private final DateAuditorService dateAuditorService;
    private final DataSetListSnapshotService dataSetListSnapshotService;
    private final JpaAttributesSortEnabledRepository jpaAttributesSortEnabledRepository;
    private final ClearCacheService clearCacheService;

    /**
     * Constructor with autowire.
     */
    @Autowired
    public AttributeServiceImpl(
            ListValueRepository listValueRepository,
            AttributeRepository attributeRepository,
            Provider<DataSetListService> dslServiceProvider,
            ObjectMapper mapper,
            DateAuditorService dateAuditorService,
            DataSetListSnapshotService dataSetListSnapshotService,
            ParameterRepository parameterRepository,
            JpaAttributesSortEnabledRepository jpaAttributesSortEnabledRepository,
            ClearCacheService clearCacheService) {
        this.listValueRepository = listValueRepository;
        this.attributeRepository = attributeRepository;
        this.dslServiceProvider = dslServiceProvider;
        this.mapper = mapper;
        this.dateAuditorService = dateAuditorService;
        this.dataSetListSnapshotService = dataSetListSnapshotService;
        this.parameterRepository = parameterRepository;
        this.jpaAttributesSortEnabledRepository = jpaAttributesSortEnabledRepository;
        this.clearCacheService = clearCacheService;
    }

    /**
     * Creates new attribute with provided parameters.
     */
    @Transactional
    @Nonnull
    public Attribute create(@Nonnull UUID dslId, @Nonnull Integer order, @Nonnull String name,
                            @Nonnull AttributeType type, @Nullable UUID dslRefId, @Nullable List<String> listValues) {
        Attribute attribute = attributeRepository.create(dslId, order, name, type, dslRefId, listValues);
        dateAuditorService.updateModifiedFields(attribute.getDataSetList().getId());
        dataSetListSnapshotService.commitEntity(dslId);
        return attribute;
    }

    @Transactional
    public boolean updateTypeDslId(@Nonnull UUID dslId, UUID id) {
        return attributeRepository.updateTypeDslId(dslId, id);
    }

    /**
     * Updates attribute name.
     */
    @Transactional
    public boolean update(@Nonnull UUID id, @Nonnull String name) {
        Attribute attribute = attributeRepository.getById(id);
        boolean isUpdated = attribute != null && attributeRepository.update(id, name);
        if (isUpdated) {
            dateAuditorService.updateModifiedFields(attribute.getDataSetList().getId());
            dataSetListSnapshotService.commitEntity(attribute.getDataSetList().getId());
        }
        return isUpdated;
    }

    /**
     * Updates attribute DSL reference.
     */
    @Transactional
    public boolean updateDslReference(@Nonnull UUID id, @Nonnull UUID dslReferenceId) {
        Attribute attribute = attributeRepository.getById(id);
        Preconditions.checkNotNull(attribute, "Attribute with id " + id + " not found.");
        Preconditions.checkArgument(attribute.getType().equals(AttributeType.DSL), "Referenced DSL can "
                + "be updated only for attributes of DSL type.");
        List<DataSet> dataSets = attribute.getDataSetList().getDataSets();
        Set<UUID> attrRefsForOverlaps = new HashSet<>();
        collectAttributesForOverlapDelete(attribute, attrRefsForOverlaps);
        Set<Parameter> paramsToDelete = new HashSet<>();
        for (DataSet ds : dataSets) {
            clearCacheService.evictDatasetListContextCache(ds.getId());
            paramsToDelete.addAll(ds.getParameters()
                    .stream()
                    .filter(param -> param.getAttribute().getId().equals(attribute.getId())
                            || param instanceof ParameterOverlap
                            && ((ParameterOverlap) param).getAttributePath().getPath()
                            .stream()
                            .anyMatch(attr -> attr.getId().equals(attribute.getId())))
                    .collect(Collectors.toSet()));
        }
        //delete all level attributes overlaps
        parameterRepository.deleteOverlaps(attribute.getId(), attributePath -> true);
        attrRefsForOverlaps.forEach(attrId -> parameterRepository.deleteOverlaps(attrId,
                attrPath -> attrPath.getPath().stream()
                        .anyMatch(attr1 -> attr1.getId().equals(attribute.getId()))));

        paramsToDelete.forEach(parameterRepository::delete);
        return attributeRepository.updateDslRef(id, dslReferenceId);
    }

    @NotNull
    private void collectAttributesForOverlapDelete(Attribute attribute, Set<UUID> attrIds) {
        if (attribute.getDataSetListReference() == null) {
            attrIds.add(attribute.getId());
        } else {
            attribute.getDataSetListReference().getAttributes().forEach(
                    attr -> {
                        attrIds.add(attr.getId());
                        collectAttributesForOverlapDelete(attr, attrIds);
                    })
            ;
        }
    }

    @Nullable
    @Override
    public Object getOptions(@Nonnull UUID id) {
        Attribute attribute = get(id);
        if (attribute == null) {
            return null;
        }
        switch (attribute.getType()) {
            case LIST:
                return attribute.getListValues();
            case DSL:
            case CHANGE:
                DataSetList dataSetList = attribute.getDataSetListReference();
                Preconditions.checkNotNull(dataSetList,
                        "Attribute [%s] should have data set reference", attribute);
                UiManDataSetList result = new UiManDataSetList();
                result.setSource(dataSetList);
                List<DataSet> dataSets = dataSetList.getDataSets();
                dataSets.sort(Comparator.comparing(Named::getName));
                for (DataSet dataSet : dataSets) {
                    UiManDataSet converted = new UiManDataSet();
                    converted.setSource(dataSet);
                    result.getDataSets().add(converted);
                }
                return result;
            default:
                return null;
        }
    }

    /**
     * Returns all attributes by dataSetList id. {@inheritDoc}
     *
     * @return collection of attributes
     */
    @Nullable
    public Collection<Attribute> getByDslId(@Nonnull UUID dslId) {
        return attributeRepository.getEagerByParentId(dslId);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public ArrayNode getByDslIdInItfFormat(@Nonnull UUID dslId) {
        DataSetList dataSetList = dslServiceProvider.get().get(dslId);
        if (dataSetList == null) {
            return null;
        }
        return Utils.serializeAttrInItfWay(dataSetList, mapper);
    }

    @Nullable
    public Attribute get(@Nonnull UUID id) {
        return attributeRepository.getById(id);
    }

    @Override
    public boolean existsById(@NotNull UUID id) {
        return attributeRepository.existsById(id);
    }

    @Nonnull
    public List<Attribute> getAll() {
        return attributeRepository.getAll();
    }

    /**
     * Deletes selected attribute by id.
     */
    @Transactional
    public boolean delete(@Nonnull UUID id) {
        Attribute attribute = attributeRepository.getById(id);
        boolean isDeleted = attributeRepository.delete(id);
        if (isDeleted && attribute != null) {
            attribute.getParameters()
                    .forEach(param -> clearCacheService.evictDatasetListContextCache(param.getDataSet().getId()));
            dateAuditorService.updateModifiedFields(attribute.getDataSetList().getId());
            dataSetListSnapshotService.commitEntity(attribute.getDataSetList().getId());
        }
        return isDeleted;
    }

    /**
     * Creates list value.
     */
    @Transactional
    @Nonnull
    public ListValue createListValue(@Nonnull UUID attributeId, @Nonnull String value) {
        ListValue listValue = listValueRepository.create(attributeId, value);
        Attribute attribute = get(attributeId);
        if (attribute != null) {
            dateAuditorService.updateModifiedFields(attribute.getDataSetList().getId());
        }
        dataSetListSnapshotService.commitEntity(attribute.getDataSetList().getId());
        return listValue;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Nonnull
    public List<UUID> createListValues(@Nonnull UUID attributeId, @Nonnull List<String> values) {
        List<UUID> listValuesIds = new ArrayList<>();
        for (String value : values) {
            listValuesIds.add(listValueRepository.create(attributeId, value).getId());
        }
        Attribute attribute = get(attributeId);
        if (attribute != null) {
            dateAuditorService.updateModifiedFields(attribute.getDataSetList().getId());
            dataSetListSnapshotService.commitEntity(attribute.getDataSetList().getId());
        }
        return listValuesIds;
    }

    /**
     * See {@link AttributeService#deleteListValue(UUID, UUID)}.
     */
    @Transactional
    public boolean deleteListValue(@Nonnull UUID attributeId, @Nonnull UUID listValueId) {
        Attribute attribute = get(attributeId);
        boolean isDeleted = false;
        if (Objects.nonNull(attribute)) {
            attribute.getParameters().forEach(param -> {
                if (param.getListValue().getId().equals(listValueId)) {
                    clearCacheService.evictParameterCache(param.getId());
                    clearCacheService.evictDatasetListContextCache(param.getDataSet().getId());
                }
            });
            checkUsedValueLockDs(attributeId, listValueId);
            isDeleted = listValueRepository.delete(listValueId);
            if (isDeleted) {
                dateAuditorService.updateModifiedFields(attribute.getDataSetList().getId());
            }
        }
        return isDeleted;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public boolean deleteListValues(UUID attributeId, @Nonnull List<UUID> ids) {
        Attribute attribute = get(attributeId);
        boolean isDeleted = false;
        if (Objects.nonNull(attribute)) {
            attribute.getParameters().forEach(param -> {
                clearCacheService.evictParameterCache(param.getId());
                clearCacheService.evictDatasetListContextCache(param.getDataSet().getId());
            });
            ids.forEach(iterListValueId -> checkUsedValueLockDs(attributeId, iterListValueId));
            isDeleted = listValueRepository.bulkDelete(ids);
            if (isDeleted) {
                dateAuditorService.updateModifiedFields(attribute.getDataSetList().getId());
                dataSetListSnapshotService.commitEntity(attribute.getDataSetList().getId());
            }
        }
        return isDeleted;
    }

    private void checkUsedValueLockDs(@NotNull UUID attributeId, @NotNull UUID listValueId) {
        List<Parameter> parameters = parameterRepository.getByAttributeId(attributeId);
        parameters.forEach(parameter -> {
            ListValue parameterListValue = parameter.getListValue();
            if (Objects.nonNull(parameterListValue) && listValueId.equals(parameterListValue.getId())) {
                DataSet dataSet = parameter.getDataSet();
                Preconditions.checkArgument(!dataSet.isLocked(), "Can not change list"
                                + " value(s) with attribute name: '%s' and dataSet name: '%s', id: %s because"
                                + " dataset locked", parameterListValue.getAttribute().getName(),
                        dataSet.getName(), dataSet.getId());
            }
        });
    }

    @Override
    public Attribute copy(@Nonnull DataSetList newParentDsl, @Nonnull Attribute attribute, int ordering) {
        UUID dslId = newParentDsl.getId();
        Attribute result;
        switch (attribute.getType()) {
            case ENCRYPTED:
            case TEXT:
                result = attributeRepository.create(dslId, ordering, attribute.getName(), attribute.getType(),
                        null, null);
                break;
            case CHANGE:
            case DSL:
                result = attributeRepository.create(dslId, ordering, attribute.getName(), attribute.getType(),
                        attribute.getDataSetListReference().getId(), null);
                break;
            case LIST:
                ArrayList<String> valueNames = new ArrayList<>();
                attribute.getListValues().forEach(listValue ->
                        valueNames.add(listValue.getName())
                );
                result = attributeRepository.create(dslId, ordering, attribute.getName(), AttributeType.LIST,
                        null, valueNames);
                break;
            case FILE:
                result = attributeRepository.create(dslId, ordering, attribute.getName(), AttributeType.FILE,
                        null, null);
                break;
            default:
                log.error("Attribute " + attribute.getName() + " invalid type " + attribute.getType() + " exception'");
                throw new AttributeTypeException(attribute.getName(), attribute.getType().toString());
        }
        return result;
    }

    @Override
    public void updateOrdering(@Nonnull List<Pair<UUID, Integer>> attributesOrdering) {
        attributeRepository.updateOrdering(attributesOrdering);
    }

    @Override
    public void saveAttributeSortConfigurationForUser(@Nonnull UUID userId, boolean isSortEnabled) {
        UserSettingsEntity sortEnabledEntity = new UserSettingsEntity();
        sortEnabledEntity.setUserId(userId);
        if (isSortEnabled) {
            sortEnabledEntity.setAttributesSortType(AttributesSortType.SORT_BY_NAME);
        } else {
            sortEnabledEntity.setAttributesSortType(AttributesSortType.UNSORTED);
        }
        jpaAttributesSortEnabledRepository.save(sortEnabledEntity);
    }

    @Override
    public UserSettingsEntity getAttributeSortConfigurationForUser(@Nonnull UUID userId) {
        return jpaAttributesSortEnabledRepository.findByUserId(userId);
    }

    /**
     * Filtering attribute values.
     */
    @Override
    public Map<String, List<UUID>> getParametersAndDataSetIdsForAttributeSorting(@NotNull UiManDataSetList tree,
                                                                                 @Nonnull UUID dataSetListId,
                                                                                 @Nonnull UUID targetAttrId,
                                                                                 @Nonnull List<UUID> attrFilterIds) {

        Map<String, List<UUID>> mapAttributeValuesAndDataSetId = new HashMap<>();
        List<UiManParameter> attributeParameters = null;
        boolean isMultiplyAttr = false;

        List<UiManAttribute> attrs = getLastLevelAttributes(tree, attrFilterIds, targetAttrId);
        for (UiManAttribute attr : attrs) {
            if (targetAttrId.equals(attr.getId())) {
                attributeParameters = attr.getParameters();
                isMultiplyAttr = CHANGE.equals(attr.getSource().getType());
                break;
            }
        }
        log.info("The targetAttribute Parameters have been parsed.");
        for (UiManParameter targetAttributeParameter : attributeParameters) {
            String paramValue = targetAttributeParameter.getValue().toString();
            UUID dataSetId = targetAttributeParameter.getDataSet();
            if (isMultiplyAttr) {
                paramValue = convertParamValueAsMultiply(paramValue, targetAttrId);
            }
            fillMapAttributeValuesAndDataSetIds(mapAttributeValuesAndDataSetId, paramValue, dataSetId);
        }
        log.info("Attribute values parsing done.");
        return mapAttributeValuesAndDataSetId;
    }

    private List<UiManAttribute> getLastLevelAttributes(@NotNull UiManDataSetList tree, List<UUID> attrFilterIds,
                                                        UUID targetAttrId) {
        List<UiManAttribute> allTreeAttrs = tree.getAttributes();
        List<UiManAttribute> targetAttrs;
        if (targetAttrId.equals(attrFilterIds.get(0))) {
            log.info("First attrFilterId is equivalent: " + targetAttrId);
            targetAttrs = tree.getAttributes();
        } else {
            log.info("First attrFilterIds is not equivalent: " + targetAttrId);
            targetAttrs = getNestedObject(attrFilterIds, allTreeAttrs);
        }
        return targetAttrs;
    }

    private List<UiManAttribute> getNestedObject(List<UUID> attrPathIds, List<UiManAttribute> attrs) {
        if (attrPathIds.size() == 0) {
            return attrs;
        }
        UUID targetAttrId = attrPathIds.get(0);
        for (UiManAttribute attr : attrs) {
            if (attr.getId().equals(targetAttrId)) {
                attrPathIds.remove(0);
                attrs = getNestedObject(attrPathIds, attr.getAttributes());
                break;
            }
        }
        return attrs;
    }

    private String convertParamValueAsMultiply(String paramValue, UUID targetAttrId) {
        UiManDataSetList options = (UiManDataSetList) getOptions(targetAttrId);
        List<UiManDataSet> dsValues = options.getDataSets();
        paramValue = paramValue.replace(multiplyPrefix, "").trim();
        for (UiManDataSet dsValue : dsValues) {
            String dsId = dsValue.getId().toString();
            String dsName = dsValue.getName();
            if (paramValue.contains(dsId)) {
                paramValue = paramValue.replace(dsId, dsName);
            }
        }
        log.info("Dataset names for Multiply attribute have been changed.");
        return paramValue;
    }

    private void fillMapAttributeValuesAndDataSetIds(Map<String, List<UUID>> mapAttributeValuesAndDataSetId,
                                                     String paramValue, UUID dataSetId) {
        if (Strings.isNonEmpty(paramValue)) {
            List<UUID> currentIds = mapAttributeValuesAndDataSetId.get(paramValue);
            if (Objects.nonNull(currentIds)) {
                currentIds.add(dataSetId);
                mapAttributeValuesAndDataSetId.put(paramValue, currentIds);
            } else {
                List<UUID> newList = new ArrayList<>();
                newList.add(dataSetId);
                mapAttributeValuesAndDataSetId.put(paramValue, newList);
            }
        }
    }
}

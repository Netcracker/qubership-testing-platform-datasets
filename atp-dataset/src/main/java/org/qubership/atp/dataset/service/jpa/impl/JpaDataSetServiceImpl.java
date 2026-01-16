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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.attribute.AttributeParentDslCopyException;
import org.qubership.atp.dataset.exception.attribute.AttributeParentDslNotExistException;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.exception.dataset.DataSetLockedException;
import org.qubership.atp.dataset.exception.dataset.DataSetNotFoundException;
import org.qubership.atp.dataset.exception.dataset.DataSetPositionException;
import org.qubership.atp.dataset.exception.dataset.DataSetSerializeItfContextException;
import org.qubership.atp.dataset.model.enums.DetailedComparisonStatus;
import org.qubership.atp.dataset.service.direct.CompareService;
import org.qubership.atp.dataset.service.direct.DateAuditorService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.jpa.ParameterValue;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.CacheCleanupService;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetTree;
import org.qubership.atp.dataset.service.jpa.model.tree.ds.itf.ItfContextSerializer;
import org.qubership.atp.dataset.service.rest.dto.manager.AbstractEntityResponse;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.client.MacrosFeignClient;
import org.qubership.atp.macros.core.clients.api.dto.macros.MacrosDto;
import org.qubership.atp.macros.core.converter.MacrosDtoConvertService;
import org.qubership.atp.macros.core.model.Macros;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JpaDataSetServiceImpl implements JpaDataSetService {

    private static final Logger LOG = LoggerFactory.getLogger(JpaDataSetServiceImpl.class);
    @Autowired
    protected JpaDataSetListService dataSetListService;
    @Autowired
    protected JpaParameterService jpaParameterService;
    @Autowired
    protected DataSetParameterProvider dataSetParameterProvider;
    @Autowired
    protected MacroContextService macroContextService;
    @Autowired
    protected ModelsProvider modelsProvider;
    @Autowired
    protected CacheCleanupService cacheCleanupService;
    @Autowired
    @Lazy
    protected JpaDataSetService self;
    @Autowired
    protected MacrosFeignClient macrosFeignClient;
    @Autowired
    protected MacrosCalculator macrosCalculator;
    @Autowired
    protected DataSetListSnapshotService dataSetListSnapshotService;
    @Autowired
    protected DateAuditorService dateAuditorService;
    @Autowired
    protected CompareService compareService;
    @Autowired
    protected JpaAttributeService jpaAttributeService;
    @Autowired
    protected GridFsService gridFsService;
    protected ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public DataSetTree getDataSetTreeInAtpFormat(UUID dataSetId, boolean evaluate,
                                                 String atpContextString, ContextType contextType) {
        DataSetListContext dataSetListContext = self.getDatasetListContext(dataSetId);
        MacroContext macroContext = new MacroContext();
        macroContext.setMacroContextService(macroContextService);
        macroContext.setMacrosCalculator(macrosCalculator);

        List<MacrosDto> macrosDtoList = macrosFeignClient
                .findAllByProject(dataSetListContext.getVisibilityAreaId()).getBody();
        List<Macros> macros = new MacrosDtoConvertService().convertList(macrosDtoList, Macros.class);
        macroContext.setMacros(macros);

        macroContext.setDataSetListContext(dataSetListContext);
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
        macroContext.fillAtpDataSetContext(dataSet);

        if (StringUtils.isNotEmpty(atpContextString)) {
            macroContext.addAtpDataSetContext(atpContextString);
        }
        return getTree(dataSetId, evaluate, macroContext, contextType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JSONObject> getDataSetTreeInAtpFormat(UUID dataSetId,
                                                      boolean evaluate,
                                                      String atpContextString,
                                                      ContextType contextType,
                                                      Integer numberOfCopies) {
        log.info("getDataSetTreeInAtpFormat start. numberOfCopies={}", numberOfCopies);
        DataSetListContext dataSetListContext = self.getDatasetListContext(dataSetId);
        List<JSONObject> result = new ArrayList<>();
        int cnt = numberOfCopies;
        List<MacrosDto> macrosDtoList = macrosFeignClient
                .findNonTechnicalMacrosByProject(dataSetListContext.getVisibilityAreaId()).getBody();
        if (null != macrosDtoList) {
            log.info("size for macrosDtoList received is: {}", macrosDtoList.size());
        }
        List<Macros> macros = new MacrosDtoConvertService().convertList(macrosDtoList, Macros.class);
        while (cnt > 0) {
            MacroContext macroContext = new MacroContext();
            macroContext.setMacroContextService(macroContextService);
            macroContext.setDataSetListContext(dataSetListContext);
            macroContext.setMacrosCalculator(macrosCalculator);
            macroContext.setMacros(macros);
            if (StringUtils.isNotEmpty(atpContextString)) {
                macroContext.addAtpDataSetContext(atpContextString);
            }
            DataSetTree tree = getTree(dataSetId, evaluate, macroContext, contextType);
            try {
                result.add(objectMapper.readValue(objectMapper.writeValueAsBytes(tree), JSONObject.class));
            } catch (Exception e) {
                log.error("Cannot write/read tree", e);
                result.add(new JSONObject());
            }
            cleanCache();
            cnt--;
        }
        log.info("getDataSetTreeInAtpFormat end. result.size = {}", result.size());
        return result;
    }

    private void cleanCache() {
        cacheCleanupService.cleanMacroContextCache();
    }

    @Override
    @Cacheable(value = CacheEnum.Constants.DATASET_LIST_CONTEXT_CACHE, key = "#dataSetId")
    public DataSetListContext getDatasetListContext(UUID dataSetId) {
        UUID dslId = modelsProvider.getDatasetListIdByDatasetId(dataSetId);
        return dataSetListService.getDataSetListContext(dslId, Collections.singletonList(dataSetId));
    }

    @Override
    @Transactional
    public void remove(UUID dataSetId) {
        DataSetList object = modelsProvider.getDataSetListById(dataSetId);
        if (object != null) {
            object.remove();
        }
    }

    @Override
    public String getDataSetTreeInItfFormat(UUID dataSetId) {
        DataSetTree dataSetTreeInAtpFormat = self.getDataSetTreeInAtpFormat(dataSetId, true, null, ContextType.OBJECT);
        return serializeToItfJson(dataSetTreeInAtpFormat);
    }

    @Override
    @Transactional
    public DataSet create(String name, UUID dataSetListId) {
        for (String dataSetName : modelsProvider.getDsNamesForDsl(dataSetListId)) {
            if (dataSetName.equalsIgnoreCase(name)) {
                log.error("Dataset with name '" + name + "' already exists");
                throw new DataSetExistsException(name);
            }
        }
        return createDataSet(name, dataSetListId, true);
    }

    @Override
    @Transactional
    public DataSet createDsSelectJavers(String name, UUID dataSetListId, boolean isJavers) {
        return createDataSet(name, dataSetListId, isJavers);
    }

    private DataSet createDataSet(String name, UUID dataSetListId, boolean isJavers) {
        DataSetList dataSetList = dataSetListService.getById(dataSetListId);
        DataSet dataSet = dataSetList.createDataSet(name);
        if (isJavers) {
            log.debug("Javers activated");
            dataSetListSnapshotService.commitEntity(dataSetListId);
        }
        return dataSet;
    }

    @Override
    @Transactional
    public DataSet replicate(UUID id, String name, UUID dataSetListId, Long ordering, UUID sourceId, Boolean isLocked)
            throws DataSetServiceException {
        return modelsProvider.replicateDataSet(id, name, dataSetListId, ordering, sourceId, isLocked);
    }

    /**
     * Sets parameter value by attribute id. Value can be String, UUID, or File.
     */
    @Override
    @Transactional
    public Parameter setParameterValue(ParameterValue value, UUID dataSetId, UUID attributeId) {
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
        Parameter parameter = dataSet.getParameterByAttributeId(attributeId);
        if (parameter == null) {
            parameter = dataSet.createParameter(attributeId);
        }
        Attribute attribute = parameter.getAttribute();
        resolveAndSetParameterValue(parameter, attribute.getAttributeType(), value);
        return parameter;
    }

    /**
     * Sets DataSet flag locked by DataSet id.
     */
    @Override
    @Transactional
    public void lock(UUID dataSetListId, List<UUID> datasetIds, boolean isLock) {
        datasetIds.forEach(dsId -> {
            DataSet dataSet = modelsProvider.getDataSetById(dsId);
            dataSet.setLocked(isLock);
            dataSet.save();
        });
        dateAuditorService.updateModifiedFields(dataSetListId);
        dataSetListSnapshotService.commitEntity(dataSetListId);
    }

    /**
     * Sets parameter value by attribute id. Value can be String, UUID, or File.
     */
    @Override
    @Transactional
    public void setOverlapValue(ParameterValue value,
                                UUID dataSetId,
                                UUID foreignAttributeId,
                                List<UUID> attributePath) {
        DataSet dataSet = modelsProvider.getDataSetById(dataSetId);
        Parameter overlapParameter = dataSet.createOverlap(foreignAttributeId, attributePath);
        Attribute foreignAttribute = modelsProvider.getAttributeById(foreignAttributeId);
        resolveAndSetParameterValue(overlapParameter, foreignAttribute.getAttributeType(), value);
    }

    @Override
    @Transactional
    public DataSet getById(UUID id) {
        return modelsProvider.getDataSetById(id);
    }

    @Override
    public List<DataSet> getByIds(Collection<UUID> ids) {
        return modelsProvider.getByIds(ids);
    }

    @Override
    @Transactional
    public void save(DataSet dataSet) {
        dataSet.save();
    }

    @Override
    @Transactional
    public void setPosition(UUID dataSetId, Integer position) {
        if (position == null || position < 0) {
            throw new DataSetPositionException();
        }
        DataSet dataSetToMove = modelsProvider.getDataSetById(dataSetId);
        if (dataSetToMove == null) {
            log.error("Dataset '" + dataSetId + "' not found");
            throw new DataSetNotFoundException();
        }
        List<DataSet> dataSets = dataSetToMove.getDataSetList().getDataSets();
        if (position > dataSets.size() - 1) {
            throw new DataSetPositionException();
        }
        dataSets.remove(dataSetToMove);
        dataSets.add(position, dataSetToMove);
        long orderingCounter = 0;
        for (DataSet dataSet : dataSets) {
            dataSet.setOrdering(orderingCounter);
            orderingCounter++;
        }
    }

    @Override
    public List<DataSet> getByNameAndDataSetListId(String name, UUID dataSetList) {
        return modelsProvider.getDataSetByNameAndDataSetListId(name, dataSetList);
    }

    @Override
    public List<DataSet> getByDataSetListId(UUID dataSetList) {
        return modelsProvider.getDataSetByDataSetListId(dataSetList);
    }

    public List<DataSet> getLockedDataSets(UUID dataSetList) {
        return modelsProvider.getLockedDatasets(dataSetList);
    }

    @Override
    public List<DataSet> getByDataSetListIdIn(Collection<UUID> dataSetListIds) {
        return modelsProvider.getByDataSetListIdIn(dataSetListIds);
    }

    @Override
    public List<DataSet> getBySourceAndDataSetListId(UUID sourceId, UUID dataSetList) {
        return modelsProvider.getDataSetBySourceAndDataSetListId(sourceId, dataSetList);
    }

    private void resolveAndSetParameterValue(Parameter parameter,
                                             AttributeTypeName attributeType, ParameterValue value) {
        switch (attributeType) {
            case TEXT:
            case ENCRYPTED:
            case CHANGE:
                parameter.setStringValue(value.getString());
                break;
            case DSL:
                parameter.setDataSetReferenceId(value.getUuid());
                break;
            case LIST:
                parameter.setListValueId(value.getUuid());
                break;
            case FILE:
                parameter.setFileValueId(value.getUuid());
                break;
            default:
                break;
        }
    }

    private DataSetTree getTree(UUID dataSetId, boolean evaluate, MacroContext macroContext, ContextType contextType) {
        DataSetListContext dataSetListContext = macroContext.getDataSetListContext();
        List<DataSetContext> rootDataSets = dataSetListContext.getDataSets();
        for (DataSetContext rootDataSet : rootDataSets) {
            if (rootDataSet.getId().equals(dataSetId)) {
                return new DataSetTree(
                        rootDataSet,
                        rootDataSet.getColumnNumber(),
                        evaluate,
                        macroContext,
                        dataSetListContext,
                        dataSetParameterProvider,
                        contextType
                );
            }
        }
        log.error("Data Set not found. Id='" + dataSetId + "'");
        throw new DataSetNotFoundException();
    }

    private String serializeToItfJson(DataSetTree tree) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(DataSetTree.class, new ItfContextSerializer());
        mapper.registerModule(module);
        try {
            return mapper.writer().writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize itf context", e);
            throw new DataSetSerializeItfContextException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbstractEntityResponse> getDatasetsIdNamesPageByNameAndVaId(String name, UUID visibilityAreaId,
                                                                            Pageable pageable) {
        return modelsProvider.getDatasetsIdNamesPageByNameAndVaId(name, visibilityAreaId, pageable);
    }

    public UUID getDataSetsListIdByDataSetId(UUID dsId) {
        return modelsProvider.getDataSetsListIdByDataSetId(dsId);
    }

    /**
     * Copy values from source ds to target ds.
     * @param sourceDsId source dataset id
     * @param targetDsId target dataset id
     */
    @Transactional
    public void copyDsAttributeValueBulk(UUID sourceDsId, UUID targetDsId) {
        log.debug("copyDsAttributeValueBulk (sourceDsId: {}, targetDsId: {})", sourceDsId, targetDsId);
        List<Attribute> attributes = modelsProvider.getAttributesByDataSetId(sourceDsId);
        attributes.addAll(modelsProvider.getAttributesByDataSetId(targetDsId));
        Map<String, List<Attribute>> groupedAttributes = attributes.stream()
                .collect(Collectors.groupingBy(Attribute::getName, HashMap::new, toList()));
        UUID targetDslId = getById(targetDsId).getDataSetList().getId();
        for (Map.Entry<String, List<Attribute>> entry : groupedAttributes.entrySet()) {
            List<Attribute> values = entry.getValue();
            if (values.size() == 2) {
                UUID leftAttrId = values.get(0).getId();
                UUID rightAttrId = values.get(1).getId();
                if (!DetailedComparisonStatus.EQUAL.equals(
                        compareService.compareAttributeValues(sourceDsId, targetDsId, leftAttrId, rightAttrId))) {
                    // if attribute values not equal then copy
                    copyDsAttributeValueWithoutCommitting(sourceDsId, targetDsId, leftAttrId, rightAttrId);
                }
            } else {
                Attribute attribute = values.get(0);
                if (targetDslId.equals(attribute.getDataSetList().getId())) {
                    copyDsAttributeValueWithoutCommitting(sourceDsId, targetDsId, null, attribute.getId());
                } else {
                    copyDsAttributeValueWithoutCommitting(sourceDsId, targetDsId, attribute.getId(), null);
                }
            }
        }

        List<AttributeKey> attributeKeys = modelsProvider.getAttributesKeyByDataSetId(sourceDsId);
        attributeKeys.addAll(modelsProvider.getAttributesKeyByDataSetId(targetDsId));
        Map<String, List<AttributeKey>> groupedAttributeKeys = attributeKeys.stream()
                .collect(Collectors.groupingBy((AttributeKey attributeKey) ->
                                attributeKey.getName() + "." + attributeKey.getPathNames(), HashMap::new, toList()));

        for (Map.Entry<String, List<AttributeKey>> entry : groupedAttributeKeys.entrySet()) {
            List<AttributeKey> values = entry.getValue();
            if (values.size() == 2) {
                UUID leftAttrId = values.get(0).getId();
                UUID rightAttrId = values.get(1).getId();
                if (!DetailedComparisonStatus.EQUAL.equals(
                        compareService.compareAttributeValues(sourceDsId, targetDsId, leftAttrId, rightAttrId))) {
                    // if attribute values not equal then copy
                    copyDsAttributeValueWithoutCommitting(sourceDsId, targetDsId, leftAttrId, rightAttrId);
                }
            } else {
                AttributeKey attributeKey = values.get(0);
                if (targetDslId.equals(attributeKey.getDataSetList().getId())) {
                    copyDsAttributeValueWithoutCommitting(sourceDsId, targetDsId, null, attributeKey.getId());
                } else {
                    copyDsAttributeValueWithoutCommitting(sourceDsId, targetDsId, attributeKey.getId(), null);
                }
            }
        }
        dataSetListSnapshotService.commitEntity(targetDslId);
    }

    private UUID copyDsAttributeValueWithoutCommitting(UUID sourceDsId, UUID targetDsId,
                                                       @Nullable UUID sourceAttrId, @Nullable UUID targetAttrId) {
        log.debug("copyDsAttributeValue (sourceDsId: {}, targetDsId: {}, sourceAttrId: {}, targetAttrId: {})",
                sourceDsId, targetDsId, sourceAttrId, targetAttrId);
        // lock check
        DataSet targetDataset = modelsProvider.getDataSetById(targetDsId);
        if (Objects.isNull(targetDataset)) {
            log.error("Data Set not found. Id='{}'", targetDsId);
            throw new DataSetNotFoundException();
        }
        if (targetDataset.isLocked()) {
            log.error("Dataset is locked. (Id: {})", targetDsId);
            throw new DataSetLockedException(targetDataset.getName());
        }

        Parameter sourceParameter = modelsProvider.getParameterByAttributeIdAndDataSetId(sourceAttrId, sourceDsId);
        Parameter targetParameter = modelsProvider.getParameterByAttributeIdAndDataSetId(targetAttrId, targetDsId);

        if (Objects.nonNull(sourceParameter) && sourceParameter.isOverlap()
                || Objects.nonNull(targetParameter) && targetParameter.isOverlap()) {
            targetAttrId = copyOverlapParameter(targetDataset, sourceParameter, targetParameter);
        } else {
            // check that attributes are comparable
            // attributes are comparable if they have same types
            if (compareService.isAttributesComparable(sourceAttrId, targetAttrId)) {
                log.debug("Attributes are comparable (sourceAttrId: {}, targetAttrId: {})", sourceAttrId, targetAttrId);
                copyAttributeIfComparable(sourceParameter, targetParameter, targetDsId, targetAttrId);
            } else {
                log.debug("Attributes are not comparable (sourceAttrId: {}, targetAttrId: {})", sourceAttrId,
                        targetAttrId);
                targetAttrId = copyAttributeIfNotComparable(sourceAttrId, sourceParameter, targetDsId, targetAttrId);
            }
        }
        return targetAttrId;
    }

    /**
     * Copy value from source ds to target ds by attr id.
     * If source attribute id and target attribute id are null then do nothing
     * @param sourceDsId source dataset id
     * @param targetDsId target dataset id
     * @param sourceAttrId source attribute id, may be null
     * @param targetAttrId target attribute id, may be null
     * @return targetAttributeId
     */
    @Transactional
    public UUID copyDsAttributeValue(UUID sourceDsId, UUID targetDsId,
                                     @Nullable UUID sourceAttrId, @Nullable UUID targetAttrId) {
        if (!DetailedComparisonStatus.EQUAL.equals(
                compareService.compareAttributeValues(sourceDsId, targetDsId, sourceAttrId, targetAttrId))) {
            targetAttrId = copyDsAttributeValueWithoutCommitting(sourceDsId, targetDsId, sourceAttrId, targetAttrId);
            dataSetListSnapshotService.commitEntity(jpaAttributeService.getById(targetAttrId).getDataSetList().getId());
        }
        return targetAttrId;
    }

    private void copyAttributeIfComparable(@Nullable Parameter sourceParameter, @Nullable Parameter targetParameter,
                                           UUID targetDsId, UUID targetAttrId) {
        if (Objects.isNull(sourceParameter)) {
            // if source parameter not exist
            // then remove target parameter if exist
            if (Objects.nonNull(targetParameter)) {
                log.debug("Remove parameter from target DS. Parameter_id: {}", targetParameter.getId());
                jpaParameterService.removeWithoutCommitting(targetParameter.getId());
            }
        } else {
            AttributeTypeName sourceAttributeType = sourceParameter.getAttribute().getAttributeType();
            UUID newListValueReference = null;
            if (AttributeTypeName.LIST.equals(sourceAttributeType)) {
                newListValueReference = jpaAttributeService.mergeListValuesAndGetListValueReference(
                        sourceParameter.getAttributeId(), targetAttrId, sourceParameter.getListValue());
            }

            Parameter newParameter;
            if (Objects.isNull(targetParameter)) {
                // create new target parameter if not exist
                log.debug("Target parameter not exist - create new one. "
                                + "(targetAttrId: {}, targetDsId: {}, sourceParameterId: {})",
                        targetAttrId, targetDsId, sourceParameter.getId());
                newParameter = jpaParameterService.createParameterWithoutCommitting(targetDsId, targetAttrId,
                        sourceParameter.getStringValue(), sourceParameter.getDataSetReferenceId(),
                        newListValueReference);
            } else {
                // update parameter if exist
                log.debug("Target parameter exist - update parameter value. "
                                + "(sourceParameterId: {}, targetParameterId: {})",
                        sourceParameter.getId(), targetParameter.getId());
                newParameter = jpaParameterService.updateParameterWithoutCommitting(targetParameter.getId(),
                        sourceParameter.getStringValue(), sourceParameter.getDataSetReferenceId(),
                        newListValueReference);
            }
            // copy file
            if (AttributeTypeName.FILE.equals(sourceAttributeType)) {
                gridFsService.copyIfExist(sourceParameter.getId(), newParameter.getId(), false);
            }
        }
    }

    private UUID copyAttributeIfNotComparable(@Nullable UUID sourceAttrId, @Nullable Parameter sourceParameter,
                                              UUID targetDsId, @Nullable UUID targetAttrId) {
        // delete target attribute cascade if exist
        if (Objects.nonNull(targetAttrId) && Objects.nonNull(sourceAttrId)) {
            log.debug("Remove attribute from target DS. (attribute_id: {})", targetAttrId);
            jpaAttributeService.remove(targetAttrId);
        }
        // if source attribute id is null then nothing to do
        // return null as new target attribute id
        if (Objects.isNull(sourceAttrId)) {
            return null;
        }

        // get source attribute
        Attribute sourceAttribute;
        if (Objects.isNull(sourceParameter)) {
            sourceAttribute = modelsProvider.getAttributeById(sourceAttrId);
        } else {
            sourceAttribute = sourceParameter.getAttribute();
        }
        AttributeTypeName sourceAttributeType = sourceAttribute.getAttributeType();

        // create new target attribute
        log.debug("Create new target attribute with type {}", sourceAttributeType);
        UUID targetDataSetListId = getById(targetDsId).getDataSetList().getId();
        Attribute newAttribute = jpaAttributeService.createWithoutCommitting(
                sourceAttribute.getName(), sourceAttributeType, targetDataSetListId);
        targetAttrId = newAttribute.getId();
        if (AttributeTypeName.DSL.equals(sourceAttributeType)) {
            newAttribute.setTypeDataSetListId(sourceAttribute.getTypeDataSetListId());
        }
        // create new parameter if source parameter exist
        if (Objects.nonNull(sourceParameter)) {
            log.debug("Create new parameter in new target attribute (attribute_id: {})", targetAttrId);
            // merge list values
            // and get ListValueReference if applicable
            UUID newListValueReference = null;
            if (AttributeTypeName.LIST.equals(sourceAttributeType)) {
                newListValueReference = jpaAttributeService.mergeListValuesAndGetListValueReference(
                        sourceAttrId, targetAttrId, sourceParameter.getListValue());
            }

            Parameter newParameter = jpaParameterService.createParameterWithoutCommitting(targetDsId, targetAttrId,
                    sourceParameter.getStringValue(), sourceParameter.getDataSetReferenceId(), newListValueReference);
            if (AttributeTypeName.FILE.equals(sourceAttributeType)) {
                gridFsService.copyIfExist(sourceParameter.getId(), newParameter.getId(), false);
            }
        }
        return targetAttrId;
    }

    private UUID copyOverlapParameter(DataSet targetDataset,
                                      @Nullable Parameter sourceParameter,
                                      @Nullable Parameter targetParameter) {

        if (Objects.isNull(sourceParameter) && Objects.nonNull(targetParameter)) {
            UUID attrId = targetParameter.getAttributeKey().getPath().get(0);
            targetParameter.remove();
            return attrId;
        } else if (Objects.isNull(targetParameter)) {

            UUID headLinkAttrId = sourceParameter.getAttributeKey().getPath().get(0);

            Attribute sourceHeadLinkAttr = jpaAttributeService.getById(headLinkAttrId);

            Optional<Attribute> targetHeadLinkAttr = targetDataset.getDataSetList().getAttributes().stream()
                    .filter(attr ->
                            attr.getName().equals(sourceHeadLinkAttr.getName())
                                    && attr.getAttributeType().equals(sourceHeadLinkAttr.getAttributeType()))
                    .findFirst();

            if (!targetHeadLinkAttr.isPresent()) {
                throw new AttributeParentDslNotExistException();
            }

            boolean isDslDifferent = modelsProvider.isDslDifferentAttributes(headLinkAttrId,
                    targetHeadLinkAttr.get().getId());

            if (isDslDifferent) {
                throw new AttributeParentDslCopyException();
            }

            UUID targetAttrId = targetHeadLinkAttr.get().getId();

            List<UUID> path = sourceParameter.getAttributeKey().getPath();
            path.remove(0);
            path.add(0, targetAttrId);

            jpaParameterService.createParameterOverlapWithoutCommitting(
                    sourceParameter.getAttributeKey().getAttributeId(),
                    path, targetDataset.getId(),
                    sourceParameter.getStringValue(), sourceParameter.getDataSetReferenceId(),
                    sourceParameter.getListValueId());

            return targetAttrId;
        } else {

            UUID sourceHeadLinkAttrId = sourceParameter.getAttributeKey().getPath().get(0);
            UUID targetHeadLinkAttrId = targetParameter.getAttributeKey().getPath().get(0);

            boolean isDslDifferent = modelsProvider.isDslDifferentAttributes(sourceHeadLinkAttrId,
                    targetHeadLinkAttrId);

            if (isDslDifferent) {
                throw new AttributeParentDslCopyException();
            }

            jpaParameterService.updateOverlapParameterWithoutCommitting(targetParameter,
                    sourceParameter.getStringValue(), sourceParameter.getDataSetReferenceId(),
                    sourceParameter.getListValueId());

            if (AttributeTypeName.FILE.equals(sourceParameter.getAttributeKey().getAttributeType())) {
                gridFsService.copyIfExist(sourceParameter.getId(), targetParameter.getId(), false);
            }
            return targetHeadLinkAttrId;
        }
    }
}

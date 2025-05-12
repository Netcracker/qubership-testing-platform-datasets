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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.DataSetRepository;
import org.qubership.atp.dataset.db.ParameterRepository;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.AtpDsSerializer;
import org.qubership.atp.dataset.model.utils.CheckedConsumer;
import org.qubership.atp.dataset.model.utils.ObjectShortResponse;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.direct.AtpMacrosCacheService;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.DateAuditorService;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.rest.PaginationResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Service
@Slf4j
public class DataSetServiceImpl implements DataSetService {

    private final ParameterService parameterService;
    private AttributeService attributeService;
    private final DataSetRepository repo;
    private final ParameterRepository paramRepo;
    private final DataSetListRepository dslRepo;
    private final ObjectMapper mapper;
    private final EvaluationService evaluationService;
    private final AtpMacrosCacheService cacheService;
    private final DateAuditorService dateAuditorService;
    private final DataSetListSnapshotService dataSetListSnapshotService;
    private final ModelsProvider modelsProvider;
    private final ClearCacheService clearCacheService;

    @Nonnull
    @Transactional
    @Override
    public DataSet create(@Nonnull UUID dslId, @Nonnull String name) {
        for (String dataSetName : repo.getOccupiedNamesByParentId(dslId)) {
            if (dataSetName.equalsIgnoreCase(name)) {
                log.error("Dataset with name '" + name + "' already exists");
                throw new DataSetExistsException(name);
            }
        }
        DataSet dataSet = repo.create(dslId, name);
        dateAuditorService.updateModifiedFields(dslId);
        dataSetListSnapshotService.commitEntity(dslId);
        return dataSet;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public ObjectNode getInItfFormat(@Nonnull MixInId id) {
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            DataSet ds = evaluator.getDataSetById(id);
            if (ds == null) {
                return null;
            }
            return Utils.serializeInItfWay(ds, mapper, evaluator);
        }
    }

    @Nullable
    @Override
    public CheckedConsumer<OutputStream, IOException> writeInAtpFormat(@Nonnull MixInId id,
                                                                       @Nullable Map<String, String> context,
                                                                       boolean evaluate) {
        DsEvaluator evaluator = evaluationService.getEvaluator(evaluate, false);
        DataSet ds;
        try {
            ds = evaluator.getDataSetById(id);
            if (ds == null) {
                evaluator.close();
                return null;
            }
        } catch (Exception e) {
            evaluator.close();
            throw e;
        }
        return outputStream -> {
            try {
                cacheService.setContext(context);
                AtpDsSerializer.writeValue(outputStream, ds, evaluator);
            } finally {
                evaluator.close();
                cacheService.destroy();
            }
        };
    }

    @Nullable
    @Override
    public DataSet get(@Nonnull UUID id) {
        return repo.getById(id);
    }

    @Override
    public boolean existsById(@NotNull UUID id) {
        return repo.existsById(id);
    }

    @Nonnull
    @Override
    public List<DataSet> getAll() {
        return repo.getAll();
    }

    /**
     * Returns bulky {@link List} of {@link DataSet}.
     *
     * @param dataSetsIds {@link List} of data sets ids
     * @return {@link List} of {@link DataSet}
     */
    @Nonnull
    @Override
    public List<DataSet> getAll(@Nonnull List<UUID> dataSetsIds) {
        return repo.getAll(dataSetsIds);
    }

    /**
     * Returns {@link List} of {@link DataSet} from DSL which contains source attribute.
     * In case attrPathIds not empty source attribute id is first id in attrPathIds list.
     * In case attrPtathIds is empty source attribute id equals to target attribute id.
     *
     * @return {@link List} of {@link DataSet}
     */
    @Nonnull
    @Override
    public List<DataSet> getAllDslDsByAttribute(UUID targetAttrId, List<UUID> attrPathIds) {

        UUID sourceAttributeId;
        log.debug("get all related datasets by attribute {} and attrPathIds {}",
                targetAttrId, attrPathIds);
        if (!isEmpty(attrPathIds)) {
            sourceAttributeId = attrPathIds.get(0);
        } else {
            sourceAttributeId = targetAttrId;
        }
        log.debug("get all related datasets for attribute {}, source attribute {}", targetAttrId, sourceAttributeId);
        Attribute attribute = attributeService.get(sourceAttributeId);
        Preconditions.checkNotNull(attribute, "Attribute with id {} not found",
                sourceAttributeId);
        DataSetList dsl = attribute.getDataSetList();
        return isNull(dsl.getDataSets()) ? Collections.emptyList() : dsl.getDataSets();
    }

    /**
     * Renames dataset.
     */
    @Transactional
    @Override
    public void rename(@Nonnull UUID id, @Nonnull String name) {
        DataSet dataSet = repo.getById(id);
        if (dataSet != null) {
            Preconditions.checkArgument(!dataSet.isLocked(), "Can not rename dataSet with name '%s',"
                    + " id : %s because dataset locked", dataSet.getName(), dataSet.getId());
            for (String dataSetName : repo.getOccupiedNamesByParentId(dataSet.getDataSetList().getId())) {
                if (dataSetName.equals(name)) {
                    throw new DataSetExistsException(name);
                }
            }
            Preconditions.checkArgument(repo.rename(id, name),
                    "Can not update name to [%s] of data set %s", name, id);
            DataSetList dataSetList = dataSet.getDataSetList();
            dateAuditorService.updateModifiedFields(dataSetList.getId());
            dataSetListSnapshotService.commitEntity(dataSetList.getId());
            evictAllAffectedDatasetsFromContextCacheByDsId(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void lock(UUID dataSetListId, @Nonnull List<UUID> uuids, boolean isLock) {
        log.info("Lock or Unlock changes to Datasets by Uuids: {}, locked flag: {}", uuids, isLock);
        if (!uuids.isEmpty()) {
            Preconditions.checkArgument(repo.lock(uuids, isLock),
                    "Can not update lock ds ids to %s of lock flag %s, dataSetList id %s",
                    uuids, isLock, dataSetListId);
            dateAuditorService.updateModifiedFields(dataSetListId);
            dataSetListSnapshotService.commitEntity(dataSetListId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void delete(@Nonnull UUID dataSetId) {
        DataSet dataSet = repo.getById(dataSetId);
        UUID dataSetListId = null;
        if (dataSet != null) {
            dataSetListId = dataSet.getDataSetList().getId();
            Preconditions.checkArgument(!dataSet.isLocked(), "Can not delete dataSet with name '%s',"
                    + " id : %s because dataset locked", dataSet.getName(), dataSet.getId());
        }
        evictAllAffectedDatasetsFromContextCacheByDsId(dataSetId);
        Preconditions.checkArgument(repo.delete(dataSetId), "Can not delete data set %s", dataSetId);
        if (dataSetListId != null) {
            dateAuditorService.updateModifiedFields(dataSetListId);
            dataSetListSnapshotService.findAndCommitIfExists(dataSetListId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public DataSet copy(@Nonnull UUID dataSetId, @Nonnull String name) {
        DataSet dataSetToCopy = get(dataSetId);
        UUID dataSetListId = dataSetToCopy.getDataSetList().getId();
        DataSet newDataSet = createDsWithParamsAndLabels(dataSetToCopy, name, dataSetListId, null);
        newDataSet.setLocked(false);
        dataSetListSnapshotService.commitEntity(newDataSet.getDataSetList().getId());
        return newDataSet;
    }

    /**
     * {@inheritDoc}
     */
    public DataSet copy(@Nonnull UUID dataSetId,
                        @Nonnull UUID dataSetListId,
                        @Nonnull Map<UUID, UUID> attributes) {
        DataSet dataSetToCopy = get(dataSetId);
        String name = dataSetToCopy.getName();
        return createDsWithParamsAndLabels(dataSetToCopy, name, dataSetListId, attributes);
    }

    /**
     * {@inheritDoc}
     */
    public DataSet copy(@Nonnull DataSet dataSet,
                        @Nonnull UUID dataSetListId,
                        @Nonnull Map<UUID, UUID> attributes) {
        return createDsWithParamsAndLabels(dataSet, dataSet.getName(), dataSetListId, attributes);
    }

    /**
     * Created dataset in dataset list with parameters and labels from parent dataset.
     *
     * @param dataSetToCopy - dataset to copy.
     * @param name - name on new ds.
     * @param dataSetListId - id of dsl where to copy.
     * @param attributes - mapping of old and new attributes if ds copied to another dsl.
     * @return data set with parameters and labels.
     */
    private DataSet createDsWithParamsAndLabels(DataSet dataSetToCopy,
                                                String name,
                                                UUID dataSetListId,
                                                @Nullable Map<UUID, UUID> attributes) {
        Preconditions.checkNotNull(dataSetToCopy, "Can not find dataSet to copy", name);
        DataSet newDataSet = create(dataSetListId, name);
        for (Parameter param : dataSetToCopy.getParameters()) {
            if (attributes != null) {
                parameterService.copy(newDataSet, param, attributes); //for copy dsl
            } else {
                parameterService.copy(newDataSet, param, null); //for copy ds
            }
        }
        for (Label label : dataSetToCopy.getLabels()) {
            repo.mark(newDataSet.getId(), label.getName());
        }
        return newDataSet;
    }

    @Nullable
    @Override
    public UiManAttribute getParametersOnAttributePath(@Nonnull UUID dsId,
                                                       @Nonnull List<UUID> attrPath,
                                                       boolean evaluate) {
        if (attrPath.isEmpty()) {
            return null;
        }
        DataSet ds = get(dsId);
        if (ds == null) {
            return null;
        }
        try (DsEvaluator evaluator = evaluationService.getEvaluator(evaluate, true)) {
            return Utils.doUiAttr(ds, evaluator, attrPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<?> getOverlapContainers(@Nonnull UUID dataSetId, @Nonnull UUID attributeId, boolean withInfo) {
        DataSet currentDs = get(dataSetId);
        Preconditions.checkNotNull(currentDs, "No data set found by id: %s", dataSetId);
        List<ParameterOverlap> res = parameterService
                .getOverlaps(attributeId, new OverlapPredicate(currentDs))
                .collect(Collectors.toList());
        if (withInfo) {
            return res.stream().map(TableResponse::fromParameterOverlap)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            return res.stream().map(Parameter::getDataSet)
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Finds dataSets with provided dataSet id.
     */
    @Override
    public List<?> getAffectedDataSetsByChangesDataSetReference(@Nonnull UUID dataSetId, boolean deleteDs) {
        if (deleteDs) {
            return repo.getAffectedInfoByChangesDataSetReference(dataSetId);
        } else {
            return repo.getAffectedDataSetsByChangesDataSetReference(dataSetId);
        }
    }

    @Transactional
    @Override
    public void deleteAllParameterOverlaps(UUID attributeId, List<UUID> dataSetsIds) {
        removeFromListDsLocked(attributeId, dataSetsIds);
        paramRepo.deleteOverlaps(attributeId, path -> dataSetsIds.contains(path.getDataSet().getId()));
        dataSetsIds
                .stream()
                .map(dataSetsId -> repo.getById(dataSetsId))
                .collect(Collectors.groupingBy(DataSet::getDataSetList))
                .keySet()
                .forEach(dataSetList -> {
                    dateAuditorService.updateModifiedFields(dataSetList.getId());
                    dataSetListSnapshotService.commitEntity(dataSetList.getId());
                });
    }

    private void removeFromListDsLocked(UUID attributeId, List<UUID> dataSetsIds) {
        List<UUID> filteredDsLocked = dataSetsIds.stream()
                .filter(dsId -> {
                    DataSet dataSet = repo.getById(dsId);
                    return isNull(dataSet) || dataSet.isLocked();
                }).collect(Collectors.toList());
        dataSetsIds.removeAll(filteredDsLocked);
        log.info("Remove for skip locked datasets id:{}, attribute id: {}", filteredDsLocked, attributeId);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    @CacheEvict(value = CacheEnum.Constants.DATASET_LIST_CONTEXT_CACHE, key = "#dsId")
    public Parameter deleteParameterOverlap(@Nonnull UUID dsId,
                                            @Nonnull UUID targetAttrId,
                                            @Nonnull List<UUID> attrPathIds) {
        DataSet ds = get(dsId);
        Preconditions.checkArgument(ds != null, "No data set found by id: %s", dsId);
        Preconditions.checkArgument(!ds.isLocked(), "Can not delete ParameterOverlap for dataSet"
                + " with name '%s', id: %s because dataset locked", ds.getName(), ds.getId());
        paramRepo.deleteOverlap(ds.getDataSetList().getId(), dsId, targetAttrId, attrPathIds);
        Parameter parameter = OverlapIterator.create(ds, targetAttrId, attrPathIds).next().getParameter().orElse(null);
        if (parameter != null) {
            dateAuditorService.updateModifiedFields(parameter.getDataSet().getDataSetList().getId());
        }
        dataSetListSnapshotService.findAndCommitIfExists(ds.getDataSetList().getId());
        return parameter;
    }

    @Nonnull
    @Override
    public Stream<DataSet> getByParentId(UUID dataSetListId, boolean evaluate, @Nullable String labelName) {
        DataSetList parent = dslRepo.getById(dataSetListId);
        if (parent == null) {
            log.warn("No dsl found by id: " + dataSetListId);
            return Stream.empty();
        }

        DsEvaluator evaluator = evaluationService.getEvaluator(evaluate, false);
        List<DataSet> children;
        if (labelName == null) {
            children = repo.getByParentId(dataSetListId);
        } else {
            children = repo.getByParentIdAndLabel(dataSetListId, labelName);
        }
        return evaluator.getDataSets(parent, children.stream());
    }

    public List<ObjectShortResponse> getByParentId(UUID dataSetListId) {
        return repo.getByParentId(dataSetListId).stream().map(dataSet -> new ObjectShortResponse(dataSet.getId(),
                dataSet.getName(), dataSet.isLocked())).collect(Collectors.toList());
    }

    /**
     * Adds new dataSetLabel with name provided.
     */
    @Transactional
    @Override
    public Label mark(@Nonnull UUID dataSetId, @Nonnull String name) {
        Label label = repo.mark(dataSetId, name);
        DataSet dataset = repo.getById(dataSetId);
        if (dataset != null) {
            dateAuditorService.updateModifiedFields(dataset.getDataSetList().getId());
        }
        dataSetListSnapshotService.commitEntity(dataset.getDataSetList().getId());
        return label;
    }

    /**
     * Returns dataSetLabels.
     */
    @Override
    public List<Label> getLabels(@Nonnull UUID dataSetId) {
        return repo.getLabels(dataSetId);
    }

    /**
     * Deletes dataSetLabel.
     */
    @Transactional
    @Override
    public boolean unmark(@Nonnull UUID dataSetId, @Nonnull UUID labelId) {
        boolean isUnmarked = repo.unmark(dataSetId, labelId);
        if (isUnmarked) {
            DataSet dataset = repo.getById(dataSetId);
            if (dataset != null) {
                dateAuditorService.updateModifiedFields(dataset.getDataSetList().getId());
                dataSetListSnapshotService.commitEntity(dataset.getDataSetList().getId());
            }
        }
        return isUnmarked;
    }

    /**
     * Restores dataSet.
     */
    @Transactional
    @Override
    public boolean restore(@Nonnull JsonNode dataSetJson) {
        UUID dataSetId = UUID.fromString(dataSetJson.get("id").asText());
        String dataSetName = dataSetJson.get("name").asText();
        UUID dataSetListId = UUID.fromString(dataSetJson.get("dataSetList").asText());
        UUID previousDataSetId = dataSetJson.get("previousDataSet") == null ? null :
                UUID.fromString(dataSetJson.get("previousDataSet").asText());
        DataSet dataSetDb = get(dataSetId);
        if (nonNull(dataSetDb)) {
            Preconditions.checkArgument(!dataSetDb.isLocked(), "Can not restore dataSet"
                    + " with name: '%s', id: %s because dataset locked", dataSetDb.getName(), dataSetDb.getId());
        }
        boolean ds = repo.restore(dataSetListId, dataSetId, dataSetName, previousDataSetId);
        if (ds) {
            JsonNode parametersNode = dataSetJson.get("parameters");
            if (parametersNode != null) {
                Iterator<JsonNode> parametersIterator = parametersNode.elements();
                while (parametersIterator.hasNext()) {
                    JsonNode parameterNode = parametersIterator.next();
                    UUID attribute = UUID.fromString(parameterNode.get("attribute").asText());
                    String text = parameterNode.get("text").asText();
                    UUID listValue = parameterNode.get("listValue") == null ? null :
                            UUID.fromString(parameterNode.get("listValue").asText());
                    UUID dataSetReference = parameterNode.get("dataSetReference") == null ? null :
                            UUID.fromString(parameterNode.get("dataSetReference").asText());
                    List<UUID> attrPathIds = null;
                    JsonNode attrKeyNode = parameterNode.get("attrKey");
                    if (attrKeyNode != null) {
                        Iterator<JsonNode> attrKeyIterator = attrKeyNode.elements();
                        attrPathIds = new ArrayList<>();
                        while (attrKeyIterator.hasNext()) {
                            attrPathIds.add(UUID.fromString(attrKeyIterator.next().asText()));
                        }
                    }
                    parameterService.set(dataSetId, attribute, attrPathIds, text, dataSetReference, listValue);
                }
            }
            JsonNode labelsNode = dataSetJson.get("labels");
            if (labelsNode != null) {
                Iterator<JsonNode> labelsIterator = labelsNode.elements();
                while (labelsIterator.hasNext()) {
                    repo.mark(dataSetId, labelsIterator.next().get("name").asText());
                }
            }
            dataSetListSnapshotService.findAndCommitIfExists(dataSetListId);
        }
        return true;
    }

    @Override
    public PaginationResponse<TableResponse> getAffectedDataSets(UUID dataSetId, Integer page, Integer size) {
        final PageRequest pageRequest = nonNull(page) && nonNull(size) ? PageRequest.of(page, size) : null;

        return modelsProvider.getParametersByDatasetId(dataSetId, pageRequest);
    }

    @Override
    public Long getAffectedDataSetsCount(UUID dataSetId) {
        return modelsProvider.getParametersByDatasetId(dataSetId, null).getTotalCount();
    }

    private class OverlapPredicate implements Predicate<AttributePath> {

        private final DataSet currentDs;

        public OverlapPredicate(DataSet currentDs) {
            this.currentDs = currentDs;
        }

        @Override
        public boolean test(AttributePath attributePath) {
            OverlapIterator iterator = OverlapIterator.from(attributePath);
            OverlapItem original = null;
            while (iterator.hasNext()) {
                original = iterator.next();
            }
            assert original != null;
            return original.isReachable() && currentDs.equals(original.asReachable().getTargetDs());
        }
    }

    /**
     * Evict affected datasets from ATP_DATASETS_DATASET_LIST_CONTEXT_CACHE cache.
     *
     * @param updatedDataSetListId updated dataset list id
     */
    public void evictAllAffectedDatasetsFromContextCacheByDslId(UUID updatedDataSetListId) {
        Set<UUID> affectedDataSetIds = collectAffectedDatasetsByDslId(updatedDataSetListId);
        clearCacheService.evictDatasetListContextCache(affectedDataSetIds);
    }

    /**
     * Evict affected datasets from ATP_DATASETS_DATASET_LIST_CONTEXT_CACHE cache.
     *
     * @param updatedDataSetId updated dataset id
     */
    public void evictAllAffectedDatasetsFromContextCacheByDsId(UUID updatedDataSetId) {
        Set<UUID> affectedDataSetIds = collectAffectedDatasetsByDsId(updatedDataSetId);
        clearCacheService.evictDatasetListContextCache(affectedDataSetIds);
    }

    /**
     * Collects all affected DataSet Ids recursively.
     * Used then dsl updated or attribute created/updated/deleted
     *
     * @param updatedDataSetListId updated dataSetList id
     * @return set of updated dataset ids include updatedDataSetId
     */
    @Nonnull
    public Set<UUID> collectAffectedDatasetsByDslId(UUID updatedDataSetListId) {
        Set<UUID> affectedDatasetListIds = new HashSet<>();
        affectedDatasetListIds.add(updatedDataSetListId);
        List<UUID> datasetListIds = modelsProvider.getAffectedDataSetsListIdsByDataSetListId(
                Collections.singletonList(updatedDataSetListId));
        affectedDatasetListIds.addAll(datasetListIds);
        boolean hasNext = !datasetListIds.isEmpty();
        while (hasNext) {
            datasetListIds =
                    modelsProvider.getAffectedDataSetsListIdsByDataSetListId(datasetListIds);

            affectedDatasetListIds.addAll(datasetListIds);
            hasNext = !datasetListIds.isEmpty();
        }
        return modelsProvider.getAffectedDataSetsIdsByDataSetListId(affectedDatasetListIds);
    }

    /**
     * Collects all affected DataSet Ids recursively.
     * Used then dataset updated or parameter updated
     *
     * @param updatedDataSetId updated dataSet id
     * @return set of updated dataset ids include updatedDataSetId
     */
    @Nonnull
    public Set<UUID> collectAffectedDatasetsByDsId(UUID updatedDataSetId) {
        Set<UUID> affectedDataSetIds = new HashSet<>();
        affectedDataSetIds.add(updatedDataSetId);
        modelsProvider.getUniqueDataSetIdsByReferenceDataSetId(updatedDataSetId)
                .stream()
                .map(this::collectAffectedDatasetsByDsId)
                .forEach(affectedDataSetIds::addAll);
        return affectedDataSetIds;
    }
}

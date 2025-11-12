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

import static java.util.Objects.nonNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.DataSetListTreeRepository;
import org.qubership.atp.dataset.db.TestPlanRepository;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.datasetlist.DataSetListAndTestPlanException;
import org.qubership.atp.dataset.exception.datasetlist.DataSetListExistsException;
import org.qubership.atp.dataset.exception.datasetlist.DataSetListNotFoundException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.api.saga.requests.RevertRequest;
import org.qubership.atp.dataset.model.impl.FlatDataImpl;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.DatasetResponse;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.dataset.service.rest.PaginationResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.AffectedDataSetList;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.client.MacrosFeignClient;
import org.qubership.atp.macros.core.clients.api.dto.macros.MacrosDto;
import org.qubership.atp.macros.core.converter.MacrosDtoConvertService;
import org.qubership.atp.macros.core.model.Macros;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class DataSetListServiceImpl implements DataSetListService {

    private final DataSetService dsService;
    private final DataSetListRepository repo;
    private final EvaluationService evaluationService;
    private final AttributeService attributeService;
    private final DataSetListTreeRepository dslTreeRepo;
    private final TestPlanRepository testPlanRepo;
    private final Provider<UserInfo> userInfoProvider;
    private final DataSetListSnapshotService dataSetListSnapshotService;
    private final ModelsProvider modelsProvider;
    private final MacrosCalculator macrosCalculator;
    private final MacroContextService macroContextService;
    private final JpaDataSetListService dataSetListService;
    private final MacrosFeignClient macrosFeignClient;
    private final DataSetParameterProvider dataSetParameterProvider;
    private final ClearCacheService clearCacheService;

    @Value("${feign.atp.macros.url}")
    private String macroFeignUrl;

    @Value("${feign.atp.macros.route}")
    private String macroFeignRoute;

    @Nullable
    @Override
    public FlatDataImpl getAsFlat(UUID id, boolean evaluate) {
        DataSetList result = get(id);
        if (result == null) {
            return null;
        }
        FlatDataImpl flatData = Utils.doFlatData(result);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(evaluate, true)) {
            flatData.getParameters().forEach(param ->
                    param.setText(evaluator.apply(param)
                            .map(Object::toString).orElse("")));
        }
        return flatData;
    }

    /**
     * To get as tree with filter by datasets and attributes.
     */
    @Nullable
    @Override
    public UiManDataSetList getAsTree(UUID id,
                                      boolean evaluate,
                                      @Nullable Collection<UUID> dataSetsFilter,
                                      @Nullable Collection<UUID> attributesFilter,
                                      Integer startIndex,
                                      Integer endIndex,
                                      boolean isSortEnabled,
                                      boolean expandAll) {
        DataSetList result = dslTreeRepo.getEagerById(id, dataSetsFilter, attributesFilter, startIndex, endIndex,
                isSortEnabled);
        if (result == null) {
            return null;
        }
        if ((!Strings.isNullOrEmpty(macroFeignUrl) || !Strings.isNullOrEmpty(macroFeignRoute)) && evaluate) {
            try (DsEvaluator evaluator = evaluationService.getEvaluator(evaluate, true)) {
                MacroContext macroContext = prepareMacros(result);
                UiManDataSetList dataSetList = Utils.doUiDs(result, macroContext,
                        dataSetParameterProvider, evaluator, evaluate, expandAll);
                sortTable(isSortEnabled, dataSetList);
                return dataSetList;
            }
        } else {
            try (DsEvaluator evaluator = evaluationService.getEvaluator(evaluate, true)) {
                UiManDataSetList dataSetList = Utils.doUiDs(result, evaluator, expandAll);
                sortTable(isSortEnabled, dataSetList);
                return dataSetList;
            }
        }
    }

    /**
     * To get as tree without any filters.
     */
    @Override
    public UiManDataSetList getAsTree(UUID id,
                                      boolean evaluate) {
        return getAsTree(id, evaluate, null, null, null, null, false, true);
    }

    /**
     * To get as tree with filter by datasets only.
     */
    @Override
    public UiManDataSetList getAsTree(UUID id,
                                      boolean evaluate,
                                      Collection<UUID> dataSetsFilter,
                                      boolean isSortEnabled) {
        return getAsTree(id, evaluate, dataSetsFilter, null, null, null, isSortEnabled, true);
    }

    private MacroContext prepareMacros(DataSetList dataSetList) {
        MacroContext macroContext = new MacroContext();
        macroContext.setMacroContextService(macroContextService);
        macroContext.setMacrosCalculator(macrosCalculator);
        UUID visibilityArea = dataSetList.getVisibilityArea().getId();
        List<MacrosDto> macrosDtoList = macrosFeignClient.findNonTechnicalMacrosByProject(visibilityArea).getBody();
        if (nonNull(macrosDtoList)) {
            log.info("macrosDtoList size is  {}", macrosDtoList.size());
        }
        List<Macros> macros = new MacrosDtoConvertService().convertList(macrosDtoList, Macros.class);
        macroContext.setMacros(macros);
        return macroContext;
    }

    private void sortTable(boolean isSortEnabled, UiManDataSetList dataSetList) {
        if (isSortEnabled) {
            dataSetList.getAttributes().forEach(attribute -> {
                if (CollectionUtils.isNotEmpty(attribute.getAttributes())) {
                    attribute.getAttributes().sort(Comparator.comparing(UiManAttribute::getName));
                }
            });
        }
    }

    /**
     * Check new name of DSL on duplicate.
     */
    public void checkOnDuplicate(UUID visibilityArea, String newNameDsl) {
        for (DataSetList dataSetList : getAll(visibilityArea)) {
            if (dataSetList.getName().equals(newNameDsl)) {
                throw new DataSetListExistsException(newNameDsl);
            }
        }
    }

    /**
     * Creates DSL.
     */
    @Transactional
    @Nonnull
    public DataSetList create(@Nonnull UUID visibilityArea, @Nonnull String name, @Nullable UUID testPlanId) {
        UUID createdBy = userInfoProvider.get().getId();
        Timestamp createdWhen = Timestamp.from(Instant.now());
        DataSetList dataSetList = repo.create(visibilityArea, name, testPlanId, createdBy, createdWhen);
        dataSetListSnapshotService.commitEntity(dataSetList.getId());
        return dataSetList;
    }

    @Nullable
    public DataSetList get(@Nonnull UUID id) {
        return dslTreeRepo.getEagerById(id, null, null,
                null, null, false);
    }

    @Nonnull
    public List<DataSetList> getAll() {
        return repo.getAll();
    }

    /**
     * Returns dataSet lists. May filter by label if present.
     */
    @Nonnull
    public List<DataSetList> getAll(@Nonnull UUID visibilityArea, @Nullable String labelName) {
        if (labelName == null) {
            return repo.getAll(visibilityArea);
        } else {
            return repo.getAllByLabel(visibilityArea, labelName);
        }
    }

    /**
     * Returns dataSet lists bulk.
     */
    @Nonnull
    public List<DataSetList> getAll(@Nonnull List<UUID> datasetListIds) {
        return repo.getAll(datasetListIds.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean modify(@Nonnull UUID dataSetListId, @Nullable String name,
                          @Nullable UUID testPlanId, boolean clearTestPlan) {
        boolean result = false;
        if (clearTestPlan) {
            UUID modifiedBy = userInfoProvider.get().getId();
            Timestamp modifiedWhen = Timestamp.from(Instant.now());
            result = repo.setTestPlan(dataSetListId, null, modifiedBy, modifiedWhen);
        }
        if (name != null && !name.isEmpty()) {
            result = rename(dataSetListId, name);
        }
        if (testPlanId != null) {
            result = setTestPlan(dataSetListId, testPlanId);
        }
        if (result) {
            dataSetListSnapshotService.findAndCommitIfExists(dataSetListId);
        }
        evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        return result;
    }

    @Override
    public boolean rename(@Nonnull UUID dataSetListId, @Nonnull String name) {
        UUID modifiedBy = userInfoProvider.get().getId();
        Timestamp modifiedWhen = Timestamp.from(Instant.now());
        return repo.rename(dataSetListId, name, modifiedBy, modifiedWhen);
    }

    /**
     * Verify that dsl's and testPlan's visibility area ids are equals and set test plan.
     */
    private boolean setTestPlan(@Nonnull UUID dataSetListId, @Nonnull UUID testPlanId) {
        UUID visibilityAreaOfDataSetList = get(dataSetListId).getVisibilityArea().getId();
        UUID visibilityAreaOftestPlan = testPlanRepo.getById(testPlanId).getVisibilityArea().getId();
        if (visibilityAreaOfDataSetList.equals(visibilityAreaOftestPlan)) {
            UUID modifiedBy = userInfoProvider.get().getId();
            Timestamp modifiedWhen = Timestamp.from(Instant.now());
            return repo.setTestPlan(dataSetListId, testPlanId, modifiedBy, modifiedWhen);
        } else {
            throw new DataSetListAndTestPlanException();
        }
    }

    @Override
    public DataSetList getByNameUnderVisibilityArea(UUID visibilityArea, String name) {
        return repo.getByNameUnderVisibilityArea(visibilityArea, name);
    }

    @Nonnull
    @Override
    public List<DataSet> getChildren(@Nonnull UUID dataSetListId, boolean evaluate, @Nullable String label) {
        return dsService.getByParentId(dataSetListId, evaluate, label).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public List<DatasetResponse> getListOfDsIdsAndNameAndDslId(@Nonnull List<UUID> dataSetListIds) {
        if (dataSetListIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<DataSetList> dataSetLists = getAll(dataSetListIds);

        return dataSetLists.stream()
                .flatMap(dataSetList -> dataSetList.getDataSets()
                        .stream()
                        .map(dataSet -> new DatasetResponse(dataSet, dataSetList)))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaginationResponse<TableResponse> getAffectedAttributes(UUID dataSetListId, Integer page, Integer size) {
        final PageRequest pageRequest = nonNull(page) && nonNull(size) ? PageRequest.of(page, size) : null;

        return modelsProvider.getAttributesByTypeDatasetListId(dataSetListId, pageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TableResponse> getAffectedAttributes(UUID dataSetListId) {
        return repo.getAffectedAttributes(dataSetListId, null, null);
    }

    private void updateModifiedFields(UUID dataSetListId) {
        log.debug("DataSetListServiceImpl#updateModifiedFields(dataSetListId: {})", dataSetListId);
        UUID modifiedBy = userInfoProvider.get().getId();
        Timestamp modifiedWhen = Timestamp.from(Instant.now());
        repo.updateModifiedFields(dataSetListId, modifiedBy, modifiedWhen);
    }

    @Override
    public void updateModifiedFields(UUID dataSetListId, UUID modifiedBy, Timestamp modifiedWhen) {
        log.debug("DataSetListServiceImpl#updateModifiedFields(dataSetListId: {}, modifiedBy: {}, modifiedWhen: {})",
                dataSetListId, modifiedBy, modifiedWhen.toString());
        repo.updateModifiedFields(dataSetListId, modifiedBy, modifiedWhen);
    }

    @Override
    public List<AffectedDataSetList> getAffectedDataSetLists(@Nonnull UUID dataSetListId,
                                                             @Nullable Integer limit,
                                                             @Nullable Integer offset) {
        log.debug("DataSetListServiceImpl#getAffectedDSL(dataSetListId: {}, limit: {}, offset: {})",
                dataSetListId, limit, offset);
        return repo.getAffectedDataSetLists(dataSetListId, limit, offset);
    }

    @Override
    public Timestamp getModifiedWhen(UUID dataSetListId) {
        log.debug("DataSetListServiceImpl#getDataSetListModifiedWhen(dataSetListId: {})", dataSetListId);
        Timestamp modifiedWhen = null;
        if (repo.existsById(dataSetListId)) {
            modifiedWhen = repo.getModifiedWhen(dataSetListId);
            if (modifiedWhen == null) {
                modifiedWhen = Timestamp.from(Instant.EPOCH);
            }
        }
        return modifiedWhen;
    }

    @Override
    public Long getAffectedAttributesCount(UUID dataSetListId) {
        log.debug("DataSetListServiceImpl#getAffectedAttributesCount(dataSetListId: {})", dataSetListId);
        return modelsProvider.getAttributesByTypeDatasetListId(dataSetListId, null).getTotalCount();
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    public void delete(@Nonnull UUID dataSetListId) {
        Set<UUID> affectedDataSetLists = getAffectedAttributes(dataSetListId)
                .stream()
                .filter(response -> !dataSetListId.equals(response.getDslId()))
                .map(TableResponse::getDslId)
                .collect(Collectors.toSet());
        evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        repo.delete(dataSetListId);
        affectedDataSetLists.forEach(uuid -> {
            updateModifiedFields(uuid);
            dataSetListSnapshotService.commitEntity(uuid);
        });
        dataSetListSnapshotService.deleteDataSetList(dataSetListId);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public DataSetList copy(@Nonnull UUID visibilityArea, @Nonnull UUID dataSetListId, @Nonnull String name,
                            @Nullable Boolean withData, @Nullable UUID testPlanId) {
        log.info("Called service copying data set list {}", dataSetListId);
        DataSetList dslToCopy = get(dataSetListId);
        Preconditions
                .checkNotNull(dslToCopy, "Can not find data set list to copy by id %s", dataSetListId);
        if (testPlanId != null) {
            Preconditions
                    .checkArgument(visibilityArea.equals(testPlanRepo.getById(testPlanId).getVisibilityArea().getId()),
                            "Data Set List and Test Plan are in different Visibility Areas");
        }
        DataSetList newDsl = create(visibilityArea, name, testPlanId);
        Map<UUID, UUID> attrToAttr = copyAttr(dslToCopy, newDsl);
        if (Boolean.TRUE.equals(withData)) {
            for (DataSet ds : dslToCopy.getDataSets()) {
                dsService.copy(ds.getId(), newDsl.getId(), attrToAttr);
            }
        }
        newDsl = get(newDsl.getId());
        dataSetListSnapshotService.commitEntity(newDsl.getId());
        return newDsl;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Map<UUID, Pair<UUID, UUID>> copy(String name, Map<UUID, Set<UUID>> data) {
        Map<UUID, Pair<UUID, UUID>> result = new HashMap<>();

        List<UUID> dslIds = new ArrayList<>(data.keySet());
        Map<UUID, DataSetList> datasetListMap;
        if (!dslIds.isEmpty()) {
            datasetListMap = getAll(dslIds).stream().collect(
                    Collectors.toMap(DataSetList::getId, dsl -> dsl));
        } else {
            log.error("Can not find DataSet lists to copy - no ids.");
            throw new DataSetListNotFoundException();
        }

        Iterator<Map.Entry<UUID, Set<UUID>>> entries = data.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Set<UUID>> entry = entries.next();
            DataSetList dslToCopy = datasetListMap.get(entry.getKey());
            Preconditions
                    .checkNotNull(dslToCopy, "Can not find data set list to copy by id %s",
                            entry.getKey());
            DataSetList newDsl = create(dslToCopy.getVisibilityArea().getId(),
                    dslToCopy.getName() + "_" + name,
                    null);

            Map<UUID, UUID> attrToAttr = copyAttr(dslToCopy, newDsl);
            for (DataSet ds : dslToCopy.getDataSets()) {
                if (entry.getValue().contains(ds.getId())) {
                    DataSet copyDs = dsService.copy(ds, newDsl.getId(), attrToAttr);
                    result.put(ds.getId(), new ImmutablePair<>(copyDs.getId(), newDsl.getId()));
                }
            }
            dataSetListSnapshotService.commitEntity(newDsl.getId());
        }
        return result;
    }

    private Map<UUID, UUID> copyAttr(DataSetList dslToCopy, DataSetList newDsl) {
        int ordering = 0;
        Map<UUID, UUID> attrToAttr = new HashMap<>();
        for (Attribute attr : dslToCopy.getAttributes()) {
            Attribute newAttr = attributeService.copy(newDsl, attr, ordering);
            attrToAttr.put(attr.getId(), newAttr.getId());
            ordering++;
        }
        return attrToAttr;
    }

    /**
     * Adds new dataSetListLabel with name provided.
     */
    @Transactional
    public Label mark(@Nonnull UUID dataSetListId, @Nonnull String name) {
        UUID modifiedBy = userInfoProvider.get().getId();
        Timestamp modifiedWhen = Timestamp.from(Instant.now());
        Label label = repo.mark(dataSetListId, name, modifiedBy, modifiedWhen);
        dataSetListSnapshotService.findAndCommitIfExists(dataSetListId);
        return label;
    }

    /**
     * Returns dataSetListLabels.
     */
    public List<Label> getLabels(@Nonnull UUID dataSetListId) {
        return repo.getLabels(dataSetListId);
    }

    /**
     * Deletes dataSetListLabel.
     */
    @Transactional
    public boolean unmark(@Nonnull UUID dataSetListId, @Nonnull UUID labelId) {
        UUID modifiedBy = userInfoProvider.get().getId();
        Timestamp modifiedWhen = Timestamp.from(Instant.now());
        boolean isUnmarked = repo.unmark(dataSetListId, labelId, modifiedBy, modifiedWhen);
        if (isUnmarked) {
            dataSetListSnapshotService.findAndCommitIfExists(dataSetListId);
        }
        return isUnmarked;
    }

    @Override
    public boolean existsById(@Nonnull UUID dataSetListId) {
        log.debug("DataSetListServiceImpl#existsById(dataSetListId: {})", dataSetListId);
        return repo.existsById(dataSetListId);
    }

    /**
     * Evict affected datasets from ATP_DATASETS_DATASET_LIST_CONTEXT_CACHE cache.
     * @param updatedDataSetListId updated dataset list id
     */
    public void evictAllAffectedDatasetsFromContextCacheByDslId(UUID updatedDataSetListId) {
        dsService.evictAllAffectedDatasetsFromContextCacheByDslId(updatedDataSetListId);
    }

    @Override
    @Transactional
    public void revert(UUID sagaSessionId, RevertRequest request) {
        Set<UUID> dataSetListIds = modelsProvider
                .getDataSetListIdsBySagaSessionIdIdAndVisibilityAreaId(sagaSessionId, request.getProjectId());
        dataSetListIds.forEach(this::delete);
    }
}

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

package org.qubership.atp.dataset.service.direct;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.api.saga.requests.RevertRequest;
import org.qubership.atp.dataset.model.impl.FlatDataImpl;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.DatasetResponse;
import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.rest.PaginationResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.AffectedDataSetList;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;

public interface DataSetListService extends IdentifiedService<DataSetList>, LabelProviderService {

    @Nullable
    FlatDataImpl getAsFlat(UUID id, boolean evaluate);

    /**
     * To get as tree with filter by datasets and attributes.
     *
     * @deprecated only for cases when the evaluate parameter equals true. Use
     * {@link JpaDataSetService#getDataSetTreeInAtpFormat(UUID, boolean, String, ContextType)} instead.
     */
    @Deprecated
    @Nullable
    UiManDataSetList getAsTree(UUID id,
                               boolean evaluate,
                               @Nullable Collection<UUID> dataSetsFilter,
                               @Nullable Collection<UUID> dataAttributeFilter,
                               Integer startIndex,
                               Integer endIndex,
                               boolean isSort,
                               boolean expandAll);

    /**
     * To get as tree with filter by datasets only.
     *
     * @deprecated only for cases when the evaluate parameter equals true. Use
     * {@link JpaDataSetService#getDataSetTreeInAtpFormat(UUID, boolean, String, ContextType)} instead.
     */
    @Nullable
    UiManDataSetList getAsTree(UUID id,
                               boolean evaluate,
                               @Nullable Collection<UUID> dataSetsFilter,
                               boolean isSortEnabled);

    /**
     * To get as tree without any filters.
     *
     * @deprecated only for cases when the evaluate parameter equals true. Use
     * {@link JpaDataSetService#getDataSetTreeInAtpFormat(UUID, boolean, String, ContextType)} instead.
     */
    @Nullable
    UiManDataSetList getAsTree(UUID id,
                               boolean evaluate);

    void checkOnDuplicate(UUID visibilityArea, String newNameDsl);

    @Nonnull
    DataSetList create(@Nonnull UUID visibilityArea, @Nonnull String name, @Nullable UUID testPlanId);

    @Nonnull
    default List<DataSetList> getAll(@Nonnull UUID visibilityArea) {
        return getAll(visibilityArea, null);
    }

    @Nonnull
    List<DataSetList> getAll(@Nonnull UUID visibilityArea, @Nullable String labelName);

    List<DataSetList> getAll(@Nonnull List<UUID> datasetListIds);

    boolean rename(@Nonnull UUID id, @Nonnull String name);

    DataSetList getByNameUnderVisibilityArea(UUID visibilityArea, String name);

    default List<DataSet> getChildren(@Nonnull UUID dataSetListId, boolean evaluate) {
        return getChildren(dataSetListId, evaluate, null);
    }

    /**
     * Returns all datasets under target {@link DataSetList}.
     *
     * @param dataSetListId - id of target {@link DataSetList}.
     * @param evaluate      - should apply structure changes or not.
     * @param labelName     label name to filter with
     */
    List<DataSet> getChildren(@Nonnull UUID dataSetListId, boolean evaluate, @Nullable String labelName);

    /**
     * Deletes DS by id.
     */
    void delete(@Nonnull UUID dataSetListId);

    /**
     * Copy data set list.
     */
    DataSetList copy(@Nonnull UUID visibilityArea, @Nonnull UUID dataSetListId, @Nonnull String name,
                     @Nullable Boolean withData, @Nullable UUID testPlanId) throws Exception;

    /**
     * Copy of dataSetLists and dataSets in it.
     *
     * @param name - new DSL name
     * @param data - DSL to copy with it's DSs to copy
     * @return - structure wich contains mapping of old DS and pair of new DS and DSL
     */
    Map<UUID, Pair<UUID, UUID>> copy(String name, Map<UUID, Set<UUID>> data);

    /**
     * Rename dsl or add test plan to dsl.
     * @param dataSetListId dsl id.
     * @param name new name of dsl.
     * @param testPlanId new test plan id for dsl.
     * @param clearTestPlan true if you want test plan in dsl be null.
     * @return success or failure of update entity.
     * @throws IllegalArgumentException if dsl's and testPlan's visibility area ids are not equals.
     */
    boolean modify(UUID dataSetListId, String name, UUID testPlanId, boolean clearTestPlan);

    /**
     * Get list of dsId, dsNme, dslId for each datasetlist.
     *
     * @param dataSetListIds dsl ids.
     * @return list of DsIdNameDslId.
     */
    List<DatasetResponse> getListOfDsIdsAndNameAndDslId(@Nonnull List<UUID> dataSetListIds);

    /**
     * Get affected attribute by  dsl.
     */
    PaginationResponse<TableResponse> getAffectedAttributes(UUID dataSetListId, Integer page, Integer size);

    /**
     * Get affected attribute by  dsl.
     */
    List<TableResponse> getAffectedAttributes(UUID dataSetListId);

    void updateModifiedFields(UUID dataSetListId, UUID modifiedBy, Timestamp modifiedWhen);

    /**
     * Check if dsl exists.
     *
     * @param dataSetListId DSL id
     * @return 'true' if dsl exists, otherwise 'false'
     */
    boolean existsById(@Nonnull UUID dataSetListId);

    /**
     * Get affected DSL by dataSetListId.
     *
     * @param dataSetListId DSL id
     * @param limit number of rows that are returned
     * @param offset number of rows that are skipped
     * @return list with DSL id and DSL name
     */
    List<AffectedDataSetList> getAffectedDataSetLists(@Nonnull UUID dataSetListId,
                                                      @Nullable Integer limit,
                                                      @Nullable Integer offset);

    /**
     * Get modifiedWhen by dataSetListId.
     *
     * @param dataSetListId dataSetListId
     * @return {@link Timestamp} of modified
     */
    Timestamp getModifiedWhen(UUID dataSetListId);

    /**
     * Get affected DSL count by dataSetListId.
     *
     * @param dataSetListId DSL id
     * @return affected DSL count
     */
    Long getAffectedAttributesCount(UUID dataSetListId);

    /**
     * Evict affected datasets from ATP_DATASETS_DATASET_LIST_CONTEXT_CACHE cache.
     * @param updatedDataSetListId updated dataset list id
     */
    void evictAllAffectedDatasetsFromContextCacheByDslId(UUID updatedDataSetListId);

    void revert(UUID sagaSessionId, RevertRequest request);
}

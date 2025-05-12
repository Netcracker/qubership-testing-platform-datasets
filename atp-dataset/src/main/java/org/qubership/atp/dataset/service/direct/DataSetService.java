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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.CheckedConsumer;
import org.qubership.atp.dataset.model.utils.ObjectShortResponse;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.rest.PaginationResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataSetService extends IdentifiedService<DataSet>, LabelProviderService {

    @Nonnull
    DataSet create(@Nonnull UUID dslId, @Nonnull String name);

    /**
     * See {@link Utils#serializeInItfWay(DataSet, ObjectMapper, DsEvaluator)}}.
     */
    @Nullable
    ObjectNode getInItfFormat(@Nonnull MixInId id);

    @Nullable
    CheckedConsumer<OutputStream, IOException> writeInAtpFormat(@Nonnull MixInId id,
                                                                @Nullable Map<String, String> context,
                                                                boolean evaluate);

    /**
     * Returns bulky {@link List} of {@link DataSet}.
     *
     * @param dataSetsIds {@link List} of data sets ids
     * @return {@link List} of {@link DataSet}
     */
    @Nonnull
    List<DataSet> getAll(@Nonnull List<UUID> dataSetsIds);

    @Nonnull
    List<DataSet> getAllDslDsByAttribute(UUID targetAttrId, List<UUID> attrPathIds);

    void rename(@Nonnull UUID id, @Nonnull String name);

    /**
     * Delete DS by id.
     */
    void delete(@Nonnull UUID dataSetId);

    /**
     * Copy DS.
     */
    DataSet copy(@Nonnull UUID dataSetId, @Nonnull String name);

    /**
     * Copy DS to another dsl.
     *
     * @param dataSetListId - id of dsl where to copy.
     * @param attributes    - mapping of old and new attributes if ds copied to another dsl.
     * @return data set with parameters and labels.
     */
    DataSet copy(@Nonnull UUID dataSetId, @Nonnull UUID dataSetListId, @Nonnull Map<UUID, UUID> attributes);

    /**
     * Copy DS to another dsl.
     */
    DataSet copy(@Nonnull DataSet dataSet, @Nonnull UUID dataSetListId, @Nonnull Map<UUID, UUID> attributes);

    /**
     * Return all datasets which contains overlapped parameters.
     *
     * @return DataSets with parameter overlaps on the target attribute provided.
     */
    List<?> getOverlapContainers(@Nonnull UUID dataSetId, @Nonnull UUID attributeId, boolean withInfo);

    /**
     * Return all datasets which contains overlapped parameters.
     */
    List<?> getAffectedDataSetsByChangesDataSetReference(@Nonnull UUID dataSetId, boolean deleteDs);

    /**
     * Returns attribute with parameters found by provided path.
     *
     * @param attrPath path to attribute inclusive
     */
    @Nullable
    UiManAttribute getParametersOnAttributePath(@Nonnull UUID dslId, @Nonnull List<UUID> attrPath, boolean evaluate);

    void deleteAllParameterOverlaps(UUID attributeId, List<UUID> dataSetsIds);

    /**
     * Delete all overlap parameters for {@link DataSet} and {@link Parameter} by attribute path.
     *
     * @return the parameter which lays underneath deleted overlap.
     */
    Parameter deleteParameterOverlap(@Nonnull UUID dsId,
                                     @Nonnull UUID targetAttrId,
                                     @Nonnull List<UUID> attrPathIds);

    /**
     * Returns all datasets under target {@link DataSetList}.
     *
     * @param dataSetListId - id of target {@link DataSetList}.
     * @param evaluate      - should apply structure changes or not.
     * @param labelName     - name of a label.
     * @return list of {@link DataSet} under target {@link DataSetList}.
     */
    Stream<DataSet> getByParentId(UUID dataSetListId, boolean evaluate, @Nullable String labelName);

    /**
     * Returns all datasets (id + name) under target {@link DataSetList}.
     *
     * @param dataSetListId - id of target {@link DataSetList}.
     * @return list of {@link DataSet} under target {@link DataSetList}.
     */
    List<ObjectShortResponse> getByParentId(UUID dataSetListId);

    boolean restore(@Nonnull JsonNode dataSetJson);

    PaginationResponse<TableResponse> getAffectedDataSets(UUID dataSetId, Integer page, Integer size);

    Long getAffectedDataSetsCount(UUID dataSetId);

    /**
     * Lock the datasets and their parameters from changes.
     * @param dataSetListId dataSetList Id
     * @param uuids Ids datasets
     * @param isLock Flag true/false lock
     */
    void lock(UUID dataSetListId, @Nonnull List<UUID> uuids, boolean isLock);

    /**
     * Evict affected datasets from ATP_DATASETS_DATASET_LIST_CONTEXT_CACHE cache.
     * @param updatedDataSetListId updated dataset list id
     */
    void evictAllAffectedDatasetsFromContextCacheByDslId(UUID updatedDataSetListId);

    /**
     * Evict affected datasets from ATP_DATASETS_DATASET_LIST_CONTEXT_CACHE cache.
     * @param updatedDataSetId updated dataset id
     */
    void evictAllAffectedDatasetsFromContextCacheByDsId(UUID updatedDataSetId);

    /**
     * Collects all affected DataSet Ids recursively.
     * Used then dsl updated or attribute created/updated/deleted
     * @param updatedDataSetListId updated dataSetList id
     * @return set of updated dataset ids include updatedDataSetId
     */
    @Nonnull
    Set<UUID> collectAffectedDatasetsByDslId(UUID updatedDataSetListId);

    /**
     * Collects all affected DataSet Ids recursively.
     * Used then dataset updated or parameter updated
     * @param updatedDataSetId updated dataSet id
     * @return set of updated dataset ids include updatedDataSetId
     */
    @Nonnull
    Set<UUID> collectAffectedDatasetsByDsId(UUID updatedDataSetId);
}

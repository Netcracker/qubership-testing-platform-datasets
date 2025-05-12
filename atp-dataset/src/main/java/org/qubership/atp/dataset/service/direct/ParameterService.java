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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.springframework.web.multipart.MultipartFile;

public interface ParameterService extends IdentifiedService<Parameter> {

    @Nonnull
    Parameter create(@Nonnull UUID dsId, @Nonnull UUID attrId, @Nullable String text,
                     @Nullable UUID listValueReference, @Nullable UUID referenceDataSet);

    /**
     * Creates or updates value of {@link Parameter} or {@link ParameterOverlap}.
     *
     * @param attrPathIds should be specified if you want an overlap value.
     */
    @Nonnull
    Parameter set(@Nonnull UUID dsId, @Nonnull UUID targetAttrId, @Nullable List<UUID> attrPathIds,
                  @Nullable String stringValue, @Nullable UUID dsRef, @Nullable UUID listValueRef);

    @Nonnull
    Parameter set(@Nonnull UUID dsId, @Nonnull UUID targetAttrId, @Nonnull String value, List<UUID> attrPathIds);

    /**
     * Creates or updates value of {@link Parameter} or {@link ParameterOverlap}.
     * Without Snapshot
     * @param attrPathIds should be specified if you want an overlap value.
     * @param isJavers Specifies whether Javers is enabled or disabled (Versioning)
     */
    @Nonnull
    Parameter setParamSelectJavers(@Nonnull UUID dsId,
                                   @Nonnull UUID targetAttrId,
                                   @Nullable List<UUID> attrPathIds,
                                   @Nullable String stringValue,
                                   @Nullable UUID dsRef,
                                   @Nullable UUID listValueRef,
                                   boolean isJavers);

    /**
     * Updates list of {@link Parameter}. Do not use it for file or {@link ParameterOverlap}.
     */
    boolean bulkUpdateValue(@Nullable String stringValue,
                            @Nullable UUID dsRef,
                            @Nullable UUID listValueRef,
                            @Nonnull List<UUID> parameterIds);

    FileData upload(UUID parameterUuid, String contentType, String fileName, InputStream file);

    FileData upload(UUID datasetId, UUID attributeId, List<UUID> attrPathIds, String contentType,
                    String fileName, InputStream file);

    List<FileData> bulkUploadAttachment(UUID dataSetListId, List<UUID> dataSetsIds, UUID attributeId,
                                        MultipartFile file, List<UUID> attrPathIds);

    List<Object> bulkUpdate(UUID dataSetListId, List<UUID> attrPathIds, List<UUID> dataSetsIds, UUID attributeId,
                            String value, MultipartFile file);

    void clearAttachment(UUID parameterId);

    boolean update(@Nonnull UUID parameterId, String text);

    boolean delete(@Nonnull Parameter parameter);

    boolean delete(@Nonnull UUID attributeId, @Nonnull UUID dataSetId,  UUID datasetListId, List<UUID> attrPathIds);

    void deleteParamSelectJavers(@Nonnull UUID attributeId, @Nonnull UUID dataSetId, UUID datasetListId,
                                 List<UUID> attrPathIds, boolean isJavers);

    /**
     * Copy Parameters to attribute.
     *
     * @param attributes - mapping of old and new attributes if ds copied to another dsl.
     */
    Parameter copy(@Nonnull DataSet newParentDs,
                   @Nonnull Parameter parameter,
                   @Nullable Map<UUID, UUID> attributes);

    /**
     * get parameter by dataset and attribute id.
     */
    @Nullable
    Parameter getByDataSetIdAttributeId(UUID dataSetId, UUID attrId);

    /**
     * Get patameters by attribute id and dataset ids.
     */
    List<Parameter> getByAttributeIdAndDatasetIds(@Nonnull UUID attributeId,
                                                  @Nonnull Set<UUID> datasetIds);

    /**
     * Get parameter by dataset and attribute id. If it doesn't exist - create new.
     */
    @Nullable
    Parameter getOrCreateByDataSetIdAttributeId(UUID dataSetId, UUID attrId);

    @Nonnull
    Parameter getOrCreateOverlap(UUID dsId, UUID targetAttrId, List<UUID> attrPathIds);

    @Nullable
    ParameterOverlap getOverlap(@Nonnull UUID dsId,
                                @Nonnull UUID attributeId,
                                @Nonnull List<UUID> attributePathIds);

    Stream<ParameterOverlap> getOverlaps(@Nonnull UUID targetAttributeId,
                                         java.util.function.Predicate<AttributePath> filter);

    List<?> getParametersAffectedByListValue(UUID listValueId, boolean withDsl);

    List<TableResponse> getParametersAffectedByListValues(List<UUID> listValueIds);
}

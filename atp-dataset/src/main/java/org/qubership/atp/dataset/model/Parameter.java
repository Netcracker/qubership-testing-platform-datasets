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

package org.qubership.atp.dataset.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.impl.file.FileData;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.google.common.base.Preconditions;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = Parameter.class)
public interface Parameter extends Identified {

    @Schema(description = "parent", required = true)
    @Nonnull
    Attribute getAttribute();

    void setAttribute(@Nonnull Attribute attribute);

    @Schema(description = "parent", required = true)
    @Nonnull
    DataSet getDataSet();

    void setDataSet(@Nonnull DataSet dataSet);

    @Schema(description = "for attribute with text type")
    @Nullable
    String getText();

    void setText(@Nullable String stringValue);

    @Schema(description = "for attribute with dsl type")
    @Nullable
    DataSet getDataSetReference();

    void setDataSetReference(@Nullable DataSet ds);

    @Schema(description = "for attribute with list type")
    @Nullable
    ListValue getListValue();

    void setListValue(@Nullable ListValue value);

    FileData getFileData();

    void setFileData(FileData fileData);

    @JsonIgnore
    default boolean isOverlap() {
        return this instanceof ParameterOverlap;
    }

    @JsonIgnore
    @Nonnull
    default ParameterOverlap asOverlap() {
        Preconditions.checkState(isOverlap(), "This is not an overlap: %s", this);
        return (ParameterOverlap) this;
    }
}
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

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = Attribute.class)
public interface Attribute extends Identified, Named {

    @Schema(description = "parent", required = true)
    DataSetList getDataSetList();

    void setDataSetList(DataSetList dataSetList);

    @Schema(required = true)
    AttributeType getType();

    void setType(AttributeType type);

    @Schema(description = "for dsl type")
    @Nullable
    DataSetList getDataSetListReference();

    void setDataSetListReference(DataSetList list);

    @ArraySchema(schema = @Schema(description = "for list type"))
    List<ListValue> getListValues();

    void setListValues(List<ListValue> listValues);

    @ArraySchema
    List<Parameter> getParameters();

    void setParameters(List<Parameter> parameters);
}

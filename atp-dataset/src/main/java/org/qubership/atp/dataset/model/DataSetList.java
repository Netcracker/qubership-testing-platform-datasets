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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = DataSetList.class)
public interface DataSetList extends Identified, Named, LabelProvider, TestPlanProvider, CreatedModified {

    @Schema(description = "parent", required = true)
    VisibilityArea getVisibilityArea();

    void setVisibilityArea(VisibilityArea visibilityArea);

    @ArraySchema
    List<DataSet> getDataSets();

    void setDataSets(List<DataSet> dataSets);

    @ArraySchema
    List<Attribute> getAttributes();

    @Nonnull
    @JsonIgnore
    default Collection<Attribute> getAttributes(@Nonnull AttributeType type) {
        return getAttributes().stream().filter(attr -> type == attr.getType()).collect(Collectors.toList());
    }

    void setAttributes(List<Attribute> attributes);
}

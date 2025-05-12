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
import java.util.UUID;

import org.qubership.atp.dataset.service.rest.View;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = DataSet.class)
public interface DataSet extends Identified, Named, LabelProvider {


    @JsonIgnore
    @Override
    default UUID getId() {
        return getMixInId().getUuid();
    }

    @Override
    default void setId(UUID id) {
        getMixInId().setUuid(id);
    }

    Boolean isLocked();

    void setLocked(Boolean isLock);

    @JsonView({View.IdName.class, View.IdNameLabelsTestPlan.class})
    @JsonProperty("id")
    MixInId getMixInId();

    void setMixInId(MixInId id);

    @Schema(description = "parent", required = true)
    DataSetList getDataSetList();

    void setDataSetList(DataSetList dataSetList);

    @ArraySchema
    List<Parameter> getParameters();

    void setParameters(List<Parameter> parameters);
}


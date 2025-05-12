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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public interface AttributePath extends Identified {

    @Schema(description = "owner", required = true)
    DataSet getDataSet();

    void setDataSet(DataSet dataSet);

    @Schema(description = "target", required = true)
    Attribute getTargetAttribute();

    void setTargetAttribute(Attribute target);

    @ArraySchema(schema = @Schema(description = "DSL type attributes path to the target", required = true))
    List<Attribute> getPath();

    void setPath(List<Attribute> attributePath);
}

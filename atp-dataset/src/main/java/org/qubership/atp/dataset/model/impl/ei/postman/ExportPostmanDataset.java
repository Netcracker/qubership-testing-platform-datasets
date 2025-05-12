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

package org.qubership.atp.dataset.model.impl.ei.postman;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExportPostmanDataset {

    private UUID id;
    private String name;
    private List<ExportPostmanParameter> values;
    @JsonProperty("_postman_variable_scope")
    private final String postmanVariableScope = "globals";

    /**
     * the ExportPostmanDataset constructor.
     */
    public ExportPostmanDataset(UUID id, String name, List<ExportPostmanParameter> values) {
        this.id = id;
        this.name = name;
        this.values = values;
    }
}

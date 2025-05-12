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

package org.qubership.atp.dataset.service.jpa.model.tree.params;

import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({"type", "value"})
public abstract class AbstractParameter {
    @JsonInclude
    public abstract AttributeTypeName getType();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract String getValue();

    @JsonIgnore()
    public abstract boolean isNullValue();

    @JsonIgnore
    public String getInItfFormatValue() {
        return getValue();
    }

    @Getter
    @Setter
    @JsonIgnore
    private Long order;
}

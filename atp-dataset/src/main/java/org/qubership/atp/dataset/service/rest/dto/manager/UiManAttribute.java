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

package org.qubership.atp.dataset.service.rest.dto.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "name", "type", "dataSetListReference", "parameters", "attributes"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UiManAttribute {

    private List<UiManAttribute> attributes;
    private List<UiManParameter> parameters;
    private Attribute source;

    public UiManAttribute() {
        attributes = new ArrayList<>();
        parameters = new ArrayList<>();
    }

    public List<UiManAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<UiManAttribute> attributes) {
        this.attributes = attributes;
    }

    @JsonIgnore
    public Attribute getSource() {
        return source;
    }

    public void setSource(Attribute source) {
        this.source = source;
    }

    public UUID getId() {
        return source.getId();
    }

    public String getName() {
        return source.getName();
    }

    public AttributeType getType() {
        return source.getType();
    }

    public List<UiManParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<UiManParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Get reference if type is DSL, and null otherwise.
     * */
    public UUID getDataSetListReference() {
        return source.getDataSetListReference() == null ? null : source.getDataSetListReference().getId();
    }
}

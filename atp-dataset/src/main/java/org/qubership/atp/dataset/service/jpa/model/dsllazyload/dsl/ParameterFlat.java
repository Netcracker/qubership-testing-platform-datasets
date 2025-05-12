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

package org.qubership.atp.dataset.service.jpa.model.dsllazyload.dsl;

import java.util.Collections;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Used for top level DSL parameter representation.
 * */
@Getter
@Setter
public class ParameterFlat {
    @JsonIgnore
    private UUID id;
    private UUID dataSet;
    private String value;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String valueRef;
    private boolean overlap = false;
    @JsonIgnore
    private DataSetParameterProvider parameterProvider;
    @JsonIgnore
    private AttributeTypeName attributeType;
    @JsonIgnore
    private ParameterPositionContext positionContext;

    /**
     * Normal parameter.
     */
    public ParameterFlat(
            Attribute attribute,
            Parameter parameter,
            int column,
            DataSetParameterProvider parameterProvider
    ) {
        id = parameter.getId();
        dataSet = parameter.getDataSet().getId();
        attributeType = attribute.getAttributeType();
        if (attributeType == AttributeTypeName.DSL) {
            UUID dataSetReferenceId = parameter.getDataSetReferenceId();
            if (dataSetReferenceId == null) {
                valueRef = null;
            } else {
                valueRef = parameter.getDataSetReferenceId().toString();
            }
            dataSet = parameter.getDataSetId();
        }
        value = parameter.getParameterValueByType();
        this.parameterProvider = parameterProvider;
        positionContext = new ParameterPositionContext(
                Collections.emptyList(),
                column,
                dataSet,
                attribute.getOrdering().longValue(),
                attribute.getId()
        );
    }

    /**
     * Parse and get value.
     * */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getValue() {
        if (attributeType != AttributeTypeName.DSL) {
            AbstractParameter dataSetParameterResolved = parameterProvider.getDataSetParameterResolved(
                    null,
                    id,
                    attributeType,
                    false,
                    null,
                    positionContext
            );
            value = dataSetParameterResolved.getValue();
        }
        return value;
    }
}

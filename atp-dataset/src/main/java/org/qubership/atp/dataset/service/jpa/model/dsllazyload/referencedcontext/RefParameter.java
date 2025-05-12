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

package org.qubership.atp.dataset.service.jpa.model.dsllazyload.referencedcontext;

import java.util.UUID;

import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Used for referenced DSL parameter representation.
 * */
@Getter
@Setter
public class RefParameter {
    private UUID dataSet;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String value;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String valueRef;
    private boolean overlap;
    @JsonIgnore
    private ParameterContext parameterContext;
    @JsonIgnore
    private AbstractParameter dataSetParameterResolved;
    @JsonIgnore
    private ParameterPositionContext positionContext;
    @JsonIgnore
    private DataSetParameterProvider parameterProvider;

    /**
     * Default constructor.
     * */
    public RefParameter(
            UUID parentDataSetId,
            ParameterContext parameter,
            boolean isOverLap,
            ParameterPositionContext positionContext,
            DataSetParameterProvider parameterProvider
    ) {
        dataSet = parentDataSetId;
        overlap = isOverLap;
        value = parameter.getValue();
        this.positionContext = positionContext;
        this.parameterProvider = parameterProvider;
        this.parameterContext = parameter;
        if (parameter.getType() == AttributeTypeName.DSL) {
            UUID dataSetReferenceId = parameter.getDataSetReferenceId();
            if (dataSetReferenceId == null) {
                valueRef = null;
            } else {
                valueRef = parameter.getDataSetReferenceId().toString();
            }
        }
        if (parameter.getType() == AttributeTypeName.LIST) {
            UUID listValueId = parameter.getListValueId();
            if (listValueId == null) {
                valueRef = null;
            } else {
                valueRef = listValueId.toString();
            }
        }
    }

    /**
     * Used for file parameters.
     * */
    public RefParameter(
            UUID parentDataSetId,
            ParameterContext parameter,
            boolean isOverLap,
            ParameterPositionContext positionContext,
            DataSetParameterProvider parameterProvider,
            FileData fileInfo
    ) {
        dataSet = parentDataSetId;
        overlap = isOverLap;
        value = parameter.getValue();
        this.positionContext = positionContext;
        this.parameterProvider = parameterProvider;
        this.parameterContext = parameter;
        if (parameter.getType() == AttributeTypeName.FILE) {
            value = fileInfo.getFileName();
            valueRef = "/attachment/" + parameter.getValue();
        }
    }

    /**
     * Parse and get value.
     * */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getValue() {
        if (value == null) {
            return null;
        }
        if (parameterContext.getType() != AttributeTypeName.DSL
                && parameterContext.getType() != AttributeTypeName.FILE
        ) {
            dataSetParameterResolved = parameterProvider.getDataSetParameterResolved(
                    null,
                    parameterContext.getParameterId(),
                    parameterContext.getType(),
                    false,
                    null,
                    positionContext
            );
            return dataSetParameterResolved.getValue();
        } else {
            return value;
        }
    }
}

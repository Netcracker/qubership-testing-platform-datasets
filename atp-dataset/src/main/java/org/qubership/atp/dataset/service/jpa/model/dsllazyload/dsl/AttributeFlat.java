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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Used for top level DSL attribute representation.
 * */
@Getter
@Setter
public class AttributeFlat {
    private UUID id;
    private String name;
    private AttributeTypeName type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean containsAttributes;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ParameterFlat> parameters = new LinkedList<>();

    /**
     * Default.
     * */
    public AttributeFlat(
            Attribute attribute,
            List<UUID> dataSetIds,
            DataSetParameterProvider parameterProvider
    ) {
        id = attribute.getId();
        name = attribute.getName();
        type = attribute.getAttributeType();
        if (attribute.getAttributeType().equals(AttributeTypeName.DSL)) {
            DataSetList typeDataSetList = attribute.getTypeDataSetList();
            containsAttributes = typeDataSetList.getAttributesCount() > 0;
        }
        List<Parameter> parametersModels = attribute.getParameters();
        int column = 0;
        for (UUID dadaSetId : dataSetIds) {
            Parameter parameterWithValue = null;
            for (Parameter parameter : parametersModels) {
                if (dadaSetId.equals(parameter.getDataSetId())) {
                    parameterWithValue = parameter;
                    break;
                }
            }
            if (parameterWithValue != null) {
                if (type == AttributeTypeName.FILE && parameterWithValue.getFileValueId() == null) {
                    continue;
                }
                parameters.add(new ParameterFlat(attribute, parameterWithValue, column, parameterProvider));
            }
            column++;
        }
    }
}

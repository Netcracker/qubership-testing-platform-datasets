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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Used for referenced DSL attribute representation.
 * */
@Getter
@Setter
public class RefDataSetListAttribute {
    private UUID id;
    private String name;
    private AttributeTypeName type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean containsAttributes;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<RefParameter> parameters = new LinkedList<>();

    /**
     * Default constructor.
     */
    public RefDataSetListAttribute(
            Attribute referencedAttribute,
            Map<UUID, ParameterContext> parameters,
            List<UUID> path,
            DataSetParameterProvider parameterProvider,
            UUID dataSetListId
    ) {
        id = referencedAttribute.getId();
        name = referencedAttribute.getName();
        type = referencedAttribute.getAttributeType();
        if (referencedAttribute.getAttributeType().equals(AttributeTypeName.DSL)) {
            DataSetList typeDataSetList = referencedAttribute.getTypeDataSetList();
            containsAttributes = typeDataSetList.getAttributesCount() > 0;
        }
        int column = 0;
        for (Map.Entry<UUID, ParameterContext> parameterContext : parameters.entrySet()) {
            if (parameterContext.getValue().isNullValue()) {
                continue;
            }
            ParameterPositionContext positionContext = new ParameterPositionContext(
                    path,
                    column,
                    parameterContext.getKey(),
                    parameterContext.getValue().getOrder(),
                    dataSetListId
            );
            if (type == AttributeTypeName.FILE) {
                FileData fileInfo = parameterProvider.getFileVariableInfo(parameterContext.getValue().getParameterId());
                if (fileInfo == null) {
                    continue;
                }
                this.parameters.add(
                        new RefParameter(
                                parameterContext.getKey(),
                                parameterContext.getValue(),
                                parameterContext.getValue().isOverlap(),
                                positionContext,
                                parameterProvider,
                                fileInfo
                        )
                );
            } else {
                this.parameters.add(
                        new RefParameter(
                                parameterContext.getKey(),
                                parameterContext.getValue(),
                                parameterContext.getValue().isOverlap(),
                                positionContext,
                                parameterProvider
                        )
                );
            }
            column++;
        }
    }
}

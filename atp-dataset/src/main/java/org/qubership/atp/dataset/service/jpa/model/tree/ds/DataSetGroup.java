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

package org.qubership.atp.dataset.service.jpa.model.tree.ds;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.GroupContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;
import org.qubership.atp.dataset.service.jpa.model.tree.OverlapNode;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataSetGroup {
    private AttributeTypeName type = AttributeTypeName.DSL;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String value;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String dsl;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, AbstractParameter> parameters = new LinkedHashMap<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, DataSetGroup> groups = new LinkedHashMap<>();
    @JsonIgnore
    private List<UUID> currentPath;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private UUID dataSetId;
    @Getter
    @JsonIgnore
    private Long order;

    /**
     * DS group default constructor for DS tree.
     * */
    public DataSetGroup(
            GroupContext groupContext,
            int column,
            boolean evaluate,
            MacroContext macroContext,
            DataSetParameterProvider dataSetParameterProvider,
            ContextType contextType,
            OverlapNode rootOverlapNode
    ) {
        currentPath = groupContext.getCurrentPath();
        order = groupContext.getOrder();
        dsl = groupContext.getDataSetListName();
        List<DataSetContext> dataSets = groupContext.getDataSets();
        DataSetContext dataSet = getDataSetByColumnNumber(dataSets, column);
        if (dataSet != null) {
            this.dataSetId = dataSet.getId();
            if (contextType == ContextType.OBJECT && this.dataSetId == null) {
                return;
            }
            if (contextType == ContextType.OBJECT_EXTENDED) {
                if (this.dataSetId == null && rootOverlapNode.containsOverlapsInPath(currentPath, column)) {
                    for (GroupContext childGroup : groupContext.getGroups()) {
                        groups.put(
                                childGroup.getName(),
                                new DataSetGroup(
                                        childGroup,
                                        column,
                                        evaluate,
                                        macroContext,
                                        dataSetParameterProvider,
                                        contextType,
                                        rootOverlapNode
                                )
                        );
                    }
                    value = dataSet.getName();
                } else {
                    return;
                }
            }
            List<ParameterContext> parametersContexts = dataSet.getParameters();
            for (ParameterContext parametersContext : parametersContexts) {
                if (contextType == ContextType.OBJECT_EXTENDED && !parametersContext.isOverlap()) {
                    continue;
                }
                AbstractParameter resolvedParameter = dataSetParameterProvider.getDataSetParameterResolved(
                        macroContext.getDataSetListContext().getDataSetListId(),
                        parametersContext.getParameterId(),
                        parametersContext.getType(),
                        evaluate,
                        macroContext,
                        new ParameterPositionContext(
                                currentPath,
                                column,
                                dataSet.getId(),
                                parametersContext.getOrder(),
                                groupContext.getDataSetListId()
                        )
                );
                if (!(contextType == ContextType.NO_NULL_VALUES && resolvedParameter.isNullValue())) {
                    parameters.put(parametersContext.getName(), resolvedParameter);
                }
            }
            for (GroupContext childGroup : groupContext.getGroups()) {
                DataSetGroup dataSetGroup = new DataSetGroup(
                        childGroup,
                        column,
                        evaluate,
                        macroContext,
                        dataSetParameterProvider,
                        contextType,
                        rootOverlapNode
                );
                if (contextType == ContextType.NO_NULL_VALUES && dataSetGroup.getParameters().isEmpty()
                        && dataSetGroup.getGroups().isEmpty()) {
                    continue;
                }
                groups.put(
                        childGroup.getName(),
                        dataSetGroup
                );
            }
            value = dataSet.getName();
        }
    }

    private DataSetContext getDataSetByColumnNumber(List<DataSetContext> dataSets, int column) {
        for (DataSetContext dataSet : dataSets) {
            if (dataSet.getColumnNumber() == column) {
                return dataSet;
            }
        }
        return null;
    }
}

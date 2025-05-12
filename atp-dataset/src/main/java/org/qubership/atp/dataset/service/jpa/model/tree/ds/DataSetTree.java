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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.GroupContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class DataSetTree {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, AbstractParameter> parameters = new LinkedHashMap<>();
    private Map<String, DataSetGroup> groups = new LinkedHashMap<>();

    /**
     * Default constructor.
     * */
    public DataSetTree(
            DataSetContext dataSetContext,
            int column,
            boolean evaluate,
            MacroContext macroContext,
            DataSetListContext dataSetListContext,
            DataSetParameterProvider dataSetParameterProvider,
            ContextType contextType
    ) {
        List<ParameterContext> parametersContexts = dataSetContext.getParameters();
        for (ParameterContext parametersContext : parametersContexts) {
            AbstractParameter resolvedParameter = dataSetParameterProvider.getDataSetParameterResolved(
                    macroContext.getDataSetListContext().getDataSetListId(),
                    parametersContext.getParameterId(),
                    parametersContext.getType(),
                    evaluate,
                    macroContext,
                    new ParameterPositionContext(
                            Collections.emptyList(),
                            column,
                            dataSetContext.getId(),
                            parametersContext.getOrder(),
                            dataSetListContext.getDataSetListId()
                    )
            );
            parameters.put(parametersContext.getName(), resolvedParameter);
        }
        for (GroupContext group : dataSetListContext.getGroups()) {
            DataSetGroup dataSetGroup = new DataSetGroup(
                    group,
                    column,
                    evaluate,
                    macroContext,
                    dataSetParameterProvider,
                    contextType,
                    dataSetListContext.getRootOverlapNode()
            );
            if (!(contextType == ContextType.NO_NULL_VALUES
                    && dataSetGroup.getGroups().isEmpty() && dataSetGroup.getParameters().isEmpty())) {
                groups.put(group.getName(), dataSetGroup);
            }
        }
    }
}

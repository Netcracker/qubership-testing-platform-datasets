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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.GroupContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;

import lombok.Getter;
import lombok.Setter;

/**
 * Used for referenced DSL node view.
 */
@Getter
@Setter
public class RefDataSetListFlat {
    private List<RefDataSetListAttribute> attributes = new LinkedList<>();
    boolean isLastPage = false;

    /**
     * Default constructor.
     */
    public RefDataSetListFlat(
            DataSetListContext dataSetListContext,
            DataSetList dataSetList,
            List<UUID> path,
            DataSetParameterProvider parameterProvider
    ) {
        GroupContext targetGroup = null;
        for (GroupContext group : dataSetListContext.getGroups()) {
            if (group.getId().equals(path.get(0))) {
                targetGroup = getGroupByPath(group, path.subList(1, path.size()));
            }
        }
        if (targetGroup != null) {
            this.isLastPage = targetGroup.isLastPage();
            List<Attribute> attributes = targetGroup.getAttributes();
            List<DataSetContext> dataSets = targetGroup.getDataSets();
            for (Attribute attribute : attributes) {
                Map<UUID, ParameterContext> parameters = new LinkedHashMap<>();
                for (DataSetContext dataSet : dataSets) {
                    for (ParameterContext parameter : dataSet.getParameters()) {
                        if (attribute.getId().equals(parameter.getAttributeId())) {
                            DataSet dataSetByColumn = dataSetList.getDataSetByColumn(dataSet.getColumnNumber());
                            parameters.put(dataSetByColumn.getId(), parameter);
                        }
                    }
                }
                this.attributes.add(
                        new RefDataSetListAttribute(
                                attribute,
                                parameters,
                                targetGroup.getCurrentPath(),
                                parameterProvider,
                                dataSetListContext.getDataSetListId()
                        )
                );
            }
        }
    }

    /**
     * Constructor for lazy pageable methods.
     */
    public RefDataSetListFlat(
            DataSetListContext dataSetListContext,
            DataSetList dataSetList,
            List<UUID> path,
            DataSetParameterProvider parameterProvider,
            boolean isLastPage) {
        this(dataSetListContext, dataSetList, path, parameterProvider);
        this.setLastPage(isLastPage);
    }

    private GroupContext getGroupByPath(GroupContext groupContext, List<UUID> path) {
        if (path.isEmpty()) {
            return groupContext;
        } else {
            for (GroupContext group : groupContext.getGroups()) {
                if (group.getId().equals(path.get(0))) {
                    return getGroupByPath(group, path.subList(1, path.size()));
                }
            }
        }
        return null;
    }

    /**
     * Mapping for top Level DS to referenced DS.
     */
    private Map<UUID, UUID> generateParentsToChildrenMap(List<DataSet> dataSets, List<Parameter> parameters) {
        Map<UUID, UUID> result = new HashMap<>();
        for (DataSet dataSet : dataSets) {
            UUID childDataSetId = null;
            for (Parameter parameter : parameters) {
                if (parameter.getDataSetId().equals(dataSet.getId())) {
                    childDataSetId = parameter.getDataSetReferenceId();
                    break;
                }
            }
            result.put(dataSet.getId(), childDataSetId);
        }
        return result;
    }
}

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

package org.qubership.atp.dataset.service.jpa.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.GroupContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

public class ParameterExtractor {
    private DataSetListContext dataSetListContext;
    private List<PathStep> referenceAttributePath;
    private List<UUID> fullAttributePath = new LinkedList<>();
    private List<UUID> macroPosition;
    private PathStep attribute;
    private int dataSetColumn;
    private UUID dataSetId;
    private Map<ParameterPositionContext, String> cachedValues = new HashMap<>();
    private DataSetParameterProvider parameterProvider;

    /**
     * Default constructor.
     * */
    public ParameterExtractor(
            DataSetListContext dataSetListContext,
            UUID dataSetId,
            int dataSetColumn,
            List<UUID> macroPosition,
            List<PathStep> referenceAttributePath,
            PathStep attribute
    ) {
        this.dataSetListContext = dataSetListContext;
        this.dataSetId = dataSetId;
        this.dataSetColumn = dataSetColumn;
        this.macroPosition = macroPosition;
        this.referenceAttributePath = referenceAttributePath;
        this.attribute = attribute;
    }

    /**
     * Extracts value from DSL context, and returns it if found.
     * */
    public String extractWithProvider(DataSetParameterProvider parameterProvider) throws DataSetServiceException {
        List<DataSetContext> dataSets = dataSetListContext.getDataSets();
        this.parameterProvider = parameterProvider;
        if (dataSets.isEmpty()) {
            throw new DataSetServiceException("Can't find referenced variable");
        }
        if (macroPosition.isEmpty()) {
            return extractFromList(dataSetListContext.getDataSetListId(), macroPosition);
        } else {
            for (GroupContext group : dataSetListContext.getGroups()) {
                if (macroPosition.get(0).equals(group.getId())) {
                    fullAttributePath.add(macroPosition.get(0));
                    try {
                        return extractFromGroup(
                                group,
                                macroPosition.subList(1, macroPosition.size()),
                                referenceAttributePath
                        );
                    } catch (DataSetServiceException e) {
                        return extractFromList(dataSetListContext.getDataSetListId(), Collections.emptyList());
                    }
                }
            }
        }
        throw new DataSetServiceException("Can't find referenced variable");
    }

    private String extractFromGroup(GroupContext group, List<UUID> macroPosition,
                                    List<PathStep> pathSteps) throws DataSetServiceException {
        if (macroPosition.isEmpty()) {
            return getFromGroupByPath(group, macroPosition, pathSteps);
        } else {
            for (GroupContext groupGroup : group.getGroups()) {
                if (macroPosition.get(0).equals(groupGroup.getId())) {
                    try {
                        return extractFromGroup(
                                groupGroup,
                                macroPosition.subList(1, macroPosition.size()),
                                pathSteps
                        );
                    } catch (DataSetServiceException e) {
                        return getFromGroupByPath(group, Collections.emptyList(), pathSteps);
                    }
                }
            }
            throw new DataSetServiceException("Can't find referenced variable");
        }
    }

    private String getFromGroupByPath(
            GroupContext group, List<UUID> macroPosition, List<PathStep> pathSteps) throws DataSetServiceException {
        if (pathSteps.isEmpty() || pathSteps.get(0).matches(
                group.getName(),
                group.getId()
        )) {
            for (DataSetContext dataSet : group.getDataSets()) {
                if (dataSet.getColumnNumber() == dataSetColumn) {
                    for (ParameterContext parameter : dataSet.getParameters()) {
                        if (attribute.matches(
                                parameter.getName(),
                                parameter.getAttributeId()
                        )) {
                            return tryGetFromCache(
                                    dataSetListContext,
                                    parameter,
                                    new ParameterPositionContext(
                                            fullAttributePath,
                                            dataSetColumn,
                                            dataSet.getId(),
                                            parameter.getOrder(),
                                            group.getDataSetListId()
                                    )
                            );
                        }
                    }
                }
            }
        } else {
            for (GroupContext groupGroup : group.getGroups()) {
                if (pathSteps.get(0).matches(
                        groupGroup.getName(),
                        groupGroup.getId()
                )) {
                    fullAttributePath.add(groupGroup.getId());
                    return extractFromGroup(
                            groupGroup,
                            macroPosition,
                            pathSteps.subList(1, pathSteps.size())
                    );
                }
            }
        }
        throw new DataSetServiceException("Can't find referenced variable");
    }

    private String extractFromList(UUID dataSetListId, List<UUID> macroPosition) throws DataSetServiceException {
        if (referenceAttributePath.isEmpty()) {
            for (DataSetContext dataSet : dataSetListContext.getDataSets()) {
                if (dataSet.getColumnNumber() == dataSetColumn) {
                    List<ParameterContext> parameters = dataSet.getParameters();
                    for (ParameterContext parameter : parameters) {
                        if (attribute.matches(
                                parameter.getName(),
                                parameter.getAttributeId()
                        )
                        ) {
                            return tryGetFromCache(
                                    dataSetListContext,
                                    parameter,
                                    new ParameterPositionContext(
                                            Collections.emptyList(),
                                            dataSetColumn,
                                            dataSet.getId(),
                                            parameter.getOrder(),
                                            dataSetListId
                                    )
                            );
                        }
                    }
                }
            }
            throw new DataSetServiceException("Can't find referenced variable");
        }
        List<GroupContext> groups = dataSetListContext.getGroups();
        for (GroupContext group : groups) {
            if (referenceAttributePath.get(0).matches(
                    group.getName(),
                    group.getId()
            )
            ) {
                fullAttributePath.add(group.getId());
                return extractFromGroup(
                        group,
                        macroPosition,
                        referenceAttributePath.subList(1, referenceAttributePath.size())
                );
            }
        }
        throw new DataSetServiceException("Can't find referenced variable");
    }

    public void setCache(Map<ParameterPositionContext, String> cachedValues) {
        this.cachedValues = cachedValues;
    }

    /**
     * Get from cache evaluated value. If value not been cached - evaluate and cache it.
     * */
    public String tryGetFromCache(
            DataSetListContext dataSetListContext,
            ParameterContext parameter,
            ParameterPositionContext positionContext
    ) {
        if (!cachedValues.containsKey(positionContext)) {
            MacroContext macroContext = new MacroContext();
            macroContext.setMacroContextService(parameterProvider.getMacroContextService());
            macroContext.setDataSetListContext(dataSetListContext);
            macroContext.setMacros(parameterProvider.getAtpMacros(dataSetListContext.getVisibilityAreaId()));
            macroContext.setMacrosCalculator(parameterProvider.getMacrosCalculator());
            AbstractParameter dataSetParameterResolved = parameterProvider.getDataSetParameterResolved(
                    dataSetListContext.getDataSetListId(),
                    parameter.getParameterId(),
                    parameter.getType(),
                    true,
                    macroContext,
                    positionContext
            );
            String value = dataSetParameterResolved.getValue();
            cachedValues.put(positionContext, value);
            return value;
        } else {
            return cachedValues.get(positionContext);
        }
    }
}

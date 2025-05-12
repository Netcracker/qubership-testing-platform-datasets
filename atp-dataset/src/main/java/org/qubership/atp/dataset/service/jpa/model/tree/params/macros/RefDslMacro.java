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

package org.qubership.atp.dataset.service.jpa.model.tree.params.macros;

import static java.lang.String.join;
import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.dataset.antlr4.TextParameterParser;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.impl.macro.CachedDslMacroResult;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.PathStep;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefDslMacro extends AbstractRef {

    private static final Logger LOG = LoggerFactory.getLogger(RefDslMacro.class);

    public static final String MACRO_NAME = "REF_DSL";

    public RefDslMacro(
            String realMacroName,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        super(realMacroName, macroContext, parameterPositionContext);
    }

    @Override
    public String getEvaluatedValue(List<String> arguments) {
        if (isNull(arguments)) {
            return null;
        }
        if (arguments.isEmpty()) {
            return EMPTY;
        }
        String[] splits = join(EMPTY, arguments).split("\\.");
        if (splits.length < 3) {
            throw new IllegalArgumentException("Invalid parameter path");
        }
        PathStep attribute;
        PathStep dataSetList = new PathStep(splits[0]);
        PathStep dataSet = new PathStep(splits[1]);
        List<PathStep> attributeGroupPath = new LinkedList<>();
        for (int i = 2; i < splits.length - 1; i++) {
            attributeGroupPath.add(new PathStep(splits[i]));
        }
        attribute = new PathStep(splits[splits.length - 1]);
        try {
            collectDataSetAndDataSetListsUuids(dataSet, dataSetList);
            UUID dataSetListId = getParameterPositionContext().getDataSetListId();
            UUID dataSetId = getParameterPositionContext().getDataSetInColumnId();
            //For case, when DSL macro references on his parent DSL.
            //So this behaviour will be like in REF_THIS macro
            //#REF_DSL(DSL.DATASET.ATTRIBUTE) == #REF_THIS(ATTRIBUTE)
            if (dataSetListId != null && dataSetId != null
                    && dataSetList.matches(null, dataSetListId)
                    && dataSet.matches(null, dataSetId)
            ) {
                return getMacroContext()
                        .getTextParameterFromCachedContextByNamesPath(
                                getParameterPositionContext(),
                                attributeGroupPath,
                                attribute
                        );
            }
            UUID visibilityAreaId = getMacroContext().getDataSetListContext().getVisibilityAreaId();
            //Cache used for use cases, when multiple parameters references
            // to the same DSL parameter with generated data,
            //like INN(), or DATE, or RANDOM_BETWEEN macros, so each reference will obtain identical value.
            CachedDslMacroResult cachedDslTextParameter = getMacroContext().getCachedDslTextParameter(
                    dataSetList, dataSet, attributeGroupPath, attribute
            );
            if (cachedDslTextParameter == null) {
                String value = getMacroContext().getDslTextParameter(
                        visibilityAreaId, dataSetList, dataSet, attributeGroupPath, attribute
                );
                TextParameterParser parser = new TextParameterParser(
                        getMacroContext(), getParameterPositionContext()
                );
                if (StringUtils.isEmpty(value)) {
                    return value;
                }
                List<AbstractTextParameter> parsedResult = parser.parse(value, isEvaluate());
                StringBuilder stringBuilder = new StringBuilder();
                for (AbstractTextParameter abstractTextParameter : parsedResult) {
                    stringBuilder.append(abstractTextParameter.getValue());
                }
                String result = stringBuilder.toString();
                getMacroContext().storeCachedDslTextParameter(
                        dataSetList, dataSet, attributeGroupPath, attribute, result
                );
                return result;
            } else {
                return cachedDslTextParameter.getValue();
            }

        } catch (DataSetServiceException e) {
            return e.getMessage();
        }
    }

    @Override
    public String getUnevaluatedValue(String childrenValue) {
        if (StringUtils.isEmpty(childrenValue)) {
            return super.getUnevaluatedValue(childrenValue);
        }

        String[] splits = childrenValue.split("\\.");
        if (splits.length < 3) {
            return super.getUnevaluatedValue(childrenValue);
        }
        try {
            List<String> content = new LinkedList<>();
            if (Utils.isUuid(splits[0])) {
                String dataSetListName = getMacroContext().getDataSetListName(UUID.fromString(splits[0]));
                if (dataSetListName == null) {
                    dataSetListName = splits[0];
                }
                content.add(dataSetListName);
            } else {
                content.add(splits[0]);
            }
            if (Utils.isUuid(splits[1])) {
                String dataSetName = getMacroContext().getDataSetName(UUID.fromString(splits[1]));
                if (dataSetName == null) {
                    dataSetName = splits[1];
                }
                content.add(dataSetName);
            } else {
                content.add(splits[1]);
            }
            for (int i = 2; i < splits.length; i++) {
                if (Utils.isUuid(splits[i])) {
                    String attributeName = getMacroContext().getAttributeName(UUID.fromString(splits[i]));
                    if (attributeName == null) {
                        attributeName = splits[i];
                    }
                    content.add(attributeName);
                } else {
                    content.add(splits[i]);
                }
            }
            return super.getUnevaluatedValue(StringUtils.join(content, "."));
        } catch (Exception e) {
            LOG.debug("Error macro values translation", e);
        }
        return super.getUnevaluatedValue(childrenValue);
    }

    private void collectDataSetAndDataSetListsUuids(PathStep dataSet, PathStep dataSetList)
            throws DataSetServiceException {
        UUID dataSetListId;
        if (dataSetList.getId() == null) {
            UUID visibilityAreaId = getMacroContext().getDataSetListContext().getVisibilityAreaId();
            DataSetList discoveredDataSetList = getMacroContext().getDataSetList(visibilityAreaId, dataSetList);
            dataSetListId = discoveredDataSetList.getId();
        } else {
            dataSetListId = dataSetList.getId();
        }
        dataSetLists.add(dataSetListId);
        if (dataSet.getId() == null) {
            UUID dataSetId = getMacroContext().getDataSetIdByNameAndDataSetList(dataSet.getName(), dataSetListId);
            if (dataSetId != null) {
                dataSets.add(dataSetId);
            }
        } else {
            dataSets.add(dataSet.getId());
        }
    }
}

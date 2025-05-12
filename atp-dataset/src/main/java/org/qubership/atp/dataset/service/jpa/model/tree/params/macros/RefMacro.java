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
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.PathStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefMacro extends AbstractRef {

    private static final Logger LOG = LoggerFactory.getLogger(RefMacro.class);

    public static final String MACRO_NAME = "REF";

    public RefMacro(
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
        if (splits.length < 2) {
            throw new IllegalArgumentException("Invalid parameter path");
        }
        List<PathStep> attributeGroupsPath = new LinkedList<>();
        PathStep dataSet = new PathStep(splits[0]);
        collectDataSetUuid(dataSet);
        for (int i = 1; i < splits.length - 1; i++) {
            attributeGroupsPath.add(new PathStep(splits[i]));
        }
        PathStep attribute = new PathStep(splits[splits.length - 1]);
        return getMacroContext().getTextParameter(dataSet, attributeGroupsPath, attribute);
    }

    @Override
    public String getUnevaluatedValue(String childrenValue) {
        if (StringUtils.isEmpty(childrenValue)) {
            return super.getUnevaluatedValue(childrenValue);
        }
        try {
            List<String> content = new LinkedList<>();
            String[] splits = childrenValue.split("\\.");
            if (Utils.isUuid(splits[0])) {
                String dataSetName = getMacroContext().getDataSetName(UUID.fromString(splits[0]));
                if (dataSetName == null) {
                    dataSetName = splits[0];
                }
                content.add(dataSetName);
            } else {
                content.add(splits[0]);
            }
            for (int i = 1; i < splits.length; i++) {
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
            super.getUnevaluatedValue(StringUtils.join(content, "."));
        } catch (Exception e) {
            LOG.debug("Error macro values translation", e);
        }
        return super.getUnevaluatedValue(childrenValue);
    }

    private void collectDataSetUuid(PathStep dataSet) {
        if (dataSet.getId() == null) {
            UUID dataSetId = getMacroContext().getDataSetIdByNameAndDataSetList(dataSet.getName(),
                    getMacroContext().getDataSetListContext().getDataSetListId());
            if (dataSetId != null) {
                dataSets.add(dataSetId);
            }
        } else {
            dataSets.add(dataSet.getId());
        }

    }
}

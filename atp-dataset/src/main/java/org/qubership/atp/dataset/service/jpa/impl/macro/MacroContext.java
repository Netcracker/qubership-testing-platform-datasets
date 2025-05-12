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

package org.qubership.atp.dataset.service.jpa.impl.macro;

import static java.util.Objects.nonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.dataset.service.jpa.model.PathStep;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.model.Macros;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MacroContext {

    @Getter
    private Map<String, Object> atpContext = new HashMap<>();
    @Getter
    private DataSetListContext dataSetListContext;
    @Getter
    @Setter
    private MacroContextService macroContextService;
    @Getter
    @Setter
    private List<Macros> macros = new ArrayList<>();
    @Getter
    @Setter
    private MacrosCalculator macrosCalculator;

    /**
     * For REF_DSL.
     */
    public CachedDslMacroResult getCachedDslTextParameter(
            PathStep dataSetList,
            PathStep dataSetId,
            List<PathStep> attributeGroups,
            PathStep attributeId
    ) throws DataSetServiceException {
        CachedDslMacroResultContainer dslMacroCache = macroContextService.getDslMacroCache();
        return dslMacroCache.getCachedValue(
                dataSetList,
                dataSetId,
                attributeGroups,
                attributeId
        );
    }

    /**
     * Cache method For REF_DSL.
     */
    public void storeCachedDslTextParameter(
            PathStep dataSetList,
            PathStep dataSetId,
            List<PathStep> attributeGroups,
            PathStep attributeId,
            String value
    ) throws DataSetServiceException {
        CachedDslMacroResultContainer dslMacroCache = macroContextService.getDslMacroCache();
        dslMacroCache.storeValue(
                dataSetList,
                dataSetId,
                attributeGroups,
                attributeId,
                value
        );
    }

    /**
     * For REF_DSL.
     */
    public String getDslTextParameter(
            UUID visibilityAreaId,
            PathStep dataSetList,
            PathStep dataSetId,
            List<PathStep> attributeGroups,
            PathStep attributeId
    ) throws DataSetServiceException {
        return macroContextService.getTextParameterByExternalListDataSetAndPath(
                visibilityAreaId,
                dataSetList,
                dataSetId,
                attributeGroups,
                attributeId
        );
    }

    /**
     * For REF.
     */
    public String getTextParameter(PathStep dataSet, List<PathStep> attributeGroups, PathStep attribute) {
        try {
            return macroContextService.getTextParameterByListDataSetAndPath(
                    dataSetListContext.getVisibilityAreaId(),
                    new PathStep(dataSetListContext.getDataSetListId()),
                    dataSet,
                    attributeGroups,
                    attribute);
        } catch (DataSetServiceException e) {
            log.error("Referenced variable for {} not found", attribute, e);
            return "[" + e.getMessage() + "]";
        }
    }

    /**
     * For REF_THIS.
     */
    public String getTextParameterFromCachedContextByNamesPath(ParameterPositionContext parameterPositionContext,
                                                               List<PathStep> attributeGroupsPath,
                                                               PathStep attribute) {
        UUID topLevelDataSetListId = getDataSetListContext().getDataSetListId();
        UUID visibilityAreaId = getDataSetListContext().getVisibilityAreaId();
        try {
            return macroContextService.getTextParameterFromCachedContextByNamesPath(
                    visibilityAreaId,
                    new PathStep(topLevelDataSetListId),
                    parameterPositionContext.getDataSetInColumnId(),
                    parameterPositionContext.getDataSetColumn(),
                    parameterPositionContext.getPath(),
                    attributeGroupsPath,
                    attribute
            );
        } catch (DataSetServiceException e) {
            log.error("Referenced variable for {} not found", attribute, e);
            return "[" + e.getMessage() + "]";
        }
    }

    public String getValueFromAtpContext(String variable) {
        Object value = atpContext.getOrDefault(variable, null);
        return nonNull(value) ? value.toString() : null;
    }

    public void addAtpDataSetContext(String jsonString) {
        atpContext.putAll(deserializeContext(jsonString));
    }

    /**
     * Set to atp context data set attribute name and value.
     * */
    public void fillAtpDataSetContext(DataSet dataSet) {
        List<Parameter> parameters = dataSet.getParameters();
        parameters
                .forEach(parameter -> {
                    Attribute attribute = parameter.getAttribute();
                    if (attribute != null) {
                        atpContext.put(attribute.getName(), parameter.getParameterValueByType());
                    }
                });
    }

    public void setDataSetListContext(DataSetListContext dataSetListContext) {
        this.dataSetListContext = dataSetListContext;
    }

    private Map<String, String> deserializeContext(String context) {
        Type itemsMapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return new Gson().fromJson(context, itemsMapType);
    }

    public String getDataSetListName(UUID dataSetListId) throws DataSetServiceException {
        return macroContextService.getDataSetListName(dataSetListId);
    }

    public String getDataSetName(UUID dataSetId) throws DataSetServiceException {
        return macroContextService.getDataSetName(dataSetId);
    }

    public String getAttributeName(UUID attributeId) throws DataSetServiceException {
        return macroContextService.getAttributeName(attributeId);
    }

    @SneakyThrows
    public UUID getDataSetIdByNameAndDataSetList(String name, UUID dataSetListId) {
        return macroContextService.getDataSetUuid(name, dataSetListId);
    }

    public DataSetList getDataSetList(UUID visibilityAreaId, PathStep dataSetList) throws DataSetServiceException {
        return macroContextService.getDataSetList(visibilityAreaId, dataSetList);
    }
}

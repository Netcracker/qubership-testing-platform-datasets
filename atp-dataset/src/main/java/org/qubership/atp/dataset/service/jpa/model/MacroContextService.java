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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.impl.macro.CachedDslMacroResultContainer;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

public interface MacroContextService {
    String getTextParameterByListDataSetAndPath(UUID visibilityAreaId,
                                                PathStep dataSetList,
                                                PathStep dataSet,
                                                List<PathStep> referenceAttributePath,
                                                PathStep parameterAttribute) throws DataSetServiceException;

    String getTextParameterByExternalListDataSetAndPath(UUID visibilityAreaId,
                                                        PathStep dataSetList,
                                                        PathStep dataSet,
                                                        List<PathStep> referenceAttributePath,
                                                        PathStep parameterAttribute) throws DataSetServiceException;

    String getTextParameterFromCachedContextByNamesPath(UUID visibilityAreaId,
                                                        PathStep topLevelDataSetList,
                                                        UUID dataSetId,
                                                        int dataSetColumn,
                                                        List<UUID> macroPosition,
                                                        List<PathStep> pathSteps,
                                                        PathStep attribute) throws DataSetServiceException;

    String getDataSetListName(UUID dataSetListId) throws DataSetServiceException;

    String getDataSetName(UUID dataSetListId) throws DataSetServiceException;

    UUID getDataSetUuid(String dataSetName, UUID dataSetListId) throws DataSetServiceException;

    DataSetList getDataSetList(UUID visibilityAreaId, PathStep dataSetListPathStep) throws DataSetServiceException;

    String getAttributeName(UUID dataSetListId) throws DataSetServiceException;

    Map<ParameterPositionContext, String> getCachedEvaluatedValues(UUID dataSetListId);

    CachedDslMacroResultContainer getDslMacroCache();

    void dropLocalThreadCache();
}

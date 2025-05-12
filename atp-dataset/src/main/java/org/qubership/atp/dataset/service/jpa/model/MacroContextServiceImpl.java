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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListContextService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.CachedDslMacroResultContainer;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.tree.DataSetListEvaluatedParametersCache;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.RefDslMacro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MacroContextServiceImpl implements MacroContextService {

    @Autowired
    protected DataSetListContextService dataSetListContextService;
    @Autowired
    protected ModelsProvider modelsProvider;

    @Lazy
    @Autowired
    protected DataSetParameterProvider parameterProvider;
    /**
     * Cached parameter values. Used for macros, if they reference to some
     * parameter with not yet evaluated macro value.
     * Value being evaluated and stored in cache, just to make sure it's values will be
     * always the same for any references.
     */
    protected ThreadLocal<DataSetListEvaluatedParametersCache> cachedEvaluatedValues = new ThreadLocal<>();

    protected ThreadLocal<CachedDslMacroResultContainer> cachedDslMacroValues = new ThreadLocal<>();

    /**
     * For REF. Gets parameter from DS context (probably cached).
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public String getTextParameterByListDataSetAndPath(UUID visibilityAreaId,
                                                       PathStep dataSetList,
                                                       PathStep dataSetPath,
                                                       List<PathStep> referenceAttributePath,
                                                       PathStep attribute) throws DataSetServiceException {
        dataSetPath = translateDataSetIdToName(dataSetPath);
        referenceAttributePath = findAndTranslateAttributesIdsToNames(referenceAttributePath);
        attribute = findAndTranslateAttributeIdToName(attribute);

        DataSetList topLevelDataSetList = getDataSetList(visibilityAreaId, dataSetList);
        Pair<Integer, UUID> dataSetColumnAndId = getDataSetColumnByIdOrName(topLevelDataSetList, dataSetPath);
        AttributeTypeName[] attributesToLoad = {
                AttributeTypeName.TEXT,
                AttributeTypeName.ENCRYPTED,
                AttributeTypeName.LIST
        };
        DataSetListContext dataSetListContext = dataSetListContextService.getDataSetListContext(
                topLevelDataSetList.getId(),
                Collections.singletonList(dataSetColumnAndId.getKey()),
                Arrays.asList(attributesToLoad),
                null
        );
        ParameterExtractor extractor = new ParameterExtractor(
                dataSetListContext,
                dataSetColumnAndId.getValue(),
                dataSetColumnAndId.getKey(),
                Collections.emptyList(),
                referenceAttributePath,
                attribute
        );
        extractor.setCache(getCachedEvaluatedValues(dataSetListContext.getDataSetListId()));
        return extractor.extractWithProvider(parameterProvider);
    }

    /**
     * For REF_DSL.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public String getTextParameterByExternalListDataSetAndPath(UUID visibilityAreaId,
                                                               PathStep dataSetList,
                                                               PathStep dataSetPath,
                                                               List<PathStep> referenceAttributePath,
                                                               PathStep attribute) throws DataSetServiceException {
        dataSetPath = translateDataSetIdToName(dataSetPath);
        referenceAttributePath = findAndTranslateAttributesIdsToNames(referenceAttributePath);
        attribute = findAndTranslateAttributeIdToName(attribute);
        DataSetList topLevelDataSetList = getDataSetList(visibilityAreaId, dataSetList);
        DataSetListParameterExtractor dataSetListParameterExtractor =
                new DataSetListParameterExtractor(
                        topLevelDataSetList, dataSetPath, referenceAttributePath, attribute
                );
        return dataSetListParameterExtractor.extract();
    }

    /**
     * For REF_THIS.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public String getTextParameterFromCachedContextByNamesPath(UUID visibilityAreaId,
                                                               PathStep topLevelDataSetList,
                                                               UUID dataSetId,
                                                               int dataSetColumn,
                                                               List<UUID> macroPosition,
                                                               List<PathStep> pathSteps,
                                                               PathStep attribute) throws DataSetServiceException {
        pathSteps = findAndTranslateAttributesIdsToNames(pathSteps);
        attribute = findAndTranslateAttributeIdToName(attribute);
        DataSetList dataSetList = getDataSetList(visibilityAreaId, topLevelDataSetList);
        AttributeTypeName[] attributesToLoad = {
                AttributeTypeName.TEXT,
                AttributeTypeName.ENCRYPTED,
                AttributeTypeName.LIST,
                AttributeTypeName.DSL
        };
        DataSetListContext dataSetListContext = dataSetListContextService.getDataSetListContext(
                dataSetList.getId(),
                Collections.singletonList(dataSetColumn),
                Arrays.asList(attributesToLoad),
                null
        );
        ParameterExtractor extractor = new ParameterExtractor(
                dataSetListContext,
                dataSetId,
                dataSetColumn,
                macroPosition,
                pathSteps,
                attribute
        );
        extractor.setCache(getCachedEvaluatedValues(dataSetList.getId()));
        return extractor.extractWithProvider(parameterProvider);
    }

    @Override
    public String getDataSetListName(UUID dataSetListId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        if (dataSetList == null) {
            return null;
        }
        return dataSetList.getName();
    }

    @Override
    public String getDataSetName(UUID dataSetListId) throws DataSetServiceException {
        DataSet dataSet = modelsProvider.getDataSetById(dataSetListId);
        if (dataSet == null) {
            return null;
        }
        return dataSet.getName();
    }

    @Override
    public UUID getDataSetUuid(String dataSetName, UUID dataSetListId) throws DataSetServiceException {
        List<DataSet> dataSets = modelsProvider.getDataSetByNameAndDataSetListId(dataSetName, dataSetListId);
        if (CollectionUtils.isEmpty(dataSets)) {
            return null;
        }
        if (dataSets.size() > 1) {
            throw new DataSetServiceException(
                    String.format("Multiple datasets found with name %s under dslId %s", dataSetName, dataSetListId));
        }
        return dataSets.get(0).getId();
    }

    @Override
    public String getAttributeName(UUID dataSetListId) throws DataSetServiceException {
        Attribute attribute = modelsProvider.getAttributeById(dataSetListId);
        if (attribute == null) {
            return null;
        }
        return attribute.getName();
    }

    private List<PathStep> findAndTranslateAttributesIdsToNames(List<PathStep> steps) {
        List<PathStep> result = new LinkedList<>();
        for (PathStep pathStep : steps) {
            result.add(findAndTranslateAttributeIdToName(pathStep));
        }
        return result;
    }

    private PathStep findAndTranslateAttributeIdToName(PathStep step) {
        if (step.getId() == null) {
            return step;
        }
        Attribute attribute = modelsProvider.getAttributeById(step.getId());
        if (attribute != null) {
            return new PathStep(attribute.getName());
        }
        return step;
    }

    private PathStep translateDataSetIdToName(PathStep step) {
        if (step.getId() == null) {
            return step;
        }
        DataSetList dataSetList = modelsProvider.getDataSetListById(step.getId());
        if (dataSetList != null) {
            return new PathStep(dataSetList.getName());
        }
        return step;
    }

    /**
     * Get dataSetList, by its name or id, provided in dataSetListPathStep parameter.
     * Useful to collect referenced from
     * {@link RefDslMacro}
     * dataSetLists. Used in Export functionality.
     */
    public DataSetList getDataSetList(UUID visibilityAreaId, PathStep dataSetListPathStep)
            throws DataSetServiceException {
        VisibilityArea visibilityArea = modelsProvider.getVisibilityAreaById(visibilityAreaId);
        DataSetList result = null;
        if (visibilityArea == null) {
            throw new DataSetServiceException("Visibility Area not found " + visibilityAreaId);
        }
        if (dataSetListPathStep.getId() != null) {
            result = visibilityArea.getDataSetListById(dataSetListPathStep.getId());
        }
        if (result == null) {
            result = visibilityArea.getDataSetListByName(dataSetListPathStep.getName());
        }
        if (result == null) {
            throw new DataSetServiceException("Data Set List not found " + dataSetListPathStep);
        }
        return result;
    }

    private Pair<Integer, UUID> getDataSetColumnByIdOrName(DataSetList dataSetList,
                                                           PathStep dataSetToSearch) throws DataSetServiceException {
        Pair<Integer, UUID> result = null;
        if (dataSetToSearch.getId() != null) {
            result = new ImmutablePair<>(
                    dataSetList.getDataSetColumnById(dataSetToSearch.getId()),
                    dataSetToSearch.getId()
            );
        }
        if (result == null) {
            result = new ImmutablePair<>(
                    dataSetList.getDataSetColumnByName(dataSetToSearch.getName()),
                    dataSetList.getDataSetByName(dataSetToSearch.getName()).getId()
            );
        }
        if (result == null) {
            throw new DataSetServiceException("Data Set not found " + dataSetToSearch);
        }
        return result;
    }

    /**
     * Returns evaluated parameters (in reference macros) by DSL ID.
     */
    public Map<ParameterPositionContext, String> getCachedEvaluatedValues(UUID dataSetListId) {
        if (cachedEvaluatedValues.get() == null) {
            cachedEvaluatedValues.set(new DataSetListEvaluatedParametersCache());
        }
        return cachedEvaluatedValues.get().getCache(dataSetListId);
    }

    public void dropLocalThreadCache() {
        cachedEvaluatedValues.remove();
        cachedDslMacroValues.remove();
    }

    /**
     * Returns evaluated parameters (in reference DSL macros).
     */
    public CachedDslMacroResultContainer getDslMacroCache() {
        if (cachedDslMacroValues.get() == null) {
            cachedDslMacroValues.set(new CachedDslMacroResultContainer());
        }
        return cachedDslMacroValues.get();
    }
}

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

package org.qubership.atp.dataset.service.direct.importexport.converters;

import static org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils.getDatasetKey;
import static org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils.isBlankRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportModel;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetParameterValue;
import org.qubership.atp.dataset.service.direct.importexport.models.ParameterImportResponse;
import org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.springframework.stereotype.Component;

import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatasetLinkAttributeImportConverter extends AbstractAttributeConverter
        implements AttributeImportConverter {

    private final ParameterService parameterService;

    /**
     * {@inheritDoc}
     */
    public ParameterImportResponse importAttributeParameter(AttributeImportModel importModel,
                                                            DatasetParameterValue datasetParameterValue,
                                                            AttributeImportContext importContext) {
        log.debug("Import dsl attribute parameter, import model: '{}', dataset param value: '{}', import context: '{}'",
                importModel, datasetParameterValue, importContext);
        final UUID attributeId = importModel.getId();
        final String attributeName = importModel.getName();
        final String datasetName = datasetParameterValue.getDatasetName();
        final UUID datasetId = datasetParameterValue.getDatasetId();
        final String parameterValue = datasetParameterValue.getTextValue();
        final String datasetListReference = datasetParameterValue.getDatasetListReference();
        final String datasetReference = datasetParameterValue.getDatasetReference();
        final String refDatasetKey = getDatasetKey(datasetListReference, datasetReference);
        final UUID refDatasetId = importContext.getRefDatasetId(refDatasetKey);
        final boolean isJavers = importContext.isJavers();

        List<UUID> attributePath = importModel.getPath();
        boolean isDataChanges = false;

        final boolean isRootDslAttribute = attributeName.equals(importModel.getRootDatasetListName());
        log.debug("Is root dsl attribute: {}", isRootDslAttribute);

        String targetRefDsName = Strings.EMPTY;
        if (!isRootDslAttribute) {
            boolean attributeShouldOverlap = isAttributeShouldOverlap(importModel, datasetParameterValue, importContext,
                    (parameter) -> !refDatasetId.equals(parameter.getDataSetReference().getId()));
            if (!attributeShouldOverlap) {
                attributePath = Collections.emptyList();
                final String refAttributeKey = getRefAttributeKey(importModel, datasetParameterValue);
                final Parameter refDsParameter = importContext.getRefParameter(refAttributeKey);
                targetRefDsName = getTargetRefDsName(refDsParameter, targetRefDsName);
            } else {
                targetRefDsName = importContext.getTargetDslOverlaps()
                        .get(datasetParameterValue.getDatasetName() + "_" + importModel.getKey());
            }
        } else {
            Parameter refDsParameter = parameterService.getByDataSetIdAttributeId(datasetId, attributeId);
            targetRefDsName = getTargetRefDsName(refDsParameter, targetRefDsName);
        }

        final String keyAttributeValue = importModel.getKey();

        if (StringUtils.isNotEmpty(datasetReference) && !Objects.equals(datasetReference, targetRefDsName)) {
            log.info("Import parameter for dsl attribute Key '{}' Id '{}' in dataset '{}' with value '{}' "
                            + "and reference dataset '{}'", keyAttributeValue, attributeId, datasetName,
                    parameterValue, refDatasetId);
            Parameter parameter = parameterService.setParamSelectJavers(datasetId, attributeId, attributePath,
                    null, refDatasetId, null, isJavers);
            isDataChanges = true;
            log.debug("Parameter with has been successfully imported: {}", parameter);
        } else if (StringUtils.isEmpty(datasetReference) && isRootDslAttribute
                && StringUtils.isNotEmpty(targetRefDsName)) {
            final DataSet dataset = importContext.getDataset(datasetId);
            final UUID datasetListId = dataset.getDataSetList().getId();
            log.info("Delete parameter of attribute Key '{}' Id '{}' for dsl '{}' and ds '{}'",
                    keyAttributeValue, attributeId, datasetListId, datasetId);
            parameterService.deleteParamSelectJavers(attributeId, datasetId, datasetListId, attributePath,
                    false);
            isDataChanges = true;
        }
        return new ParameterImportResponse(isDataChanges ? datasetId : null);
    }

    private String getTargetRefDsName(Parameter refDsParameter, String targetRefDsName) {
        org.qubership.atp.dataset.model.DataSet targetRefDs;
        if (Objects.nonNull(refDsParameter)) {
            targetRefDs = refDsParameter.getDataSetReference();
            targetRefDsName = Objects.nonNull(targetRefDs) ? targetRefDs.getName() : targetRefDsName;
        }
        return targetRefDsName;
    }

    @Override
    public AttributeImportModel mapRowToImportModel(Map<Integer, String> row, AttributeImportContext importContext,
                                                    ListIterator<Map<Integer, String>> rowsIterator) {
        log.debug("Map excel row to DSL attribute import model");
        AttributeImportModel rootDslAttributeImportModel = mapTextRowToImportModel(row, importContext);

        final String rootDatasetListName = rootDslAttributeImportModel.getRootDatasetListName();
        if (rootDatasetListName == null) {
            String rootAttributeName = rootDslAttributeImportModel.getName();
            log.debug("Set root DSL list name: {}", rootAttributeName);
            rootDslAttributeImportModel.setRootDatasetListName(rootAttributeName);
        }

        // check if next row is DSL child attribute or standalone
        if (rowsIterator.hasNext()) {
            final Map<Integer, String> nextRow = rowsIterator.next();
            if (!isBlankRow(nextRow)) {
                final String nextAttributeKey = getAttributeKey(nextRow);
                log.debug("Next row attribute key: {}", nextAttributeKey);

                boolean isDslChildAttribute = ImportUtils.isArrowDelimiterPresent(nextAttributeKey);
                log.debug("Rollback to previous row");
                rowsIterator.previous();
                if (isDslChildAttribute) {
                    return mapChildrenRowsToImportModel(rootDslAttributeImportModel, importContext, rowsIterator);
                } else {
                    return rootDslAttributeImportModel;
                }
            }
        }

        return rootDslAttributeImportModel;
    }

    private AttributeImportModel mapChildrenRowsToImportModel(AttributeImportModel parentModel,
                                                              AttributeImportContext importContext,
                                                              ListIterator<Map<Integer, String>> rowsIterator) {
        log.debug("Map DSL attribute children excel rows to import model. Parent import model: {}", parentModel);
        final String parentAttributeName = parentModel.getKey();
        log.debug("Parent attribute name: {}", parentAttributeName);
        while (rowsIterator.hasNext()) {
            final Map<Integer, String> row = rowsIterator.next();

            if (!isBlankRow(row)) {
                final String attributeName = getRowAttributeName(row);
                log.debug("Child attribute name: {}", attributeName);
                if (attributeName.startsWith(parentAttributeName)) {
                    AttributeImportModel childModel = mapTextRowToImportModel(row, importContext);
                    childModel.setParent(parentModel);
                    log.debug("Child import model: {}", childModel);
                    String attributeKey = childModel.getKey();
                    UUID childAttributeId = importContext.getAttributeId(attributeKey);
                    log.debug("Child attribute id: {}", childAttributeId);
                    AttributeType attributeType = childModel.getType();

                    childModel.setId(childAttributeId);
                    childModel.addPath(parentModel);
                    String parentDslReference = importContext.getAttributeDslRef(parentModel.getKey());
                    childModel.setDatasetListReference(parentDslReference);
                    boolean isChildDslAttribute = AttributeType.DSL.equals(attributeType);
                    if (isChildDslAttribute) {
                        childModel.setRootDatasetListName(parentAttributeName);
                        log.debug("Set root DSL name: {}", parentAttributeName);
                        parentModel.addChildren(mapChildrenRowsToImportModel(childModel, importContext, rowsIterator));
                    } else {
                        childModel.fillParentDslReferences(parentModel);
                        log.debug("Fill parent DSL references");
                        parentModel.addChildren(childModel);
                    }
                } else {
                    rowsIterator.previous();
                    break;
                }
            }
        }

        return parentModel;
    }

    @Override
    public List<ParameterImportResponse> validate(AttributeImportModel model, AttributeImportContext context) {
        log.debug("Validate DSL attribute import model. Import model: '{}', context: '{}'", model, context);

        return new ArrayList<>();
    }
}

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

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils.getCellValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportModel;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetParameterValue;
import org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAttributeConverter {

    private static final int ATTR_NAME_ROW_INDEX = 0;

    /**
     * Checks if attribute parameter should be overlapped by comparing current and reference parameter values.
     *
     * @param importModel           import attribute model
     * @param datasetParameterValue dataset parameter value
     * @param importContext         import context
     * @param parameterPredicate    predicate for different attribute types value comparing
     */
    protected boolean isAttributeShouldOverlap(AttributeImportModel importModel,
                                               DatasetParameterValue datasetParameterValue,
                                               AttributeImportContext importContext,
                                               Predicate<Parameter> parameterPredicate) {
        final String attributeName = importModel.getName();
        log.debug("Checking is attribute '{}' should be overlapped", attributeName);
        final String datasetName = datasetParameterValue.getDatasetName();
        final String refDatasetListName = datasetParameterValue.getDatasetListReference();
        final String refDatasetName = datasetParameterValue.getDatasetReference();
        log.debug("DS name: {}, DSL ref: {}, DS ref: {}", attributeName, refDatasetListName, refDatasetName);

        final String attributeKey = importModel.getKey();
        final String value = datasetParameterValue.getTextValue();
        final boolean isDslAttribute = ImportUtils.isArrowDelimiterPresent(attributeKey);

        final String overlapKey = ImportUtils.getOverlapKey(datasetName, attributeKey);
        boolean isOverlapExist = importContext.isOverlapExist(overlapKey);
        if (isOverlapExist) {
            log.debug("Is Overlap Exist key: {}, value {}, Dataset name {}", overlapKey, value, datasetName);
            return true;
        }

        if (isDslAttribute) {
            log.debug("Check if parent DSL attribute doesn't have DS reference");
            final AttributeImportModel parent = importModel.getParent();
            final DatasetParameterValue parentDatasetParameterValue = parent.getDatasetParameterValue(datasetName);
            final String parentDatasetReference = parentDatasetParameterValue.getDatasetReference();

            final boolean isDslAttributeWithoutDsRef = StringUtils.isEmpty(parentDatasetReference);
            if (isDslAttributeWithoutDsRef) {
                log.debug("Parent DSL '{}' attribute doesn't have DS reference", parent.getName());
                return !StringUtils.isEmpty(value);
            }
        }

        final boolean isDslSubAttribute = isDslSubAttribute(datasetParameterValue);
        if (isDslSubAttribute) {
            final String refAttributeKey = getRefAttributeKey(importModel, datasetParameterValue);
            log.debug("Ref attribute key: {}", refAttributeKey);

            final Parameter refParameter = importContext.getRefParameter(refAttributeKey);
            log.debug("Ref parameter: {}", refParameter);

            final boolean isRefParameter = Objects.isNull(refParameter);
            if (isRefParameter) {
                return !StringUtils.isEmpty(value);
            }
            boolean isParameterValueChanged;
            try {
                isParameterValueChanged = parameterPredicate.test(refParameter);
            } catch (NullPointerException e) {
                log.warn("Parameter value is '{}', Attribute key: '{}',  datasetParameterValue: '{}'",
                        e.getCause(), attributeKey, datasetParameterValue);
                isParameterValueChanged = false;
            }
            log.debug("Parameter should be overlapped");

            return isParameterValueChanged;
        }
        log.debug("Parameter shouldn't be overlapped");

        return false;
    }

    /**
     * Gets reference attribute key for attribute model.
     *
     * @param importModel                import attribute model
     * @param childDatasetParameterValue child dataset parameter value
     * @return attribute key
     */
    protected String getRefAttributeKey(AttributeImportModel importModel,
                                        DatasetParameterValue childDatasetParameterValue) {
        final AttributeType type = importModel.getType();
        final String attributeName = importModel.getName();
        log.debug("Get key for reference attribute with name '{}' and type '{}'", attributeName, type);

        DatasetParameterValue datasetParameterValue;
        if (AttributeType.DSL.equals(type)) {
            final AttributeImportModel parent = importModel.getParent();
            final String parentDatasetName = childDatasetParameterValue.getDatasetName();

            datasetParameterValue = parent.getDatasetParameterValue(parentDatasetName);
        } else {
            datasetParameterValue = childDatasetParameterValue;
        }

        final String refAttributeKey = ImportUtils.getDsParameterKey(datasetParameterValue, attributeName);
        log.debug("Result ref attribute key: {}", refAttributeKey);

        return refAttributeKey;
    }

    /**
     * Detects if current dataset parameter value is DSL sub attribute.
     *
     * @param datasetParameterValue dataset parameter value
     */
    protected boolean isDslSubAttribute(DatasetParameterValue datasetParameterValue) {
        log.debug("Detects if dataset parameter value '{}' is DSL sub attribute", datasetParameterValue);

        final String datasetListReference = datasetParameterValue.getDatasetListReference();
        final String datasetReference = datasetParameterValue.getDatasetReference();
        log.debug("DSL reference: {}, DS reference: {}", datasetListReference, datasetReference);

        final boolean isDslSubAttribute = isNotEmpty(datasetListReference) && isNotEmpty(datasetReference);
        log.debug("Is DSl sub attribute: {}", isDslSubAttribute);

        return isDslSubAttribute;
    }

    /**
     * Detects if current dataset parameter value is DSL sub attribute and return corresponding or reference dataset id.
     *
     * @param datasetParameterValue dataset parameter value
     * @param context               import context
     * @return dataset id
     */
    protected UUID getDatasetReference(DatasetParameterValue datasetParameterValue, AttributeImportContext context) {
        log.debug("Get dataset id for dataset parameter value: {}", datasetParameterValue);

        final UUID datasetId;
        if (isDslSubAttribute(datasetParameterValue)) {
            final String datasetListReference = datasetParameterValue.getDatasetListReference();
            final String datasetReference = datasetParameterValue.getDatasetReference();
            final String datasetKey = ImportUtils.getDatasetKey(datasetListReference, datasetReference);
            log.debug("Dataset key: {}", datasetKey);

            datasetId = context.getRefDatasetId(datasetKey);
        } else {
            datasetId = datasetParameterValue.getDatasetId();
        }
        log.debug("Found dataset id: {}", datasetId);

        return datasetId;
    }

    /**
     * Gets attribute name from imported excel file row.
     *
     * @param row excel file row
     * @return attribute name
     */
    protected String getAttributeName(Map<Integer, String> row) {
        log.debug("Get attribute name for excel row");
        String attributeName = getCellValue(row, ATTR_NAME_ROW_INDEX);
        log.debug("Cell attribute name value: {}", attributeName);

        if (ImportUtils.isArrowDelimiterPresent(attributeName)) {
            String[] dslAttributeNameParts = attributeName.split(ImportUtils.DSL_ATTRIBUTE_REF_DELIMITER);
            log.debug("DSL attribute name parts: {}", Arrays.toString(dslAttributeNameParts));
            int dslAttributeNamePartsIndex = dslAttributeNameParts.length - 1;
            attributeName = dslAttributeNameParts[dslAttributeNamePartsIndex];
        }
        log.debug("Result attribute name: {}", attributeName);

        return attributeName;
    }

    /**
     * Gets attribute key from excel row.
     *
     * @param row excel file row
     * @return attribute key
     */
    protected String getAttributeKey(Map<Integer, String> row) {
        log.debug("Get attribute name for excel row");
        return getCellValue(row, ATTR_NAME_ROW_INDEX);
    }

    /**
     * Get row attribute name.
     *
     * @param row excel file row
     * @return attribute name
     */
    protected String getRowAttributeName(Map<Integer, String> row) {
        log.debug("Get row attribute name");
        return getCellValue(row, ATTR_NAME_ROW_INDEX);
    }

    /**
     * Map text row (Text, Encrypted and List row types) to import model.
     *
     * @param row           excel file row
     * @param importContext import context
     * @return import model
     */
    protected AttributeImportModel mapTextRowToImportModel(Map<Integer, String> row,
                                                           AttributeImportContext importContext) {
        final String key = getAttributeKey(row);
        final Attribute attribute = importContext.getAttribute(key);
        final UUID id = attribute.getId();
        final String name = getAttributeName(row);
        final String cellType = ImportUtils.getAttributeType(row).toUpperCase();
        final AttributeType type = AttributeType.valueOf(cellType);
        final boolean isDslAttribute = AttributeType.DSL.equals(type);
        log.debug("Map attribute '{}' row with type '{}' to import model", name, type);
        final AttributeImportModel importModel = new AttributeImportModel(id, name, key, type);

        final List<DatasetParameterValue> datasetParameterValues = importContext.getDatasetsCellIndexMap()
                .entrySet()
                .stream()
                .map(entry -> {
                    final Integer datasetCellIndex = entry.getValue();
                    final String datasetName = entry.getKey();
                    final String dsParamValue = getCellValue(row, datasetCellIndex);
                    final UUID datasetId = importContext.getDatasetId(datasetName);

                    if (isDslAttribute) {
                        String dslRefName = attribute.getTypeDataSetList().getName();
                        String dsRefName = ImportUtils.getParameterDsRefName(dsParamValue);
                        importModel.setDatasetListReference(dslRefName);
                        log.debug("Set values: datasetId='{}', datasetName='{}', dsParamValue='{}', dslRefName='{}', "
                                + "dsRefName='{}'", datasetId, datasetName, dsParamValue, dslRefName, dsRefName);

                        return new DatasetParameterValue(datasetId, datasetName, dsParamValue, dslRefName, dsRefName);
                    }

                    log.debug("Set values: datasetId='{}', datasetName='{}', dsParamValue='{}'", datasetId, datasetName,
                            dsParamValue);
                    return new DatasetParameterValue(datasetId, datasetName, dsParamValue);
                })
                .collect(Collectors.toList());
        importModel.setDatasetParameterValues(datasetParameterValues);

        return importModel;
    }

    protected String getParamValueTarget(boolean attributeShouldOverlap, DatasetParameterValue datasetParameterValue,
                                       AttributeImportContext importContext, UUID attributeId, UUID datasetId,
                                        AttributeImportModel importModel, ParameterService parameterService) {

        if (!attributeShouldOverlap) {
            log.debug("is not Overlap, attributeId: {}, datasetId: {}", attributeId, datasetId);
            return getValueTarget(datasetParameterValue, importModel, importContext, attributeId, datasetId,
                    parameterService);
        } else {
            String paramOverlapValueTarget = importContext.getTargetDslOverlaps()
                    .get(datasetParameterValue.getDatasetName() + "_" + importModel.getKey());
            log.debug("is overlap, attributeId: {}, datasetId: {}, paramOverlapValueTarget: {}", attributeId,
                    datasetId, paramOverlapValueTarget);
            return Objects.isNull(paramOverlapValueTarget)
                    ? getValueTarget(datasetParameterValue, importModel, importContext, attributeId, datasetId,
                    parameterService)
                    : paramOverlapValueTarget;
        }
    }

    private String getValueTarget(DatasetParameterValue datasetParameterValue, AttributeImportModel importModel,
                                  AttributeImportContext importContext, UUID attributeId, UUID datasetId,
                                  ParameterService parameterService) {
        String valueTarget = Strings.EMPTY;
        UUID targetRefDatasetId = importContext.getRefDatasetsNameIdMap().get(datasetParameterValue
                .getDatasetListReference() + "_" + datasetParameterValue.getDatasetReference());
        targetRefDatasetId = Objects.isNull(targetRefDatasetId) ? datasetId : targetRefDatasetId;
        Parameter parameterTarget = parameterService.getByDataSetIdAttributeId(targetRefDatasetId, attributeId);
        if (nonNull(parameterTarget)) {
            AttributeType type = importModel.getType();
            if (AttributeType.LIST.equals(type)) {
                ListValue targetListValueValue = parameterTarget.getListValue();
                valueTarget = nonNull(targetListValueValue)
                        ? targetListValueValue.getName()
                        : valueTarget;
            } else if (AttributeType.TEXT.equals(type)) {
                valueTarget = parameterTarget.getText();
            }
        }
        return valueTarget;
    }
}

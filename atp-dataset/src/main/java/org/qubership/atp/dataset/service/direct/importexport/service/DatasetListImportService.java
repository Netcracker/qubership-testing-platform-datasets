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

package org.qubership.atp.dataset.service.direct.importexport.service;

import static org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils.getAttributeType;
import static org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils.isBlankRow;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.opc.OPCPackage;
//import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.dataset.exception.excel.ExcelImportEmptyExcelException;
import org.qubership.atp.dataset.exception.excel.ExcelImportNotExistingAttributeException;
import org.qubership.atp.dataset.exception.excel.ExcelImportNotExistingChildAttributeException;
import org.qubership.atp.dataset.exception.excel.ExcelImportNotExistingRefParameterException;
import org.qubership.atp.dataset.exception.excel.ExcelImportUnexpectedException;
import org.qubership.atp.dataset.exception.excel.ImportExcelNotEqualsAttributeTypeException;
import org.qubership.atp.dataset.exception.excel.ImportExcelNotSupportedAttributeTypeException;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.importexport.converters.AttributeImportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.XlsxToListConverter;
import org.qubership.atp.dataset.service.direct.importexport.exceptions.ImportFailedException;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportModel;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportResponse;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetListImportResponse;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetParameterValue;
import org.qubership.atp.dataset.service.direct.importexport.models.ParameterImportResponse;
import org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils;
import org.qubership.atp.dataset.service.direct.importexport.utils.StreamUtils;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import clover.com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//@Isolated
@Service
@AllArgsConstructor
@Slf4j
public class DatasetListImportService {

    private static final List<AttributeType> supportedImportAttributeTypes =
            Arrays.asList(AttributeType.TEXT, AttributeType.LIST, AttributeType.DSL, AttributeType.ENCRYPTED);

    private static final String UNDERSCORE = "_";
    private static final Map<String, Integer> requiredHeadingIndexesMap = ImmutableMap.of("Attribute", 0, "Type", 1);

    private final DataSetListImportExportFactory factory;
    private final JpaDataSetListService dataSetListService;
    private final JpaDataSetService dataSetService;
    private final JpaAttributeService attributeService;
    private final ParameterService parameterService;
    private final ClearCacheService clearCacheService;

    /**
     * Import attribute parameters into specified dataset list from Excel file.
     *
     * @param targetProjectId target project
     * @param targetDslId target data set list
     * @param inputStream excel file input stream
     * @return import response
     * @throws Exception possible import exception
     */

    public DatasetListImportResponse importDataSetList(UUID targetProjectId, UUID targetDslId,
                                                       InputStream inputStream, boolean isJavers) throws Exception {
        log.info("Start process importing from Excel file, [ProjectId] = '{}', [DslId] = '{}'",
                targetProjectId, targetDslId);
        validateDataSetsAreNotLocked(targetDslId);
        //Read XLSX file and convert to ListMap
        OPCPackage pkg = OPCPackage.open(inputStream);
        List<Map<Integer, String>> sheetConvertList = new ArrayList<>();
        try {
            XlsxToListConverter xlsxToListConverter = new XlsxToListConverter(pkg, sheetConvertList);
            xlsxToListConverter.process();
        } finally {
            pkg.revert();
        }

        ListIterator<Map<Integer, String>> rowsIterator = sheetConvertList.listIterator();

        // Prepare import context
        AttributeImportContext context = prepareImportContext(targetProjectId, targetDslId,
                sheetConvertList.listIterator(), isJavers);

        // Validate import file schema
        validateImportFileSchema(targetDslId, sheetConvertList.listIterator(), context);

        // skip heading row
        rowsIterator.next();

        log.info("Start final phase importing excel to DataBase, [ProjectId] = '{}', [DslId] = '{}'",
                targetProjectId, targetDslId);

        DatasetListImportResponse response = importExcel(rowsIterator, context);

        log.info("Finish process importing from Excel file: [ProjectId] = '{}', [DslId] = '{}'",
                targetProjectId, targetDslId);
        return response;
    }

    private DatasetListImportResponse importExcel(ListIterator<Map<Integer, String>> rowsIterator,
                                                  AttributeImportContext context) {
        try {
            // Prepare import response
            DatasetListImportResponse importResponse = new DatasetListImportResponse();

            // Iterating throw all Excel file rows
            while (rowsIterator.hasNext()) {
                Map<Integer, String> row = rowsIterator.next();

                // check if current row is blank and skip it
                if (isBlankRow(row)) {
                    log.debug("Skip blank row");
                    continue;
                }

                // detect attribute type(text, list, encrypted etc)
                String attributeType = getAttributeType(row);
                log.debug("Attribute type: {}", attributeType);

                // get corresponding attribute type import converter
                AttributeImportConverter importConverter = factory.getAttributeImportConverter(attributeType);
                log.debug("Import converter class: {}", importConverter.getClass().getName());

                // map row to attribute model
                AttributeImportModel model = importConverter.mapRowToImportModel(row, context, rowsIterator);
                log.debug("Import model: {}", model);

                // validate attribute parameters
                List<ParameterImportResponse> parameterImportErrors = importConverter.validate(model, context);
                AttributeImportResponse attributeResponse = importResponse.addAttributeResponse(model);
                attributeResponse.setErrors(parameterImportErrors);

                // import attribute parameters
                importAttributeParameters(model, importConverter, context, importResponse);
            }

            return importResponse;
        } catch (Exception ex) {
            log.error("Unexpected error while excel import", ex);
            throw new ExcelImportUnexpectedException(ex);
        }
    }

    /**
     * Imports attribute and its children parameters. All exceptions are handled silently, collecting reasons into
     * import response.
     *
     * @param model     import model
     * @param converter attribute type import converter
     * @param context   import context
     * @param response  import response
     */
    void importAttributeParameters(AttributeImportModel model, AttributeImportConverter converter,
                                   AttributeImportContext context, DatasetListImportResponse response) {
        final String attributeName = model.getName();
        log.debug("Start importing parameters for attribute '{}'", attributeName);

        final AttributeImportResponse attributeResponse = response.getAttributeResponse(model);

        for (DatasetParameterValue datasetParameterValue : model.getDatasetParameterValues()) {
            final String datasetName = datasetParameterValue.getDatasetName();
            final String parameterValue = datasetParameterValue.getTextValue();
            log.debug("Import parameter value '{}' for dataset '{}'", parameterValue, datasetName);

            try {
                ParameterImportResponse successParameterImportResponse = converter.importAttributeParameter(
                        model, datasetParameterValue, context);
                if (Objects.nonNull(successParameterImportResponse.getDataSetId())) {
                    Parameter parameter =
                            parameterService.getByDataSetIdAttributeId(datasetParameterValue.getDatasetId(),
                                    model.getId());
                    if (parameter != null) {
                        clearCacheService.evictParameterCache(parameter.getId());
                        clearCacheService.evictDatasetListContextCache(datasetParameterValue.getDatasetId());
                    }
                    attributeResponse.setSuccess(successParameterImportResponse);
                }
            } catch (Exception e) {
                UUID datasetId = datasetParameterValue.getDatasetId();
                String keyAttributeValue = model.getKey();
                String errorMessage = String.format("Failed to import key attribute: '%s' in dataset id: '%s', "
                        + "DsName: '%s', Reason: '%s'", keyAttributeValue, datasetId, datasetName, e.getMessage());
                log.error(errorMessage, e);
                attributeResponse.setError(new ParameterImportResponse(datasetId, errorMessage));
            }
        }

        final List<AttributeImportModel> children = model.getChildren();
        if (!isEmpty(children)) {
            log.debug("Children attributes: {}", StreamUtils.extractIds(children, AttributeImportModel::getId));
            children.forEach(childImportModel -> {
                final String childAttributeName = childImportModel.getName();
                final AttributeType type = childImportModel.getType();
                final String childAttributeType = type.getName();
                final AttributeImportConverter childConverter = factory.getAttributeImportConverter(childAttributeType);

                if (AttributeType.TEXT.equals(type) || AttributeType.LIST.equals(type)
                        || AttributeType.DSL.equals(type)) {
                    log.debug("Prepare parameter context for attribute: {}", childAttributeName);
                    prepareParameterContext(childImportModel, context);
                }

                final List<ParameterImportResponse> importErrors = childConverter.validate(childImportModel, context);
                final AttributeImportResponse childAttributeResponse = response.getAttributeResponse(childImportModel);
                childAttributeResponse.setErrors(importErrors);

                log.debug("Start importing parameters for child attribute '{}'", childAttributeName);
                importAttributeParameters(childImportModel, childConverter, context, response);

                context.clearParametersContext();
                log.debug("Finish importing parameters for child attribute '{}'", childAttributeName);
            });
        }
        log.debug("Finish importing parameters for attribute '{}'", attributeName);
    }

    /**
     * Prepares parameter context which need to detect, if attribute parameter should be overlapped or not.
     *
     * @param importModel import model
     * @param context     import context
     */
    private void prepareParameterContext(AttributeImportModel importModel, AttributeImportContext context) {
        final Map<String, Set<UUID>> refDslDatasetsMap = context.getRefDslDatasetIdsMap();
        final String datasetListReference = getDatasetListReference(importModel);
        final Set<UUID> datasetIds = refDslDatasetsMap.get(datasetListReference);
        final String attributeKey = importModel.getKey();
        final Attribute attribute = context.getAttribute(attributeKey);
        final UUID refAttributeId = attribute.getId();
        log.debug("Dataset ids: {}, reference attribute id: {}", datasetIds, refAttributeId);

        List<Parameter> parameters = parameterService.getByAttributeIdAndDatasetIds(refAttributeId, datasetIds);
        log.debug("Found parameters: {}", StreamUtils.extractIds(parameters, Parameter::getId));

        context.setAttributeParameters(parameters);
    }

    private String getDatasetListReference(AttributeImportModel importModel) {
        AttributeType type = importModel.getType();
        if (AttributeType.DSL.equals(type)) {
            return importModel.getDatasetListReference();
        } else {
            return importModel.getDatasetParameterValuesDslReference();
        }
    }

    void validateDataSetsAreNotLocked(UUID targetDslId) {
        List<DataSet> dataSets = dataSetService.getLockedDataSets(targetDslId);
        if (Objects.nonNull(dataSets) && !dataSets.isEmpty()) {
            Map<String, UUID> map = dataSets.stream().collect(Collectors.toMap(DataSet::getName, DataSet::getId));
            String dslName = dataSetListService.getById(targetDslId).getName();
            String message = String.format(
                    "Failed to import DataSetList '%s' with id: '%s'. Reason: some datasets are locked: %s",
                    dslName, targetDslId, map);
            log.error(message);
            throw new ImportFailedException(message);
        }
    }

    /**
     * Validates import file schema and throws exception if some specified attribute is missed in database.
     * Also validates required import file headings.
     *
     * @param targetDslId  target dataset list identifier
     * @param rowsIterator Excel file rows iterator
     */
    void validateImportFileSchema(UUID targetDslId, ListIterator<Map<Integer, String>> rowsIterator,
                                  AttributeImportContext context) {
        log.info("Start validating import file schema, target dataset list: {}", targetDslId);

        String emptyFileContentErrorMessage = "Failed to import data set list. Reason: empty import file content";
        if (!rowsIterator.hasNext()) {
            log.error(emptyFileContentErrorMessage);
            throw new ExcelImportEmptyExcelException();
        }

        final Map<Integer, String> headingRow = rowsIterator.next();
        if (isBlankRow(headingRow)) {
            log.error(emptyFileContentErrorMessage);
            throw new ExcelImportEmptyExcelException();
        }

        validateRequiredHeading(headingRow);

        final Map<String, UUID> existedDslAttributesMap = getDslAttributesMap(targetDslId);
        while (rowsIterator.hasNext()) {
            final Map<Integer, String> row = rowsIterator.next();
            if (isBlankRow(row)) {
                log.debug("Skip blank row");
                continue;
            }
            final String attributeName = ImportUtils.getAttributeName(row);
            log.debug("Attribute name: {}", attributeName);

            boolean isListRefNestedAttribute = attributeName.contains(ImportUtils.DSL_ATTRIBUTE_REF_DELIMITER);
            log.debug("Is list ref nested attribute: {}", isListRefNestedAttribute);

            boolean isAttributeExistsInDslSchema = existedDslAttributesMap.containsKey(attributeName);
            log.debug("Is attribute exists in DSL schema: {}", isAttributeExistsInDslSchema);

            boolean isAttributeChildExistsInDslSchema = context.getAttributesNameMap().containsKey(attributeName);
            log.debug("Is attribute child exists in DSL schema: {}", isAttributeExistsInDslSchema);

            if (!isAttributeExistsInDslSchema && !isListRefNestedAttribute) {
                log.error(String.format("Failed to import data set list: attribute '%s' doesn't exist "
                        + "in DSL schema", attributeName));
                throw new ExcelImportNotExistingAttributeException(attributeName);
            } else if (!isAttributeChildExistsInDslSchema && isListRefNestedAttribute) {
                log.error(String.format("Failed to import child dsl : attribute '%s' doesn't "
                        + "exist in DSL schema", attributeName));
                throw new ExcelImportNotExistingChildAttributeException(attributeName);
            }

            final AttributeType attributeType = validateAttributeTypeCells(row, attributeName, context);

            validateDslCellParameterValues(row, attributeName, attributeType, context);
        }

        log.info("Finish validating import file schema, target dataset list: {}", targetDslId);
    }

    private AttributeType validateAttributeTypeCells(Map<Integer, String> row, String attributeName,
                                                     AttributeImportContext context) {
        final String attributeTypeValue = getAttributeType(row);
        log.debug("Attribute type value: {}", attributeTypeValue);

        boolean isSupportedAttributeType = supportedImportAttributeTypes.stream()
                .anyMatch(attributeType -> attributeType.name().equalsIgnoreCase(attributeTypeValue));
        if (!isSupportedAttributeType) {
            log.error(String.format("Invalid attribute '%s' type: '%s'. Supported import types: %s",
                    attributeName, attributeTypeValue, supportedImportAttributeTypes));
            throw new ImportExcelNotSupportedAttributeTypeException(attributeName, attributeTypeValue,
                    supportedImportAttributeTypes);
        }
        boolean isAttrTypeEquals = context.getAttributesNameMap().get(attributeName)
                .getAttributeType().getName().equalsIgnoreCase(attributeTypeValue);
        if (!isAttrTypeEquals) {
            log.error(String.format("The attribute '%s' with type '%s' is different compared to DSL", attributeName,
                    attributeTypeValue));
            throw new ImportExcelNotEqualsAttributeTypeException(attributeName, attributeTypeValue);
        }

        return AttributeType.valueOf(attributeTypeValue);
    }

    private void validateDslCellParameterValues(Map<Integer, String> row, String attributeName,
                                                AttributeType attributeType, AttributeImportContext context) {
        final boolean isDslAttributeType = AttributeType.DSL.equals(attributeType);

        if (isDslAttributeType) {
            int lastColumn = row.size();
            for (int columnIndex = 0; columnIndex < lastColumn; columnIndex++) {
                boolean isDatasetColumn = columnIndex > ImportUtils.ATTR_TYPE_ROW_INDEX;
                if (isDatasetColumn) {
                    final String parameterValue = ImportUtils.getCellValue(row, columnIndex);
                    log.debug("Parameter value: {}", parameterValue);

                    if (!StringUtils.isEmpty(parameterValue)) {
                        String idRefDslStr = context.getAttributesNameMap().get(attributeName)
                                .getTypeDataSetListId().toString();
                        final String parameterDsRefName = ImportUtils.getParameterDsRefName(parameterValue);
                        if (!context.containsRefDataset(idRefDslStr + UNDERSCORE + parameterDsRefName)) {

                            String errorMessage = String.format("Provided referenced [%s], DS '%s', in attribute '%s' "
                                    + "parameter doesn't exist", parameterValue, parameterDsRefName, attributeName);
                            if (Objects.nonNull(parameterDsRefName)) {
                                log.error(errorMessage);
                                throw new ExcelImportNotExistingRefParameterException(parameterValue,
                                                                                      parameterDsRefName,
                                                                                      attributeName);
                            } else {
                                log.warn(errorMessage);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads all necessary entities for further attribute parameters import process and collects all this data into
     * import context object. Key goal is to decrease a lot of single database calls.
     *
     * @param targetDslId  target dataset list identifier
     * @param rowsIterator Excel file rows iterator
     * @param isJavers versioning snapshot from javers
     * @return attribute import context
     */
    AttributeImportContext prepareImportContext(UUID targetProjectId, UUID targetDslId,
                                                ListIterator<Map<Integer, String>> rowsIterator,
                                                boolean isJavers) {
        log.info("Start preparing attribute import context with versioning Javers = '{}', target dataset list: {}",
                isJavers, targetDslId);
        AttributeImportContext importContext = new AttributeImportContext();
        importContext.setJavers(isJavers);
        importContext.setTargetDslId(targetDslId);

        final Map<Integer, String> headingRow = rowsIterator.next();
        final int headingRowSize = headingRow.size();
        final Map<String, Integer> existedDatasetsIndexMap = getExistedDatasetsCellIndexMap(headingRow);
        importContext.setDatasetsCellIndexMap(existedDatasetsIndexMap);
        log.debug("Existed datasets: {}", existedDatasetsIndexMap.keySet());

        final List<DataSet> dslDatasets = dataSetService.getByDataSetListId(targetDslId);
        importContext.setDatasets(dslDatasets);
        createNotExistedImportedDatasets(importContext);
        log.debug("Existed DSL datasets: {}", StreamUtils.extractIds(dslDatasets, DataSet::getId));

        final List<Attribute> attributes = attributeService.getByDataSetListId(targetDslId);
        importContext.setAttributes(attributes);
        log.debug("Existed DSL attributes: {}", StreamUtils.extractIds(attributes, Attribute::getId));

        checkNames(targetProjectId);

        final List<DataSetList> projectDatasetLists = dataSetListService.getByVisibilityAreaId(targetProjectId);
        Map<String, DataSetList> projectDslMap = StreamUtils.toEntityMap(projectDatasetLists, DataSetList::getName);
        log.debug("Existed dataset lists: {}", StreamUtils.extractIds(projectDatasetLists, DataSetList::getId));

        final Map<String, Set<String>> referencedDslDatasetsMap = new HashMap<>();
        prepareAttributesMap(rowsIterator, referencedDslDatasetsMap, importContext, headingRowSize);
        final Set<UUID> refDatasetListIds = referencedDslDatasetsMap.keySet()
                .stream()
                .map(projectDslMap::get)
                .map(DataSetList::getId)
                .collect(Collectors.toSet());
        log.debug("Existed dataset list ids: {}", refDatasetListIds);

        final List<DataSet> refDslDatasets = dataSetService.getByDataSetListIdIn(refDatasetListIds);
        importContext.setRefDatasets(refDslDatasets);
        log.debug("Existed ref DSL datasets: {}", StreamUtils.extractIds(refDslDatasets, DataSet::getId));

        final Map<String, Set<UUID>> refDslDatasetsMap = getRefDslDatasetsMap(referencedDslDatasetsMap, importContext);
        importContext.setRefDslDatasetIdsMap(refDslDatasetsMap);

        final List<AttributeKey> overlaps = attributeService.getAttributeKeysByDatasetListId(targetDslId);
        importContext.setOverlaps(overlaps);

        log.info("Finish preparing attribute import context, target dataset list: {}", targetDslId);

        return importContext;
    }

    private void checkNames(UUID visibilityAreaId) {
        dataSetListService.checkDslNames(visibilityAreaId);
    }

    /**
     * Gets all dataset list name to it datasets ids map. Need for further parameter context creating.
     *
     * @param referencedDslDatasetsMap DSL to all dataset names map
     * @param importContext            import context
     * @return DSL to all dataset ids map
     */
    Map<String, Set<UUID>> getRefDslDatasetsMap(Map<String, Set<String>> referencedDslDatasetsMap,
                                                AttributeImportContext importContext) {
        log.debug("Get reference DSL datasets map");
        final Map<String, Set<UUID>> refDslDatasetsMap = new HashMap<>();
        final Map<String, UUID> refDatasetsNameIdMap = importContext.getRefDatasetsNameIdMap();
        referencedDslDatasetsMap.forEach((dslName, dslDatasetNames) -> {
            Set<UUID> dslDatasetIds = dslDatasetNames.stream()
                    .map(datasetName -> {
                        final String datasetKey = ImportUtils.getDatasetKey(dslName, datasetName);
                        log.debug("Dataset key: {}", datasetKey);

                        return refDatasetsNameIdMap.get(datasetKey);
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            log.debug("Put value: {}={}", dslName, dslDatasetIds);
            refDslDatasetsMap.put(dslName, dslDatasetIds);
        });

        return refDslDatasetsMap;
    }

    /**
     * Parses all imported Excel file, finds all DSL attribute cells and gets referenced DSL datasets map.
     * Example of DSL attribute cell format, "DSL 3 -> DS 5"
     *
     * @param rowsIterator  Excel file rows iterator
     * @param importContext import context
     */
    private void prepareAttributesMap(ListIterator<Map<Integer, String>> rowsIterator,
                                      Map<String, Set<String>> refDslDatasetsMap,
                                      AttributeImportContext importContext, int headingRowSize) {
        log.debug("Get referenced dataset list names");

        while (rowsIterator.hasNext()) {
            final Map<Integer, String> row = rowsIterator.next();
            if (!isBlankRow(row)) {
                final String attributeTypeValue = getAttributeType(row);
                final String attributeName = ImportUtils.getAttributeName(row);
                final AttributeType attributeType = AttributeType.valueOf(attributeTypeValue);
                log.debug("Attribute type: {}", attributeType);

                boolean isDslAttributeType = AttributeType.DSL.equals(attributeType);
                if (isDslAttributeType) {
                    //bind the DSL iteration cycle to the length of the Excel table header
                    for (int idxRow = 0; idxRow < headingRowSize; idxRow++) {
                        final String parameterValue = ImportUtils.getCellValue(row, idxRow);
                        log.debug("Parameter value: {}", parameterValue);

                        // dataset column index should be greater than last required heading
                        boolean isDatasetColumn = idxRow > ImportUtils.ATTR_TYPE_ROW_INDEX;
                        log.debug("Is dataset column: {}", isDatasetColumn);

                        boolean isDslSubAttribute = ImportUtils.isArrowDelimiterPresent(attributeName);
                        log.debug("Is DSL reference attribute: {}", isDslSubAttribute);

                        boolean isParameterContainDslPrefix = ImportUtils.isArrowDelimiterPresent(parameterValue);

                        if (isDatasetColumn) {
                            final String parameterDsRefName;
                            if (isParameterContainDslPrefix) {
                                parameterDsRefName = ImportUtils.getParameterDsRefName(parameterValue);
                            } else {
                                parameterDsRefName = parameterValue;
                            }

                            final Attribute attribute = importContext.getAttribute(attributeName);
                            final DataSetList dataSetList = attribute.getTypeDataSetList();
                            final String parameterDslRefName = dataSetList.getName();
                            final List<Attribute> refAttributes = dataSetList.getAttributes();
                            importContext.setAttributes(attributeName, refAttributes);
                            if (Objects.nonNull(parameterDsRefName)) {
                                refDslDatasetsMap.computeIfAbsent(parameterDslRefName, param -> new HashSet<>())
                                        .add(parameterDsRefName);
                            }

                            log.debug("DSL ref name: {}, DS ref name: {}", parameterDslRefName, parameterDsRefName);
                        }
                    }
                }
            }
        }
        log.debug("Result referenced dataset list datasets map: {}", refDslDatasetsMap);
    }

    /**
     * Checks what datasets from imported Excel file don't exist in database and create them.
     *
     * @param importContext import context
     */
    void createNotExistedImportedDatasets(AttributeImportContext importContext) {
        final UUID targetDslId = importContext.getTargetDslId();
        final Map<String, UUID> dslDatasetsNameIdMap = importContext.getDatasetsNameIdMap();
        final Set<String> sheetDslDatasets = importContext.getDatasetsCellIndexMap().keySet();
        log.debug("Create not existed imported datasets for DSL with id '{}', file dataset names: '{}'",
                targetDslId, sheetDslDatasets);

        sheetDslDatasets.forEach(datasetName -> {
            final boolean isImportedDatasetNotExist = !dslDatasetsNameIdMap.containsKey(datasetName);
            if (isImportedDatasetNotExist) {
                try {
                    log.debug("Dataset '{}' not exist in database, creating new dataset entity", datasetName);
                    DataSet dataSet = dataSetService.createDsSelectJavers(datasetName, targetDslId,
                            importContext.isJavers());
                    UUID dataSetId = dataSet.getId();
                    importContext.setDataset(dataSet);
                    log.debug("Dataset '{}' has been created, database id: '{}'", datasetName, dataSetId);
                } catch (DataSetServiceException e) {
                    String errorMessage = "Failed to create not existed imported dataset: " + datasetName;
                    log.error(errorMessage);
                    throw new ImportFailedException(errorMessage);
                }
            }
        });
        log.debug("Finish creating not existed imported datasets for DSL with id '{}'", targetDslId);
    }

    /**
     * Loads attributes for specified target DSL from database and groups them into name to id map.
     *
     * @param targetDslId target data set list identifier
     * @return DSL attributes name to id map.
     */
    Map<String, UUID> getDslAttributesMap(UUID targetDslId) {
        log.debug("Get attributes map for dataset list with id: {}", targetDslId);
        List<Attribute> dslAttributes = attributeService.getByDataSetListId(targetDslId);
        if (!isEmpty(dslAttributes)) {
            log.debug("Found attributes: {}", StreamUtils.extractIds(dslAttributes, Attribute::getId));

            return StreamUtils.toNameIdEntityMap(dslAttributes, Attribute::getName, Attribute::getId);
        }

        return Collections.emptyMap();
    }

    /**
     * Parses heading row, skips required headings and collects existed datasets to Excel file cells index map.
     * Example, ['DS 1' = 2, 'DS 2' = 3].
     *
     * @param headingRow heading row
     * @return cell index map
     */
    Map<String, Integer> getExistedDatasetsCellIndexMap(Map<Integer, String> headingRow) {
        log.debug("Get existed datasets cell index map from heading row");
        final Map<String, Integer> datasetsCellIndexMap = new LinkedHashMap<>();
        final Iterator<Map.Entry<Integer, String>> cellIterator = headingRow.entrySet().iterator();

        while (cellIterator.hasNext()) {
            final Map.Entry<Integer, String> cell = cellIterator.next();
            final String datasetName = cell.getValue();
            final boolean isDatasetNameNotEmpty = !datasetName.isEmpty();
            final boolean isNotRequiredHeading = !requiredHeadingIndexesMap.containsKey(datasetName);
            log.debug("Processed dataset name cell value: {}", datasetName);

            if (isDatasetNameNotEmpty && isNotRequiredHeading) {
                int datasetColumnIndex = cell.getKey();
                log.debug("Put dataset '{}' with column index '{}' into map", datasetName, datasetColumnIndex);
                datasetsCellIndexMap.put(datasetName, datasetColumnIndex);
            }
        }

        if (datasetsCellIndexMap.isEmpty()) {
            String errorMessage = "Imported dataset columns are absent";
            log.error(errorMessage);
            throw new ImportFailedException(errorMessage);
        }
        log.debug("Result datasets cell index map: {}", datasetsCellIndexMap);

        return datasetsCellIndexMap;
    }

    /**
     * Validates required file headings. Initially, these are the headers 'Attribute' and 'Type'.
     *
     * @param headingRow Excel heading row
     */
    private void validateRequiredHeading(Map<Integer, String> headingRow) {
        log.debug("Start validating file required headings, expected: {}", requiredHeadingIndexesMap.keySet());
        requiredHeadingIndexesMap.forEach((requiredHeader, requiredHeaderIndex) -> {
            //final Cell rowHeaderCell = headingRow.get(requiredHeaderIndex);

            final boolean isRowHeaderCellIsTrue = headingRow.containsKey(requiredHeaderIndex);
            if (!isRowHeaderCellIsTrue) {
                String errorMessage = String.format("Required heading '%s' is absent", requiredHeader);
                log.error(errorMessage);
                throw new ImportFailedException(errorMessage);
            } else {
                final String rowHeader = headingRow.get(requiredHeaderIndex);
                log.debug("Row header value: {}", rowHeader);

                final boolean isRowHeaderValueDoesntMatch = !requiredHeader.equals(rowHeader);
                if (isRowHeaderValueDoesntMatch) {
                    String errorMessage = String.format("Required heading '%s' has incorrect name", requiredHeader);
                    log.error(errorMessage);
                    throw new ImportFailedException(errorMessage);
                }
            }
        });
        log.debug("Finish required headings validation");
    }
}

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.qubership.atp.dataset.exception.excel.ExcelExportNotExistDatasetListlException;
import org.qubership.atp.dataset.exception.excel.ExcelExportUnexpectedException;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.importexport.converters.AttributeExportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.AttributeTypeConverterEnum;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSet;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class DatasetListExportService {

    private static final String ATTRIBUTE_NAME_COLUMN = "Attribute";
    private static final String ATTRIBUTE_TYPE_COLUMN = "Type";
    private static final String EXCEL_EXT = ".xlsx";
    private static final String OUT_OF_SCOPE = "OOS";
    private static final int START_ROW_INDEX = 0;
    public static final String DEFAULT_PASSWORD_MASK = "********";
    public static final String REFERENCE_DELIMITER = "->";
    public static final int ATTRIBUTE_NAME_COLUMN_INDEX = 0;
    public static final int ATTRIBUTE_TYPE_COLUMN_INDEX = 1;
    public static final int SHIFT_PARAMETER_COLUMN_INDEX = 2;
    public static int nextRowIndex;

    private final DataSetListService dslService;

    private final DataSetListImportExportFactory factory;

    /**
     * Perform export DSL to a file.
     *
     * @param datasetListId DSL identifier
     * @return export File
     */
    public File exportDataSetList(UUID datasetListId) {
        log.info("Start main process exporting Excel file, "
                + "and start process building a tree DSL from the DB, datasetListId = '{}'", datasetListId);
        UiManDataSetList dslData = dslService.getAsTree(datasetListId, false);
        log.info("Finish process building a tree DSL from the DB");
        if (Objects.isNull(dslData)) {
            throw new ExcelExportNotExistDatasetListlException(datasetListId);
        }
        return convertDataToExcelFile(dslData);
    }

    /**
     * Convert DataSetList entity to File format.
     *
     * @param dslData DSL data
     * @return export File
     */
    public File convertDataToExcelFile(UiManDataSetList dslData) {
        log.info("Start process exporting to Excel file");
        String dslName = dslData.getName().replaceAll("[:\\\\/|?*\"<>\\[\\]]", "_");
        Map<UUID, String> dataSetMap = new LinkedHashMap<>();
        List<UiManDataSet> dataSets = dslData.getDataSets();
        dataSets.forEach(dataSet -> dataSetMap.put(dataSet.getId(), dataSet.getName()));
        try {
            File file = new File(Files.createTempFile(dslName, EXCEL_EXT).toString());
            try (FileOutputStream outputStream = new FileOutputStream(file);
                SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
                workbook.setCompressTempFiles(true);
                // Create Sheet
                SXSSFSheet sheet = workbook.createSheet(dslName);
                // Populate Headers
                List<String> dsNamesListNames = new ArrayList<>();
                dsNamesListNames.add(ATTRIBUTE_NAME_COLUMN);
                dsNamesListNames.add(ATTRIBUTE_TYPE_COLUMN);
                dsNamesListNames.addAll(dataSetMap.values());
                SXSSFRow headerRow = sheet.createRow(START_ROW_INDEX);
                formExcelHeaderRow(workbook, headerRow, dsNamesListNames);

                // Populate Attributes and Parameters
                nextRowIndex = START_ROW_INDEX + 1;
                List<UiManAttribute> dslAttributesList = dslData.getAttributes();
                dslAttributesList.forEach(attrItem -> {
                    String attrName = attrItem.getName();
                    String attrType = getAttributeType(attrItem);
                    mapAttributeToRow(sheet, attrName, attrType, dataSetMap.keySet(), attrItem);
                    if (AttributeTypeConverterEnum.DSL.getName().equals(attrType)) {
                        mapAttributeReferencesToRow(sheet, dataSetMap.keySet(), attrItem, attrName);
                    }
                });

                workbook.write(outputStream);
                workbook.dispose();
                log.info("Finish process exporting to Excel file, and finish exporting.");
                return file;
            } catch (Exception e) {
                log.error("Error while creating export data file for DSL: " + dslName, e);
                throw new ExcelExportUnexpectedException(dslName, e);
            } finally {
                file.deleteOnExit();
            }
        } catch (IOException e) {
            log.error("Error while creating export data file for DSL: " + dslName, e);
            throw new ExcelExportUnexpectedException(dslName, e);
        }
    }

    /**
     * Form Excel header row of export file.
     *
     * @param workbook Excel workbook
     * @param row Excel row
     * @param columns list columns
     */
    private void formExcelHeaderRow(SXSSFWorkbook workbook, SXSSFRow row, List<String> columns) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < columns.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Create header row and set mandatory header fields.
     *
     * @param sheet Excel sheet
     * @param attrName target attribute name
     * @param attrType target attribute type
     * @return new Excel row
     */
    private SXSSFRow createNewRowAndSetAttrNameAndType(SXSSFSheet sheet, String attrName, String attrType) {
        SXSSFRow row = sheet.createRow(DatasetListExportService.nextRowIndex++);
        row.createCell(ATTRIBUTE_NAME_COLUMN_INDEX).setCellValue(attrName);
        row.createCell(ATTRIBUTE_TYPE_COLUMN_INDEX).setCellValue(attrType);
        return row;
    }

    /**
     * Get Attribute Type from entity object.
     *
     * @param attribute entity object
     * @return Attribute Type name
     */
    public static String getAttributeType(UiManAttribute attribute) {
        String attrType = OUT_OF_SCOPE;
        String type = attribute.getType().getName();
        if (EnumUtils.isValidEnum(AttributeTypeConverterEnum.class, type)) {
            attrType = AttributeTypeConverterEnum.valueOf(type).getName();
        }
        return attrType;
    }

    /**
     * Map Attribute entity to Excel row.
     *
     * @param datasetIds DSL identifier
     * @param attribute entity object
     */
    private void mapAttributeToRow(SXSSFSheet sheet, String attrName, String attrType,
                                   Set<UUID> datasetIds, UiManAttribute attribute) {
        if (!OUT_OF_SCOPE.equals(attrType)) {
            SXSSFRow row = createNewRowAndSetAttrNameAndType(sheet, attrName, attrType);
            AttributeExportConverter exportConverter = factory.getAttributeExportConverter(attrType);
            exportConverter.mapAttributeToRow(row, datasetIds, attribute);
        }
    }

    /**
     * Map all DSL-type attribute references to Excel row.
     *
     * @param sheet Excel sheet
     * @param datasetIds list dataset identifiers
     * @param attribute entity object
     * @param attrName attribute name
     */
    private void mapAttributeReferencesToRow(SXSSFSheet sheet, Set<UUID> datasetIds,
                                             UiManAttribute attribute, String attrName) {
        List<UiManAttribute> dslAttributesList = attribute.getAttributes();
        for (UiManAttribute attrItem : dslAttributesList) {
            String attrItemName = String.format("%s %s %s", attrName, REFERENCE_DELIMITER, attrItem.getName());
            String attrType = getAttributeType(attrItem);
            mapAttributeToRow(sheet, attrItemName, attrType, datasetIds, attrItem);
            if (AttributeTypeConverterEnum.DSL.getName().equals(attrType)) {
                mapAttributeReferencesToRow(sheet, datasetIds, attrItem, attrItemName);
            }
        }
    }
}

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

package org.qubership.atp.dataset.service.direct.importexport.utils;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetParameterValue;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImportUtils {

    private static final int DSL_ATTRIBUTE_REF_DS_PART_INDEX = 1;
    private static final String UNDERSCORE = "_";
    public static final String DSL_ATTRIBUTE_REF_DELIMITER = " -> ";
    public static final int ATTR_NAME_ROW_INDEX = 0;
    public static final int ATTR_TYPE_ROW_INDEX = 1;

    /**
     * Get  parameter dataset reference name.
     * Example of input value: DSL 2 -> DS 4
     *
     * @param parameterValue input combined reference name
     * @return dataset reference name
     */
    public static String getParameterDsRefName(String parameterValue) {
        if (isArrowDelimiterPresent(parameterValue)) {
            String[] parameterValueParts = parameterValue.split(DSL_ATTRIBUTE_REF_DELIMITER);
            if (parameterValueParts.length > 1) {
                return parameterValueParts[DSL_ATTRIBUTE_REF_DS_PART_INDEX];
            } else {
                log.debug("Error in DSL parameter [{}] in Excel file, ", parameterValue);
                return null;
            }
        } else {
            return parameterValue;
        }
    }

    /**
     * Get attribute type from excel row.
     *
     * @param row excel row
     * @return parsed type
     */
    public static String getAttributeType(Map<Integer, String> row) {
        return getCellValue(row, ATTR_TYPE_ROW_INDEX).toUpperCase();
    }

    /**
     * Get attribute name from excel row.
     *
     * @param row excel row
     * @return parsed attribute name
     */
    public static String getAttributeName(Map<Integer, String> row) {
        return getCellValue(row, ATTR_NAME_ROW_INDEX);
    }

    /**
     * Determine cell type and return its string representation, getStringCellValue() method call could throw exception
     * if cell has number value or another type.
     *
     * @param row excel row
     * @param key excel cell index
     * @return string value string representation
     */
    public static String getCellValue(Map<Integer, String> row, int key) {
        return row.getOrDefault(key, Strings.EMPTY);

    }

    public static boolean isBlankRow(Map<Integer, String> row) {
        return row.entrySet().stream().allMatch(cell -> cell.getValue().isEmpty());
    }

    public static boolean isArrowDelimiterPresent(String attributeName) {
        return !isEmpty(attributeName) && attributeName.contains(DSL_ATTRIBUTE_REF_DELIMITER);
    }

    public static String getDslAttributeKey(String datasetListName, String attributeName) {
        return datasetListName + UNDERSCORE + attributeName;
    }

    public static String getDslAttributeKey(Attribute attribute) {
        return getDslAttributeKey(attribute.getDataSetList().getName(), attribute.getName());
    }

    /**
     * Get dataset parameter key as concatenation of DSL, DS and attribute names.
     * Example, "DSL_DS_attribute".
     *
     * @param parameter inpute parameter
     * @return key
     */
    public static String getDsParameterKey(Parameter parameter) {
        final String datasetListName = parameter.getDataSet().getDataSetList().getName();
        final String datasetName = parameter.getDataSet().getName();
        final String attributeName = parameter.getAttribute().getName();

        return getDsParameterKey(datasetListName, datasetName, attributeName);
    }

    /**
     * Get dataset parameter key as concatenation of DSL, DS and attribute names.
     * Example, "DSL_DS_attribute.
     *
     * @param datasetParameterValue dataset parameter value
     * @param attributeName         attribute name
     * @return key
     */
    public static String getDsParameterKey(DatasetParameterValue datasetParameterValue, String attributeName) {
        final String datasetListName = datasetParameterValue.getDatasetListReference();
        final String datasetName = datasetParameterValue.getDatasetReference();

        return getDsParameterKey(datasetListName, datasetName, attributeName);
    }

    public static String getDsParameterKey(String datasetListName, String datasetName, String attributeName) {
        return datasetListName + UNDERSCORE + datasetName + UNDERSCORE + attributeName;
    }

    public static String getDatasetKey(String datasetListName, String datasetName) {
        return datasetListName + UNDERSCORE + datasetName;
    }

    public static String getDatasetKey(DataSet dataset) {
        return getDatasetKey(dataset.getDataSetList().getName(), dataset.getName());
    }

    /**
     * Get dataset key as concatenation of DSL and DS names.
     * Example, "DSL_DS".
     *
     * @param datasetParameterValue dataset parameter value.
     * @return key
     */
    public static String getDatasetKey(DatasetParameterValue datasetParameterValue) {
        final String datasetListReference = datasetParameterValue.getDatasetListReference();
        final String datasetReference = datasetParameterValue.getDatasetReference();

        return getDatasetKey(datasetListReference, datasetReference);
    }

    /**
     * Get dataset key as concatenation of id DSL and DS name.
     * Example, "IdDSL_NameDS".
     * @param dataset input dataset
     * @return key
     */
    public static String getDatasetKeyPathName(DataSet dataset) {
        return getDatasetKey(dataset.getDataSetList().getId().toString(), dataset.getName());
    }

    /**
     * Get overlap key as concatenation of DS name, attribute key and name.
     * Example, "DS 1_a -> b -> c.
     *
     * @param attributeKey overlap entity.
     * @return overlap key
     */
    public static String getOverlapKey(AttributeKey attributeKey, AttributeImportContext importContext) {
        String datasetName = attributeKey.getDataSet().getName();
        String attributeName = attributeKey.getAttribute().getName();

        String key = attributeKey.getKey();
        List<String> keyParts = Arrays.asList(key.split(UNDERSCORE));
        String attributePath = keyParts.stream()
                .map(subKey -> importContext.getAttribute(UUID.fromString(subKey)))
                .map(Attribute::getName)
                .collect(Collectors.joining(DSL_ATTRIBUTE_REF_DELIMITER));

        attributeName = attributePath.concat(DSL_ATTRIBUTE_REF_DELIMITER).concat(attributeName);

        return getOverlapKey(datasetName, attributeName);
    }

    public static String getOverlapKey(String datasetName, String attributeName) {
        return datasetName + UNDERSCORE + attributeName;
    }
}


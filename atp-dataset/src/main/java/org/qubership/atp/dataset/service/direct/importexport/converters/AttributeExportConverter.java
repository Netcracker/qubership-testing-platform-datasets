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

import static org.qubership.atp.dataset.service.direct.importexport.service.DatasetListExportService.DEFAULT_PASSWORD_MASK;
import static org.qubership.atp.dataset.service.direct.importexport.service.DatasetListExportService.SHIFT_PARAMETER_COLUMN_INDEX;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManParameter;

public interface AttributeExportConverter {

    void mapAttributeToRow(SXSSFRow row, Set<UUID> datasetIds, UiManAttribute attribute);

    /**
     * Map simple attribute to row.
     */
    default void mapAttributeToRow(SXSSFRow row, Set<UUID> datasetIds, UiManAttribute attribute, boolean isEncrypted) {

        Map<UUID, UiManParameter> mapParameterWithDataSet = new HashMap<>();
        attribute.getParameters().forEach(parameter -> mapParameterWithDataSet
                .put(parameter.getDataSet(), parameter));

        // Fill parameters
        int nextCellIndex = SHIFT_PARAMETER_COLUMN_INDEX;
        for (UUID datasetId : datasetIds) {
            Cell cell = row.createCell(nextCellIndex++);
            UiManParameter parameter = mapParameterWithDataSet.get(datasetId);
            if (Objects.nonNull(parameter)) {
                if (isEncrypted) {
                    cell.setCellValue(DEFAULT_PASSWORD_MASK);
                } else {
                    cell.setCellValue(String.valueOf(parameter.getValue()));
                }
            }
        }
    }
}

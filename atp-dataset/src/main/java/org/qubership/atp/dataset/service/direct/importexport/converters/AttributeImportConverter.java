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

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportModel;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetParameterValue;
import org.qubership.atp.dataset.service.direct.importexport.models.ParameterImportResponse;
import org.springframework.stereotype.Component;

@Component
public interface AttributeImportConverter {

    AttributeImportModel mapRowToImportModel(Map<Integer, String> row, AttributeImportContext importContext,
                                             ListIterator<Map<Integer, String>> rowsIterator);

    List<ParameterImportResponse> validate(AttributeImportModel importModel,
                                           AttributeImportContext importContext);

    /**
     * Import attribute parameter.
     *
     * @param importModel import model which contains parsed data from excel row
     * @param datasetParameterValue parameter values for each attribute dataset cortasian
     * @param importContext import context which contains key entities for processing, required to decrease
     *                      number of additional calls to database
     * @return parameter import response if import was success
     * @throws Exception if import wasn't success for some reason
     */
    ParameterImportResponse importAttributeParameter(AttributeImportModel importModel,
                                                     DatasetParameterValue datasetParameterValue,
                                                     AttributeImportContext importContext) throws Exception;
}

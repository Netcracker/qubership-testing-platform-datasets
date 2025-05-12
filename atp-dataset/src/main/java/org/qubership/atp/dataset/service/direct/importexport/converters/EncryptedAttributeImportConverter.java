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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.crypt.ConverterTools;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportModel;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetParameterValue;
import org.qubership.atp.dataset.service.direct.importexport.models.ParameterImportResponse;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EncryptedAttributeImportConverter extends AbstractAttributeConverter implements AttributeImportConverter {

    private static final String PARAM_VALUE_OVERWRITE_STAMP = "*********";

    private final ParameterService parameterService;

    @Override
    public ParameterImportResponse importAttributeParameter(AttributeImportModel importModel,
                                                            DatasetParameterValue datasetParameterValue,
                                                            AttributeImportContext importContext) throws Exception {
        log.debug("Import encrypted attribute parameter, import model: '{}', dataset param value: '{}', context: '{}'",
                importModel, datasetParameterValue, importContext);

        final UUID attributeId = importModel.getId();
        final String datasetName = datasetParameterValue.getDatasetName();
        final UUID datasetId = datasetParameterValue.getDatasetId();
        final String parameterValue = datasetParameterValue.getTextValue();
        final String encodedParameterValue = ConverterTools.encode(parameterValue.getBytes());
        final List<UUID> attributePath = importModel.getPath();
        final String keyAttributeValue = importModel.getKey();
        final boolean isParameterShouldBeOverwritten = !PARAM_VALUE_OVERWRITE_STAMP.equals(parameterValue);
        final boolean isJavers = importContext.isJavers();
        log.debug("Is parameter should be overwritten: {}", isParameterShouldBeOverwritten);

        if (isParameterShouldBeOverwritten) {
            log.info("Import parameter for encrypted attribute Key '{}' Id '{}' in dataset '{}' with path '{}'",
                    keyAttributeValue, attributeId, datasetName, attributePath);
            Parameter parameter = parameterService.setParamSelectJavers(datasetId, attributeId, attributePath,
                    encodedParameterValue,null, null, isJavers);

            log.debug("Parameter with has been successfully imported: {}", parameter);
        }

        return new ParameterImportResponse(datasetId);
    }

    @Override
    public AttributeImportModel mapRowToImportModel(Map<Integer, String> row, AttributeImportContext importContext,
                                                    ListIterator<Map<Integer, String>> rowsIterator) {
        log.debug("Map excel row to encrypted attribute import model");
        return mapTextRowToImportModel(row, importContext);
    }

    @Override
    public List<ParameterImportResponse> validate(AttributeImportModel model, AttributeImportContext context) {
        log.debug("Validate encrypted attribute import model. Import model: '{}', context: '{}'", model, context);

        return new ArrayList<>();
    }
}

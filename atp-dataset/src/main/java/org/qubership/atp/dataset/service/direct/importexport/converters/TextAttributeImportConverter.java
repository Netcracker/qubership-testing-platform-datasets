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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
public class TextAttributeImportConverter extends AbstractAttributeConverter implements AttributeImportConverter {

    private final ParameterService parameterService;

    @Override
    public ParameterImportResponse importAttributeParameter(AttributeImportModel importModel,
                                                            DatasetParameterValue datasetParameterValue,
                                                            AttributeImportContext importContext) throws Exception {
        log.debug("Import text attribute parameter, import model: '{}', dataset param value: '{}',  context: '{}'",
                importModel, datasetParameterValue, importContext);

        final UUID attributeId = importModel.getId();
        final UUID datasetId = datasetParameterValue.getDatasetId();
        final String attributeName = importModel.getName();
        final String datasetName = datasetParameterValue.getDatasetName();
        final String keyAttributeValue = importModel.getKey();
        final String parameterValue = datasetParameterValue.getTextValue();
        boolean isDataChanges = false;
        final boolean isJavers = importContext.isJavers();

        boolean attributeShouldOverlap = isAttributeShouldOverlap(importModel, datasetParameterValue, importContext,
                (parameter) -> !parameterValue.equals(parameter.getText()));

        List<UUID> attributePath = !attributeShouldOverlap ? Collections.emptyList() : importModel.getPath();

        String paramValueTarget = getParamValueTarget(attributeShouldOverlap, datasetParameterValue,
                importContext, attributeId, datasetId, importModel, parameterService);

        if (!Objects.equals(parameterValue, paramValueTarget)) {
            log.info("Import parameter for text attribute Key '{}' Name '{}' in dataset '{}' with value '{}'"
                            + " and path '{}'", keyAttributeValue, attributeName, datasetName, parameterValue,
                    attributePath);
            Parameter parameter = parameterService.setParamSelectJavers(datasetId, attributeId, attributePath,
                    parameterValue,null, null, isJavers);
            isDataChanges = true;
            log.debug("Parameter with has been successfully imported to target database. Parameter: {} ", parameter);
        } else {
            log.debug("Parameter import is complete. This parameter already exists in the target database, "
                            + "datasetParameterValue: {}", datasetParameterValue);
        }
        return new ParameterImportResponse(isDataChanges ? datasetId : null);
    }

    @Override
    public AttributeImportModel mapRowToImportModel(Map<Integer, String> row, AttributeImportContext importContext,
                                                    ListIterator<Map<Integer, String>> rowsIterator) {
        log.debug("Map excel row to text attribute import model");
        return mapTextRowToImportModel(row, importContext);
    }

    @Override
    public List<ParameterImportResponse> validate(AttributeImportModel model, AttributeImportContext context) {
        log.debug("Validate text attribute import model. Import model: '{}', context: '{}'", model, context);
        return new ArrayList<>();
    }
}

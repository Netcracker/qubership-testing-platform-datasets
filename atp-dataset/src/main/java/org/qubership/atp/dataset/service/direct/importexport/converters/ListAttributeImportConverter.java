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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportModel;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetParameterValue;
import org.qubership.atp.dataset.service.direct.importexport.models.ParameterImportResponse;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListAttributeImportConverter extends AbstractAttributeConverter implements AttributeImportConverter {

    private final ParameterService parameterService;
    private final JpaAttributeService attributeService;

    @Override
    public ParameterImportResponse importAttributeParameter(AttributeImportModel importModel,
                                                            DatasetParameterValue datasetParameterValue,
                                                            AttributeImportContext importContext) throws Exception {
        log.debug("Import list attribute parameter, import model: '{}', dataset param value: '{}',  context: '{}'",
                importModel, datasetParameterValue, importContext);

        final String attributeName = importModel.getName();
        final UUID attributeId = importModel.getId();
        final String datasetName = datasetParameterValue.getDatasetName();
        final UUID datasetId = datasetParameterValue.getDatasetId();
        final String parameterValue = datasetParameterValue.getTextValue();
        final String keyAttributeValue = importModel.getKey();
        boolean isDataChanges = false;
        boolean hasNotParent = Objects.isNull(importModel.getParent());
        final boolean isJavers = importContext.isJavers();

        boolean attributeShouldOverlap = isAttributeShouldOverlap(importModel, datasetParameterValue, importContext,
                (parameter) -> !parameterValue.equals(parameter.getListValue().getName())
                        && (Strings.isNotBlank(parameterValue) || hasNotParent));

        List<UUID> attributePath = !attributeShouldOverlap ? Collections.emptyList() : importModel.getPath();

        String paramValueTarget = getParamValueTarget(attributeShouldOverlap, datasetParameterValue,
                importContext, attributeId, datasetId, importModel, parameterService);

        if (StringUtils.isNotEmpty(parameterValue) && !Objects.equals(parameterValue, paramValueTarget)) {
            final ListValue listValue = attributeService.getListValueByAttributeIdAndValue(attributeId, parameterValue);
            final UUID listValueId = listValue.getId();
            log.info("Import parameter for list attribute Key '{}' Id '{}' in dataset name '{}' with value '{}', path "
                            + "'{}' and list value id '{}'", keyAttributeValue, attributeId, datasetName,
                    parameterValue, attributePath, listValueId);
            Parameter parameter = parameterService.setParamSelectJavers(datasetId, attributeId, attributePath,
                    null, null, listValueId, isJavers);
            isDataChanges = true;
            log.debug("Parameter with has been successfully imported: {}", parameter);
        } else if (StringUtils.isNotEmpty(paramValueTarget) && StringUtils.isEmpty(parameterValue) && hasNotParent) {
            final DataSet dataset = importContext.getDataset(datasetId);
            final UUID datasetListId = dataset.getDataSetList().getId();
            log.info("Delete parameter for list attribute Key '{}' Attribute name '{}' Id '{}' with value "
                            + "'{}', path '{}'", keyAttributeValue, attributeName, attributeId, parameterValue,
                    attributePath);
            parameterService.deleteParamSelectJavers(attributeId, datasetId, datasetListId, attributePath, isJavers);
            isDataChanges = true;
        }
        return new ParameterImportResponse(isDataChanges ? datasetId : null);
    }

    @Override
    public AttributeImportModel mapRowToImportModel(Map<Integer, String> row, AttributeImportContext importContext,
                                                    ListIterator<Map<Integer, String>> rowsIterator) {
        log.debug("Map excel row to list attribute import model");
        return mapTextRowToImportModel(row, importContext);
    }

    @Override
    public List<ParameterImportResponse> validate(AttributeImportModel model, AttributeImportContext context) {
        log.debug("Validate list attribute import model. Import model: '{}', context: '{}'", model, context);

        final UUID attributeId = model.getId();
        final List<ListValue> existedListValues = attributeService.getListValuesByAttributeId(attributeId);
        log.debug("Existed list attribute values in schema: {}", existedListValues);

        AtomicReference<String> listValueKeyTemp = new AtomicReference<>();
        final Map<String, ListValue> existedListValuesMap = existedListValues.stream()
                .collect(Collectors.toMap(listValueListValue -> {
                            String keyValue = listValueListValue.getText();
                            listValueKeyTemp.set(keyValue);
                            return keyValue;
                        }, Function.identity(), (firstListValueFound, anyNextListValue) -> {
                            log.warn("List value validate is duplicate map key: '{}', keyPathAttribute '{}',  dslRef "
                                            + "'{}'", listValueKeyTemp.get(), model.getKey(),
                                    model.getDatasetListReference());
                            return anyNextListValue;
                        })
                );
        final List<ParameterImportResponse> errors = new ArrayList<>();
        final List<DatasetParameterValue> datasetParameterValues = model.getDatasetParameterValues();

        final Iterator<DatasetParameterValue> iterator = datasetParameterValues.iterator();
        while (iterator.hasNext()) {
            DatasetParameterValue datasetParameterValue = iterator.next();
            String listAttributeTextValue = datasetParameterValue.getTextValue();
            log.debug("Validate dataset param with list value: {}", listAttributeTextValue);

            final boolean notExistedListValue = !existedListValuesMap.containsKey(listAttributeTextValue);
            if (!listAttributeTextValue.isEmpty() && notExistedListValue) {
                UUID datasetId = datasetParameterValue.getDatasetId();
                String errMsg = "Imported list value attribute parameter has invalid value: " + listAttributeTextValue;
                log.warn(errMsg);
                errors.add(new ParameterImportResponse(datasetId, errMsg));
                iterator.remove();
            }
        }
        return errors;
    }
}

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

package org.qubership.atp.dataset.service.jpa.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.EncryptedParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.FileParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.ListParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.ReferenceDataSetListParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.TextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.client.MacrosFeignClient;
import org.qubership.atp.macros.core.clients.api.dto.macros.MacrosDto;
import org.qubership.atp.macros.core.converter.MacrosDtoConvertService;
import org.qubership.atp.macros.core.model.Macros;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import lombok.Getter;

/**
 * Service class used for creating root parameters entities for DS tree.
 */
@Service
@Lazy
public class DataSetParameterProvider {
    @Autowired
    protected GridFsRepository gridFsRepository;
    @Getter
    @Autowired
    protected MacroContextService macroContextService;
    @Autowired
    protected ModelsProvider modelsProvider;
    @Autowired
    protected MacrosFeignClient macrosFeignClient;
    @Autowired
    @Getter
    protected MacrosCalculator macrosCalculator;

    /**
     * Returns parameter implementation, depending on it's type.
     */
    public AbstractParameter getDataSetParameterResolved(
            UUID dataSetListId,
            UUID dataSetParameterId,
            AttributeTypeName attributeType,
            boolean evaluate,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        if (macroContext == null) {
            macroContext = new MacroContext();
            macroContext.setMacroContextService(macroContextService);
        }
        Parameter parameter = null;
        if (dataSetParameterId != null) {
            parameter = modelsProvider.getParameterById(dataSetParameterId);
        }
        switch (attributeType) {
            case ENCRYPTED:
                return processAsEncryptedDataSetParameter(
                        dataSetListId,
                        parameter,
                        macroContext,
                        parameterPositionContext
                );
            case CHANGE:
            case TEXT:
                return processAsTextDataSetParameter(
                    dataSetListId,
                    parameter,
                    evaluate,
                    macroContext,
                    parameterPositionContext
                );
            case FILE:
                return processAsFileDataSetParameter(parameter, parameterPositionContext);
            case LIST:
                return processAsListDataSetParameter(dataSetListId,
                    parameter,
                    evaluate,
                    macroContext,
                    parameterPositionContext
            );
            case DSL:
                return processReferenceDataSetParameter(parameter, parameterPositionContext);
            default:
                return null;
        }
    }

    private AbstractParameter processAsTextDataSetParameter(
            UUID dataSetListId,
            Parameter parameter,
            boolean evaluate,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        if (parameter == null) {
            return new TextParameter(null, parameterPositionContext);
        }
        TextParameter textParameter = new TextParameter(
                parameter.getStringValue(),
                evaluate,
                macroContext,
                parameterPositionContext
        );
        Map<ParameterPositionContext, String> cachedEvaluatedValues =
                macroContextService.getCachedEvaluatedValues(dataSetListId);
        textParameter.setCachedValues(cachedEvaluatedValues);
        return textParameter;
    }

    private AbstractParameter processAsEncryptedDataSetParameter(
            UUID dataSetListId,
            Parameter parameter,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        if (parameter == null) {
            return new EncryptedParameter(null, parameterPositionContext);
        }
        EncryptedParameter encryptedParameter = new EncryptedParameter(
                parameter.getStringValue(),
                false,
                macroContext,
                parameterPositionContext
        );
        Map<ParameterPositionContext, String> cachedEvaluatedValues =
                macroContextService.getCachedEvaluatedValues(dataSetListId);
        encryptedParameter.setCachedValues(cachedEvaluatedValues);
        return encryptedParameter;
    }

    private AbstractParameter processAsFileDataSetParameter(
            Parameter parameter, ParameterPositionContext parameterPositionContext
    ) {
        if (parameter == null) {
            return new FileParameter(parameterPositionContext);
        }
        Optional<FileData> fileDataOptional = gridFsRepository.getFileInfo(parameter.getId());
        FileData fileData = null;
        if (fileDataOptional.isPresent()) {
            fileData = fileDataOptional.get();
        }
        return new FileParameter(parameter, fileData, parameterPositionContext);
    }

    private AbstractParameter processAsListDataSetParameter(
            UUID dataSetListId,
            Parameter parameter,
            boolean evaluate,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        if (parameter == null) {
            return new ListParameter(null, parameterPositionContext);
        }
        ListParameter listParameter = new ListParameter(
                parameter.getListValue() != null ? parameter.getListValue().getText() : null,
                evaluate,
                macroContext,
                parameterPositionContext
        );
        Map<ParameterPositionContext, String> cachedEvaluatedValues =
                macroContextService.getCachedEvaluatedValues(dataSetListId);
        listParameter.setCachedValues(cachedEvaluatedValues);
        return listParameter;
    }

    private AbstractParameter processReferenceDataSetParameter(
            Parameter parameter, ParameterPositionContext parameterPositionContext
    ) {
        if (parameter == null) {
            return new ReferenceDataSetListParameter(parameterPositionContext);
        }
        return new ReferenceDataSetListParameter(parameter, parameterPositionContext);
    }

    public FileData getFileVariableInfo(UUID parameterId) {
        Optional<FileData> fileInfo = gridFsRepository.getFileInfo(parameterId);
        return fileInfo.orElse(null);
    }

    /**
     * Get model macros.
     */
    public List<Macros> getAtpMacros(UUID visibilityAreaId) {
        List<MacrosDto> macrosDtoList = macrosFeignClient
                .findAllByProject(visibilityAreaId).getBody();
        return new MacrosDtoConvertService().convertList(macrosDtoList, Macros.class);
    }
}

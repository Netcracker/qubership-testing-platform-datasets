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

package org.qubership.atp.dataset.service.jpa.model.tree.params;

import java.util.HashMap;
import java.util.Map;

import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import com.fasterxml.jackson.annotation.JsonInclude;

public class FileParameter extends AbstractParameter {
    private Map<String, String> valueRef = new HashMap<>();
    private String value;

    /**
     * Default constructor.
     * */
    public FileParameter(
            Parameter parameter,
            FileData fileData,
            ParameterPositionContext parameterPositionContext
    ) {
        if (fileData != null) {
            value = parameter.getId().toString() + "." + fileData.getFileType();
            valueRef.put("contentType", fileData.getContentType());
            valueRef.put("url", fileData.getUrl());
        }
        setOrder(parameterPositionContext.getOrder());
    }

    /**
     * Default constructor.
     * */
    public FileParameter(
            ParameterPositionContext parameterPositionContext
    ) {
        setOrder(parameterPositionContext.getOrder());
    }

    @Override
    public AttributeTypeName getType() {
        return AttributeTypeName.FILE;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getInItfFormatValue() {
        return valueRef.get("url");
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> getValueRef() {
        return valueRef;
    }

    @Override
    public boolean isNullValue() {
        return value == null;
    }
}

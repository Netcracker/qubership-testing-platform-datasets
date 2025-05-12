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

package org.qubership.atp.dataset.versioning.model.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.exception.file.FileDsNotFoundException;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

import joptsimple.internal.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
@Value
public class ParameterSnapshot {
    private final UUID id;
    private final UUID dataSetListId;
    private final UUID dataSetId;
    private String text;
    private AttributeTypeName type;
    private UUID dataSetReference;
    private UUID listValueId;
    private String listValueName;
    private FileDataSnapshot fileData;

    /**
     * Constructor.
     */
    public ParameterSnapshot(Parameter model) {
        id = model.getId();
        dataSetListId = model.getDataSet().getDataSetList().getId();
        dataSetId = model.getDataSet().getId();
        Attribute attribute = model.getAttribute();
        if (attribute == null) {
            attribute = model.getAttributeKey().getAttribute();
        }
        type = attribute.getAttributeType();
        switch (type) {
            case ENCRYPTED:
            case TEXT:
            case CHANGE:
                text = model.getStringValue();
                break;
            case LIST:
                ListValueEntity listValue = model.getListValue();
                if (listValue != null) {
                    listValueId = listValue.getId();
                    listValueName = listValue.getText();
                }
                break;
            case DSL:
                DataSet dataSetReferenceValue = model.getDataSetReferenceValue();
                if (dataSetReferenceValue != null) {
                    dataSetReference = dataSetReferenceValue.getId();
                    text = dataSetReferenceValue.getName();
                }
                break;
            case FILE:
                try {
                    FileData parameterFileData = model.getFileData();
                    fileData = parameterFileData == null ? null : new FileDataSnapshot(parameterFileData);
                } catch (FileDsNotFoundException e) {
                    log.debug("File variable {} not found", model.getId());
                }
                break;
            default:
        }
    }

    /**
     * Mask for encrypted, special text view for change type.
     * */
    public Object getValuePretty(ModelsProvider modelsProvider) {
        if (type == null) {
            return getValueByType();
        }
        switch (type) {
            case ENCRYPTED:
                return "***********";
            case CHANGE:
                return getMultipleValuePretty(text, modelsProvider);
            case LIST:
                return listValueName;
            case FILE:
                return fileData;
            case DSL:
            case TEXT:
            default:
                return text;
        }
    }

    /**
     * Special text view for change type.
     * */
    public static Object getMultipleValuePretty(String value, ModelsProvider modelsProvider) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        String[] split = value.replaceFirst("MULTIPLY ", "").split(" ");
        List<String> result = new LinkedList<>();
        for (String uuidString : split) {
            DataSet dataSetById = modelsProvider.getDataSetById(UUID.fromString(uuidString));
            if (dataSetById != null) {
                result.add(dataSetById.getName());
            } else {
                result.add(uuidString);
            }
        }
        return String.join(", ", result);
    }

    /**
     * Get value object by attribute type.
     */
    public Object getValueByType() {
        if (text != null) {
            return text;
        }
        if (listValueId != null) {
            return listValueName;
        }
        if (dataSetReference != null) {
            return dataSetReference;
        }
        if (fileData != null) {
            return fileData;
        }
        return null;
    }
}

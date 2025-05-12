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

import static java.util.Objects.isNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.qubership.atp.dataset.exception.attribute.AttributeExportException;
import org.qubership.atp.dataset.exception.attribute.AttributeImportException;
import org.qubership.atp.dataset.service.direct.importexport.converters.AttributeExportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.AttributeImportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.AttributeTypeConverterEnum;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class DataSetListImportExportFactory {

    private final List<AttributeImportConverter> importConvertorsList;
    private final List<AttributeExportConverter> exportConvertorsList;

    private Map<Class<? extends AttributeImportConverter>, ? extends AttributeImportConverter> importConvertorsMap;
    private Map<Class<? extends AttributeExportConverter>, ? extends AttributeExportConverter> exportConvertorsMap;

    @PostConstruct
    private void initImportConverterMap() {
        this.importConvertorsMap = importConvertorsList.stream()
                .collect(Collectors.toMap(AttributeImportConverter::getClass, Function.identity()));
        this.exportConvertorsMap = exportConvertorsList.stream()
                .collect(Collectors.toMap(AttributeExportConverter::getClass, Function.identity()));
    }

    /**
     * Get attribute import converter by specified type name.
     *
     * @param typeName type name
     * @return import converter
     */
    public AttributeImportConverter getAttributeImportConverter(String typeName) {
        Class<? extends AttributeImportConverter> importConverterClazz =
                AttributeTypeConverterEnum.getImportConverterClazzByName(typeName);
        AttributeImportConverter attributeImportConverter = importConvertorsMap.get(importConverterClazz);
        if (isNull(attributeImportConverter)) {
            log.error("Cannot found  attribute import convertor implementation by type name: " + typeName);
            throw new AttributeImportException();
        }
        return attributeImportConverter;
    }

    /**
     * Get attribute export converter by specified type name.
     *
     * @param typeName type name
     * @return export converter
     */
    public AttributeExportConverter getAttributeExportConverter(String typeName) {
        Class<? extends AttributeExportConverter> exportConverterClazz =
                AttributeTypeConverterEnum.getExportConverterClazzByName(typeName);
        AttributeExportConverter attributeExportConverter = exportConvertorsMap.get(exportConverterClazz);
        if (isNull(attributeExportConverter)) {
            log.error("Cannot found Attribute export convertor implementation by type: " + typeName);
            throw new AttributeExportException();
        }
        return attributeExportConverter;
    }
}

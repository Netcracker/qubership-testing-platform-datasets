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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public enum AttributeTypeConverterEnum {

    TEXT("Text", TextAttributeImportConverter.class, TextAttributeExportConverter.class),
    LIST("List", ListAttributeImportConverter.class, ListAttributeExportConverter.class),
    ENCRYPTED("Encrypted", EncryptedAttributeImportConverter.class, EncryptedAttributeExportConverter.class),
    DSL("DSL", DatasetLinkAttributeImportConverter.class, DatasetLinkAttributeExportConverter.class);

    private String name;
    private Class<? extends AttributeImportConverter> importConverterClazz;
    private Class<? extends AttributeExportConverter> exportConverterClazz;

    private static Map<String, Class<? extends AttributeImportConverter>> importConverterClazzMap;
    private static Map<String, Class<? extends AttributeExportConverter>> exportConverterClazzMap;

    static {
        importConverterClazzMap = Arrays.stream(values())
                .collect(Collectors.toMap(Enum::name, AttributeTypeConverterEnum::getImportConverterClazz));
        exportConverterClazzMap = Arrays.stream(values())
                .collect(Collectors.toMap(Enum::name, AttributeTypeConverterEnum::getExportConverterClazz));
    }

    AttributeTypeConverterEnum(String name,
                               Class<? extends AttributeImportConverter> importConverterClazz,
                               Class<? extends AttributeExportConverter> exportConverterClazz) {
        this.name = name;
        this.importConverterClazz = importConverterClazz;
        this.exportConverterClazz = exportConverterClazz;
    }

    public static Class<? extends AttributeImportConverter> getImportConverterClazzByName(String name) {
        log.debug("Get import converter class by name: {}", name);
        return importConverterClazzMap.get(name.toUpperCase());
    }

    public static Class<? extends AttributeExportConverter> getExportConverterClazzByName(String name) {
        log.debug("Get export converter class by name: {}", name);
        return exportConverterClazzMap.get(name.toUpperCase());
    }
}

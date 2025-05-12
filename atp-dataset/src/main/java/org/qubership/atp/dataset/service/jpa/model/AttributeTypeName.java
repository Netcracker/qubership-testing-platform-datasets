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

package org.qubership.atp.dataset.service.jpa.model;

import lombok.Getter;

public enum AttributeTypeName {
    TEXT("TEXT", 1L),
    FILE("FILE", 2L),
    LIST("LIST", 3L),
    DSL("DSL", 4L),
    CHANGE("CHANGE", 5L),
    ENCRYPTED("ENCRYPTED", 6L);

    @Getter
    private String name;
    @Getter
    private Long id;

    AttributeTypeName(String name, Long id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Enum by it's name.
     * */
    public static AttributeTypeName getTypeByName(String name) {
        for (AttributeTypeName value : values()) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        throw new RuntimeException("Unknown attribute type " + name);
    }

    /**
     * Enum by it's id.
     * */
    public static AttributeTypeName getTypeById(Long id) {
        for (AttributeTypeName value : values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        throw new RuntimeException("Unknown attribute type with id " + id);
    }
}

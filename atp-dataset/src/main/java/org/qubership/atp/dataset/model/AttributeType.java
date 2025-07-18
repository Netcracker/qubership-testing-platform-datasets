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

package org.qubership.atp.dataset.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

public enum AttributeType {
    TEXT((short) 1), FILE((short) 2), LIST((short) 3), DSL((short) 4), CHANGE((short) 5), ENCRYPTED((short) 6);

    private static final Map<Short, AttributeType> shortMapping = new HashMap<>();
    private final short id;

    AttributeType(short id) {
        this.id = id;
    }

    /**
     * Finds AttributeType with specified id.
     *
     * @param id of attribute type.
     * @return AttributeType of that id.
     */
    @Nonnull
    public static AttributeType from(short id) {
        return shortMapping.computeIfAbsent(id,
                someId -> Arrays.stream(AttributeType.values())
                        .filter(type -> someId.equals(type.getId()))
                        .findFirst().orElseThrow(
                                () -> new IllegalStateException("Illegal attribute type specified. Expected: "
                                        + Arrays.toString(AttributeType.values())
                                )
                        ));
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return this.name();
    }
}

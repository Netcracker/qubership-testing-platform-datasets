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

package org.qubership.atp.dataset.service.jpa.model.copy;

import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;

import lombok.Data;

@Data
public class AttributeCopyData {
    private Attribute attributeCopy;
    private Map<UUID, UUID> listValuesMap;

    public AttributeCopyData(Attribute attribute, Map<UUID, UUID> listValuesMap) {
        this.attributeCopy = attribute;
        this.listValuesMap = listValuesMap;
    }

    public UUID getCopyId() {
        return attributeCopy.getId();
    }
}

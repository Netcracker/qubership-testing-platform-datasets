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

import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@EqualsAndHashCode
@Value
public class AttributeKeySnapshot {
    private final UUID id;
    private final String attributeName;
    private final String namesPath;
    private final UUID dataSetId;
    private final UUID attributeId;
    private final ParameterSnapshot parameter;
    @Setter
    private List<UUID> attributePath;

    /**
     * Constructor from snapshot.
     */
    public AttributeKeySnapshot(AttributeKey attributeKey, Parameter parameterOverlap) {
        id = attributeKey.getId();
        dataSetId = attributeKey.getDataSetId();
        attributePath = attributeKey.getPath();
        namesPath = attributeKey.getPathNames();
        Attribute attribute = attributeKey.getAttribute();
        attributeId = attribute.getId();
        attributeName = attribute.getName();
        parameter = new ParameterSnapshot(parameterOverlap);
    }
}

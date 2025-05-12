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

package org.qubership.atp.dataset.versioning.service.changes;

import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.annotation.Entity;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;
import org.qubership.atp.dataset.versioning.model.domain.AttributeKeySnapshot;
import org.qubership.atp.dataset.versioning.model.domain.ParameterSnapshot;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Entity
@TypeName("AttributeKey")
public class AttributeKeyComparable {
    @Id
    private final UUID id;
    private final String attributeName;
    private final UUID dataSetId;
    private final List<UUID> attributePath;
    private final String attributePathNames;
    private final UUID attributeId;
    private final ParameterSnapshot parameter;

    /**
     * Constructor from snapshot.
     */
    public AttributeKeyComparable(AttributeKeySnapshot attributeKeySnapshot) {
        id = attributeKeySnapshot.getId();
        dataSetId = attributeKeySnapshot.getDataSetId();
        attributePath = attributeKeySnapshot.getAttributePath();
        attributePathNames = attributeKeySnapshot.getNamesPath();
        attributeName = attributeKeySnapshot.getAttributeName();
        attributeId = attributeKeySnapshot.getAttributeId();
        parameter = attributeKeySnapshot.getParameter();
    }
}

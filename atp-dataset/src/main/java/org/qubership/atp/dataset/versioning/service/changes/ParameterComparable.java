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

import java.util.UUID;

import org.javers.core.metamodel.annotation.Entity;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.versioning.model.domain.ParameterSnapshot;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Entity
@TypeName("Parameter")
public class ParameterComparable {
    @Id
    private UUID id;
    private UUID dataSet;
    private AttributeTypeName type;
    private Object value;
    private String attributeName;

    /**
     * Constructor.
     */
    public ParameterComparable(String attributeName, ParameterSnapshot snapshot) {
        this.id = snapshot.getId();
        this.dataSet = snapshot.getDataSetId();
        this.value = snapshot.getValueByType();
        this.type = snapshot.getType();
        this.attributeName = attributeName;
    }
}

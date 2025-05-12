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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.javers.core.metamodel.annotation.Entity;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;
import org.qubership.atp.dataset.versioning.model.domain.AttributeKeySnapshot;
import org.qubership.atp.dataset.versioning.model.domain.AttributeSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.DataSetSnapshot;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Entity
@TypeName("DataSetList")
public class DataSetListComparable {

    @Id
    private final UUID id;
    private final String name;
    private final Set<DataSetComparable> dataSets = new HashSet<>();
    private final Set<AttributeComparable> attributes = new HashSet<>();
    private final Set<AttributeKeyComparable> overlaps = new HashSet<>();

    /**
     * Constructor.
     */
    public DataSetListComparable(DataSetListSnapshot snapshot) {
        id = snapshot.getId();
        name = snapshot.getName();
        for (DataSetSnapshot dataSet : snapshot.getDataSets()) {
            dataSets.add(new DataSetComparable(dataSet));
        }
        for (AttributeSnapshot attribute : snapshot.getAttributes()) {
            attributes.add(new AttributeComparable(attribute));
        }
        for (AttributeKeySnapshot overlap : snapshot.getOverlaps()) {
            overlaps.add(new AttributeKeyComparable(overlap));
        }
    }
}

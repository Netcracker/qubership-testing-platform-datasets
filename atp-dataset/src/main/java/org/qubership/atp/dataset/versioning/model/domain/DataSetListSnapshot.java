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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javers.core.metamodel.annotation.Entity;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Entity
@TypeName("DataSetListSnapshot")
public class DataSetListSnapshot {
    @Id
    private final UUID id;
    private final List<DataSetSnapshot> dataSets = new LinkedList<>();
    private final List<AttributeSnapshot> attributes = new LinkedList<>();
    private final List<AttributeKeySnapshot> overlaps = new LinkedList<>();
    private final String name;
    private final UUID visibilityAreaId;

    /**
     * Constructor from domain model.
     */
    public DataSetListSnapshot(DataSetList model) {
        id = model.getId();
        name = model.getName();
        visibilityAreaId = model.getVisibilityArea().getId();
        for (DataSet dataSet : model.getDataSets()) {
            dataSets.add(new DataSetSnapshot(dataSet));
        }
        for (Attribute attribute : model.getAttributes()) {
            attributes.add(new AttributeSnapshot(attribute));
        }
        for (Map.Entry<AttributeKey, Parameter> entry : model.getOverLapParametersMapping().entrySet()) {
            overlaps.add(new AttributeKeySnapshot(entry.getKey(), entry.getValue()));
        }
    }

    public Set<UUID> getAttributeIds() {
        return getAttributes().stream()
                .map(AttributeSnapshot::getId).collect(Collectors.toSet());
    }

    public Set<UUID> getDataSetsIds() {
        return getDataSets().stream()
                .map(DataSetSnapshot::getId).collect(Collectors.toSet());
    }

    public Set<UUID> getOverlapsIds() {
        return getOverlaps().stream()
                .map(AttributeKeySnapshot::getId).collect(Collectors.toSet());
    }

    /**
     * Get dataset by id.
     */
    public DataSetSnapshot getDataSetById(UUID dataSetId) {
        for (DataSetSnapshot dataSet : getDataSets()) {
            if (dataSet.getId().equals(dataSetId)) {
                return dataSet;
            }
        }
        return null;
    }

    /**
     * Get attribute by id.
     */
    public AttributeSnapshot getAttributeById(UUID attributeId) {
        for (AttributeSnapshot attributeSnapshot : getAttributes()) {
            if (attributeSnapshot.getId().equals(attributeId)) {
                return attributeSnapshot;
            }
        }
        return null;
    }

    /**
     * Get overlap by id.
     */
    public AttributeKeySnapshot getOverlapById(UUID overlapId) {
        for (AttributeKeySnapshot overlap : getOverlaps()) {
            if (overlap.getId().equals(overlapId)) {
                return overlap;
            }
        }
        return null;
    }
}

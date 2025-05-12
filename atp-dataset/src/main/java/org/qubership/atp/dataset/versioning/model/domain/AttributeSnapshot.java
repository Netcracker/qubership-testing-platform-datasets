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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@Value
public class AttributeSnapshot {

    private final AttributeTypeName type;
    private final Integer ordering;
    private final List<ListValueSnapshot> listValues = new LinkedList<>();
    private final List<ParameterSnapshot> parameters = new LinkedList<>();
    private final UUID id;
    private final String name;
    private final UUID dataSetListReference;

    /**
     * Constructor.
     */
    public AttributeSnapshot(Attribute model) {
        id = model.getId();
        name = model.getName();
        type = model.getAttributeType();
        dataSetListReference = model.getTypeDataSetListId();
        ordering = model.getOrdering();
        for (ListValue listValue : model.getListValues()) {
            listValues.add(new ListValueSnapshot(listValue));
        }
        for (Parameter parameter : model.getParameters()) {
            parameters.add(new ParameterSnapshot(parameter));
        }
    }

    public Set<UUID> getParametersIds() {
        return getParameters().stream()
                .map(ParameterSnapshot::getId).collect(Collectors.toSet());
    }

    /**
     * Get parameter by id.
     */
    public ParameterSnapshot getParameterById(UUID parameterId) {
        for (ParameterSnapshot parameter : getParameters()) {
            if (parameter.getId().equals(parameterId)) {
                return parameter;
            }
        }
        return null;
    }

    public Set<UUID> getListValuesIds() {
        return getListValues().stream()
                .map(ListValueSnapshot::getId).collect(Collectors.toSet());
    }

    /**
     * Get list value by id.
     */
    public ListValueSnapshot getListValueById(UUID listValueId) {
        for (ListValueSnapshot listValue : getListValues()) {
            if (listValue.getId().equals(listValueId)) {
                return listValue;
            }
        }
        return null;
    }
}

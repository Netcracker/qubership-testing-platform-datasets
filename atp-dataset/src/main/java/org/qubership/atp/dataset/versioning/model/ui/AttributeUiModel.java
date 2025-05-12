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

package org.qubership.atp.dataset.versioning.model.ui;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.versioning.model.domain.AttributeKeySnapshot;
import org.qubership.atp.dataset.versioning.model.domain.AttributeSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.ListValueSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.ParameterSnapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@Value
public class AttributeUiModel {
    private UUID id;
    private String name;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ParameterUiModel> parameters = new LinkedList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<AttributeUiModel> attributes = new LinkedList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ListValueSnapshot> listValues = new LinkedList<>();
    private AttributeTypeName type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID dataSetListReference;

    /**
     * Top-level constructor.
     */
    public AttributeUiModel(
            AttributeSnapshot snapshot,
            List<AttributeKeySnapshot> overlaps,
            ModelsProvider modelsProvider
    ) {
        id = snapshot.getId();
        name = snapshot.getName();
        type = snapshot.getType();
        dataSetListReference = snapshot.getDataSetListReference();
        for (ParameterSnapshot parameter : snapshot.getParameters()) {
            parameters.add(new ParameterUiModel(parameter, modelsProvider, false));
        }
        List<OverlapUiNode> nodes = OverlapUiNode.getNodes(overlaps, modelsProvider);
        for (OverlapUiNode node : nodes) {
            if (node.getAttributeId().equals(id)) {
                for (OverlapUiNode childNode : node.getNodes().values()) {
                    attributes.add(new AttributeUiModel(childNode, modelsProvider));
                }
                for (AttributeKeySnapshot attributeKeySnapshot : node.getOverlapsList()) {
                    addAttributeIfNotExists(attributes, attributeKeySnapshot, modelsProvider);
                }
            }
        }
    }

    private void addAttributeIfNotExists(
            List<AttributeUiModel> attributes,
            AttributeKeySnapshot overlap,
            ModelsProvider modelsProvider
    ) {
        AttributeUiModel attribute = getAttributeById(overlap.getAttributeId());
        if (attribute == null) {
            attributes.add(new AttributeUiModel(overlap, modelsProvider));
        } else {
            attribute.parameters.add(new ParameterUiModel(overlap.getParameter(), modelsProvider, true));
        }
    }

    private AttributeUiModel getAttributeById(UUID attributeId) {
        for (AttributeUiModel attribute : attributes) {
            if (attribute.getId().equals(attributeId)) {
                return attribute;
            }
        }
        return null;
    }

    /**
     * Attribute path entity. Can't have parameters.
     */
    public AttributeUiModel(
            OverlapUiNode node,
            ModelsProvider modelsProvider
    ) {
        id = node.getAttributeId();
        name = node.getName();
        type = AttributeTypeName.DSL;
        for (OverlapUiNode childNode : node.getNodes().values()) {
            attributes.add(new AttributeUiModel(childNode, modelsProvider));
        }
        for (AttributeKeySnapshot attributeKeySnapshot : node.getOverlapsList()) {
            addAttributeIfNotExists(attributes, attributeKeySnapshot, modelsProvider);
        }
    }

    /**
     * Overlapped attribute entity. Must have parameters.
     */
    public AttributeUiModel(
            AttributeKeySnapshot overlap,
            ModelsProvider modelsProvider
    ) {
        id = overlap.getAttributeId();
        name = overlap.getAttributeName();
        type = AttributeTypeName.DSL;
        parameters.add(new ParameterUiModel(overlap.getParameter(), modelsProvider, true));
    }
}

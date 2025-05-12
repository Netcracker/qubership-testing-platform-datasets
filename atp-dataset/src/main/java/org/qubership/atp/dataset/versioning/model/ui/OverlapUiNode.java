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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.versioning.model.domain.AttributeKeySnapshot;

import lombok.Getter;

@Getter
public class OverlapUiNode {
    private final UUID attributeId;
    private final String name;
    private final int level;
    private final Map<UUID, AttributeKeySnapshot> overLaps = new LinkedHashMap<>();
    private final Map<UUID, OverlapUiNode> nodes = new LinkedHashMap<>();

    private OverlapUiNode(int level, String name, UUID attributeId) {
        this.attributeId = attributeId;
        this.name = name;
        this.level = level;
    }

    /**
     * Return root nodes list.
     */
    public static List<OverlapUiNode> getNodes(
            List<AttributeKeySnapshot> overlaps,
            ModelsProvider modelsProvider
    ) {
        Map<UUID, OverlapUiNode> result = new LinkedHashMap<>();
        for (AttributeKeySnapshot overlap : overlaps) {
            List<UUID> attributePath = overlap.getAttributePath();
            if (!attributePath.isEmpty()) {
                UUID attributePathId = attributePath.get(0);
                if (!result.containsKey(attributePathId)) {
                    result.put(attributePathId, new OverlapUiNode(
                            0, getAttributeName(attributePathId, modelsProvider), attributePathId)
                    );
                }
                result.get(attributePathId).propagateAttributeKey(overlap, modelsProvider);
            }
        }
        return new LinkedList<>(result.values());
    }

    private static String getAttributeName(UUID attributeId, ModelsProvider modelsProvider) {
        Attribute attribute = modelsProvider.getAttributeById(attributeId);
        if (attribute == null) {
            return "[NOT FOUND]";
        }
        return attribute.getName();
    }

    private void propagateAttributeKey(
            AttributeKeySnapshot overlap,
            ModelsProvider modelsProvider
    ) {
        List<UUID> attributePath = overlap.getAttributePath();
        if (attributePath.size() == level + 1) {
            overLaps.put(overlap.getAttributeId(), overlap);
        } else {
            if (!attributePath.isEmpty()) {
                UUID newAttributePathId = attributePath.get(level + 1);
                if (!nodes.containsKey(newAttributePathId)) {
                    nodes.put(newAttributePathId, new OverlapUiNode(
                            level + 1, getAttributeName(newAttributePathId, modelsProvider), newAttributePathId)
                    );
                }
                nodes.get(newAttributePathId).propagateAttributeKey(overlap, modelsProvider);
            }
        }
    }

    public List<AttributeKeySnapshot> getOverlapsList() {
        return new LinkedList<>(overLaps.values());
    }
}

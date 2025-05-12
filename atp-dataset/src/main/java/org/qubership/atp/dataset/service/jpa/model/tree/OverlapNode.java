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

package org.qubership.atp.dataset.service.jpa.model.tree;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;

import lombok.Getter;
import lombok.Setter;

/**
 * Overlap model. Makes hierarchy from attribute keys. Repeats DSL structure.
 * If overlap already exists for some parameter,
 * that means it was overlapped by some DSL from higher level, so we will ignore it.
 * */
@Getter
@Setter
public class OverlapNode implements Serializable {
    private static final long serialVersionUID = 2784126984197713421L;
    private UUID dataSetListId;
    private UUID id;
    private Overlap[] overlaps;
    private List<OverlapNode> childNodes = new LinkedList<>();

    /**
     * Default constructor.
     * */
    public OverlapNode(UUID id, UUID dataSetListId, int columns) {
        this.id = id;
        this.dataSetListId = dataSetListId;
        overlaps = new Overlap[columns];
    }

    /**
     * New overlap to structure. If field already overlapped - operation will skipped,
     * to save hierarchy multiple overlaps.
     * */
    public void addOverlap(List<UUID> path, int columnNumber, AttributeKey attributeKey) {
        if (attributeKey.getParameter() == null) {
            return;
        }
        if (path.isEmpty()) {
            if (overlaps[columnNumber] != null) {
                if (!overlaps[columnNumber].isTheSameAttributeOverlap(attributeKey)) {
                    UUID attributeId = attributeKey.getAttribute().getId();
                    if (!overlaps[columnNumber].getOverlappedAttributes().containsKey(attributeId)) {
                        overlaps[columnNumber].getOverlappedAttributes().put(attributeId, attributeKey);
                    }
                }
            } else {
                Overlap newOverlap = new Overlap();
                UUID attributeId = attributeKey.getAttribute().getId();
                newOverlap.getOverlappedAttributes().put(attributeId, attributeKey);
                overlaps[columnNumber] = newOverlap;
            }
        } else {
            UUID childNodeId = path.get(0);
            boolean nodeExists = false;
            for (OverlapNode childNode : childNodes) {
                boolean samePath = childNodeId.equals(childNode.getId());
                if (samePath) {
                    childNode.addOverlap(path.subList(1, path.size()), columnNumber, attributeKey);
                    nodeExists = true;
                    break;
                }
            }
            if (!nodeExists) {
                OverlapNode newNode = new OverlapNode(childNodeId, dataSetListId, overlaps.length);
                childNodes.add(newNode);
                newNode.addOverlap(path.subList(1, path.size()), columnNumber, attributeKey);
            }
        }
    }

    /**
     * Returns overlap by parameter context.
     * */
    public AttributeKey getOverlap(List<UUID> path, int column, UUID attributeId) {
        if (path.isEmpty()) {
            if (overlaps[column] != null) {
                return overlaps[column].getOverlapByAttributeId(attributeId);
            }
            return null;
        } else {
            for (OverlapNode childNode : childNodes) {
                if (childNode.getId().equals(path.get(0))) {
                    return childNode.getOverlap(path.subList(1, path.size()), column, attributeId);
                }
            }
            return null;
        }
    }

    /**
     * True if there is any overlaps by path OR deeper.
     * */
    public boolean containsOverlapsInPath(List<UUID> path, int column) {
        if (path.isEmpty()) {
            if (overlaps[column] != null) {
                return true;
            }
            return false;
        } else {
            for (OverlapNode childNode : childNodes) {
                if (childNode.getId().equals(path.get(0))) {
                    return childNode.containsOverlapsInPath(path.subList(1, path.size()), column);
                }
            }
            return false;
        }
    }
}

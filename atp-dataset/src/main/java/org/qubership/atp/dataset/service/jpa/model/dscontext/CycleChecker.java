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

package org.qubership.atp.dataset.service.jpa.model.dscontext;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.exception.datasetlist.DataSetListCycleException;

/**
 * Class used for graph cycling check. If we opened node twice, that means this graph is cycled.
 * */
public class CycleChecker {
    private List<UUID> openedNodes = new LinkedList<>();

    /**
     * Opens new node. Throws exception if there is cycle.
     */
    public void openNode(UUID nodeId) {
        if (openedNodes.contains(nodeId)) {
            throw new DataSetListCycleException();
        }
        openedNodes.add(nodeId);
    }

    /**
     * Closes node. Throws exception if there is cycle.
     */
    public void closeNode(UUID nodeId) {
        openedNodes.remove(nodeId);
    }
}

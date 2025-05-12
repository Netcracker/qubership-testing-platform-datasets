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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import lombok.Getter;

public class DataSetListEvaluatedParametersCache {
    @Getter
    private Map<UUID, DataSetListEvaluatedParametersMap> cachedValues = new HashMap<>();

    /**
     * Get cached parameters values by DSL id.
     * */
    public Map<ParameterPositionContext, String> getCache(UUID dataSetListId) {
        if (dataSetListId == null) {
            return new HashMap<>();
        }
        if (!cachedValues.containsKey(dataSetListId)) {
            cachedValues.put(dataSetListId, new DataSetListEvaluatedParametersMap());
        }
        return cachedValues.get(dataSetListId);
    }
}

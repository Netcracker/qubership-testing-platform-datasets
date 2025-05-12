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

package org.qubership.atp.dataset.service.jpa.impl.macro;

import java.util.LinkedList;
import java.util.List;

import org.qubership.atp.dataset.service.jpa.model.PathStep;

public class CachedDslMacroResultContainer {
    private List<CachedDslMacroResult> cachedValues = new LinkedList<>();

    /**
     * Returns matched DSL parameter cache.
     * */
    public CachedDslMacroResult getCachedValue(
            PathStep dataSetList, PathStep dataSet, List<PathStep> attributeGroups, PathStep attribute
    ) {
        for (CachedDslMacroResult cachedValue : cachedValues) {
            if (cachedValue.getDataSetList().matches(
                    dataSetList.getName(),
                    dataSetList.getId()
                )
                && cachedValue.getDataSet().matches(dataSet)
                && cachedValue.getAttribute().matches(attribute)
                && cachedValue.getAttributeGroups().size() == attributeGroups.size()
            ) {
                boolean groupsMatch = true;
                for (int i = 0; i < attributeGroups.size(); i++) {
                    if (!cachedValue.getAttributeGroups().get(i).matches(
                            attributeGroups.get(i)
                    )) {
                        groupsMatch = false;
                    }
                }

                if (groupsMatch) {
                    return cachedValue;
                }
            }
        }

        return null;
    }

    /**
     * Add new DSL parameter value to cache.
     * */
    public void storeValue(
            PathStep dataSetList, PathStep dataSet, List<PathStep> attributeGroups, PathStep attribute, String value
    ) {
        CachedDslMacroResult newValue = new CachedDslMacroResult();
        newValue.setDataSetList(dataSetList);
        newValue.setDataSet(dataSet);
        newValue.setAttributeGroups(new LinkedList<>(attributeGroups));
        newValue.setAttribute(attribute);
        newValue.setValue(value);
        cachedValues.add(newValue);
    }
}

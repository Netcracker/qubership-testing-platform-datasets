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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

public class DataSetListEvaluatedParametersMap implements Map<ParameterPositionContext, String> {
    private Map<ParameterPositionContext, String> cachedValues = new LinkedHashMap<>();

    @Override
    public int size() {
        return cachedValues.size();
    }

    @Override
    public boolean isEmpty() {
        return cachedValues.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cachedValues.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cachedValues.containsValue(value);
    }

    @Override
    public String get(Object key) {
        String s = cachedValues.get(key);
        return s;
    }

    @Nullable
    @Override
    public String put(ParameterPositionContext key, String value) {
        return cachedValues.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return cachedValues.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends ParameterPositionContext, ? extends String> m) {
        cachedValues.putAll(m);
    }

    @Override
    public void clear() {
        cachedValues.clear();
    }

    @NotNull
    @Override
    public Set<ParameterPositionContext> keySet() {
        return cachedValues.keySet();
    }

    @NotNull
    @Override
    public Collection<String> values() {
        return cachedValues.values();
    }

    @NotNull
    @Override
    public Set<Entry<ParameterPositionContext, String>> entrySet() {
        return cachedValues.entrySet();
    }
}

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

package org.qubership.atp.dataset.model.utils.tree;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Leaf implements Map.Entry<List<String>, Object> {

    private final List<String> path;
    private final Object leaf;

    public Leaf(@Nonnull List<String> path, @Nullable Object leaf) {
        this.path = path;
        this.leaf = leaf;
    }

    @Nonnull
    @Override
    public List<String> getKey() {
        return path;
    }

    @Nullable
    @Override
    public Object getValue() {
        return leaf;
    }

    @Override
    public Object setValue(Object value) {
        throw new UnsupportedOperationException("designed to be immutable");
    }

    @Nonnull
    public List<String> getPath() {
        return getKey();
    }

    @Nullable
    public Object getLeaf() {
        return getValue();
    }
}

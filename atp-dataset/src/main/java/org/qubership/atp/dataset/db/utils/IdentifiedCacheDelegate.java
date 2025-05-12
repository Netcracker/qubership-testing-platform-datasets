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

package org.qubership.atp.dataset.db.utils;

import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.IdentifiedCache;
import org.qubership.atp.dataset.model.Identified;

public abstract class IdentifiedCacheDelegate implements IdentifiedCache {

    @Nonnull
    @Override
    public <T extends Identified> T computeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                    @Nonnull Function<UUID, ? extends T> sourceFunc) {
        return getCache().computeIfAbsent(type, id, sourceFunc);
    }

    @Nullable
    @Override
    public <T extends Identified> T tryComputeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                       @Nonnull Function<UUID, ? extends T> sourceFunc) {
        return getCache().tryComputeIfAbsent(type, id, sourceFunc);
    }

    @Nullable
    @Override
    public <T extends Identified> T getIfPresent(@Nonnull Class<T> type, @Nonnull UUID id) {
        return getCache().getIfPresent(type, id);
    }

    @Override
    public <T extends Identified> void put(@Nonnull Class<T> type, @Nonnull T identified) {
        getCache().put(type, identified);
    }

    @Override
    public void clear() {
        getCache().clear();
    }

    protected abstract IdentifiedCache getCache();
}

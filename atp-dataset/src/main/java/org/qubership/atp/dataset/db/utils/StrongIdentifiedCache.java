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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.IdentifiedCache;
import org.qubership.atp.dataset.model.Identified;

import com.google.common.base.Preconditions;

public class StrongIdentifiedCache implements IdentifiedCache {
    private Map<Class<? extends Identified>, Map<UUID, ? extends Identified>> caches = null;

    public StrongIdentifiedCache() {
        this(null);
    }

    public StrongIdentifiedCache(@Nullable Map<Class<? extends Identified>, Map<UUID, ? extends Identified>> caches) {
        this.caches = caches;
    }

    @Nonnull
    @Override
    public <T extends Identified> T computeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                    @Nonnull Function<UUID, ? extends T> sourceFunc) {
        return getCache(type).computeIfAbsent(id, uuid -> Preconditions.checkNotNull(sourceFunc.apply(uuid),
                "[%s] with id [%s] is not provided by [%s]", type, id, sourceFunc));
    }

    @Nullable
    @Override
    public <T extends Identified> T getIfPresent(@Nonnull Class<T> type, @Nonnull UUID id) {
        if (caches == null) {
            return null;
        }
        Map<UUID, ? extends Identified> cache = caches.get(type);
        if (cache == null) {
            return null;
        }
        return (T) cache.get(id);
    }

    public <T extends Identified> void put(@Nonnull Class<T> type, @Nonnull T identified) {
        getCache(type).put(identified.getId(), identified);
    }

    @Override
    public void clear() {
        if (caches == null) {
            return;
        }
        for (Map<UUID, ? extends Identified> cache : caches.values()) {
            cache.clear();
        }
    }

    @Nonnull
    private <T extends Identified> Map<UUID, T> getCache(@Nonnull Class<T> type) {
        if (caches == null) {
            caches = new HashMap<>();
        }
        Map<UUID, ? extends Identified> cache = caches.get(type);
        if (cache == null) {
            cache = createCache();
            caches.put(type, cache);
        }
        return (Map<UUID, T>) cache;
    }

    private <T extends Identified> Map<UUID, T> createCache() {
        return new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StrongIdentifiedCache that = (StrongIdentifiedCache) o;
        return Objects.equals(caches, that.caches);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caches);
    }
}

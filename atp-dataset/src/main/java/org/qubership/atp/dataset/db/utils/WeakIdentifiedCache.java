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
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.IdentifiedCache;
import org.qubership.atp.dataset.model.Identified;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class WeakIdentifiedCache implements IdentifiedCache {
    private static CacheBuilder CACHE_BUILDER = CacheBuilder.from("weakValues");
    private Map<Class<? extends Identified>, Cache<UUID, Identified>> caches = null;

    @Nonnull
    @Override
    public <T extends Identified> T computeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                    @Nonnull Function<UUID, ? extends T> sourceFunc) {
        try {
            return getCache(type).get(id, () -> Preconditions.checkNotNull(sourceFunc.apply(id),
                    "[%s] with id [%s] is not provided by [%s]", type, id, sourceFunc));
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    @Nullable
    @Override
    public <T extends Identified> T getIfPresent(@Nonnull Class<T> type, @Nonnull UUID id) {
        if (caches == null) {
            return null;
        }
        Cache<UUID, Identified> cache = caches.get(type);
        if (cache == null) {
            return null;
        }
        return (T) cache.getIfPresent(id);
    }

    public <T extends Identified> void put(@Nonnull Class<T> type, @Nonnull T identified) {
        getCache(type).put(identified.getId(), identified);
    }

    @Override
    public void clear() {
        if (caches == null) {
            return;
        }
        for (Cache<UUID, Identified> cache : caches.values()) {
            cache.invalidateAll();
        }
    }

    @Nonnull
    private <T extends Identified> Cache<UUID, T> getCache(@Nonnull Class<T> type) {
        if (caches == null) {
            caches = new HashMap<>();
        }
        Cache<UUID, Identified> cache = caches.get(type);
        if (cache == null) {
            cache = createCache();
            caches.put(type, cache);
        }
        return (Cache<UUID, T>) cache;
    }

    private <T extends Identified> Cache<UUID, T> createCache() {
        return CACHE_BUILDER.build();
    }
}

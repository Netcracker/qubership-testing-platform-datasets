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

package org.qubership.atp.dataset.db;

import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.utils.IdentifiedCacheDelegate;
import org.qubership.atp.dataset.db.utils.WeakIdentifiedCache;
import org.qubership.atp.dataset.model.Identified;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;

@Repository
public class CacheRepository extends IdentifiedCacheDelegate {
    public static final IdentifiedCache DISABLED = new IdentifiedCache() {

        @Nonnull
        @Override
        public <T extends Identified> T computeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                        @Nonnull Function<UUID, ? extends T> sourceFunc) {
            return Preconditions.checkNotNull(sourceFunc.apply(id),
                    "[%s] with id [%s] is not provided by [%s]", type, id, sourceFunc);
        }

        @Nullable
        @Override
        public <T extends Identified> T tryComputeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                           @Nonnull Function<UUID, ? extends T> sourceFunc) {
            return sourceFunc.apply(id);
        }

        @Nullable
        @Override
        public <T extends Identified> T getIfPresent(@Nonnull Class<T> type, @Nonnull UUID id) {
            return null;
        }

        @Override
        public <T extends Identified> void put(@Nonnull Class<T> type, @Nonnull T identified) {
        }

        @Override
        public void clear() {

        }
    };
    private final ThreadLocal<CacheSwitch> caches = ThreadLocal.withInitial(CacheSwitch::new);

    @Nonnull
    @Override
    public IdentifiedCache getCache() {
        return caches.get().cache;
    }

    public boolean isCacheEnabled() {
        return caches.get().isEnabled();
    }

    public void enableCache() {
        caches.get().enable();
    }

    public void disableCache() {
        caches.get().disable();
    }

    private static class CacheSwitch {
        IdentifiedCache cache = DISABLED;
        private int usages = -1;

        public void enable() {
            if (++usages == 0) {
                cache = new WeakIdentifiedCache();
            }
        }

        public void disable() {
            if (--usages == -1) {
                cache = DISABLED;
            }
        }

        public boolean isEnabled() {
            return usages > -1;
        }
    }
}

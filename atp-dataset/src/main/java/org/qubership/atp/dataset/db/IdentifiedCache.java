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

import org.qubership.atp.dataset.model.Identified;

import com.google.common.base.Preconditions;

public interface IdentifiedCache {

    /**
     * Returns value if it is already present in the cache. Invokes function otherwise. Function
     * should not return null. Caches new provided value.
     */
    @Nonnull
    default <T extends Identified> T computeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                     @Nonnull Function<UUID, ? extends T> sourceFunc) {
        T result = tryComputeIfAbsent(type, id, sourceFunc);
        Preconditions.checkNotNull(result,
                "[%s] with id [%s] is not provided by [%s]", type, id, sourceFunc);
        return result;
    }

    /**
     * Returns value if it is already present in the cache. Invokes function otherwise. Function may
     * return null. Caches new provided value (if it is not null).
     */
    @Nullable
    default <T extends Identified> T tryComputeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                        @Nonnull Function<UUID, ? extends T> sourceFunc) {
        T result = getIfPresent(type, id);
        if (result != null) {
            return result;
        }
        result = sourceFunc.apply(id);
        if (result != null) {
            put(type, result);
        }
        return result;
    }

    @Nullable
    <T extends Identified> T getIfPresent(@Nonnull Class<T> type, @Nonnull UUID id);

    <T extends Identified> void put(@Nonnull Class<T> type, @Nonnull T identified);

    void clear();
}

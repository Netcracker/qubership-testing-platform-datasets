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

package org.qubership.atp.dataset.macros.cache;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleCache implements Cache {
    private Map<Object, MacroCacheKey> map = new HashMap<>();

    public SimpleCache() {
    }

    @Nonnull
    @Override
    public MacroCacheKey newKey(@Nonnull Object of) {
        MacroCacheKey result = map.get(of);
        if (result == null) {
            result = new MacroCacheEntry(of);
        }
        return result;
    }

    private class MacroCacheEntry implements MacroCacheKey {
        private final Object key;
        private boolean inMap = false;
        private String value = null;

        private MacroCacheEntry(@Nonnull Object key) {
            this.key = key;
        }

        @Override
        public void cacheValue(@Nonnull String value) {
            this.value = value;
            if (!inMap) {
                map.put(key, this);
                inMap = true;
            }
        }

        @Nullable
        @Override
        public String lookupValue() {
            return value;
        }
    }
}

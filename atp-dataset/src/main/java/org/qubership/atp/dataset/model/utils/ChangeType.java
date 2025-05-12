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

package org.qubership.atp.dataset.model.utils;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Joiner;

public enum ChangeType {
    MULTIPLY;

    private static final Joiner JOINER = Joiner.on(' ');

    /**
     * Serializes change.
     */
    @Nonnull
    public String toText(@Nullable Collection<UUID> args) {
        Stream<String> parts = Stream.of(name());
        if (args != null) {
            parts = Stream.concat(parts, args.stream().map(Object::toString));
        }
        return JOINER.join(parts.iterator());
    }
}

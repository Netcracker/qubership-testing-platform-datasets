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

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class Change {
    private static final Splitter SPLITTER = Splitter.on(' ');
    public final ChangeType type;
    public final List<UUID> arguments;

    public Change(ChangeType type, List<UUID> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    /**
     * Deserializes instance from text.
     *
     * @param text form.
     */
    @Nonnull
    public static Change fromText(@Nonnull String text) {
        Iterator<String> iterator = SPLITTER.split(text).iterator();
        Preconditions.checkArgument(iterator.hasNext(), "Macro is empty and can not be parsed");
        String next = iterator.next();
        ChangeType change = ChangeType.valueOf(next);
        List<UUID> args = Lists.newArrayList(Iterators.transform(iterator, UUID::fromString));
        return new Change(change, args);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("arguments", arguments)
                .toString();
    }
}

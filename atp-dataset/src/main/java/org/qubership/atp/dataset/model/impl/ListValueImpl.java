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

package org.qubership.atp.dataset.model.impl;

import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.ListValue;

public class ListValueImpl extends AbstractNamed implements ListValue {
    private Attribute attribute;

    public ListValueImpl() {
    }

    /**
     * see {@link ListValue}.
     */
    public ListValueImpl(@Nonnull UUID id, @Nonnull Attribute attribute, @Nonnull String name) {
        this.id = id;
        this.attribute = attribute;
        this.name = name;
    }

    @Override
    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.of(getAttribute());
    }
}

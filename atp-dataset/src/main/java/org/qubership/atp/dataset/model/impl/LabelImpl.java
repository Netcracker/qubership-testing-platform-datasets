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

import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Label;

public class LabelImpl extends AbstractNamed implements Label {

    /**
     * Just a bean.
     */
    public LabelImpl(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.empty();
    }
}

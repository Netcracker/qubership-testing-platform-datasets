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

import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * https://stackoverflow.com/questions/32131987/how-can-i-make-cartesian-product-with-java-8-streams
 */
class Prefix<T> {
    public final T value;
    public final Prefix<T> parent;

    public Prefix(@Nullable Prefix<T> parent, @Nonnull T value) {
        this.parent = parent;
        this.value = value;
    }

    /**
     * Puts the whole prefix into given collection.
     */
    public <A> A addTo(@Nonnull A in, @Nonnull BiConsumer<A, T> acc) {
        if (parent != null) {
            parent.addTo(in, acc);
        }
        acc.accept(in, value);
        return in;
    }
}

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

package org.qubership.atp.dataset.utils;

import java.util.function.Supplier;

public class MutableSupplier<T> implements Supplier<T> {
    private boolean valueWasSet;

    private T value;

    private MutableSupplier() {
    }

    public static <T> MutableSupplier<T> create() {
        return new MutableSupplier<>();
    }

    @Override
    public T get() {
        if (!valueWasSet) {
            throw new NullPointerException("Value has not been set yet");
        }
        return value;
    }

    /**
     * Sets a value to a supplier.
     */
    public T set(final T value) {
        if (valueWasSet) {
            throw new IllegalStateException("Value has already been set and should not be reset");
        }
        this.value = value;
        this.valueWasSet = true;
        return value;
    }
}

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

package org.qubership.atp.dataset.model.utils.tree;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.AbstractIterator;

public abstract class ChainIterator<I, O> extends AbstractIterator<O> {
    private final Iterator<I> toDecorate;
    private O previous;


    protected ChainIterator(Iterator<I> toDecorate) {
        this.toDecorate = toDecorate;
    }

    @Nullable
    protected abstract O getChildren(@Nonnull I toDecorate, @Nullable O previous);

    @Override
    protected O computeNext() {
        if (!toDecorate.hasNext()) {
            return endOfData();
        }
        I next = toDecorate.next();
        assert next != null;
        O result = getChildren(next, previous);
        if (result == null) {
            return endOfData();
        }
        previous = result;
        return result;
    }
}

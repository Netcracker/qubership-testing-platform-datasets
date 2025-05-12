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
import java.util.LinkedList;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class TraverseHandlerToMock<T> implements TraverseAndLeafsHandler<T, T, T> {

    protected final LinkedList<T> currentPath = new LinkedList<>();
    private final Function<T, Iterator<? extends T>> childrenSup;

    protected TraverseHandlerToMock(Function<T, Iterator<? extends T>> childrenSup) {
        this.childrenSup = childrenSup;
    }

    @Override
    public Iterator<? extends T> getChildren(@Nonnull T item) {
        return childrenSup.apply(item);
    }

    @Nonnull
    @Override
    public TraverseAndLeafsHandler<?, T, T> backToPreviousParent() {
        currentPath.removeLast();
        return this;
    }

    @Nonnull
    @Override
    public TraverseAndLeafsHandler<T, ?, T> forwardToNewParent(@Nonnull T parent) {
        currentPath.add(parent);
        return this;
    }

    @Nullable
    @Override
    public T constructLeaf(@Nonnull T leaf) {
        return leaf;
    }
}

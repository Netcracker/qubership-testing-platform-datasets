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

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Sets;

/**
 * Takes parents in form of iterator. Iterates over parents themselves plus over it's children,
 * returned by {@link #getChildren(Object)}. Omits nulls and duplicates. May be used to traverse a
 * tree.
 */
public abstract class AllRefsIterator<T> extends AbstractIterator<T> {

    private final Deque<Iterator<? extends T>> items;
    private final Predicate<T> itemsFilter;
    private T lastProcessed;

    /**
     * See {@link AllRefsIterator}.
     */
    public AllRefsIterator(@Nonnull Iterator<? extends T> parents, boolean omitDuplicates) {
        this(parents, omitDuplicates ? new RegularNoCycleGarant<>() : always -> true);
    }

    protected AllRefsIterator(@Nonnull Iterator<? extends T> parents, Predicate<T> itemsFilter) {
        items = new LinkedList<>();
        this.itemsFilter = itemsFilter;
        items.add(parents);
    }

    @Override
    protected T computeNext() {
        if (lastProcessed != null) {
            Iterator<? extends T> children = getChildren(lastProcessed);
            if (children != null && children.hasNext()) {
                items.push(children);
                forwardToNewParent(lastProcessed);
            }
            lastProcessed = null;
        }
        //lastProcessed always null here
        Iterator<? extends T> temp = items.peek();
        while (!temp.hasNext()) {
            items.remove();
            temp = items.peek();
            if (temp == null) {
                return endOfData();
            }
            backToPreviousParent();
        }
        T result = temp.next();
        if (result == null || !itemsFilter.test(result)) {
            return computeNext();
        }
        lastProcessed = result;
        return result;
    }

    @Nullable
    protected abstract Iterator<? extends T> getChildren(@Nonnull T parent);

    protected void forwardToNewParent(T parent) {
    }

    protected void backToPreviousParent() {
    }

    private static class RegularNoCycleGarant<T> implements Predicate<T> {

        private final Set<T> cyclicProtection = Sets.newHashSetWithExpectedSize(5);

        @Override
        public boolean test(@Nullable T t) {
            if (cyclicProtection.contains(t)) {
                return false;
            }
            cyclicProtection.add(t);
            return true;
        }
    }
}
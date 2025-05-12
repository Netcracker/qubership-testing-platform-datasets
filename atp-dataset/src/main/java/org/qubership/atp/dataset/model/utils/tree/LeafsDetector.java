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
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Adds leafs detector functionality to {@link AllRefsIterator}. <br/>Designed to be used in
 * aggregation: <br/>{@link AllRefsIterator#getChildren(Object)} becomes {@link
 * TraverseHandler#getChildren(Object)} <br/>and {@link AllRefsIterator#backToPreviousParent()}
 * becomes {@link TraverseHandler#backToPreviousParent()}.
 */
class LeafsDetector<I extends T, O extends T, T> extends AllRefsIterator<T> {

    protected TraverseHandler<I, O> currentTraverseHandler;

    /**
     * <br/>Iterates over a tree hierarchy. <br/>The {@link Iterator#next()} invocation returns item
     * of current iteration, same as {@link AllRefsIterator}. <br/>Reports about vertical level
     * changes using childrenSup and backToParentsCb. <br/>Does not resolve recursion by default.
     * You can do that in childrenSup or itemsFilter. <br/>Reports about leafs found using
     * leafsConsumer. <br/>One iteration may result in invocation of leafsConsumer no more then one
     * time.
     *
     * @param parents     to iterate over. Inclusive.
     * @param itemsFilter filters objects to iterate over. Delegates to {@link
     *                    AllRefsIterator#itemsFilter}
     */
    LeafsDetector(@Nonnull Iterator<? extends I> parents,
                  @Nonnull TraverseHandler<I, O> traverseHandler,
                  @Nullable Predicate<T> itemsFilter) {
        super(parents, itemsFilter == null ? always -> true : itemsFilter);
        this.currentTraverseHandler = traverseHandler;
    }

    @Nullable
    @Override
    protected Iterator<? extends O> getChildren(@Nonnull T parent) {
        //noinspection unchecked
        I typeSafeParent = (I) parent;
        return currentTraverseHandler.getChildren(typeSafeParent);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void forwardToNewParent(T parent) {
        I typeSafeParent = (I) parent;
        currentTraverseHandler = (TraverseHandler<I, O>) currentTraverseHandler.forwardToNewParent(typeSafeParent);
    }

    @Override
    protected void backToPreviousParent() {
        //noinspection unchecked
        currentTraverseHandler = (TraverseHandler<I, O>) currentTraverseHandler.backToPreviousParent();
    }
}


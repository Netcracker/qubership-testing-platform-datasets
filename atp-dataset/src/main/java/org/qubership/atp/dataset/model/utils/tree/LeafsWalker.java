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

import com.google.common.collect.AbstractIterator;

public class LeafsWalker<I, O, L> extends AbstractIterator<L> {

    private final LeafsDetector<I, O, Object> leafsSup;
    private L leaf;

    /**
     * Iterates over leafs.
     *
     * @param parents         to iterate over. Inclusive.
     * @param traverseHandler describes paths to iterate over and explains how construct leaf.
     * @param itemsFilter     may filter any tree node. See {@link AllRefsIterator#itemsFilter}. May
     *                        be used to resolve recursion.
     */
    public LeafsWalker(@Nonnull Iterator<? extends I> parents,
                       @Nonnull TraverseAndLeafsHandler<I, O, L> traverseHandler,
                       @Nullable Predicate<Object> itemsFilter) {
        leafsSup = new LeafsDetector<I, O, Object>(parents,
                traverseHandler,
                itemsFilter) {

            @Nullable
            @Override
            protected Iterator<? extends O> getChildren(@Nonnull Object parent) {
                Iterator<? extends O> result = super.getChildren(parent);
                if (result == null || !result.hasNext()) {
                    consume(parent);
                }
                return result;
            }
        };
    }

    @Override
    protected L computeNext() {
        this.leaf = null;
        while (this.leaf == null && leafsSup.hasNext()) {
            leafsSup.next();
        }
        if (this.leaf == null) {
            return endOfData();
        }
        return this.leaf;
    }

    private void consume(Object leaf) {
        I typeSafeLeaf = (I) leaf;
        TraverseAndLeafsHandler<I, O, L> currentTraverseHandler =
                (TraverseAndLeafsHandler<I, O, L>) this.leafsSup.currentTraverseHandler;
        this.leaf = currentTraverseHandler.constructLeaf(typeSafeLeaf);
    }
}

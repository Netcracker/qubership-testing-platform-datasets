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

public class RefsVisitor<I, O> extends LeafsDetector<I, O, Object> {

    /**
     * <br/>Iterates over a tree hierarchy. <br/>The {@link Iterator#next()} invocation returns item
     * of current iteration, same as {@link AllRefsIterator}. <br/>Reports about vertical level
     * changes using childrenSup and backToParentsCb. <br/>Does not resolve recursion by default.
     * You can do that in childrenSup or itemsFilter. <br/>Reports about leafs found using
     * leafsConsumer. <br/>One iteration may result in invocation of leafsConsumer no more then one
     * time.
     *
     * @param parents     to iterate over. Inclusive.
     * @param visitor     to handle visit events.
     * @param itemsFilter filters objects to iterate over. Delegates to {@link
     *                    AllRefsIterator#itemsFilter}
     */
    public RefsVisitor(@Nonnull Iterator<? extends I> parents,
                       @Nonnull TraverseVisitor<I, O> visitor,
                       @Nullable Predicate<Object> itemsFilter) {
        super(parents, visitor, itemsFilter);
    }

    @Nullable
    @Override
    protected Iterator<? extends O> getChildren(@Nonnull Object parent) {
        Iterator<? extends O> result = super.getChildren(parent);
        if (result == null || !result.hasNext()) {
            TraverseVisitor<I, O> visitor = ((TraverseVisitor<I, O>) currentTraverseHandler);
            visitor.notifyItemStarts((I) parent);
            visitor.notifyItemEnds();
        }
        return result;
    }

    @Override
    protected void forwardToNewParent(Object parent) {
        TraverseVisitor<I, O> visitor = ((TraverseVisitor<I, O>) currentTraverseHandler);
        visitor.notifyItemStarts((I) parent);
        visitor.notifyProcessingChildren();
        super.forwardToNewParent(parent);
    }

    @Override
    protected void backToPreviousParent() {
        super.backToPreviousParent();
        TraverseVisitor<I, O> visitor = ((TraverseVisitor<I, O>) currentTraverseHandler);
        visitor.notifyChildrenProcessed();
        visitor.notifyItemEnds();
    }
}

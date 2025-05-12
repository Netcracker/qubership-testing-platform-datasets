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

/**
 * Describes paths to iterate over and explains how to construct leaf. Make possible to iterate over
 * tree of different types of objects.
 *
 * @param <I> type of current depth items to iterate over.
 * @param <O> type of the deeper depth items to iterate over.
 */
public interface TraverseHandler<I, O> {

    /**
     * Invoked when iteration goes upper to parents in a tree hierarchy.
     *
     * @return handler of previous depth.
     */
    @Nonnull
    TraverseHandler<?, I> backToPreviousParent();

    /**
     * Invoked when iteration goes deeper to children which becomes new parent in a tree hierarchy.
     *
     * @return handler of deeper depth level.
     */
    @Nonnull
    TraverseHandler<O, ?> forwardToNewParent(@Nonnull I parent);

    /**
     * Explains how to get children from parent. May return null. Invoked when iteration goes
     * deeper.
     *
     * @param item go get children from
     * @return children
     */
    @Nullable
    Iterator<? extends O> getChildren(@Nonnull I item);
}

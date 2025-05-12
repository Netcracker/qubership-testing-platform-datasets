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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.mockito.InOrder;
import org.mockito.Mockito;

class Position<T> {

    private final TraverseHandlerToMock<T> h;
    private final Iterator<T> iterator;
    private final InOrder inOrder;

    private Position(TraverseHandlerToMock<T> h, Iterator<T> iterator, InOrder inOrder) {
        this.h = h;
        this.iterator = iterator;
        this.inOrder = inOrder;
    }

    @SafeVarargs
    public static <T> Position<T> ofLeafsDetector(Function<T, Iterator<? extends T>> childrenSup, T... roots) {
        return ofLeafsDetector(childrenSup, Arrays.asList(roots).iterator());
    }

    public static <T> Position<T> ofLeafsDetector(Function<T, Iterator<? extends T>> childrenSup, Iterator<? extends T> data) {
        TraverseHandlerToMock<T> h = Mockito.spy(new TraverseHandlerToMock<>(childrenSup));
        LeafsDetector<T, T, T> detector = new LeafsDetector<T, T, T>(data, h, null) {
            @Nullable
            @Override
            protected Iterator<? extends T> getChildren(@Nonnull T parent) {
                Iterator<? extends T> result = super.getChildren(parent);
                if (result == null || !result.hasNext()) {
                    h.constructLeaf(parent);
                }
                return result;
            }
        };
        return new Position<>(h, detector, Mockito.inOrder(h));
    }

    @SafeVarargs
    public static <T> Position<T> ofLeafsWalker(Function<T, Iterator<? extends T>> childrenSup, T... roots) {
        return ofLeafsWalker(childrenSup, Arrays.asList(roots).iterator());
    }

    public static <T> Position<T> ofLeafsWalker(Function<T, Iterator<? extends T>> childrenSup, Iterator<? extends T> data) {
        TraverseHandlerToMock<T> h = new TraverseHandlerToMock<>(childrenSup);
        h = Mockito.spy(h);
        LeafsWalker<T, T, T> walker = new LeafsWalker<>(data, h, null);
        return new Position<>(h, walker, Mockito.inOrder(h));
    }

    /**
     * Validates {@link LeafsDetector} does expected set of commands after the next iteration.
     *
     * @param node        item should be reached after next iteration.
     * @param explanation explains how {@link LeafsDetector} should reach the item of the next
     *                    iteration.
     */
    public Position<T> toTheNextItem(@Nonnull T node, Consumer<HandlerMock> explanation, List<T> nodePath) {
        Mockito.reset(h);
        Assertions.assertTrue(iterator.hasNext());
        T next = iterator.next();
        Assertions.assertEquals(node, next);
        verifyExplanation(explanation, nodePath);
        return this;
    }

    /**
     * Validates {@link LeafsDetector} does expected set of commands after the iteration finishes.
     *
     * @param explanation explains which commands {@link LeafsDetector} should execute before
     *                    reaching end of data.
     */
    public Position<T> toTheFinish(Consumer<HandlerMock> explanation) {
        Mockito.reset(h);
        Assertions.assertFalse(iterator.hasNext());
        verifyExplanation(explanation, Collections.emptyList());
        return this;
    }

    private void verifyExplanation(Consumer<HandlerMock> explanation, List<T> nodePath) {
        HandlerMock explanationH = new HandlerMock();
        explanation.accept(explanationH);
        explanationH.explained();
        Assertions.assertEquals(nodePath, h.currentPath);
    }

    public class HandlerMock {

        public HandlerMock leafFound(@Nonnull T leaf) {
            inOrder.verify(h, times(1)).constructLeaf(leaf);
            return this;
        }

        public HandlerMock wentUnder(T toItem) {
            inOrder.verify(h, times(1)).forwardToNewParent(toItem);
            return this;
        }

        public HandlerMock checkChildren(T of) {
            inOrder.verify(h, times(1)).getChildren(of);
            return this;
        }

        public HandlerMock wentUpper(int times) {
            inOrder.verify(h, times(times)).backToPreviousParent();
            return this;
        }

        private Position<T> explained() {
            inOrder.verify(h, never()).constructLeaf(any());
            inOrder.verify(h, never()).backToPreviousParent();
            inOrder.verify(h, never()).getChildren(any());
            inOrder.verify(h, never()).forwardToNewParent(any());
            return Position.this;
        }
    }
}

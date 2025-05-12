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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class LeafsWalkerTest extends TestData {
    private static final Logger LOG = LoggerFactory.getLogger(LeafsWalkerTest.class);

    LinkedList path = new LinkedList();
    private StringToInt stringToInt = new StringToInt();
    private IntToString intToString = new IntToString();

    @Test
    public void IterateOverTree_OneRoot_TraverseHandlerMethodsInvocationOrderIsCorrect() throws Exception {
        animals.printTree(LOG::info);
        Position.ofLeafsWalker(item -> item.children.iterator(), animals)
                .toTheNextItem(fish, h -> h.checkChildren(animals).wentUnder(animals)//path points to animals now
                                .checkChildren(fish).leafFound(fish),
                        animals.getPathInclusive())//path points to animals inclusive
                .toTheNextItem(lizard, h -> h.checkChildren(reptile).wentUnder(reptile)//path points to reptile now
                                .checkChildren(lizard).leafFound(lizard).wentUpper(1),//path points to animals now
                        animals.getPathInclusive())//path points to animals inclusive
                .toTheNextItem(equine, h -> h.checkChildren(mammal).wentUnder(mammal)//path points to mammal now
                                .checkChildren(equine).leafFound(equine),
                        mammal.getPathInclusive())//path points to mammal inclusive
                .toTheNextItem(bovine, h -> h.checkChildren(bovine).leafFound(bovine)//path points to mammal now
                                .wentUpper(2),//no path
                        Collections.emptyList())//no path
                .toTheFinish(h -> {
                });
    }

    @Test
    public void IterateOverMultiTypeTree_MultipleRootsTwoTypes_TraverseHandlerTypeSafeIsGuaranteed() throws Exception {
        LeafsWalker<String, Integer, Leaf> walker = new LeafsWalker<>(ImmutableList.of("1", "2").iterator(), stringToInt, null);
        /* Tree:
            "1"  "2"
             0   0 1
                  "0"
           Leafs: 0, 0, "0" */
        Leaf next = walker.next();
        Assertions.assertFalse(next.isString);
        Assertions.assertEquals(0, next.leaf);
        Assertions.assertEquals(new LinkedList<Object>() {{
            add("1");
        }}, next.path);
        next = walker.next();
        Assertions.assertFalse(next.isString);
        Assertions.assertEquals(0, next.leaf);
        Assertions.assertEquals(new LinkedList<Object>() {{
            add("2");
        }}, next.path);
        next = walker.next();
        Assertions.assertTrue(next.isString);
        Assertions.assertEquals("0", next.leaf);
        Assertions.assertEquals(new LinkedList<Object>() {{
            add("2");
            add(1);
        }}, next.path);
    }

    private static class Leaf {

        private final Object leaf;
        private final boolean isString;
        private final List path;

        private Leaf(Object leaf, boolean isString, List path) {
            this.leaf = leaf;
            this.isString = isString;
            this.path = path;
        }
    }

    private class StringToInt implements TraverseAndLeafsHandler<String, Integer, Leaf> {

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<?, String, Leaf> backToPreviousParent() {
            path.removeLast();
            return LeafsWalkerTest.this.intToString;
        }

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<Integer, ?, Leaf> forwardToNewParent(@Nonnull String parent) {
            assertThat(parent, instanceOf(String.class));
            path.add(parent);
            return LeafsWalkerTest.this.intToString;
        }

        @Override
        public Iterator<? extends Integer> getChildren(@Nonnull String item) {
            assertThat(item, instanceOf(String.class));
            int i = Integer.parseInt(item);
            return IntStream.range(0, i).boxed().iterator();
        }

        @Nullable
        @Override
        public Leaf constructLeaf(@Nonnull String leaf) {
            assertThat(leaf, instanceOf(String.class));
            return new Leaf(leaf, true, ImmutableList.copyOf(path));
        }
    }

    private class IntToString implements TraverseAndLeafsHandler<Integer, String, Leaf> {

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<?, Integer, Leaf> backToPreviousParent() {
            path.removeLast();
            return LeafsWalkerTest.this.stringToInt;
        }

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<String, ?, Leaf> forwardToNewParent(@Nonnull Integer parent) {
            assertThat(parent, instanceOf(Integer.class));
            path.add(parent);
            return LeafsWalkerTest.this.stringToInt;
        }

        @Override
        public Iterator<? extends String> getChildren(@Nonnull Integer item) {
            assertThat(item, instanceOf(Integer.class));
            return IntStream.range(0, item).mapToObj(String::valueOf).iterator();
        }

        @Nullable
        @Override
        public Leaf constructLeaf(@Nonnull Integer leaf) {
            assertThat(leaf, instanceOf(Integer.class));
            return new Leaf(leaf, false, ImmutableList.copyOf(path));
        }
    }
}

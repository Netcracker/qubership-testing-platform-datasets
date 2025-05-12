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

package org.qubership.atp.dataset.model.utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.utils.tree.TraverseAndLeafsHandler;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class DslAttrHandler implements TraverseAndLeafsHandler<DataSetList, Attribute, List<String>> {

    private final AttrDslHandler attrDsl = new AttrDslHandler();

    @Nonnull
    @Override
    public TraverseAndLeafsHandler<?, DataSetList, List<String>> backToPreviousParent() {
        attrDsl.attrPath.removeLast();
        return attrDsl;
    }

    @Nonnull
    @Override
    public TraverseAndLeafsHandler<Attribute, ?, List<String>> forwardToNewParent(@Nonnull DataSetList parent) {
        return attrDsl;
    }

    @Override
    public Iterator<? extends Attribute> getChildren(@Nonnull DataSetList item) {
        return item.getAttributes().iterator();
    }

    @Nullable
    @Override
    public List<String> constructLeaf(@Nonnull DataSetList leaf) {
        return null;
    }

    private class AttrDslHandler implements TraverseAndLeafsHandler<Attribute, DataSetList, List<String>> {

        final LinkedList<Attribute> attrPath = new LinkedList<>();

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<?, Attribute, List<String>> backToPreviousParent() {
            return DslAttrHandler.this;
        }

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<DataSetList, ?, List<String>> forwardToNewParent(@Nonnull Attribute parent) {
            attrPath.add(parent);
            return DslAttrHandler.this;
        }

        @Override
        public Iterator<? extends DataSetList> getChildren(@Nonnull Attribute item) {
            DataSetList ref = item.getDataSetListReference();
            return ref == null ? null : Iterators.singletonIterator(ref);
        }

        @Nullable
        @Override
        public List<String> constructLeaf(@Nonnull Attribute leaf) {
            List<String> path = Lists.newArrayListWithExpectedSize(this.attrPath.size() + 1);
            this.attrPath.stream().map(Named::getName).forEach(path::add);
            path.add(leaf.getName());
            return path;
        }
    }
}

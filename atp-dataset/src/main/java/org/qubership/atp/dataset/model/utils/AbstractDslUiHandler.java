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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.utils.tree.TraverseVisitor;

import com.google.common.collect.Iterators;

public abstract class AbstractDslUiHandler implements TraverseVisitor<DataSetList, Attribute> {

    private final AttrUiHandler attrH = new AttrUiHandler();
    private final boolean expandAll;

    public AbstractDslUiHandler(boolean expandAll) {
        this.expandAll = expandAll;
    }

    protected abstract void attributeStarts(Attribute attr);

    /**
     * Not invoked for leafs.
     */
    protected abstract void goForwardUnderAttribute();

    /**
     * Not invoked for leafs.
     */
    protected abstract void goBackFromAttribute();

    protected abstract void attributeEnds();

    @Nullable
    @Override
    public Iterator<? extends Attribute> getChildren(@Nonnull DataSetList item) {
        return item.getAttributes().iterator();
    }

    @Nonnull
    @Override
    public TraverseVisitor<?, DataSetList> backToPreviousParent() {
        return attrH;
    }

    @Nonnull
    @Override
    public TraverseVisitor<Attribute, ?> forwardToNewParent(@Nonnull DataSetList parent) {
        return attrH;
    }

    private class AttrUiHandler implements TraverseVisitor<Attribute, DataSetList> {

        @Override
        public void notifyItemStarts(Attribute item) {
            attributeStarts(item);
        }

        @Override
        public void notifyProcessingChildren() {
            goForwardUnderAttribute();
        }

        @Override
        public void notifyChildrenProcessed() {
            goBackFromAttribute();
        }

        @Override
        public void notifyItemEnds() {
            attributeEnds();
        }

        @Nonnull
        @Override
        public TraverseVisitor<?, Attribute> backToPreviousParent() {
            return AbstractDslUiHandler.this;
        }

        @Nonnull
        @Override
        public TraverseVisitor<DataSetList, ?> forwardToNewParent(@Nonnull Attribute parent) {
            return AbstractDslUiHandler.this;
        }

        @Nullable
        @Override
        public Iterator<? extends DataSetList> getChildren(@Nonnull Attribute item) {
            if (expandAll && item.getType() == AttributeType.DSL) {
                return item.getDataSetListReference() == null ? null
                        : Iterators.singletonIterator(item.getDataSetListReference());
            }
            return null;
        }
    }
}
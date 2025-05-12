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
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.utils.tree.TraverseVisitor;

import com.google.common.collect.Iterators;

public abstract class AbstractDsUiHandler implements TraverseVisitor<DataSet, Attribute> {
    private final DslVisitor dslH = new DslVisitor();
    private final AttrVisitor attrH = new AttrVisitor();
    protected DataSet curDs;
    protected DataSetList curDsl;
    protected Attribute curAttr;
    protected int dslDepth = 0;


    protected void attributeStarts(Attribute attr) {
        curAttr = attr;
    }

    /**
     * Not invoked for leafs.
     */
    protected void goForwardUnderAttribute() {

    }

    /**
     * Not invoked for leafs.
     */
    protected void goBackFromAttribute() {

    }

    protected void attributeEnds() {

    }

    @Nullable
    protected Iterator<? extends DataSetList> getChildren(@Nonnull Attribute item) {
        if (item.getType() == AttributeType.DSL) {
            return item.getDataSetListReference() == null ? null
                    : Iterators.singletonIterator(item.getDataSetListReference());
        }
        return null;
    }

    @Nullable
    protected Iterator<? extends Attribute> getChildren(@Nonnull DataSetList item) {
        return item.getAttributes().iterator();
    }

    @Nullable
    @Override
    public Iterator<? extends Attribute> getChildren(@Nonnull DataSet item) {
        return getChildren(item.getDataSetList());
    }

    protected void dslStarts(DataSetList dsl) {
        curDsl = dsl;
    }

    /**
     * Not invoked for leafs.
     */
    protected void goForwardUnderDsl() {
        dslDepth++;
    }

    /**
     * Not invoked for leafs.
     */
    protected void goBackFromDsl() {
        dslDepth--;
    }

    protected void dslEnds() {

    }

    @Override
    public void notifyItemStarts(DataSet item) {
        curDs = item;
        dslStarts(item.getDataSetList());
    }

    @Override
    public void notifyProcessingChildren() {
        goForwardUnderDsl();
    }

    @Override
    public void notifyChildrenProcessed() {
        goBackFromDsl();
    }

    @Override
    public void notifyItemEnds() {
        dslEnds();
    }

    @Nonnull
    @Override
    public TraverseVisitor<?, DataSet> backToPreviousParent() {
        throw new UnsupportedOperationException("Unsupported since data set is root and has no parent");
    }

    @Nonnull
    @Override
    public TraverseVisitor<Attribute, ?> forwardToNewParent(@Nonnull DataSet parent) {
        return dslH.forwardToNewParent(parent.getDataSetList());
    }

    private class DslVisitor implements TraverseVisitor<DataSetList, Attribute> {

        @Override
        public void notifyItemStarts(DataSetList item) {
            dslStarts(item);
        }

        @Override
        public void notifyProcessingChildren() {
            goForwardUnderDsl();
        }

        @Override
        public void notifyChildrenProcessed() {
            goBackFromDsl();
        }

        @Override
        public void notifyItemEnds() {
            dslEnds();
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

        @Nullable
        @Override
        public Iterator<? extends Attribute> getChildren(@Nonnull DataSetList item) {
            return AbstractDsUiHandler.this.getChildren(item);
        }
    }

    private class AttrVisitor implements TraverseVisitor<Attribute, DataSetList> {

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
            if (dslDepth == 0) {
                return AbstractDsUiHandler.this;
            }
            return dslH;
        }

        @Nonnull
        @Override
        public TraverseVisitor<DataSetList, ?> forwardToNewParent(@Nonnull Attribute parent) {
            return dslH;
        }

        @Nullable
        @Override
        public Iterator<? extends DataSetList> getChildren(@Nonnull Attribute item) {
            return AbstractDsUiHandler.this.getChildren(item);
        }
    }
}

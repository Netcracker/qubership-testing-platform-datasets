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

package org.qubership.atp.dataset.service.direct.macros.schange;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.LabelProvider;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.AbstractIdentified;
import org.qubership.atp.dataset.model.impl.file.FileData;


import com.google.common.base.MoreObjects;

public class TraverseMixinSupport {

    private static void setterInvoked() {
        throw new UnsupportedOperationException("It's readonly");
    }

    public DataSetList proxy(DataSetList of) {
        return new DataSetListDelegate(of);
    }

    public DataSet proxy(DataSet of) {
        return new DataSetDelegate(of);
    }

    public Attribute proxy(Attribute of) {
        return new AttributeDelegate(of);
    }

    public Parameter proxy(Parameter of) {
        return new ParameterDelegate(of);
    }

    protected List<Attribute> getAttributes(DataSetList source) {
        return source.getAttributes().stream().map(this::proxy).collect(Collectors.toList());
    }

    protected List<DataSet> getDataSets(DataSetList source) {
        return source.getDataSets().stream().map(this::proxy).collect(Collectors.toList());
    }

    protected List<Parameter> getParameters(DataSet source) {
        return source.getParameters().stream().map(this::proxy).collect(Collectors.toList());
    }

    protected List<Parameter> getParameters(Attribute source) {
        return source.getParameters().stream().map(this::proxy).collect(Collectors.toList());
    }

    protected DataSetList getDataSetList(DataSet source) {
        return proxy(source.getDataSetList());
    }

    protected DataSetList getDataSetList(Attribute source) {
        return proxy(source.getDataSetList());
    }

    protected DataSet getDataSet(Parameter source) {
        return proxy(source.getDataSet());
    }

    protected Attribute getAttribute(Parameter source) {
        return proxy(source.getAttribute());
    }

    private static class IdentifiedDelegate<T extends Identified> extends AbstractIdentified implements Identified {
        protected final T source;

        private IdentifiedDelegate(T source) {
            this.source = source;
        }

        @Override
        public UUID getId() {
            return source.getId();
        }

        @Override
        public void setId(UUID id) {
            source.setId(id);
        }

        @Nonnull
        @Override
        public Stream<Identified> getReferences() {
            throw new UnsupportedOperationException();
        }
    }

    private static class NamedDelegate<T extends Named & Identified> extends IdentifiedDelegate<T> implements Named {

        private NamedDelegate(T source) {
            super(source);
        }

        @Override
        public String getName() {
            return source.getName();
        }

        @Override
        public void setName(String name) {
            source.setName(name);
        }
    }

    private abstract static class LabeledDelegate<T extends LabelProvider & Named & Identified>
            extends NamedDelegate<T> implements LabelProvider {

        private LabeledDelegate(T source) {
            super(source);
        }

        @Override
        public List<Label> getLabels() {
            return source.getLabels();
        }

        @Override
        public void setLabels(List<Label> labels) {
            source.setLabels(labels);
        }
    }

    private class DataSetListDelegate extends LabeledDelegate<DataSetList> implements DataSetList {

        private DataSetListDelegate(DataSetList source) {
            super(source);
        }

        @Override
        public VisibilityArea getVisibilityArea() {
            return source.getVisibilityArea();
        }

        @Override
        public void setVisibilityArea(VisibilityArea visibilityArea) {
            setterInvoked();
        }

        @Override
        public List<DataSet> getDataSets() {
            return TraverseMixinSupport.this.getDataSets(source);
        }

        @Override
        public void setDataSets(List<DataSet> dataSets) {
            setterInvoked();
        }

        @Override
        public List<Attribute> getAttributes() {
            return TraverseMixinSupport.this.getAttributes(source);
        }

        @Override
        public void setAttributes(List<Attribute> attributes) {
            setterInvoked();
        }

        @Override
        public TestPlan getTestPlan() {
            return source.getTestPlan();
        }

        @Override
        public void setTestPlan(TestPlan testPlan) {
            source.setTestPlan(testPlan);
        }

        @Override
        public Timestamp getCreatedWhen() {
            return source.getCreatedWhen();
        }

        @Override
        public void setCreatedWhen(Timestamp createdWhen) {
            setterInvoked();
        }

        @Override
        public UUID getCreatedBy() {
            return source.getCreatedBy();
        }

        @Override
        public void setCreatedBy(UUID createdBy) {
            setterInvoked();
        }

        @Override
        public Timestamp getModifiedWhen() {
            return source.getModifiedWhen();
        }

        @Override
        public void setModifiedWhen(Timestamp modifiedWhen) {
            setterInvoked();
        }

        @Override
        public UUID getModifiedBy() {
            return source.getModifiedBy();
        }

        @Override
        public void setModifiedBy(UUID modifiedBy) {
            setterInvoked();
        }
    }

    private class DataSetDelegate extends LabeledDelegate<DataSet> implements DataSet {

        private DataSetDelegate(DataSet source) {
            super(source);
        }

        @Override
        public Boolean isLocked() {
            return source.isLocked();
        }

        @Override
        public void setLocked(Boolean isLock) {
            source.setLocked(isLock);
        }

        @Override
        public MixInId getMixInId() {
            return source.getMixInId();
        }

        @Override
        public void setMixInId(MixInId id) {
            source.setMixInId(id);
        }

        @Override
        public DataSetList getDataSetList() {
            return TraverseMixinSupport.this.getDataSetList(source);
        }

        @Override
        public void setDataSetList(DataSetList dataSetList) {
            setterInvoked();
        }

        @Override
        public List<Parameter> getParameters() {
            return TraverseMixinSupport.this.getParameters(source);
        }

        @Override
        public void setParameters(List<Parameter> parameters) {
            setterInvoked();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("source", source)
                    .add("id", id)
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getMixInId());
        }

        @Override
        public boolean equals(Object target) {
            return target != null
                    && DataSet.class.isAssignableFrom(target.getClass())
                    && Objects.equals(getMixInId(), DataSet.class.cast(target).getMixInId());
        }
    }

    private class AttributeDelegate extends NamedDelegate<Attribute> implements Attribute {

        private AttributeDelegate(Attribute source) {
            super(source);
        }

        @Override
        public DataSetList getDataSetList() {
            return TraverseMixinSupport.this.getDataSetList(source);
        }

        @Override
        public void setDataSetList(DataSetList dataSetList) {
            setterInvoked();
        }

        @Override
        public AttributeType getType() {
            return source.getType();
        }

        @Override
        public void setType(AttributeType type) {
            setterInvoked();
        }

        @Override
        public DataSetList getDataSetListReference() {
            return source.getDataSetListReference();
        }

        @Override
        public void setDataSetListReference(DataSetList list) {
            setterInvoked();
        }

        @Override
        public List<ListValue> getListValues() {
            return source.getListValues();
        }

        @Override
        public void setListValues(List<ListValue> listValues) {
            setterInvoked();
        }

        @Override
        public List<Parameter> getParameters() {
            return TraverseMixinSupport.this.getParameters(source);
        }

        @Override
        public void setParameters(List<Parameter> parameters) {
            setterInvoked();
        }
    }

    private class ParameterDelegate extends IdentifiedDelegate<Parameter> implements Parameter {

        private ParameterDelegate(Parameter source) {
            super(source);
        }

        @Nonnull
        @Override
        public Attribute getAttribute() {
            return TraverseMixinSupport.this.getAttribute(source);
        }

        @Override
        public void setAttribute(@Nonnull Attribute attribute) {
            setterInvoked();
        }

        @Nonnull
        @Override
        public DataSet getDataSet() {
            return TraverseMixinSupport.this.getDataSet(source);
        }

        @Override
        public void setDataSet(@Nonnull DataSet dataSet) {
            setterInvoked();
        }

        @Nullable
        @Override
        public String getText() {
            return source.getText();
        }

        @Override
        public void setText(@Nullable String stringValue) {
            setterInvoked();
        }

        @Nullable
        @Override
        public DataSet getDataSetReference() {
            return source.getDataSetReference();
        }

        @Override
        public void setDataSetReference(@Nullable DataSet ds) {
            setterInvoked();
        }

        @Nullable
        @Override
        public ListValue getListValue() {
            return source.getListValue();
        }

        @Override
        public void setListValue(@Nullable ListValue value) {
            setterInvoked();
        }

        @Override
        public FileData getFileData() {
            return source.getFileData();
        }

        @Override
        public void setFileData(FileData fileData) {
            source.setFileData(fileData);
        }
    }
}

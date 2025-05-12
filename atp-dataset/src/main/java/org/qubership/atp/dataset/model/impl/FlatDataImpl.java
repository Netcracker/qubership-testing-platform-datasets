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

package org.qubership.atp.dataset.model.impl;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.Parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class FlatDataImpl implements Consumer<Identified> {
    private Collection<DataSetList> dataSetListLists;
    private Collection<DataSet> dataSets;
    private Collection<Attribute> attributes;
    private Collection<Parameter> parameters;
    private Collection<Label> labels;

    public Collection<DataSetList> getDataSetLists() {
        return dataSetListLists;
    }

    public void setDataSetLists(Collection<DataSetList> dataSetListLists) {
        this.dataSetListLists = dataSetListLists;
    }

    public Collection<DataSet> getDataSets() {
        return dataSets;
    }

    public void setDataSets(Collection<DataSet> dataSets) {
        this.dataSets = dataSets;
    }

    public Collection<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<Attribute> attributes) {
        this.attributes = attributes;
    }

    public Collection<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Collection<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Collection<Label> getLabels() {
        return labels;
    }

    public void setLabels(Collection<Label> labels) {
        this.labels = labels;
    }

    @JsonIgnore
    @Override
    public void accept(@Nonnull Identified entry) {
        Preconditions.checkNotNull(entry, "[%s] could not contain nulls", this);
        Class<? extends Identified> clazz = entry.getClass();
        if (Label.class.isAssignableFrom(clazz)) {
            fillEntry((Label) entry, this::getLabels, this::setLabels);
        } else if (Parameter.class.isAssignableFrom(clazz)) {
            fillEntry((Parameter) entry, this::getParameters, this::setParameters);
        } else if (Attribute.class.isAssignableFrom(clazz)) {
            fillEntry((Attribute) entry, this::getAttributes, this::setAttributes);
        } else if (DataSet.class.isAssignableFrom(clazz)) {
            fillEntry((DataSet) entry, this::getDataSets, this::setDataSets);
        } else if (DataSetList.class.isAssignableFrom(clazz)) {
            fillEntry((DataSetList) entry, this::getDataSetLists, this::setDataSetLists);
        } else {
            throw new IllegalArgumentException(String.format("[%s] is not designed to accept [%s]", this, clazz));
        }
    }

    @JsonIgnore
    private <T> void fillEntry(T entry, Supplier<Collection<T>> getter, Consumer<Collection<T>> setter) {
        Collection<T> lst = getter.get();
        if (lst == null) {
            lst = Lists.newArrayList();
            setter.accept(lst);
        }
        lst.add(entry);
    }
}

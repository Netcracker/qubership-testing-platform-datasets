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

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;

public class AttributeImpl extends AbstractNamed implements Attribute {

    private DataSetList dataSetList;
    private AttributeType type;
    private DataSetList dataSetListReference;
    private List<Parameter> parameters;
    private List<ListValue> listValues;

    public AttributeImpl() {
    }

    /**
     * Just a bean.
     */
    public AttributeImpl(UUID id, String name, DataSetList dataSetList, AttributeType type,
                         DataSetList dataSetListReference, List<ListValue> listValues,
                         List<Parameter> parameters) {
        this.id = id;
        this.name = name;
        this.dataSetList = dataSetList;
        this.type = type;
        this.dataSetListReference = dataSetListReference;
        this.parameters = parameters;
        this.listValues = listValues;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        Stream<Identified> result = Stream.of(getDataSetList(), getDataSetListReference());
        if (getParameters() != null) {
            result = Stream.concat(result, getParameters().stream());
        }
        if (getListValues() != null) {
            result = Stream.concat(result, getListValues().stream());
        }
        return result;
    }

    @Override
    public DataSetList getDataSetList() {
        return dataSetList;
    }

    @Override
    public void setDataSetList(DataSetList dataSetList) {
        this.dataSetList = dataSetList;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    @Override
    public DataSetList getDataSetListReference() {
        return this.dataSetListReference;
    }

    @Override
    public void setDataSetListReference(DataSetList list) {
        this.dataSetListReference = list;
    }

    @Override
    public List<ListValue> getListValues() {
        return listValues;
    }

    @Override
    public void setListValues(List<ListValue> listValues) {
        this.listValues = listValues;
    }

    @Override
    public List<Parameter> getParameters() {
        return this.parameters;
    }

    @Override
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }
}

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

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;

public class DataSetListImpl extends AbstractCreatedModified implements DataSetList {

    private TestPlan testPlan;
    private VisibilityArea visibilityArea;
    private List<DataSet> dataSets;
    private List<Attribute> attributes;

    public DataSetListImpl() {
    }

    /**
     * Just a bean.
     */
    public DataSetListImpl(UUID id, VisibilityArea visibilityArea, String name,
                           List<DataSet> dataSets, List<Attribute> attributes, List<Label> labels,
                           TestPlan testPlan, UUID createdBy, Timestamp createdWhen,
                           UUID modifiedBy, Timestamp modifiedWhen) {
        this.id = id;
        this.visibilityArea = visibilityArea;
        this.name = name;
        this.dataSets = dataSets;
        this.attributes = attributes;
        this.labels = labels;
        this.testPlan = testPlan;
        this.createdBy = createdBy;
        this.createdWhen = createdWhen;
        this.modifiedBy = modifiedBy;
        this.modifiedWhen = modifiedWhen;
    }

    @Override
    public VisibilityArea getVisibilityArea() {
        return visibilityArea;
    }

    @Override
    public void setVisibilityArea(VisibilityArea visibilityArea) {
        this.visibilityArea = visibilityArea;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.concat(Stream.concat(getDataSets().stream(), getAttributes().stream()), getLabels().stream());
    }

    public List<DataSet> getDataSets() {
        return this.dataSets;
    }

    @Override
    public void setDataSets(List<DataSet> dataSets) {
        this.dataSets = dataSets;
    }

    @Override
    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    @Override
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public TestPlan getTestPlan() {
        return this.testPlan;
    }

    @Override
    public void setTestPlan(TestPlan testPlan) {
        this.testPlan = testPlan;
    }
}

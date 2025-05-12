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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.model.Parameter;

import com.google.common.base.MoreObjects;

public class DataSetImpl extends AbstractLabelProvider implements DataSet {

    private MixInId mixInId;
    private DataSetList dataSetList;
    private List<Parameter> parameters;
    private Boolean locked;

    public DataSetImpl() {
    }

    /**
     * Just a bean.
     */
    public DataSetImpl(MixInId id, String name,
                       DataSetList dataSetList,
                       List<Parameter> parameters,
                       List<Label> labels,
                       Boolean locked) {
        this.mixInId = id;
        this.name = name;
        this.dataSetList = dataSetList;
        this.parameters = parameters;
        this.labels = labels;
        this.locked = locked;
    }

    /**
     * Just a bean.
     */
    public DataSetImpl(UUID id, String name,
                       DataSetList dataSetList,
                       List<Parameter> parameters,
                       List<Label> labels,
                       Boolean locked) {
        this(new MixInIdImpl(id), name, dataSetList, parameters, labels, locked);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public UUID getId() {
        return getMixInId().getUuid();
    }

    @Override
    public void setId(UUID id) {
        getMixInId().setUuid(id);
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        Stream<Identified> result = Stream.of(getDataSetList());
        return Stream.concat(result, getParameters().stream());
    }

    @Override
    public Boolean isLocked() {
        return locked;
    }

    @Override
    public void setLocked(Boolean isLock) {
        this.locked = isLock;
    }

    @Override
    public MixInId getMixInId() {
        return mixInId;
    }

    @Override
    public void setMixInId(MixInId id) {
        this.mixInId = id;
    }

    @Override
    public DataSetList getDataSetList() {
        return this.dataSetList;
    }

    @Override
    public void setDataSetList(DataSetList dataSetList) {
        this.dataSetList = dataSetList;
    }

    @Override
    public List<Parameter> getParameters() {
        return this.parameters;
    }

    @Override
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMixInId());
    }

    @Override
    public boolean equals(Object target) {
        return target != null
                && DataSet.class.isAssignableFrom(target.getClass())
                && Objects.equals(mixInId, DataSet.class.cast(target).getMixInId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mixInId", mixInId)
                .add("name", name)
                .toString();
    }


}


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
import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Identified;

public class AttributePathImpl extends AbstractIdentified implements AttributePath {
    private DataSet dataSet;
    private Attribute target;
    private List<Attribute> path;

    /**
     * Attribute path, works like attr_key, but has more user friendly api.
     *
     * @param id      - attribute path id (aka attr_key.id)
     * @param dataSet - target dataset which has overridden parameter {@link DataSet}
     * @param target  - target attribute, which should be overrriden for {@code dataSet}
     * @param path    - to original parameter.
     */
    public AttributePathImpl(UUID id, DataSet dataSet, Attribute target, List<Attribute> path) {
        this.dataSet = dataSet;
        this.target = target;
        this.id = id;
        this.path = path;
    }

    @Override
    public DataSet getDataSet() {
        return dataSet;
    }

    @Override
    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public Attribute getTargetAttribute() {
        return target;
    }

    @Override
    public void setTargetAttribute(Attribute target) {
        this.target = target;
    }

    @Override
    public List<Attribute> getPath() {
        return path;
    }

    @Override
    public void setPath(List<Attribute> attributePath) {
        this.path = attributePath;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.empty();
    }
}

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

import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.VisibilityArea;

public class VisibilityAreaImpl extends AbstractNamed implements VisibilityArea {

    private List<DataSetList> dataSetLists;

    public VisibilityAreaImpl() {
    }

    /**
     * Just a bean.
     */
    public VisibilityAreaImpl(UUID id, String name, List<DataSetList> dataSetLists) {
        this.id = id;
        this.name = name;
        this.dataSetLists = dataSetLists;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.empty();
    }

    @Override
    public List<DataSetList> getDataSetLists() {
        return dataSetLists;
    }

    @Override
    public void setDataSetLists(List<DataSetList> dataSetLists) {
        this.dataSetLists = dataSetLists;
    }
}

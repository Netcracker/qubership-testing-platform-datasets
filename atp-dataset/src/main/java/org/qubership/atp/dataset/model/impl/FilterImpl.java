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

import org.qubership.atp.dataset.model.Filter;
import org.qubership.atp.dataset.model.Identified;

public class FilterImpl extends AbstractNamed implements Filter {
    private UUID dataSetList;
    private List<UUID> dslLabels;
    private List<UUID> dsLabels;

    public FilterImpl(UUID id, String name) {
        this.setName(name);
        this.setId(id);
    }

    @Override
    public UUID getVisibilityAreaId() {
        return this.dataSetList;
    }

    @Override
    public void setVisibilityAreaId(UUID dataSetList) {
        this.dataSetList = dataSetList;
    }

    @Override
    public List<UUID> getDataSetListLabels() {
        return this.dslLabels;
    }

    @Override
    public void setDataSetListLabels(List<UUID> dslLabels) {
        this.dslLabels = dslLabels;
    }

    @Override
    public List<UUID> getDataSetLabels() {
        return this.dsLabels;
    }

    @Override
    public void setDataSetLabels(List<UUID> dsLabels) {
        this.dsLabels = dsLabels;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.empty(); //Do not resolve references here.
    }
}

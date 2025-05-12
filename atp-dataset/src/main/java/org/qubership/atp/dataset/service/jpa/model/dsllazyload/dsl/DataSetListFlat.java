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

package org.qubership.atp.dataset.service.jpa.model.dsllazyload.dsl;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;

import lombok.Getter;
import lombok.Setter;

/**
 * Used for top level DSL representation.
 * */
@Getter
@Setter
public class DataSetListFlat {
    private UUID id;
    private String name;
    private List<DataSetFlat> dataSets = new LinkedList<>();
    private List<AttributeFlat> attributes = new LinkedList<>();

    /**
     * Default.
     * */
    public DataSetListFlat(DataSetList dataSetList, DataSetParameterProvider parameterProvider) {
        id = dataSetList.getId();
        name = dataSetList.getName();
        List<DataSet> dataSetsModels = dataSetList.getDataSets();
        List<UUID> dataSetIds = new LinkedList<>();
        for (DataSet dataSet : dataSetsModels) {
            dataSets.add(new DataSetFlat(dataSet));
            dataSetIds.add(dataSet.getId());
        }
        List<Attribute> attributesModels = dataSetList.getAttributes();
        for (Attribute attribute : attributesModels) {
            attributes.add(new AttributeFlat(attribute, dataSetIds, parameterProvider));
        }
    }
}

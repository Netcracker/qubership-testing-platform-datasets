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

package org.qubership.atp.dataset.service.direct.impl;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;

import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.AbstractTest;
import lombok.SneakyThrows;

public abstract class DataSetBuilder extends AbstractTest {

    Parameter source;
    UUID parameterId;
    DataSetList dataSetList;
    UUID vaId;
    UUID dsId1;
    UUID dsId2;
    Attribute attr;
    DataSet dataSet1;
    DataSet dataSet2;
    UUID dslId;

    @BeforeEach
    @SneakyThrows(DataSetExistsException.class)
    public void setUp() throws EvalException {
        vaId = visibilityAreaService.create("DataSetBuilder").getId();
        dslId = dataSetListService.create(vaId, "DSL", null).getId();
        dataSetList = dataSetListService.get(dslId);
        dsId1 = dataSetService.create(dslId, "DS").getId();
        dsId2 = dataSetService.create(dslId, "DS2").getId();
        dataSet1 = dataSetService.get(dsId1);
        dataSet2 = dataSetService.get(dsId2);
        attr = attributeService.create(dslId, 0, "ATTR", AttributeType.TEXT, null, null);
        parameterId = parameterService.create(dsId1, attr.getId(), "txt", null, null).getId();
        source = parameterService.get(parameterId);
    }
}

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

package org.qubership.atp.dataset.macros.impl;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.VisibilityAreaService;
import lombok.SneakyThrows;

@Isolated
@ContextConfiguration(classes = {TestMacrosConfiguration.class})
public class ReferenceDslAliasMacrosTest extends AbstractMacrosTest {

    @Autowired
    private DataSetListService dslService;
    @Autowired
    private DataSetService dataSetService;
    @Autowired
    private AttributeService attributeService;
    @Autowired
    private ParameterService parameterService;
    @Autowired
    private VisibilityAreaService visibilityAreaService;
    @Autowired
    private EvaluationService evaluationService;
    private DataSetList dataSetList;
    private DataSet dataSet;
    private UUID dsId;
    private UUID dslId;
    private UUID attributeId;
    private UUID vaId;

    @BeforeEach
    public void setUp() {
        dataSetList = createDSL();
    }

    @After
    public void tearDown() {
        visibilityAreaService.delete(vaId);
    }

    @SneakyThrows(DataSetExistsException.class)
    private DataSetList createDSL() {
        //region Objects creation
        vaId = visibilityAreaService.create("RefAliasTestVA").getId();
        dslId = dslService.create(vaId, "ExternalDSL", null).getId();
        DataSetList dsl = dslService.get(dslId);
        dataSet = dataSetService.create(dsl.getId(), "ExternalDS");
        dsId = dataSet.getId();
        Attribute attribute =
                attributeService.create(dsl.getId(), 0, "ExternalAttr", AttributeType.TEXT, null, null);
        attributeId = attribute.getId();
        parameterService.create(dsId, attribute.getId(), "Passed", null, null);
        return dsl;
    }

    @Test
    public void testCalcReferenceInItfSerialization() {
        UUID testVa = null;
        try {
            String macros = "#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)" +
                    "#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)#CHARS_UPPERCASE(1)";
            testVa = visibilityAreaService.create("TestVa").getId();
            Parameter targetParameter = createTargetDsl(testVa);
            Parameter parentParameter = createParentDsl(testVa, targetParameter.getDataSet());
            Parameter childParameter = createChildDsl(testVa, targetParameter.getDataSet());
            DataSetList parentDsl = parentParameter.getDataSet().getDataSetList();
            parameterService.set(
                    parentParameter.getDataSet().getId(),
                    targetParameter.getAttribute().getId(),
                    Collections.singletonList(
                            parentParameter.getAttribute().getId()
                    ),
                    macros,
                    null,
                    null);
            ObjectNode itfFormat = dataSetService.getInItfFormat(parentParameter.getDataSet().getMixInId());
            assertNotEquals("Default", itfFormat.get("Address").get("Postal Code").get("Second").asText());
            assertEquals(itfFormat.get("Address").get("Provice").asText(),
                    itfFormat.get("Address").get("Postal Code").get("Second").asText());
        } finally {
            if (testVa != null) {
                visibilityAreaService.delete(testVa);
            }
        }
    }

    @SneakyThrows(DataSetExistsException.class)
    private Parameter createChildDsl(UUID testVa, DataSet dataSet) {
        UUID testDsl = dslService.create(testVa, "Postal Code", null).getId();
        UUID testDS = dataSetService.create(testDsl, "PC #001").getId();
        Attribute child = attributeService
                .create(dataSet.getDataSetList().getId(), 1, "Postal Code", AttributeType.DSL, testDsl, null);
        parameterService.create(dataSet.getId(), child.getId(), null, null, testDS);
        Attribute attribute = attributeService.create(testDsl, 0, "Second", AttributeType.TEXT, null, null);
        return parameterService.create(testDS, attribute.getId(), "#REF_DSL(Customer.Customer 1.Address.Provice)", null, null);
    }

    @SneakyThrows(DataSetExistsException.class)
    private Parameter createParentDsl(UUID testVa, DataSet targetDs) {
        UUID testDsl = dslService.create(testVa, "Customer", null).getId();
        UUID testDS = dataSetService.create(testDsl, "Customer 1").getId();
        Attribute group = attributeService
                .create(testDsl, 0, "Address", AttributeType.DSL, targetDs.getDataSetList().getId(), null);
        return parameterService.create(testDS, group.getId(), null, null, targetDs.getId());
    }

    @SneakyThrows(DataSetExistsException.class)
    private Parameter createTargetDsl(UUID testVa) {
        UUID testDsl = dslService.create(testVa, "Address", null).getId();
        UUID testDS = dataSetService.create(testDsl, "ADR #001").getId();
        Attribute provice = attributeService
                .create(testDsl, 0, "Provice", AttributeType.TEXT, null, null);
        return parameterService
                .create(testDS, provice.getId(), "Default", null, null);
    }
}

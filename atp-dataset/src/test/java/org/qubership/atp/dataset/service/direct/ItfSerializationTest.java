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

package org.qubership.atp.dataset.service.direct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.AbstractTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class ItfSerializationTest extends AbstractTest {

    @Autowired
    ObjectMapper mapper;

    private VisibilityArea va;
    private DataSet parentWithOverlap;
    private DataSet parentWithoutOverlap;
    private DataSet child1;
    private Attribute childAttr;
    private Parameter parentChild1;

    @BeforeEach
    public void setUp() {
        va = factory.va("ItfSerializationTest");
        child1 = factory.ds(va, "Child", "Child1");
        childAttr = factory.textAttr(child1.getDataSetList(), "Without parameter");
        //no parameter assigned for childAttr
        parentWithOverlap = factory.ds(va, "Parent", "parentWithOverlap");
        parentWithoutOverlap = factory.ds(parentWithOverlap.getDataSetList(), "parentWithoutOverlap");
        parentChild1 = factory.refParam(parentWithOverlap, "Child", child1);
        factory.refParam(parentWithoutOverlap, parentChild1.getAttribute(), child1);
        factory.overrideParam(parentWithOverlap, childAttr, "Overlap", null, null, null, parentChild1.getAttribute());
    }

    @AfterEach
    public void destroy() {
        visibilityAreaService.delete(va.getId());
    }

    @Test
    public void serializeDataSet_ParameterOverlapHasNoDefaultParameter_OverlappedParameterHandled() throws IOException {
        String expectedJson = "{\"Child\":{\"Without parameter\":\"Overlap\"}}";
        //validate
        JsonNode expected = mapper.readTree(expectedJson);
        ObjectNode actual = dataSetService.getInItfFormat(parentWithOverlap.getMixInId());//should be no exception
        assertEquals(expected, actual);
    }

    @Test
    public void targetParameterSerializedWhenItIsNotInitialized() throws IOException {
        String expectedJson = "{\"Child\":{\"Without parameter\":\"\"}}";
        //validate
        JsonNode expected = mapper.readTree(expectedJson);
        ObjectNode actual = dataSetService.getInItfFormat(parentWithoutOverlap.getMixInId());//should be no exception
        assertEquals(expected, actual);
    }

    @Test
    public void overlapSerializedWhenItIsRecreatedAsListValue() throws IOException {
        attributeService.delete(childAttr.getId());
        childAttr = factory.listAttr(child1.getDataSetList(), "Without parameter", "", "Overlap");
        //no parameter assigned for childAttr
        factory.overrideParam(parentWithOverlap, childAttr, null, "Overlap", null, null, parentChild1.getAttribute());
        String expectedJson = "{\"Child\":{\"Without parameter\":\"Overlap\"}}";
        //validate
        JsonNode expected = mapper.readTree(expectedJson);
        ObjectNode actual = dataSetService.getInItfFormat(parentWithOverlap.getMixInId());//should be no exception
        assertEquals(expected, actual);
    }

    @Test
    public void targetParameterSerializedWhenItIsRecreatedAsListValue() throws IOException {
        attributeService.delete(childAttr.getId());
        childAttr = factory.listAttr(child1.getDataSetList(), "Without parameter", "", "Overlap");
        //no parameter assigned for childAttr
        String expectedJson = "{\"Child\":{\"Without parameter\":\"\"}}";
        //validate
        JsonNode expected = mapper.readTree(expectedJson);
        ObjectNode actual = dataSetService.getInItfFormat(parentWithOverlap.getMixInId());//should be no exception
        assertEquals(expected, actual);
    }

    @Disabled
    @Test
    public void targetDslParameterSerializedWhenItIsNotInitialized() throws IOException {
        Attribute link_to_ds = factory.refAttr(parentWithoutOverlap.getDataSetList(), "Link To DS",
                child1.getDataSetList());
        factory.refParam(parentWithoutOverlap, link_to_ds, null);
        String expectedJson = "{\"Child\":{\"Without parameter\":\"Overlap\"}, \"Link To DS\": \"\"}";
        //validate
        JsonNode expected = mapper.readTree(expectedJson);
        ObjectNode actual = dataSetService.getInItfFormat(parentWithOverlap.getMixInId());//should be no exception
        assertEquals(expected, actual);
    }

    @Test
    public void itfIntegration_calculateReferencesFormulas_valueIsTheSameAsReferencedValue() {
        final DataSet[] rootDs_1 = new DataSet[1];
        VisibilityArea va = super.createTestData(f -> {
            VisibilityArea resultVa = f.va("ATPII-1508");
            DataSetList dslRoot = f.dsl(resultVa, "root");
            rootDs_1[0] = f.ds(dslRoot, "ds");
            DataSetList dls_child_1 = f.dsl(resultVa, "dsl_child_1");
            DataSet ds_child_1 = f.ds(dls_child_1, "ds_child_1");
            DataSetList dls_child_2 = f.dsl(resultVa, "dsl_child_2");
            DataSet ds_child_2 = f.ds(dls_child_2, "ds_child_2");
            f.textParam(ds_child_2, "random_text", "#RANDOMBETWEEN(10, 100)");
            f.refParam(rootDs_1[0], "ref_to_ds_child_1", ds_child_1);
            f.refParam(rootDs_1[0], "ref_to_ds_child_2", ds_child_2);
            f.textParam(ds_child_1, "reference_to_random_text_from_child_1",
                    "#REF_DSL(dsl_child_2.ds_child_2.random_text)");
            f.textParam(rootDs_1[0], "reference_to_random_text",
                    "#REF_DSL(dsl_child_2.ds_child_2.random_text)");
            return resultVa;
        });
        ObjectNode result = dataSetService.getInItfFormat(rootDs_1[0].getMixInId());
        assertNotEquals(result.get("ref_to_ds_child_1").get("reference_to_random_text_from_child_1").textValue(),
                result.get("ref_to_ds_child_2").get("random_text").textValue());
        assertNotEquals(result.get("reference_to_random_text").textValue(),
                result.get("ref_to_ds_child_2").get("random_text").textValue());
    }
}

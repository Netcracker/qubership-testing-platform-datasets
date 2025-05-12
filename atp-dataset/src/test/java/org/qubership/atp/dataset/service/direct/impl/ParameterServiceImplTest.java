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



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.ImmutableList;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.service.AbstractTest;
import lombok.SneakyThrows;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class ParameterServiceImplTest extends AbstractTest {

    private VisibilityArea visibilityArea;
    DataSetList dsl1;
    DataSetList dsl2;
    private DataSet ds_11;
    private DataSet ds_21;
    private Attribute attr11;
    private Attribute attr12;
    private Attribute attr13;
    private Attribute attr21;
    private Attribute attr22;
    Parameter param11;
    Parameter param12;
    Parameter param21;
    Parameter param22;
    String originalTextValue;
    UUID originalListValue;
    UUID newListValue;

    @BeforeEach
    @SneakyThrows(DataSetExistsException.class)
    public void setUp() throws Exception {
        visibilityArea = visibilityAreaService.create("TestVA");
        dsl1 = dataSetListService.create(visibilityArea.getId(), "Dsl1", null);
        ds_11 = dataSetService.create(dsl1.getId(), "Ds11");
        attr11 = attributeService.create(dsl1.getId(), 0, "Attr11", AttributeType.TEXT, null, null);
        attr12 = attributeService.create(dsl1.getId(), 1, "Attr12", AttributeType.LIST, null, Arrays.asList("list1", "list2"));
        attr13 = attributeService.create(dsl1.getId(), 2, "Attr13", AttributeType.TEXT, null, null);
        originalTextValue = "textOriginalValue";
        originalListValue = attr12.getListValues().get(0).getId();
        newListValue = attr12.getListValues().get(1).getId();
        param11 = parameterService.create(ds_11.getId(), attr11.getId(), originalTextValue, null, null);
        param12 = parameterService.create(ds_11.getId(), attr12.getId(), null, originalListValue, null);

        dsl2 = dataSetListService.create(visibilityArea.getId(), "Dsl2", null);
        ds_21 = dataSetService.create(dsl2.getId(), "Ds21");
        attr21 = attributeService.create(dsl2.getId(), 0, "Attr21", AttributeType.DSL, dsl1.getId(), null);
        attr22 = attributeService.create(dsl2.getId(), 1, "Attr22", AttributeType.DSL, dsl1.getId(), null);
        param21 = parameterService.create(ds_21.getId(), attr21.getId(), null, null, ds_11.getId());
        param22 = parameterService.create(ds_21.getId(), attr22.getId(), null, null, ds_11.getId());
    }

    @Test
    public void deleteParameterOverlap_createAndRevertTextOverlap_overlappedTextAttributeShouldChangeAndRestoreValue() {
        String textOverlap = "overlappedTextValue";
        parameterService.set(ds_21.getId(), attr11.getId(), ImmutableList.of(attr21.getId()), textOverlap, null, null);
        ds_21 = dataSetService.get(ds_21.getId());
        Parameter actualTextValue = OverlapIterator.create(ds_21, attr11.getId(), ImmutableList.of(attr21.getId())).next().getParameter().get();

        assertEquals(textOverlap, actualTextValue.getText());

        actualTextValue = dataSetService.deleteParameterOverlap(ds_21.getId(), attr11.getId(), ImmutableList.of(attr21.getId()));

        assertEquals(originalTextValue, actualTextValue.getText());
    }

    @Test
    public void deleteParameterOverlap_createAndRevertListOverlap_overlappedListAttributeShouldChangeAndRestoreValue() {
        parameterService.set(ds_21.getId(), attr12.getId(), ImmutableList.of(attr22.getId()), null, null, newListValue);
        ds_21 = dataSetService.get(ds_21.getId());
        Parameter actualListValue = OverlapIterator.create(ds_21, attr12.getId(), ImmutableList.of(attr22.getId())).next().getParameter().get();

        assertEquals(newListValue, actualListValue.getListValue().getId());

        actualListValue = dataSetService.deleteParameterOverlap(ds_21.getId(), attr12.getId(), ImmutableList.of(attr22.getId()));

        assertEquals(originalListValue, actualListValue.getListValue().getId());
    }

    @Test
    public void set_editParentTextParameter_childReferenceParameterShouldChanged() throws Exception {
        Parameter textParameter = parameterService.get(param21.getId());

        assertEquals(1L, textParameter.getDataSetReference().getParameters().stream().filter(
                parameter -> Objects.nonNull(parameter.getText()) && originalTextValue.equals(parameter.getText()))
                .count());

        String newTextValue = "newTextValue";
        parameterService.set(ds_11.getId(), attr11.getId(), newTextValue, null);
        textParameter = parameterService.get(param21.getId());

        assertEquals(1L, textParameter.getDataSetReference().getParameters().stream().filter(
                parameter -> Objects.nonNull(parameter.getText()) && newTextValue.equals(parameter.getText()))
                .count());
    }

    @Test
    public void set_editParentListParameter_childReferenceParameterShouldChanged() throws Exception {
        Parameter refParameter = parameterService.get(param22.getId());

        assertEquals(1L, refParameter.getDataSetReference().getParameters().stream().filter(
                parameter -> Objects.nonNull(parameter.getListValue()) && originalListValue.equals(parameter.getListValue().getId()))
                .count());

        parameterService.set(ds_11.getId(), attr12.getId(), null, null, null, newListValue);
        refParameter = parameterService.get(param22.getId());

        assertEquals(1L, refParameter.getDataSetReference().getParameters().stream().filter(
                parameter -> Objects.nonNull(parameter.getListValue()) && newListValue.equals(parameter.getListValue().getId()))
                .count());

    }

    @Test
    public void deleteListValues_deleteLastListValueInParentListParameter_listValueInChildParameterHasEmptyValue() {
        Parameter refParameter = parameterService.get(param22.getId());

        assertEquals(1L, refParameter.getDataSetReference().getParameters().stream().filter(
                        parameter -> Objects.nonNull(parameter.getListValue()) && originalListValue.equals(parameter.getListValue().getId())).count());

        attributeService.deleteListValues(attr12.getId(), Arrays.asList(originalListValue, newListValue));
        refParameter = parameterService.get(param22.getId());

        assertEquals(0L, refParameter.getDataSetReference().getParameters().stream().filter(
                        parameter -> Objects.nonNull(parameter.getListValue()) && originalListValue.equals(parameter.getListValue().getId())).count());
    }

    @Test
    public void set_deleteOverlapListValueInChildParameter_overlapParameterShouldHasEmptyValue() {
        Parameter parameter = parameterService.set(ds_21.getId(), attr12.getId(), ImmutableList.of(attr22.getId()), null, null, originalListValue);
        ds_21 = dataSetService.get(ds_21.getId());
        Parameter actualListValue = OverlapIterator.create(ds_21, attr12.getId(), ImmutableList.of(attr22.getId())).next().getParameter().get();

        assertEquals(originalListValue, actualListValue.getListValue().getId());

        parameter = parameterService.set(ds_21.getId(), attr12.getId(), ImmutableList.of(attr22.getId()), null, null,
                null);

        Assertions.assertNull(parameter.getListValue());
    }

    @Test
    public void testParameterIsCreatedByServiceAndHasValue() {
        Parameter simpleText = parameterService.create(ds_11.getId(), attr13.getId(), "SimpleText", null, null);
        Parameter parameter = parameterService.get(simpleText.getId());

        assertNotNull(parameter);

        parameterService.delete(parameter);
    }

    @Test
    public void testGetOrCreateParameterOverlap() {
        Parameter parameter = parameterService.getOrCreateOverlap(ds_11.getId(), attr11.getId(), Collections.singletonList(attr11.getId()));

        assertNotNull(parameter);
    }

    @Test
    public void set_setMultiplyParameterValue_newValueOfParameterInDB() {
        String value = "MULTIPLY " + UUID.randomUUID();

        Parameter parameter = parameterService.set(ds_11.getId(), attr11.getId(), null, value, null, null);

        assertEquals(value, parameter.getText());
    }

    @Test
    public void set_changeMultiplyParameterValue_newValueOfParameterInDB() {
        String value = "MULTIPLY " + UUID.randomUUID();
        parameterService.set(ds_11.getId(), attr11.getId(), null, value, null, null);
        value = "MULTIPLY " + UUID.randomUUID() + " " + UUID.randomUUID();

        Parameter parameter = parameterService.set(ds_11.getId(), attr11.getId(), null, value, null, null);

        assertEquals(value, parameter.getText());
    }

    @AfterEach
    public void tearDown() throws Exception {
        visibilityAreaService.delete(visibilityArea.getId());
    }
}

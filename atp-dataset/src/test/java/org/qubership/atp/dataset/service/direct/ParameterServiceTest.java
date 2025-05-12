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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.ImmutableList;
import org.qubership.atp.dataset.TestUtils;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.service.AbstractTest;
import lombok.SneakyThrows;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class ParameterServiceTest extends AbstractTest {

    private UUID visibilityAreaId;
    private UUID dataSetListId;
    private List<String> listValues = new ArrayList<>(Arrays.asList("ListValue1", "ListValue2"));
    private List<String> dataSetNames = new ArrayList<>(Arrays.asList("DataSet1", "DataSet2"));
    private List<String> parameterTextValues = new ArrayList<>(Arrays.asList("ParameterValue1", "ParameterValue2"));

    @BeforeEach
    public void setUp() {
        this.visibilityAreaId = visibilityAreaService.create("ParameterServiceTestVa").getId();
        this.dataSetListId = dataSetListService.create(
                this.visibilityAreaId, "ParameterServiceTestDsl1", null
        ).getId();
    }

    @AfterEach
    public void tearDown() {
        visibilityAreaService.delete(this.visibilityAreaId);
    }

    /**
     * Create parameter tests
     */
    @Test
    public void createTextParameter_withAllParameterValues_onlyTextNotNull() {
        String expectedTextValue = parameterTextValues.get(0);
        UUID parameterId = createParameterWithDataSetAnAllValues(
                AttributeType.TEXT,
                dataSetNames.get(0),
                expectedTextValue,
                null,
                null
        );
        verifyParameterTextValue(parameterId, expectedTextValue);
    }

    @Test
    public void createListParameter_withAllParameterValues_onlyListNotNull() {
        String expectedListValueName = listValues.get(0);
        UUID parameterId = createParameterWithDataSetAnAllValues(
                AttributeType.LIST,
                dataSetNames.get(0),
                null,
                expectedListValueName,
                null
        );
        verifyParameterListValue(parameterId, expectedListValueName);
    }

    @Test
    public void createReferenceParameter_withAllParameterValues_onlyReferenceNotNull() {
        String expectedReferencedDataSetName = dataSetNames.get(1);
        UUID parameterId = createParameterWithDataSetAnAllValues(
                AttributeType.DSL,
                dataSetNames.get(0),
                null,
                null,
                expectedReferencedDataSetName
        );
        verifyParameterReferenceValue(parameterId, expectedReferencedDataSetName);
    }

    /**
     * Update parameter test
     */
    @Test
    public void updateTextParameter_withAllParameterValues_onlyTextNotNull() {
        UUID parameterId = createParameterWithDataSetAnAllValues(
                AttributeType.TEXT,
                dataSetNames.get(0),
                parameterTextValues.get(0),
                null,
                null
        );
        String updatedTextValue = parameterTextValues.get(1);
        parameterService.update(parameterId, updatedTextValue);
        verifyParameterTextValue(parameterId, updatedTextValue);
    }

    /**
     * Overlap test
     */
    @Disabled
    @Test
    public void overlapParameter_withTwoDSLs_overlapIsComputedProperly() throws DuplicateKeyException {
        //Postal Code DSL+DS+Parameter
        UUID postalCodeDataSetListId = dataSetListService.create(visibilityAreaId, "PostalCode", null).getId();
        DataSet postalCodeDataSet = dataSetService.create(postalCodeDataSetListId, "PC#1");
        Attribute codeAttribute =
                attributeService.create(postalCodeDataSetListId, 0, "Code", AttributeType.TEXT, null, null);
        String codeOriginalValue = "OriginalValue";
        parameterService.create(postalCodeDataSet.getId(), codeAttribute.getId(), codeOriginalValue, null, null);

        //Address DSL+DS+Parameter, overlaps parameter from Postal Code
        UUID addressDataSetList = dataSetListService.create(visibilityAreaId, "Address", null).getId();
        DataSet addressDataSet1 = dataSetService.create(addressDataSetList, "ADR#1");
        Attribute postalCodeReferenceAttribute = attributeService
                .create(addressDataSetList, 0, "PostalCodeRef", AttributeType.DSL, postalCodeDataSetListId, null);
        parameterService.create(addressDataSet1.getId(), postalCodeReferenceAttribute.getId(), null, null,
                postalCodeDataSet.getId());
        String codeOverlappedByAddressValue = "Overlapped by address";
        parameterService.set(
                addressDataSet1.getId(),
                codeAttribute.getId(),
                ImmutableList.of(postalCodeReferenceAttribute.getId()),
                codeOverlappedByAddressValue, null, null
        );

        //Customer DSL+DS+Parameter, overlaps parameter from Address Code
        UUID customerDataSetList = dataSetListService.create(visibilityAreaId, "Customer", null).getId();
        DataSet customerDataSet = dataSetService.create(customerDataSetList, "Customer1");
        Attribute addressReferenceAttribute = attributeService
                .create(customerDataSetList, 0, "AddressRef", AttributeType.DSL, addressDataSetList, null);
        parameterService.create(customerDataSet.getId(), addressReferenceAttribute.getId(), null, null,
                addressDataSet1.getId());
        String codeOverlappedByCustomerValue = "Overlapped by customer";
        parameterService.set(
                customerDataSet.getId(),
                codeAttribute.getId(),
                ImmutableList.of(
                        addressReferenceAttribute.getId(),
                        postalCodeReferenceAttribute.getId()
                ),
                codeOverlappedByCustomerValue, null, null
        );

        //Check customer overlaps parameter properly
        Parameter actualCode = OverlapIterator.create(
                customerDataSet,
                codeAttribute.getId(),
                ImmutableList.of(
                        addressReferenceAttribute.getId(),
                        postalCodeReferenceAttribute.getId()
                )).next().getParameter().get();
        Assertions.assertEquals(codeOverlappedByCustomerValue, actualCode.getText());
        parameterService.delete(actualCode);

        //Check customer don't overlap parameter anymore and see address overlapped value
        customerDataSet = dataSetService.get(customerDataSet.getId());
        actualCode = OverlapIterator.create(
                customerDataSet, codeAttribute.getId(),
                ImmutableList.of(addressReferenceAttribute.getId(), postalCodeReferenceAttribute.getId())
        ).next().getParameter().get();
        Assertions.assertEquals(codeOverlappedByAddressValue, actualCode.getText());

        //Check address don't overlap parameter anymore and see original value
        actualCode = dataSetService.deleteParameterOverlap(
                addressDataSet1.getId(),
                codeAttribute.getId(),
                ImmutableList.of(
                        postalCodeReferenceAttribute.getId()
                )
        );
        Assertions.assertEquals(codeOriginalValue, actualCode.getText());
    }

//    @Test(expected = IllegalArgumentException.class)
    @Test
    public void setValue_updateParameterWithLockDataSet_notUpdateParameterThrowException() throws DuplicateKeyException {
        //Postal Code DSL+DS+Parameter
        UUID postalCodeDataSetListId = dataSetListService.create(visibilityAreaId, "PostalCode", null).getId();
        DataSet postalCodeDataSet = dataSetService.create(postalCodeDataSetListId, "PC#1");
        Attribute codeAttribute =
                attributeService.create(postalCodeDataSetListId, 0, "Code", AttributeType.TEXT, null, null);
        String codeOriginalValue = "OriginalValue";
        parameterService.create(postalCodeDataSet.getId(), codeAttribute.getId(), codeOriginalValue, null, null);
        String codeNewValue = "newValue";
        //Setting the dataset lock = true
        dataSetService.lock(postalCodeDataSetListId, Collections.singletonList(postalCodeDataSet.getId()), true);
        assertThrows(IllegalArgumentException.class, ()-> {
            parameterService.set(
                    postalCodeDataSet.getId(),
                    codeAttribute.getId(),
                    Collections.emptyList(),
                    codeNewValue, null, null
            );
        });

    }

    /**
     * Collect parameters by list value tests
     */
    @Test
    public void checkGetAffectedParametersByListValue_twoDataSetsInList_returnTwoParams() throws DuplicateKeyException {
        UUID dataSetList = dataSetListService.create(visibilityAreaId, "dsl", null).getId();
        DataSet dataSet1 = dataSetService.create(dataSetList, "ds1");
        DataSet dataSet2 = dataSetService.create(dataSetList, "ds2");
        Attribute listAttr = attributeService.create(
                dataSetList, 0, "lvAttr", AttributeType.LIST, null, Arrays.asList("Value1", "Value2")
        );
        ListValue listValueToDelete = listAttr.getListValues().get(0);

        Parameter param1 = parameterService.create(
                dataSet1.getId(), listAttr.getId(), null, listValueToDelete.getId(), null
        );
        Parameter param2 = parameterService.create(
                dataSet2.getId(), listAttr.getId(), null, listValueToDelete.getId(), null
        );

        List<?> result = parameterService.getParametersAffectedByListValue(listValueToDelete.getId(), true);
        Assertions.assertEquals(2, result.size());

        Assertions.assertTrue(result.get(0) instanceof TableResponse);
        List<TableResponse> possibleResponse = (List<TableResponse>) result;

        List<UUID> ids = possibleResponse.stream().map(TableResponse::getId).collect(Collectors.toList());
        assertThat("Contains in any order", Arrays.asList(param1.getId(), param2.getId()),
                containsInAnyOrder(ids.toArray()));
    }

    @Test
    public void checkGetAffectedParametersByListValues_twoAndOneParamsAndThreeParams_returnThreeParams() throws DuplicateKeyException {
        UUID dataSetListId = dataSetListService.create(visibilityAreaId, "dsl", null).getId();
        DataSet dataSet1 = dataSetService.create(dataSetListId, "ds1");
        DataSet dataSet2 = dataSetService.create(dataSetListId, "ds2");
        DataSet dataSet3 = dataSetService.create(dataSetListId, "ds3");
        Attribute listAttr = attributeService.create(
                dataSetListId, 0, "lvAttr", AttributeType.LIST, null, Arrays.asList("Value1", "Value2", "Value3")
        );
        ListValue listValueToDelete1 = listAttr.getListValues().get(0);
        ListValue listValueToDelete2 = listAttr.getListValues().get(2);
        Parameter param1 = parameterService.create(
                dataSet1.getId(), listAttr.getId(), null, listValueToDelete1.getId(), null
        );
        Parameter param2 = parameterService.create(
                dataSet2.getId(), listAttr.getId(), null, listValueToDelete1.getId(), null
        );
        Parameter param3 = parameterService.create(
                dataSet3.getId(), listAttr.getId(), null, listValueToDelete2.getId(), null
        );

        List<?> result = parameterService.getParametersAffectedByListValues(
                Arrays.asList(listValueToDelete1.getId(), listValueToDelete2.getId())
        );

        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.get(0) instanceof TableResponse);
        List<TableResponse> possibleResponse = (List<TableResponse>) result;

        List<UUID> ids = possibleResponse.stream().map(TableResponse::getId).collect(Collectors.toList());
        assertThat("Contains in any order", Arrays.asList(param1.getId(), param2.getId(), param3.getId()),
                containsInAnyOrder(ids.toArray()));
    }

    /**
     * Bulk edit tests
     */
    @Test
    public void bulkUpdateParametersListValue_twoParams_parametersUpdatedSuccessfully() throws DuplicateKeyException {
        UUID dataSetListId = dataSetListService.create(visibilityAreaId, "dsl", null).getId();
        DataSet dataSet1 = dataSetService.create(dataSetListId, "ds1");
        DataSet dataSet2 = dataSetService.create(dataSetListId, "ds2");
        Attribute listAttr = attributeService
                .create(dataSetListId, 0, "lvAttr", AttributeType.LIST,
                        null, Arrays.asList("123", "234"));
        ListValue listValueOriginal = listAttr.getListValues().get(0);
        ListValue listValueChanged = listAttr.getListValues().get(1);
        Parameter param1 = parameterService.create(
                dataSet1.getId(), listAttr.getId(), null, listValueOriginal.getId(), null
        );
        Parameter param2 = parameterService.create(
                dataSet2.getId(), listAttr.getId(), null, listValueOriginal.getId(), null
        );

        parameterService.bulkUpdateValue(null, null, listValueChanged.getId(),
                Arrays.asList(param1.getId(), param2.getId())
        );

        Parameter updatedParam1 = parameterService.get(param1.getId());
        Parameter updatedParam2 = parameterService.get(param2.getId());
        Assertions.assertEquals(listValueChanged.getId(), updatedParam1.getListValue().getId());
        Assertions.assertEquals(listValueChanged.getId(), updatedParam2.getListValue().getId());
    }

    @Test
    public void bulkUpdateParametersReferences_twoParameters_parametersUpdatedSuccessfully() throws DuplicateKeyException {
        UUID dataSetList1Id = dataSetListService.create(visibilityAreaId, "dsl1", null).getId();
        UUID dataSetList2Id = dataSetListService.create(visibilityAreaId, "dsl2", null).getId();
        DataSet dataSet11 = dataSetService.create(dataSetList1Id, "ds11");
        DataSet dataSet12 = dataSetService.create(dataSetList1Id, "ds12");
        DataSet dataSet21 = dataSetService.create(dataSetList2Id, "ds21");
        DataSet dataSet22 = dataSetService.create(dataSetList2Id, "ds22");
        DataSet dataSet23 = dataSetService.create(dataSetList2Id, "ds23");

        Attribute dsRefAttr = attributeService.create(
                dataSetList1Id, 0, "dsAttr", AttributeType.DSL, dataSetList2Id, null);

        Parameter param1 = parameterService.create(
                dataSet11.getId(), dsRefAttr.getId(), null, null, dataSet21.getId()
        );

        Parameter param2 = parameterService.create(
                dataSet12.getId(), dsRefAttr.getId(), null, null, dataSet22.getId()
        );

        parameterService.bulkUpdateValue(
                (String) null, dataSet23.getId(), null, Arrays.asList(param1.getId(), param2.getId())
        );

        Parameter updatedParam1 = parameterService.get(param1.getId());
        Parameter updatedParam2 = parameterService.get(param2.getId());
        Assertions.assertEquals(dataSet23.getId(), updatedParam1.getDataSetReference().getId());
        Assertions.assertEquals(dataSet23.getId(), updatedParam2.getDataSetReference().getId());
    }

    @Test
    public void bulkUpdate_dataSet2Locked_param1UpdatedAndParam2Skipped() throws DuplicateKeyException {
        //Postal Code DSL+DS+Parameter
        dataSetListId = dataSetListService.create(visibilityAreaId, "PostalCode", null).getId();
        DataSet dataSet1 = dataSetService.create(dataSetListId, "PC#1");
        DataSet dataSet2 = dataSetService.create(dataSetListId, "PC#2");
        Attribute attribute =
                attributeService.create(dataSetListId, 0, "Code", AttributeType.TEXT, null, null);
        String codeOriginalValue1 = "OriginalValue1";
        String codeOriginalValue2 = "OriginalValue2";
        Parameter param1 = parameterService.create(dataSet1.getId(), attribute.getId(),
                codeOriginalValue1, null, null);
        Parameter param2 = parameterService.create(dataSet2.getId(), attribute.getId(),
                codeOriginalValue2, null, null);
        String updateValue = "updateValue";
        //Setting the dataset lock = true
        dataSetService.lock(dataSetListId, Collections.singletonList(dataSet2.getId()), true);
        List<UUID> dataSetIds = new ArrayList<>();
        dataSetIds.add(dataSet1.getId());
        dataSetIds.add(dataSet2.getId());

        parameterService.bulkUpdate(dataSetListId, Collections.emptyList(), dataSetIds,
                attribute.getId(),updateValue, null);

        Assertions.assertEquals(updateValue, parameterService.get(param1.getId()).getText());
        Assertions.assertEquals(codeOriginalValue2, parameterService.get(param2.getId()).getText());
    }

        private void verifyParameterTextValue(UUID paramId, String expectedTextValue) {
        Parameter parameter = validateAndGetParameter(paramId);
        Assertions.assertEquals(expectedTextValue, parameter.getText());
    }

    private void verifyParameterListValue(UUID paramId, String listValueName) {
        Parameter parameter = validateAndGetParameter(paramId);
        ListValue listValue = parameter.getListValue();
        Assertions.assertNotNull(listValue);
        Assertions.assertEquals(listValueName, listValue.getName());
    }

    private void verifyParameterReferenceValue(UUID paramId, String referencedDataSetExpectedName) {
        Parameter parameter = validateAndGetParameter(paramId);
        DataSet dataSetReference = parameter.getDataSetReference();
        Assertions.assertNotNull(dataSetReference);
        Assertions.assertEquals(referencedDataSetExpectedName, dataSetReference.getName());
    }

    private Parameter validateAndGetParameter(UUID paramId) {
        Assertions.assertNotNull(paramId);
        Parameter parameter = parameterService.get(paramId);
        Assertions.assertNotNull(parameter);
        boolean hasOnlyText = TestUtils
                .onlyOneIsNotNull(parameter.getText(), parameter.getListValue(), parameter.getDataSetReference());
        Assertions.assertTrue(hasOnlyText, "Variable has multiple values");
        return parameter;
    }

    private UUID createAttribute(AttributeType attributeType, UUID referencedDataSetList, List<String> listValues) {
        return attributeService.create(
                dataSetListId, 0, "ParameterServiceTestAttr", attributeType, referencedDataSetList, listValues
        ).getId();
    }

    @SneakyThrows(DataSetExistsException.class)
    private UUID createParameterWithDataSetAnAllValues(AttributeType attributeType, String dataSetName,
                                                       String textValue, String listValueName,
                                                       String referencedDataSetName) {
        UUID attributeId = createAttribute(attributeType, null, listValues);
        UUID dataSetId = dataSetService.create(dataSetListId, dataSetName).getId();
        UUID listValueId = Objects.nonNull(listValueName) ?
                attributeService.createListValue(attributeId, listValueName).getId() : null;
        UUID referencedDataSetId = Objects.nonNull(referencedDataSetName) ?
                dataSetService.create(dataSetListId, referencedDataSetName).getId() : null;
        Parameter parameter =
                parameterService.create(dataSetId, attributeId, textValue, listValueId, referencedDataSetId);
        return parameter.getId();
    }

    @Test
    public void deleteParameterWithoutOverlap() {
        UUID parameterId = createParameterWithDataSetAnAllValues(
                AttributeType.TEXT,
                dataSetNames.get(0),
                parameterTextValues.get(0),
                null,
                null
        );
        Parameter parameter = parameterService.get(parameterId);
        DataSet dataset = parameter.getDataSet();
        boolean result = parameterService.delete(parameter.getAttribute().getId(),
                dataset.getId(), dataset.getDataSetList().getId(), null);
        Assertions.assertTrue(result);
    }

    @Test
    public void delete_ParameterWithDatasetLocked_NonDeleteAndThrowException() {
        UUID parameterId = createParameterWithDataSetAnAllValues(
                AttributeType.TEXT,
                dataSetNames.get(0),
                parameterTextValues.get(0),
                null,
                null
        );
        Parameter parameter = parameterService.get(parameterId);
        DataSet dataset = parameter.getDataSet();
        //Setting the dataset lock = true
        dataSetService.lock(dataSetListId, Collections.singletonList(dataset.getId()), true);

        assertThrows(IllegalArgumentException.class, () ->
                        parameterService.delete(parameter.getAttribute().getId(), dataset.getId(), dataset.getDataSetList().getId(), null));
    }

    @Disabled //runs locally but doesn't run in a pod
    @Test
    public void deleteParameterWithOverlap() throws DuplicateKeyException {
        UUID visibilityAreaId = visibilityAreaService.create("ParameterServiceTestVa").getId();

        //Postal Code DSL+DS+Parameter
        UUID postalCodeDataSetListId = dataSetListService.create(visibilityAreaId, "PostalCode", null).getId();
        DataSet postalCodeDataSet = dataSetService.create(postalCodeDataSetListId, "PC#1");
        Attribute codeAttribute =
                attributeService.create(postalCodeDataSetListId, 0, "Code", AttributeType.TEXT, null, null);
        String codeOriginalValue = "OriginalValue";
        parameterService.create(postalCodeDataSet.getId(), codeAttribute.getId(), codeOriginalValue, null, null);

        //Address DSL+DS+Parameter, overlaps parameter from Postal Code
        UUID addressDataSetList = dataSetListService.create(visibilityAreaId, "Address", null).getId();
        DataSet addressDataSet1 = dataSetService.create(addressDataSetList, "ADR#1");
        Attribute postalCodeReferenceAttribute = attributeService
                .create(addressDataSetList, 0, "PostalCodeRef", AttributeType.DSL, postalCodeDataSetListId, null);
        parameterService.create(addressDataSet1.getId(), postalCodeReferenceAttribute.getId(), null, null,
                postalCodeDataSet.getId());
        String codeOverlappedByAddressValue = "Overlapped by address";
        Parameter overlapParam = parameterService.set(
                addressDataSet1.getId(),
                codeAttribute.getId(),
                ImmutableList.of(postalCodeReferenceAttribute.getId()),
                codeOverlappedByAddressValue, null, null
        );

        addressDataSet1 = dataSetService.get(addressDataSet1.getId());

        List<Parameter> addressParameters = addressDataSet1.getParameters();
        Set<UUID> addressParameterIds = addressParameters.stream().map(Parameter::getId).collect(Collectors.toSet());
        Assertions.assertTrue(addressParameterIds.contains(overlapParam.getId()));
        List<UUID> attrPathIds = Collections.singletonList(postalCodeReferenceAttribute.getId());
        boolean result = parameterService.delete(overlapParam.getAttribute().getId(),
                addressDataSet1.getId(), addressDataSet1.getDataSetList().getId(), attrPathIds);
        Assertions.assertTrue(result);
        addressDataSet1 = dataSetService.get(addressDataSet1.getId());

        addressParameters = addressDataSet1.getParameters();
        addressParameterIds = addressParameters.stream().map(Parameter::getId).collect(Collectors.toSet());
        Assertions.assertFalse(addressParameterIds.contains(overlapParam.getId()));
    }

    @Test
    public void create_updateEmptyRefParameter_parameterUpdatedSuccessfully() throws DuplicateKeyException {
        UUID dataSetList1Id = dataSetListService.create(visibilityAreaId, "dsl1", null).getId();
        UUID dataSetList2Id = dataSetListService.create(visibilityAreaId, "dsl2", null).getId();
        DataSet dataSet11 = dataSetService.create(dataSetList1Id, "ds11");
        DataSet dataSet21 = dataSetService.create(dataSetList2Id, "ds21");

        Attribute textAttr = attributeService
                .create(dataSetList2Id, 0, "TextAttribute", AttributeType.TEXT, null, null);
        Parameter textParam = parameterService
                .create(dataSet11.getId(), textAttr.getId(), "text", null, null);
        Attribute dsRefAttr = attributeService
                .create(dataSetList1Id, 0, "RefAttribute", AttributeType.DSL, dataSetList2Id, null);
        Parameter refParamBefore = parameterService
                .create(dataSet11.getId(), dsRefAttr.getId(), null, null, null);
        Parameter refParamAfter = parameterService.set(dataSet11.getId(), dsRefAttr.getId(),
                Collections.singletonList(textAttr.getId()), null, dataSet21.getId(), null);

        Assertions.assertNull(refParamBefore.getDataSetReference());
        Assertions.assertEquals(dataSet21.getId(), refParamAfter.getDataSetReference().getId());
    }

    @Test
    public void create_updateEmptyTexParameter_parameterUpdatedSuccessfully() throws DuplicateKeyException {
        String paramValue = "TextAttribute";
        UUID dataSetListId = dataSetListService.create(visibilityAreaId, "dsl", null).getId();
        DataSet dataSet = dataSetService.create(dataSetListId, "ds");

        Attribute textAttr = attributeService
                .create(dataSetListId, 0, paramValue, AttributeType.TEXT, null, null);

        Assertions.assertEquals(0, textAttr.getParameters().size());

        parameterService.create(dataSet.getId(), textAttr.getId(), "text", null, null);

        Assertions.assertEquals(paramValue, dataSetListService.get(dataSetListId).getAttributes().get(0).getName());
    }

}

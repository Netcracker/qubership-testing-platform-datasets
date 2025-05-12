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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.ImmutableList;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.service.direct.DuplicateKeyException;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSet;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class AttributeServiceImplTest extends DataSetBuilder {

    private UUID dslId1;
    private UUID dslId2;
    private UUID dslId3;
    private UUID dsId11;
    private UUID dsId12;
    private UUID dsId21;
    private UUID dsId31;
    private UUID attrId11;
    private UUID attrId12;
    private UUID attrId13;
    private UUID attrId14;
    private UUID attrId15;
    private UUID attr22;
    private UUID attr31;

    @After
    public void tearDown() throws Exception {
        visibilityAreaService.delete(vaId);
    }

    @Test
    public void testRenameAttributeWillReplaceRefs() throws DuplicateKeyException {
        dataSetService.rename(dataSet1.getId(), "DataSet + MagicName");
        attributeService.update(attr.getId(), "Attribute");
        String stringValue = "#REF(DataSet + MagicName.Attribute) asd #REF(DataSet + MagicName.Attribute)";
        parameterService.update(source.getId(), stringValue);
        //rename attribute
        attributeService.update(attr.getId(), "SZ_UPDATED");
        Assertions.assertEquals(
                "#REF(DataSet + MagicName.SZ_UPDATED) asd #REF(DataSet + MagicName.SZ_UPDATED)",
                wrapperService.unWrapAlias(parameterService.get(source.getId()).getText())
        );
    }

    @Test
    public void testRenameAttributeWillReplaceRefsDsl() throws DuplicateKeyException {
        dataSetService.rename(dataSet1.getId(), "DataSet + MagicName");
        attributeService.update(attr.getId(), "Attribute");
        dataSetListService.rename(dataSetList.getId(), "Input");
        String stringValue = "#REF(DataSet + MagicName.Attribute) #REF_DSL(Input.DataSet + MagicName.Attribute)";
        parameterService.update(source.getId(), stringValue);
        //rename attribute
        attributeService.update(attr.getId(), "SZ_UPDATED");

        Assertions.assertEquals(
                "#REF(DataSet + MagicName.SZ_UPDATED) #REF_DSL(Input.DataSet + MagicName.SZ_UPDATED)",
                wrapperService.unWrapAlias(parameterService.get(source.getId()).getText())
        );
    }

    @Test
    public void testRenameAttributeWillNotReplaceRefsDslIfAttrNameMismatch() throws DuplicateKeyException {
        dataSetService.rename(dataSet1.getId(), "DataSet + MagicName");
        attributeService.update(attr.getId(), "Attribute");
        dataSetListService.rename(dataSetList.getId(), "Input");
        String stringValue = "#REF(DataSet + MagicName.Attribute1) #REF_DSL(Input.DataSet + MagicName.1Attribute)";
        parameterService.update(source.getId(), stringValue);
        //rename attribute
        attributeService.update(attr.getId(), "SZ_UPDATED");

        Assertions.assertEquals(
                "#REF(DataSet + MagicName.Attribute1) #REF_DSL(Input.DataSet + MagicName.1Attribute)",
                wrapperService.unWrapAlias(parameterService.get(source.getId()).getText())
        );
    }

    @Test
    public void update_renameAttribute_attributeHaseTheSameOrdering() {
        int orderBefore = modelsProvider.getAttributeById(attr.getId()).getOrdering();

        attributeService.update(attr.getId(), "NewName");

        Assertions.assertEquals(orderBefore, (int) modelsProvider.getAttributeById(attr.getId()).getOrdering());
        Assertions.assertEquals("NewName", modelsProvider.getAttributeById(attr.getId()).getName());
    }

    @Test
    public void getAttrOptions_ReferenceAttr_ReferencesAreSorted() {
        MultipleRefOptions t = createTestDataInstance(MultipleRefOptions::new);
        UiManDataSetList er = new UiManDataSetList();
        er.setSource(t.childDsl);
        er.getDataSets().add(new UiManDataSet(t.dsA));
        er.getDataSets().add(new UiManDataSet(t.dsZ));
        Object ar = attributeService.getOptions(t.refAttr.getId());

        Assertions.assertEquals(er, ar);
    }

    @Test
    public void deleteListValuesBulk_listValuesWereDeleted() {
        ListValues listValues = createTestDataInstance(ListValues::new);
        Attribute beforeDeleting = attributeService.get(listValues.listAttrId);
        ListValue listValue1 = beforeDeleting.getListValues().get(0);
        ListValue listValue2 = beforeDeleting.getListValues().get(1);

        Assertions.assertNotNull(listValues.dsA.getParameters().get(0).getListValue());
        Assertions.assertNotNull(listValues.dsZ.getParameters().get(0).getListValue());

        attributeService.deleteListValues(beforeDeleting.getId(),
                Arrays.asList(listValue1.getId(), listValue2.getId()));

        Attribute afterDeleting = attributeService.get(listValues.listAttrId);
        Assertions.assertEquals(Arrays.asList(), afterDeleting.getListValues());

        Parameter after1 = parameterService.get(listValues.dsA.getParameters().get(0).getId());
        Parameter after2 = parameterService.get(listValues.dsZ.getParameters().get(0).getId());

        Assertions.assertNull(after1.getListValue(), "Parameter was not deleted, but lv is empty");
        Assertions.assertNull(after2.getListValue(), "Parameter was not deleted, but lv is empty");
    }

    @Test
    public void deleteListValue_deleteListValue1_listValue1WereDeleted() {
        ListValues listValues = createTestDataInstance(ListValues::new);
        Attribute beforeDeleting = attributeService.get(listValues.listAttrId);
        ListValue listValue1 = beforeDeleting.getListValues().get(0);
        ListValue listValue2 = beforeDeleting.getListValues().get(1);

        Assertions.assertEquals(beforeDeleting.getListValues().size(), 2);

        attributeService.deleteListValue(beforeDeleting.getId(), listValue1.getId());

        Attribute afterDeleting = attributeService.get(listValues.listAttrId);
        Assertions.assertEquals(afterDeleting.getListValues().size(), 1);
        Assertions.assertNotEquals(listValue1.getId(), afterDeleting.getListValues().get(0).getId());
        Assertions.assertEquals(listValue2.getId(), afterDeleting.getListValues().get(0).getId());
    }

    @Test
    public void deleteListValue_deleteListValue1ForLockDs_listValue1NotDeleted() {
        ListValues listValues = createTestDataInstance(ListValues::new);
        Attribute beforeDeleting = attributeService.get(listValues.listAttrId);
        ListValue listValue1 = beforeDeleting.getListValues().get(0);

        dataSetService.lock(listValues.dsA.getDataSetList().getId(),
                Collections.singletonList(listValues.dsA.getId()), true);

        Assertions.assertEquals(beforeDeleting.getListValues().size(), 2);
        Attribute afterDeleting;
        try {
            Assertions.assertThrows(IllegalArgumentException.class, ()-> {
                attributeService.deleteListValue(beforeDeleting.getId(), listValue1.getId());
            });
        } catch (IllegalArgumentException e) {
            afterDeleting = attributeService.get(listValues.listAttrId);
            Assertions.assertEquals(afterDeleting.getListValues().size(), 2);
            throw new IllegalArgumentException(e);

        }

    }

    @Test
    public void replaceListValues_replaceListValuesForLockDs_listValuesNotDeleted() {
        ListValues listValues = createTestDataInstance(ListValues::new);
        Attribute beforeDeleting = attributeService.get(listValues.listAttrId);
        ListValue listValue1 = beforeDeleting.getListValues().get(0);
        ListValue listValue2 = beforeDeleting.getListValues().get(1);

        dataSetService.lock(listValues.dsA.getDataSetList().getId(),
                Collections.singletonList(listValues.dsA.getId()), true);

        Assertions.assertEquals(beforeDeleting.getListValues().size(), 2);
        Attribute afterDeleting;
        try {
            Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            attributeService.deleteListValues(beforeDeleting.getId(),
                    Arrays.asList(listValue1.getId(), listValue2.getId()));
            });
        } catch (IllegalArgumentException e) {
            afterDeleting = attributeService.get(listValues.listAttrId);
            Assertions.assertEquals(afterDeleting.getListValues().size(), 2);
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    public void deleteListValue_deleteAttributeListValue_attributeHasOneListValue() {
        ListValues listValues = createTestDataInstance(ListValues::new);
        Attribute attribute = attributeService.get(listValues.listAttrId);
        List<ListValue> lvsBefore = attribute.getListValues();

        attributeService.deleteListValue(listValues.listAttrId, lvsBefore.get(0).getId());
        attribute = attributeService.get(listValues.listAttrId);
        List<ListValue> lvsAfter = attribute.getListValues();

        Assertions.assertEquals(2, lvsBefore.size());
        Assertions.assertEquals(1, lvsAfter.size());
    }

    @Test
    public void createListValuesBulk_listValuesWereCreated() {
        ListValues listValues = createTestDataInstance(ListValues::new);
        attributeService.createListValues(listValues.listAttrId, Arrays.asList("456", "567"));

        Attribute attribute = attributeService.get(listValues.listAttrId);
        Assertions.assertEquals("456", attribute.getListValues().get(2).getName());
        Assertions.assertEquals("567", attribute.getListValues().get(3).getName());
    }

    @Test
    public void testUpdateDslReference_with1Level_checkOverlapsAreDeletedProperly() {
        MultipleRefOptions testData = createTestDataInstance(MultipleRefOptions::new);
        parameterService.set(testData.dsA.getId(), testData.textAttr.getId(), "ololo",
                Arrays.asList(testData.refAttr.getId()));
        boolean result = attributeService.updateDslReference(testData.refAttr.getId(),
               testData.childDsl2.getId());

        Assertions.assertTrue(result);
        Assertions.assertEquals(Collections.emptyList(), testData.textAttr.getParameters());
    }

    @Test
    public void testUpdateDslReference_with2Levels_checkOverlapsAreDeletedProperly() throws DuplicateKeyException {
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

        //Another Postal Code DSL+DS+Parameter
        UUID anotherPostalCodeDataSetListId =
                dataSetListService.create(visibilityAreaId, "PostalCode another", null).getId();
        DataSet anotherPostalCodeDataSet = dataSetService.create(anotherPostalCodeDataSetListId, "PC another#1");
        Attribute anotherCodeAttribute =
                attributeService.create(anotherPostalCodeDataSetListId,
                        0, "Code", AttributeType.TEXT, null, null);
        String anotherCodeOriginalValue = "Another OriginalValue";
        parameterService.create(anotherPostalCodeDataSet.getId(), anotherCodeAttribute.getId(),
                anotherCodeOriginalValue, null, null);

        customerDataSet = dataSetService.get(customerDataSet.getId());
        addressDataSet1 = dataSetService.get(addressDataSet1.getId());

        List<Parameter> beforeCustomersParametersList = customerDataSet.getParameters();
        Assertions.assertEquals(2, beforeCustomersParametersList.size());

        List<Parameter> beforeAddressParametersList = addressDataSet1.getParameters();
        Assertions.assertEquals(2, beforeAddressParametersList.size());

        //update postal code reference with another postal code
        attributeService.updateDslReference(postalCodeReferenceAttribute.getId(), anotherPostalCodeDataSetListId);

        //Check customer don't overlap parameter anymore
        customerDataSet = dataSetService.get(customerDataSet.getId());
        addressDataSet1 = dataSetService.get(addressDataSet1.getId());

        List<Parameter> afterDslUpdateParametersList = customerDataSet.getParameters();
        Assertions.assertEquals(1, afterDslUpdateParametersList.size());

        List<Parameter> afterAddressParametersList = addressDataSet1.getParameters();
        Assertions.assertEquals(0, afterAddressParametersList.size());
    }

    @Test
    public void getParametersAndDataSetIdsForAttributeSorting_testTextAttributeLevel1_ReturnDataSetIds() throws DuplicateKeyException {
        prepareDataForFilteringAttributeValues();
        UiManDataSetList tree = dataSetListService.getAsTree(dslId1, false, null, Collections.singletonList(attrId11), null, null, false, true);

        Map<String, List<UUID>> result = attributeService.getParametersAndDataSetIdsForAttributeSorting(tree, dslId1, attrId11, Collections.singletonList(attrId11));
        List<UUID> ids = result.get("txt11");

        Assertions.assertEquals(1, ids.size());
        Assertions.assertTrue(ids.contains(dsId11));
    }

    @Test
    public void getParametersAndDataSetIdsForAttributeSorting_testTextAttributeTheSameParamValues_ReturnDataSetIds() throws DuplicateKeyException {
        prepareDataForFilteringAttributeValues();
        UiManDataSetList tree = dataSetListService.getAsTree(dslId1, false, null, Collections.singletonList(attrId15), null, null, false, true);

        Map<String, List<UUID>> result = attributeService.getParametersAndDataSetIdsForAttributeSorting(tree, dslId1, attrId15, Collections.singletonList(attrId15));
        List<UUID> ids = result.get("txt15");

        Assertions.assertEquals(2, ids.size());
        Assertions.assertTrue(ids.contains(dsId11));
        Assertions.assertTrue(ids.contains(dsId12));
    }

    @Test
    public void getParametersAndDataSetIdsForAttributeSorting_testMultiplyAttributeLevel1_ReturnDataSetIds() throws DuplicateKeyException {
        prepareDataForFilteringAttributeValues();
        UiManDataSetList tree = dataSetListService.getAsTree(dslId1, false, null, Collections.singletonList(attrId13), null, null, false, true);

        Map<String, List<UUID>> result = attributeService.getParametersAndDataSetIdsForAttributeSorting(tree, dslId1, attrId13, Collections.singletonList(attrId13));
        List<UUID> ids = result.get("DS21");

        Assertions.assertEquals(1, ids.size());
        Assertions.assertTrue(ids.contains(dsId11));
    }

    @Test
    public void getParametersAndDataSetIdsForAttributeSorting_testListAttributeLevel1_ReturnDataSetIds() throws DuplicateKeyException {
        prepareDataForFilteringAttributeValues();
        UiManDataSetList tree = dataSetListService.getAsTree(dslId1, false, null, Collections.singletonList(attrId14), null, null, false, true);

        Map<String, List<UUID>> result = attributeService.getParametersAndDataSetIdsForAttributeSorting(tree, dslId1, attrId14, Collections.singletonList(attrId14));
        List<UUID> ids = result.get("Link11");

        Assertions.assertEquals(1, ids.size());
        Assertions.assertTrue(ids.contains(dsId11));
    }

    @Test
    public void getParametersAndDataSetIdsForAttributeSorting_testDslAttributeLevel3_ReturnDataSetIds() throws DuplicateKeyException {
        prepareDataForFilteringAttributeValues();
        List<UUID> attrFilterIds = new ArrayList<>();
        attrFilterIds.add(attrId12);
        attrFilterIds.add(attr22);
        UiManDataSetList tree = dataSetListService.getAsTree(dslId1, false, null, Collections.singletonList(attrId12), null, null, false, true);

        Map<String, List<UUID>> result = attributeService.getParametersAndDataSetIdsForAttributeSorting(tree, dslId1, attr31, attrFilterIds);
        List<UUID> ids = result.get("txt31");

        Assertions.assertEquals(1, ids.size());
        Assertions.assertTrue(ids.contains(dsId11));
    }

    private void prepareDataForFilteringAttributeValues() throws DuplicateKeyException {
        // Create DSL
        dslId1 = dataSetListService.create(vaId, "DSL1", null).getId();
        dslId2 = dataSetListService.create(vaId, "DSL2", null).getId();
        dslId3 = dataSetListService.create(vaId, "DSL3", null).getId();
        // Create DS
        dsId11 = dataSetService.create(dslId1, "DS11").getId();
        dsId12 = dataSetService.create(dslId1, "DS12").getId();
        dsId21 = dataSetService.create(dslId2, "DS21").getId();
        dsId31 = dataSetService.create(dslId3, "DS31").getId();
        // Create attributes
        attrId11 = attributeService.create(dslId1, 1, "attr11", AttributeType.TEXT, null, null).getId();
        attrId12 = attributeService.create(dslId1, 2, "attr12", AttributeType.DSL, dslId2, null).getId();
        attrId13 = attributeService.create(dslId1, 3, "attr13", AttributeType.CHANGE, dslId2, null).getId();
        attrId15 = attributeService.create(dslId1, 4, "attr15", AttributeType.TEXT, null, null).getId();
        List<String> listValues = new ArrayList<>();
        listValues.add("Link11");
        listValues.add("Link12");
        Attribute attr14 = attributeService.create(dslId1, 4, "attr14", AttributeType.LIST, null, listValues);
        attrId14 = attr14.getId();
        attr22 = attributeService.create(dslId2, 0, "attr21", AttributeType.DSL, dslId3, null).getId();
        attr31 = attributeService.create(dslId3, 0, "attr31", AttributeType.TEXT, null, null).getId();
        // Create parameters
        parameterService.create(dsId11, attrId11, "txt11", null, null).getId();
        parameterService.create(dsId11, attrId12, null, null, dsId21).getId();
        parameterService.create(dsId11, attrId13, "MULTIPLY " + dsId21, null, null).getId();
        parameterService.create(dsId11, attrId15, "txt15", null, null).getId();
        parameterService.create(dsId12, attrId15, "txt15", null, null).getId();
        parameterService.create(dsId11, attrId14, null, attr14.getListValues().get(0).getId(), null).getId();
        parameterService.create(dsId21, attr22, null, null, dsId31).getId();
        parameterService.create(dsId31, attr31, "txt31", null, null).getId();
    }

    private static class MultipleRefOptions implements Supplier<VisibilityArea> {
        public final Attribute refAttr;
        public final Attribute textAttr;
        private final VisibilityArea va;
        private final DataSetList parentDsl;
        private final DataSetList childDsl;
        private final DataSetList childDsl2;
        private final DataSet dsA;
        private final DataSet dsZ;
        private final DataSet dsB;

        public MultipleRefOptions(CreationFacade create) {
            va = create.va("ATPII-6329");
            parentDsl = create.dsl(va, "Parent DSL");
            childDsl = create.dsl(va, "Child DSL");
            childDsl2 = create.dsl(va, "Child DSL 2");
            dsZ = create.ds(childDsl, "Z");
            dsA = create.ds(childDsl, "A");
            dsB = create.ds(childDsl2, "B");
            refAttr = create.refAttr(parentDsl, "ref", childDsl);
            textAttr = create.textAttr(childDsl, "Text attr");
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }

    private static class ListValues implements Supplier<VisibilityArea> {
        private final VisibilityArea va;
        private final DataSetList parentDsl;
        private final DataSet dsA;
        private final DataSet dsZ;
        private final UUID listAttrId;

        public ListValues(CreationFacade create) {
            va = create.va("ATPII-9090");
            parentDsl = create.dsl(va, "Parent DSL");
            dsZ = create.ds(parentDsl, "Z");
            dsA = create.ds(parentDsl, "A");
            Attribute listAttr = create.listAttr(parentDsl, "ref", "123", "234");
            listAttrId = listAttr.getId();
            create.listParam(dsA, listAttr, "123");
            create.listParam(dsZ, listAttr, "234");
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }
}

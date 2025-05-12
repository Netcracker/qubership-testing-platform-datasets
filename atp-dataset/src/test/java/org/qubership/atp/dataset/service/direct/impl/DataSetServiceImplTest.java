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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.db.LabelRepository;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.service.direct.DuplicateKeyException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import lombok.SneakyThrows;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class DataSetServiceImplTest extends DataSetBuilder {

    DataSetList addressDsl;
    DataSetList postalCodeDsl;
    org.qubership.atp.dataset.model.DataSet addrFirstDs;
    org.qubership.atp.dataset.model.DataSet addrSecondDs;
    org.qubership.atp.dataset.model.DataSet postalCodeFirstDs;
    org.qubership.atp.dataset.model.DataSet postalCodeSecondDs;
    Attribute firstAttribute;
    Attribute postalCodeRef;
    @Autowired
    LabelRepository labelRepository;
    @Autowired
    JpaDataSetService jpaDataSetService;
    private DataSetList customerDsl;
    private org.qubership.atp.dataset.model.DataSet customerDs;
    private Attribute addressRefAttr;
    private DataSetList datasetList1;
    private DataSetList dataSetList2;
    private org.qubership.atp.dataset.model.DataSet dataSetToDelete;
    private Parameter parameterToCheck;

    @Override
    @BeforeEach
    public void setUp() throws EvalException {
        super.setUp();
        createAddressDsl();
        createPostalCodeDsl();
        createAttributes();
        overlapParameters();
        createDatasetAndRedParam();
    }

    @Test
    public void testRenameOfDataSetWillReplaceRef() throws DuplicateKeyException {
        parameterService.update(source.getId(), "#REF_DSL(DSL.DS.ATTR) #REF(DS.ATTR)");
        dataSetService.rename(dataSet1.getId(), "SZ_UPDATED");
        String text = parameterService.get(source.getId()).getText();
        assertEquals(
                "#REF_DSL(DSL.SZ_UPDATED.ATTR) #REF(SZ_UPDATED.ATTR)",
                wrapperService.unWrapAlias(text)
        );
        assertThat(text, not(containsString("DataSet + MagicName.")));
    }

    @Test
    public void testDeleteDataSetCascade() {
        Parameter parameter = parameterService
                .create(dataSet2.getId(), attr.getId(), "", null, null);
        dataSetService.delete(dataSet2.getId());
        assertNotNull(dataSetListService.get(dataSetList.getId()));
        assertNull(dataSetService.get(dataSet2.getId()));
        assertNull(parameterService.get(parameter.getId()));
        assertNotNull(attributeService.get(attr.getId()));
        List<VisibilityArea> all = visibilityAreaService.getAll();
        VisibilityArea area = all.stream().filter(va -> va.getId().equals(vaId))
                .findFirst().orElseThrow(() -> new AssertionError("VisibilityArea not found"));
        assertNotNull(area);
    }

    @Test
    public void getAffectedDatasetsByChangesAttribute_Scenary_AffectedDatasetsHasTaken() {
        DataSetList dsl_source = dataSetListService.create(vaId, "dsl_source", null);
        org.qubership.atp.dataset.model.DataSet dsSource = dataSetService.create(dsl_source.getId(), "ds_source");
        Attribute textAttr = attributeService.create(dsl_source.getId(), 0, "TXT", AttributeType.TEXT, null, null);
        parameterService.create(dsSource.getId(), textAttr.getId(), "text", null, null);

        DataSetList dsl = dataSetListService.create(vaId, "dsl", null);
        org.qubership.atp.dataset.model.DataSet ds1 = dataSetService.create(dsl.getId(), "ds");
        Attribute refAttr = attributeService.create(dsl.getId(), 0, "ref_attr", AttributeType.DSL, dsl_source.getId(), null);
        parameterService.create(ds1.getId(), refAttr.getId(), null, null, dsSource.getId());
        parameterService.set(ds1.getId(), textAttr.getId(), Collections.singletonList(refAttr.getId()), "newText", null,
                null);

        List<org.qubership.atp.dataset.model.DataSet> datasets
                = (List<org.qubership.atp.dataset.model.DataSet>) dataSetService.getOverlapContainers(dsSource.getId(), textAttr.getId(), false);

        assertEquals( 1, datasets.size());
    }

    @Test
    public void testGetAffectedDatasetsWhenInHierarchy() {
        createCustomerDsl();
        parameterService.set(
                customerDs.getId(), firstAttribute.getId(),
                Arrays.asList(addressRefAttr.getId(), postalCodeRef.getId()),
                "Customer_override", null, null
        );
        List<org.qubership.atp.dataset.model.DataSet> overlapDataSets
                = (List<org.qubership.atp.dataset.model.DataSet>) dataSetService.getOverlapContainers(postalCodeFirstDs.getId(), firstAttribute.getId(), false);

        List<TableResponse> overlapData
                = (List<TableResponse>) dataSetService.getOverlapContainers(postalCodeFirstDs.getId(), firstAttribute.getId(), true);

        assertEquals(0, overlapDataSets.size(), "Size must be 0, because no one parameter is overridden in Hierarchy for one dataset");
        assertEquals(0, overlapData.size(), "Size must be 0, because no one parameter is overridden in Hierarchy for one dataset");
        assertEquals(0, overlapDataSets.size());
        assertEquals(0, overlapData.size());

        assertEquals(0, overlapData.size(), "Size must be 0, because no one parameter is overridden in Hierarchy for one dataset");
        List<UUID> ids = overlapData.stream().map(TableResponse::getDsId).collect(Collectors.toList());
        assertEquals(0, ids.size());

    }

    @Test
    public void testGetAffectedDatasetsByChangesAttributeReturnOnlyAffectedDS() {
        List<org.qubership.atp.dataset.model.DataSet> overlapDataSets
                = (List<org.qubership.atp.dataset.model.DataSet>) dataSetService.getOverlapContainers(postalCodeFirstDs.getId(), firstAttribute.getId(), false);
        List<TableResponse> overlapData
                = (List<TableResponse>) dataSetService.getOverlapContainers(postalCodeFirstDs.getId(), firstAttribute.getId(), true);
        assertEquals(0, overlapDataSets.size(), "Size must be 0, because no one ds is affected");
        assertEquals(0, overlapData.size(), "Size must be 0, because no one ds is affected");

        assertEquals(0, overlapDataSets.size());
        assertEquals(0, overlapData.size());
    }

    @Test
    public void testGetAffectedDatasetsByChangesDataSetReferenceReturnOnlyAffectedDS() {
        List<org.qubership.atp.dataset.model.DataSet> affectedDataSetsByChangesDataSetReference =
                (List<org.qubership.atp.dataset.model.DataSet>) dataSetService
                        .getAffectedDataSetsByChangesDataSetReference(postalCodeFirstDs.getId(), false);
        assertEquals(0, affectedDataSetsByChangesDataSetReference.size(), "Size must be 0, because no one ds is "
                + "affected");

        assertEquals(0, affectedDataSetsByChangesDataSetReference.size());
    }

    @Test
    public void getAffectedInfoByDeletingDataSet_fullInfoReturned() {
        List<TableResponse> affectedInfoByTryingToDeleteDs =
                (List<TableResponse>) dataSetService
                        .getAffectedDataSetsByChangesDataSetReference(dataSetToDelete.getId(), true);
        assertEquals(0, affectedInfoByTryingToDeleteDs.size(), "No one param is affected");

        assertEquals(0, affectedInfoByTryingToDeleteDs.size());
    }

    @Test
    public void testMarkDataSet() {
        createCustomerDsl();
        String labelName = "Customer Label #001";
        dataSetService.mark(customerDs.getId(), labelName);
        assertEquals(labelName, dataSetService.getLabels(customerDs.getId()).get(0).getName());
    }

    @Test
    public void testUnmarkDataSet() {
        createCustomerDsl();
        Label label = dataSetService.mark(customerDs.getId(), "Label#001");
        dataSetService.unmark(customerDs.getId(), label.getId());
        Assertions.assertFalse(dataSetService.getLabels(customerDs.getId()).contains(label));
    }

    @Test
    public void testDeleteDataSetLabelCascade() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        createCustomerDsl();
        dataSetService.mark(customerDs.getId(), "Label#001"); // Add ds label
        org.qubership.atp.dataset.model.DataSet ds = dataSetService.get(customerDs.getId());
        assertNotNull(ds);
        assertThat(ds.getLabels(), Matchers.hasSize(1)); // Check that there is a ds label
        dataSetService.delete(customerDs.getId());
        assertThat(customerDs.getLabels(), Matchers.empty());
        Method method = labelRepository.getClass().getDeclaredMethod("getLabelsOfDs", UUID.class);
        method.setAccessible(true);
        // Get label by dataSetId
        List<Label> labels = (List<Label>) method.invoke(labelRepository, customerDs.getId());
        assertThat(labels, Matchers.empty()); // Check that there are no ds labels
    }

    @Test
    public void testGetAllDslDsByAttribute() {
        createCustomerDsl();
        List<org.qubership.atp.dataset.model.DataSet> resultDs = dataSetService.getAllDslDsByAttribute(addressRefAttr.getId(), Collections.emptyList());
        assertEquals(addressRefAttr.getDataSetList().getDataSets(), resultDs);
    }

    @Test
    public void lockDataSet_setLockTrue_flagUpdate() throws DuplicateKeyException {
        org.qubership.atp.dataset.model.DataSet dsLock = dataSetService.create(datasetList1.getId(), "dsLock");

        //checking the default parameter
        assertEquals(Objects.requireNonNull(dataSetService.get(dsLock.getId())).isLocked(), false);
        //Setting the dataset lock = true
        dataSetService.lock(datasetList1.getId(), Collections.singletonList(dsLock.getId()), true);
        //checking the lock parameter
        assertEquals(Objects.requireNonNull(dataSetService.get(dsLock.getId())).isLocked(), true);
    }

//    @Test(expected = IllegalArgumentException.class)
    @Test
    public void renameDataSet_datasetIsLockTrue_NotRenameThrowException() throws DuplicateKeyException {
        org.qubership.atp.dataset.model.DataSet dsLock = dataSetService.create(datasetList1.getId(), "dsLock");

        //checking the default parameter
        assertEquals(Objects.requireNonNull(dataSetService.get(dsLock.getId())).isLocked(), false);
        //Setting the dataset lock = true
        dataSetService.lock(datasetList1.getId(), Collections.singletonList(dsLock.getId()), true);
        //checking the lock parameter
        assertEquals(Objects.requireNonNull(dataSetService.get(dsLock.getId())).isLocked(), true);
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            dataSetService.rename(dsLock.getId(), "dsUnLock");
        });

    }

//    @Test(expected = DataSetExistsException.class)
    @Test
    public void testCreateDataSetWithNonUniqueName() {
        Assertions.assertThrows(DataSetExistsException.class, ()-> {
            dataSetService.create(dataSetList.getId(), "DS");
        });

    }

//    @Test(expected = DataSetExistsException.class)
    @Test
    public void testRenameDataSetWithNonUniqueName() {
        Assertions.assertThrows(DataSetExistsException.class, ()-> {
            dataSetService.rename(dataSet1.getId(), "DS2");
        });

    }

//    @Test(expected = DataSetExistsException.class)
    @Test
    public void testDuplicateDataSetWithNonUniqueName() {
        Assertions.assertThrows(DataSetExistsException.class, ()-> {
            dataSetService.copy(dataSet1.getId(), "DS");
        });

    }

    @SneakyThrows(DataSetExistsException.class)
    private void createCustomerDsl() {
        customerDsl = dataSetListService.create(vaId, "CustomerDsl", null);
        addressRefAttr = attributeService.create(
                customerDsl.getId(), 0, "addressRef", AttributeType.DSL, addressDsl.getId(), null);
        customerDs = dataSetService.create(customerDsl.getId(), "CustomerDs");
        parameterService.create(customerDs.getId(), addressRefAttr.getId(), null, null, addrFirstDs.getId());
    }

    @SneakyThrows(DataSetExistsException.class)
    public void createAddressDsl() {
        addressDsl = dataSetListService.create(vaId, "addressDsl", null);
        addrFirstDs = dataSetService.create(addressDsl.getId(), "addrFirstDs");
        addrSecondDs = dataSetService.create(addressDsl.getId(), "addrSecondDs");
    }

    @SneakyThrows(DataSetExistsException.class)
    public void createPostalCodeDsl() {
        postalCodeDsl = dataSetListService.create(vaId, "postalCodeDsl", null);
        postalCodeFirstDs = dataSetService.create(postalCodeDsl.getId(), "postalCodeFirstDs");
        firstAttribute = attributeService.create(postalCodeDsl.getId(), 0, "PC_firstAttribute", AttributeType.TEXT, null, null);
        parameterService.create(
                postalCodeFirstDs.getId(), firstAttribute.getId(), "PC1_firstValue", null, null
        );
        postalCodeSecondDs = dataSetService.create(postalCodeDsl.getId(), "postalCodeSecondDs");
        parameterService.create(postalCodeSecondDs.getId(), firstAttribute.getId(), "postalCodeSecondDs", null, null).getId();
    }

    public void createAttributes() {
        postalCodeRef = attributeService.create(addressDsl.getId(), 2, "postalCodeRef", AttributeType.DSL,
                postalCodeDsl.getId(),
                null);
        parameterService.create(addrFirstDs.getId(), postalCodeRef.getId(), "", null, postalCodeFirstDs.getId()).getId();
        parameterService.create(addrSecondDs.getId(), postalCodeRef.getId(), "", null, postalCodeSecondDs.getId()).getId();
    }

    public void overlapParameters() {
        parameterService.set(addrFirstDs.getId(), firstAttribute.getId(),
                Collections.singletonList(postalCodeRef.getId()), "ADDR1_overlapped_firstValue", null, null);
        parameterService.set(addrSecondDs.getId(), firstAttribute.getId(),
                Collections.singletonList(postalCodeRef.getId()), "ADDR2_overlapped_firstValue", null, null);
    }

    @SneakyThrows(DataSetExistsException.class)
    private void createDatasetAndRedParam() {
        datasetList1 = dataSetListService.create(vaId, "dsl1", null);
        dataSetList2 = dataSetListService.create(vaId, "dsl2", null);
        dataSetToDelete = dataSetService.create(datasetList1.getId(), "dsToDelete");
        org.qubership.atp.dataset.model.DataSet dsl2ds = dataSetService.create(dataSetList2.getId(), "ds");
        Attribute attribute = attributeService.create(dataSetList2.getId(), 0, "refAttr",
                AttributeType.DSL, datasetList1.getId(), null);
        parameterToCheck = parameterService.create(dsl2ds.getId(), attribute.getId(), "refToDs",
                null, dataSetToDelete.getId());
    }

    @Test
    public void mark_setLabelToDataset_dataSetHasLabel() {
        String labelName = "Label";

        dataSetService.mark(dsId1, labelName);
        List<Label> labels = dataSetService.get(dsId1).getLabels();

        assertEquals(labelName, labels.get(0).getName());
    }

    @Test
    public void unmark_deleteLabelFromDataset_dataSetHasNotLabel() {
        String labelName = "Label";
        Label label = dataSetService.mark(dsId1, labelName);

        dataSetService.unmark(dsId1, label.getId());
        List<Label> labels = dataSetService.get(dsId1).getLabels();

        assertEquals(0, labels.size());
    }

    @Test
    public void rename_renameDataSetWithUniqueName_newDataSetNameInDB() throws DuplicateKeyException {
        assertEquals("DS", dataSetService.get(dataSet1.getId()).getName());

        dataSetService.rename(dataSet1.getId(), "DS_new");

        assertEquals("DS_new", dataSetService.get(dataSet1.getId()).getName());
    }

    @Test
    public void copy_duplicateDataSetWithNameProvidedAndChangeParameter_parentParameterShouldNotChangeInDB() throws DuplicateKeyException {
        org.qubership.atp.dataset.model.DataSet newDataSet = dataSetService.copy(dataSet1.getId(), "DS_Copy");
        String valueBefore = source.getText();
        parameterService.set(newDataSet.getId(), firstAttribute.getId(), null, "txt_new", null, null);

        assertEquals(valueBefore, parameterService.get(parameterId).getText());
    }

    @Test
    public void copy_duplicateDataSetWithNameProvided_newDataSetIsPresent() throws DuplicateKeyException {
        DataSetList dsl1 = dataSetListService.create(vaId, "DSL-1", null);
        DataSetList dsl2 = dataSetListService.create(vaId, "DSL-2", null);
        org.qubership.atp.dataset.model.DataSet ds1 = dataSetService.create(dsl1.getId(), "DS1");
        org.qubership.atp.dataset.model.DataSet ds2 = dataSetService.create(dsl2.getId(), "DS2");
        Attribute textAttr1 = attributeService.create(dsl1.getId(), 0, "TXT1", AttributeType.TEXT, null, null);
        Attribute textAttr2 = attributeService.create(dsl2.getId(), 0, "TXT2", AttributeType.TEXT, null, null);
        Attribute refAttr2 = attributeService.create(dsl2.getId(), 1, "REF", AttributeType.DSL, dsl1.getId(), null);
        Attribute listAttr2 = attributeService.create(dsl2.getId(), 2, "LIST", AttributeType.LIST, null, Collections.singletonList("list1"));
        String textValue = "txt2";
        UUID refId = ds1.getId();
        UUID listId = listAttr2.getListValues().get(0).getId();
        parameterService.create(ds1.getId(), textAttr1.getId(), "txt1", null, null);
        parameterService.create(ds2.getId(), textAttr2.getId(), textValue, null, null);
        parameterService.create(ds2.getId(), refAttr2.getId(), null, null, refId);
        parameterService.create(ds2.getId(), listAttr2.getId(), null, listId, null);

        org.qubership.atp.dataset.model.DataSet newDs = dataSetService.copy(ds2.getId(), "DS2_Copy");
        List<Parameter> parameters = newDs.getParameters();

        assertEquals("DS2_Copy", newDs.getName());
        assertEquals(dsl2.getId(), newDs.getDataSetList().getId());
        assertNotNull(jpaDataSetService.getById(newDs.getId()));
        for (Parameter parameter : parameters) {
            if (Objects.nonNull(parameter.getText())) {
                assertEquals(textValue, parameter.getText());
            }
            if (Objects.nonNull(parameter.getDataSetReference())) {
                assertEquals(refId, parameter.getDataSetReference().getId());
            }
            if (Objects.nonNull(parameter.getListValue())) {
                assertEquals(listId, parameter.getListValue().getId());
            }
        }
    }

    @Test
    public void delete_deleteDataSetWithEmptyTextParameter_dataSetWasDeletedInDB() throws DuplicateKeyException {
        org.qubership.atp.dataset.model.DataSet dsBefore = dataSetService.get(dataSetToDelete.getId());
        Attribute attribute = attributeService.create(datasetList1.getId(), 0, "TextAttr", AttributeType.TEXT, null, null);
        parameterService.create(dataSetToDelete.getId(), attribute.getId(), null, null, null);

        dataSetService.delete(dataSetToDelete.getId());
        org.qubership.atp.dataset.model.DataSet dsAfter = dataSetService.get(dataSetToDelete.getId());

        assertEquals(0, dsBefore.getParameters().size());
        assertNotNull(dsBefore);
        assertNull(dsAfter);
    }

    @Test
    public void delete_deleteDataSetWithEmptyRefParameter_dataSetWasDeletedInDB() throws DuplicateKeyException {
        org.qubership.atp.dataset.model.DataSet dsBefore = dataSetService.get(dataSetToDelete.getId());
        Attribute attribute = attributeService.create(datasetList1.getId(), 0, "RefAttr", AttributeType.DSL, null, null);
        parameterService.create(dataSetToDelete.getId(), attribute.getId(), null, null, null);

        dataSetService.delete(dataSetToDelete.getId());
        org.qubership.atp.dataset.model.DataSet dsAfter = dataSetService.get(dataSetToDelete.getId());

        assertEquals(0, dsBefore.getParameters().size());
        assertNotNull(dsBefore);
        assertNull(dsAfter);
    }

    @Test
    public void delete_deleteDataSetWithEmptyTextAttribute_dataSetWasDeletedInDB() throws DuplicateKeyException {
        org.qubership.atp.dataset.model.DataSet dsBefore = dataSetService.get(dataSetToDelete.getId());

        dataSetService.delete(dataSetToDelete.getId());
        org.qubership.atp.dataset.model.DataSet dsAfter = dataSetService.get(dataSetToDelete.getId());

        assertEquals(0, dsBefore.getParameters().size());
        assertNotNull(dsBefore);
        assertNull(dsAfter);
    }

    @Test
    public void copy_copyDataSetWithEmptyRefParam_newDataSetHasNullRefParameter() throws DuplicateKeyException {
        dataSetService.create(dslId, "DS_Test").getId();
        Attribute refAttribute = attributeService.create(dslId, 0, "RefAttr", AttributeType.DSL, dataSetList2.getId(), null);

        org.qubership.atp.dataset.model.DataSet dataSetCopy = dataSetService.copy(dataSet1.getId(), "DS_Test_Copy");

        assertNull(parameterService.getByDataSetIdAttributeId(dataSetCopy.getId(), refAttribute.getId()));
    }

    @Test
    public void copy_copyDataSetWithEmptyTextParam_newDataSetHasNullTextParameter() throws DuplicateKeyException {
        dataSetService.create(dslId, "DS_Test").getId();
        Attribute textAttribute = attributeService.create(dslId, 0, "TextAttr", AttributeType.TEXT, null, null);

        org.qubership.atp.dataset.model.DataSet dataSetCopy = dataSetService.copy(dataSet1.getId(), "DS_Test_Copy");

        assertNull(parameterService.getByDataSetIdAttributeId(dataSetCopy.getId(), textAttribute.getId()));
    }

    @Test
    public void restore_deleteDataSetAndRestoreDataSet_dataSetRestoredSuccessfully() throws DuplicateKeyException, IOException {
        DataSetList dsl = dataSetListService.create(vaId, "DSL1", null);
        org.qubership.atp.dataset.model.DataSet ds1 = dataSetService.create(dsl.getId(), "DS1");
        org.qubership.atp.dataset.model.DataSet ds2 = dataSetService.create(dsl.getId(), "DS2");
        Attribute textAttribute = attributeService.create(dsl.getId(), 0, "TextAttr", AttributeType.TEXT, null, null);
        Parameter parameter1 = parameterService.create(ds1.getId(), textAttribute.getId(), "Text1", null, null);
        Parameter parameter2 = parameterService.create(ds2.getId(), textAttribute.getId(), "Text2", null, null);
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String expectedJson = "{"
                + "\"dataSetList\": \"" + dsl.getId() + "\","
                + "\"id\": \"" + ds2.getId() + "\","
                + "\"name\": \"" + ds2.getName() + "\","
                + "\"previousDataSet\": \"" + ds1.getId() + "\","
                + "\"parameters\": [{"
                + "\"attribute\": \"" + textAttribute.getId() + "\","
                + "\"text\": \"Text2\","
                + "\"attrKey\": []"
                + "}]"
                + "}";
        JsonNode dataSetJson = mapper.readTree(expectedJson);

        dataSetService.delete(ds2.getId());

        assertNotNull(dataSetService.get(ds1.getId()));
        assertNull(dataSetService.get(ds2.getId()));

        dataSetService.restore(dataSetJson);
        org.qubership.atp.dataset.model.DataSet newDs = dataSetService.get(ds2.getId());

        assertNotNull(dataSetService.get(ds1.getId()));
        assertNotNull(dataSetService.get(ds2.getId()));

        assertEquals(dsl.getId(), newDs.getDataSetList().getId());
        assertEquals(1, newDs.getParameters().size());
        assertEquals(textAttribute.getId(), newDs.getParameters().get(0).getAttribute().getId());
        assertEquals("Text2", newDs.getParameters().get(0).getText());
        assertEquals(newDs.getId(), newDs.getParameters().get(0).getDataSet().getId());
        assertEquals(0, newDs.getLabels().size());

        DataSet dpaDs1 = jpaDataSetService.getById(ds1.getId());
        DataSet dpaDs2 = jpaDataSetService.getById(ds2.getId());
        assertTrue(dpaDs2.ordering() > dpaDs1.ordering());
    }

    @Test
    public void restore_deleteDataSetAndRestoreDataSet_childDataSetNotChanged() throws DuplicateKeyException, IOException {
        DataSetList dsl1 = dataSetListService.create(vaId, "DSL1", null);
        DataSetList dsl2 = dataSetListService.create(vaId, "DSL2", null);
        Attribute textAttribute = attributeService.create(dsl1.getId(), 0, "TextAttr", AttributeType.TEXT, null, null);
        Attribute refAttribute = attributeService.create(dsl2.getId(), 0, "RefAttr", AttributeType.DSL, dsl2.getId(), null);
        org.qubership.atp.dataset.model.DataSet dsChild = dataSetService.create(dsl1.getId(), "dsChild");
        org.qubership.atp.dataset.model.DataSet dsParent = dataSetService.create(dsl2.getId(), "dsParent");
        Parameter parameter1 = parameterService.create(dsChild.getId(), textAttribute.getId(), "Text", null, null);
        Parameter parameter2 = parameterService.create(dsParent.getId(), textAttribute.getId(), null, null, dsChild.getId());
        int numberOfParams = dsChild.getParameters().size();
        String value = parameter1.getText();
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String expectedJson = "{"
                + "\"dataSetList\": \"" + dsl1.getId() + "\","
                + "\"id\": \"" + dsParent.getId() + "\","
                + "\"name\": \"" + dsParent.getName() + "\","
                + "\"previousDataSet\": \"" + dsChild.getId() + "\","
                + "\"parameters\": [{"
                + "\"attribute\": \"" + refAttribute.getId() + "\","
                + "\"text\": \"\","
                + "\"attrKey\": []"
                + "}]"
                + "}";
        JsonNode dataSetJson = mapper.readTree(expectedJson);
        dataSetService.delete(dsParent.getId());

        dataSetService.restore(dataSetJson);

        assertEquals(numberOfParams, dsChild.getParameters().size());
        assertEquals(value, dsChild.getParameters().get(0).getText());
    }

    @Test
    public void collectAffectedDatasetsByDsId() {
        // given
        DataSetList dsl1 = dataSetListService.create(vaId, "DSL1", null);
        org.qubership.atp.dataset.model.DataSet ds1 = dataSetService.create(dsl1.getId(), "DS1");
        DataSetList dsl2 = dataSetListService.create(vaId, "DSL2", null);
        org.qubership.atp.dataset.model.DataSet ds2 = dataSetService.create(dsl2.getId(), "DS2");
        DataSetList dsl3 = dataSetListService.create(vaId, "DSL3", null);
        org.qubership.atp.dataset.model.DataSet ds3 = dataSetService.create(dsl2.getId(), "DS3");
        Attribute refAttr1 = attributeService.create(dsl2.getId(), 0, "ref_attr", AttributeType.DSL, dsl1.getId(), null);
        Parameter parameter1 = parameterService.create(ds2.getId(), refAttr1.getId(), null, null, ds1.getId());
        Attribute refAttr2 = attributeService.create(dsl3.getId(), 0, "ref_attr", AttributeType.DSL, dsl2.getId(), null);
        Parameter parameter2 = parameterService.create(ds3.getId(), refAttr2.getId(), null, null, ds2.getId());

        // then
        Set<UUID> result = dataSetService.collectAffectedDatasetsByDsId(ds1.getId());
        assertEquals(3, result.size());
        assertTrue(result.contains(ds1.getId()));
        assertTrue(result.contains(ds2.getId()));
        assertTrue(result.contains(ds3.getId()));
    }

    @Test
    public void collectAffectedDatasetsByDslId() {
        // given
        DataSetList dsl1 = dataSetListService.create(vaId, "DSL1", null);
        org.qubership.atp.dataset.model.DataSet ds1_1 = dataSetService.create(dsl1.getId(), "DS1_1");
        org.qubership.atp.dataset.model.DataSet ds1_2 = dataSetService.create(dsl1.getId(), "DS1_2");
        DataSetList dsl2 = dataSetListService.create(vaId, "DSL2", null);
        org.qubership.atp.dataset.model.DataSet ds2 = dataSetService.create(dsl2.getId(), "DS2");
        DataSetList dsl3 = dataSetListService.create(vaId, "DSL3", null);
        org.qubership.atp.dataset.model.DataSet ds3 = dataSetService.create(dsl2.getId(), "DS3");
        Attribute refAttr1 = attributeService.create(dsl2.getId(), 0, "ref_attr", AttributeType.DSL, dsl1.getId(), null);
        Parameter parameter1 = parameterService.create(ds2.getId(), refAttr1.getId(), null, null, ds1_1.getId());
        Attribute refAttr2 = attributeService.create(dsl3.getId(), 0, "ref_attr", AttributeType.DSL, dsl2.getId(), null);
        Parameter parameter2 = parameterService.create(ds3.getId(), refAttr2.getId(), null, null, ds2.getId());

        // then
        Set<UUID> result = dataSetService.collectAffectedDatasetsByDslId(dsl1.getId());
        assertEquals(4, result.size());
        assertTrue(result.contains(ds1_1.getId()));
        assertTrue(result.contains(ds1_2.getId()));
        assertTrue(result.contains(ds2.getId()));
        assertTrue(result.contains(ds3.getId()));
    }
}

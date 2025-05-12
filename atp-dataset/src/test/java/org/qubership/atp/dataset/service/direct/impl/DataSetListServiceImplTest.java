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

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableList;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.repositories.JpaAttributeRepository;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.DatasetResponse;
import org.qubership.atp.dataset.service.direct.DuplicateKeyException;
import org.qubership.atp.dataset.service.rest.dto.manager.AffectedDataSetList;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.qubership.atp.macros.core.clients.api.dto.macros.MacrosDto;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.client.MacrosFeignClient;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {
        "feign.atp.macros.url=",
        "feign.atp.macros.route=",
        "atp-dataset.javers.enabled=false"
})
public class DataSetListServiceImplTest extends DataSetBuilder {

    @MockBean
    private DataSetListSnapshotService dataSetListSnapshotService;
    @MockBean
    private JpaAttributeRepository attributeRepository;
    @MockBean
    private MacrosCalculator macrosCalculator;
    @MockBean
    private MacrosFeignClient macrosFeignClient;
    @Autowired
    protected ModelsProvider modelsProvider;
    @Mock
    MacrosDto macrosDto;

    @AfterEach
    public void tearDown() throws Exception {
        visibilityAreaService.delete(vaId);
    }

    @Test
    public void testRenameDslReplaceRefs() {
        parameterService.update(source.getId(), "#REF_DSL(DSL.DS.ATTR)");
        dataSetListService.rename(dataSetList.getId(), "SZ_Updated");
        assertEquals("#REF_DSL(SZ_Updated.DS.ATTR)",
                wrapperService.unWrapAlias(parameterService.get(source.getId()).getText()));
    }

    @Test
    public void testRenameDslReplaceRefsInCurrentDsl() {
        parameterService.update(source.getId(), "#REF_DSL(DSL.DS.ATTR)");
        dataSetListService.rename(dataSetList.getId(), "SZ_Updated");
        String wrappedText = parameterService.get(source.getId()).getText();
        assertEquals("#REF_DSL(SZ_Updated.DS.ATTR)", wrapperService.unWrapAlias(wrappedText));
    }

    @Disabled
    @Test
    public void testRenameDslReplaceRef() {
        parameterService.update(source.getId(), "#REF(DS.DSL.ATTR)");
        dataSetListService.rename(dataSetList.getId(), "SZ_Updated");
        String wrappedText = parameterService.get(source.getId()).getText();
        assertEquals("#REF(DS.SZ_Updated.ATTR)", wrappedText);
    }

    @Disabled
    @Test
    public void testRenameDslWillNotReplaceRefsInCurrentDslDslNameEntriesInAnotherDslName() {
        dataSetListService.rename(dataSetList.getId(), "SZ_Updated");
        assertEquals("#REF_DSL(DS.Input1.Attribute)", source.getText());
    }

    @Disabled
    @Test
    public void testRenameDslWillReplaceFewRefs() {
        source.setText("#REF_DSL(DS.Input.Attribute)REF_Abrakadabra#REF(DS.Input.Attribute)");
//        dslService.rename(UUID.randomUUID(), "SZ_Updated");
        assertEquals("#REF_DSL(DS.SZ_Updated.Attribute)REF_Abrakadabra#REF(DS.SZ_Updated.Attribute)"
                , source.getText());
    }

    @Test
    public void addTestPlanToDataSetList_testPlanWasAdded() {
        TestPlan testPlan = testPlanService.create(vaId, "newTP").getFirst();
        dataSetListService.modify(dataSetList.getId(), dataSetList.getName(), testPlan.getId(), false);
        DataSetList dsl = dataSetListService.get(dataSetList.getId());
        assertEquals("newTP", dsl.getTestPlan().getName());
    }

    @Test
    public void clearTestPlanInDataSetList_testPlanWasDeleted() {
        TestPlan testPlan = testPlanService.create(vaId, "TPtoDelete").getFirst();
        dataSetListService.modify(dataSetList.getId(), dataSetList.getName(), testPlan.getId(), false);
        dataSetListService.modify(dataSetList.getId(), dataSetList.getName(), null, true);
        DataSetList dsl = dataSetListService.get(dataSetList.getId());
        assertNull(dsl.getTestPlan());
    }

    @Test
    public void testDeleteDataSetListCascade() throws DuplicateKeyException {
        DataSetList dsl = dataSetListService.create(vaId, "MustBeRemoved", null);
        DataSet dataSet = dataSetService.create(dsl.getId(), "MustBeRemoved");
        Attribute attribute = attributeService
                .create(dsl.getId(), 0, "MustBeRemoved", AttributeType.TEXT, null, null);
        Parameter parameter = parameterService.create(dataSet.getId(), attribute.getId(), "Text", null, null);
        dataSetListService.delete(dsl.getId());
        Assertions.assertNull(dataSetListService.get(dsl.getId()));
        Assertions.assertNull(dataSetService.get(dataSet.getId()));
        Assertions.assertNull(attributeService.get(attribute.getId()));
        Assertions.assertNull(parameterService.get(parameter.getId()));
        List<VisibilityArea> all = visibilityAreaService.getAll();
        VisibilityArea area = all.stream().filter(va -> va.getId().equals(vaId))
                .findFirst().orElseThrow(() -> new AssertionError("VisibilityArea not found"));
        assertNotNull(area);
    }

    @Test
    public void testCopyDsl_withTextAndDslRefAndOverlapAttrs_dslWasCopiedWithOrWithoutDataInTestPlan() throws Exception {
        TestPlan testPlanToCopy = testPlanService.create(vaId, "TP").getFirst();
        UUID postalCode = dataSetListService.create(vaId, "PostalCode", null).getId();
        DataSet pc1 = dataSetService.create(postalCode, "PC#1");

        UUID postalCode2 = dataSetListService.create(vaId, "PostalCode2", null).getId();
        DataSet pc12 = dataSetService.create(postalCode2, "PC#2");

        Attribute attributeToOverlap = attributeService
                .create(postalCode, 0, "text", AttributeType.TEXT, null, null);
        Parameter parameterToOverlap = parameterService.create(dataSet1.getId(), attributeToOverlap.getId(),
                "original", null, null);

        DataSetList dslToCopy = dataSetListService.create(vaId, "toCopy", null);
        DataSet dataSet = dataSetService.create(dslToCopy.getId(), "ds");
        Attribute attributeText = attributeService
                .create(dslToCopy.getId(), 1, "textAttr", AttributeType.TEXT, null,
                        null);
        Parameter parameterText = parameterService.create(dataSet.getId(), attributeText.getId(), "Text",
                null, null);
        Attribute pcAttrRef2 = attributeService.create(dslToCopy.getId(), 2, "PostalCodeRef2",
                AttributeType.DSL, postalCode2, null);
        Parameter pcParamRef2 = parameterService.create(dataSet.getId(), pcAttrRef2.getId(), null,
                null, pc12.getId());

        Attribute pcAttrRef = attributeService.create(postalCode2, 3, "PostalCodeRef",
                AttributeType.DSL, postalCode, null);
        Parameter pcParamRef = parameterService.create(pc12.getId(), pcAttrRef.getId(), null,
                null, pc1.getId());

        parameterService.set(dataSet.getId(), attributeToOverlap.getId(), ImmutableList.of(pcAttrRef2.getId()),
                "Overlapped", null, null);

        DataSetList withData = dataSetListService.copy(vaId, dslToCopy.getId(), "dslWithData",
                true, testPlanToCopy.getId());
        DataSetList withoutData = dataSetListService.copy(vaId, dslToCopy.getId(), "dslWithoutData",
                false, testPlanToCopy.getId());

        assertNotNull(withoutData);
        assertEquals(0, withoutData.getDataSets().size());

        assertNotNull(withData);
        assertEquals(1, withData.getDataSets().size());

        List<Attribute> attrs = withoutData.getAttributes();
        assertEquals(attrs.get(0).getName(), "textAttr");
        assertEquals(attrs.get(0).getType(), AttributeType.TEXT);
        assertEquals(attrs.get(1).getName(), "PostalCodeRef2");
        assertEquals(attrs.get(1).getType(), AttributeType.DSL);

        List<Attribute> attrsWD = withData.getAttributes();
        assertEquals(attrsWD.get(0).getName(), "textAttr");
        assertEquals(attrsWD.get(0).getType(), AttributeType.TEXT);
        assertEquals(attrsWD.get(1).getName(), "PostalCodeRef2");
        assertEquals(attrsWD.get(1).getType(), AttributeType.DSL);

        assertEquals(withData.getTestPlan().getName(), "TP");
        assertEquals(withoutData.getTestPlan().getName(), "TP");
    }

    @Test
    public void getDatasetsIdsWithItsNameAndDatasetList_correct_StructureWereReturned() throws DuplicateKeyException {
        DataSetList dslWithTwoDatasets = dataSetListService.create(vaId, "dslWith2ds", null);
        DataSet ds1InsideDslWith2ds = dataSetService.create(dslWithTwoDatasets.getId(), "ds1InsideDslWith2ds");
        DataSet ds2InsideDslWith2ds = dataSetService.create(dslWithTwoDatasets.getId(), "ds2InsideDslWith2ds");

        DataSetList dslWithOneDataSet = dataSetListService.create(vaId, "dslWith1ds", null);
        DataSet dsInsideDslWith1ds = dataSetService.create(dslWithOneDataSet.getId(), "dsInsideDslWith1ds");

        DataSetList dslWithoutDataSets = dataSetListService.create(vaId, "dslWithoutDatasets", null);

        List<DatasetResponse> result = dataSetListService.getListOfDsIdsAndNameAndDslId(
                asList(dslWithTwoDatasets.getId(), dslWithOneDataSet.getId(), dslWithoutDataSets.getId()));

        assertEquals(3, result.size(), "Result doesn't have 3 elements");

        assertThat(
                result
                        .stream()
                        .map(DatasetResponse::getDataSetName)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(
                        ds1InsideDslWith2ds.getName(),
                        ds2InsideDslWith2ds.getName(),
                        dsInsideDslWith1ds.getName()
                )
        );

        assertThat(
                result
                        .stream()
                        .map(DatasetResponse::getDataSetListId)
                        .collect(Collectors.toSet()),
                containsInAnyOrder(
                        dslWithTwoDatasets.getId(),
                        dslWithOneDataSet.getId()
                )
        );
    }

    @Test
    public void getDatasetsIdsWithItsNameAndDatasetList_correct_StructureWereReturnedWithSkippedNotFoundDsl() throws DuplicateKeyException {
        DataSetList dsl = dataSetListService.create(vaId, "dslist", null);
        DataSet dataSet1 = dataSetService.create(dsl.getId(), "ds1");

        List<DatasetResponse> result = dataSetListService.getListOfDsIdsAndNameAndDslId(
                asList(dsl.getId(), UUID.randomUUID()));

        assertEquals(1, result.size(), "Result have 1 element");

        assertEquals(result.get(0).getDataSetName(), dataSet1.getName());

        assertEquals(result.get(0).getDataSetListId(), dsl.getId());
        assertEquals(result.get(0).getDataSetListName(), dsl.getName());
    }

    @Test
    public void getFilteredDatasets_datasetsWereFilteredById() throws DuplicateKeyException {
        DataSetList dsl = dataSetListService.create(vaId, "dsl", null);
        DataSet dataSet1 = dataSetService.create(dsl.getId(), "ds1");
        DataSet dataSet2 = dataSetService.create(dsl.getId(), "ds2");
        DataSet dataSet3 = dataSetService.create(dsl.getId(), "ds3");
        DataSet dataSet4 = dataSetService.create(dsl.getId(), "ds4");

        UiManDataSetList resultBefore = dataSetListService.getAsTree(dsl.getId(), false);
        assertEquals(4, resultBefore.getDataSets().size());

        List<UUID> filter = asList(dataSet1.getId(), dataSet2.getId());
        UiManDataSetList resultAfter = dataSetListService.getAsTree(dsl.getId(), false, filter, false);

        assertEquals(2, resultAfter.getDataSets().size());
        assertEquals(dataSet1.getId(), resultAfter.getDataSets().get(0).getId());
        assertEquals(dataSet2.getId(), resultAfter.getDataSets().get(1).getId());
    }

    @Test
    public void getFilteredDatasets_datasetsWereFilteredByAttributes() throws DuplicateKeyException {
        DataSetList dsl = dataSetListService.create(vaId, "dsl", null);
        DataSet dataSet1 = dataSetService.create(dsl.getId(), "ds1");
        Attribute attribute1 = attributeService
                .create(dsl.getId(), 0, "text1", AttributeType.TEXT, null, null);
        Attribute attribute2 = attributeService
                .create(dsl.getId(), 1, "text2", AttributeType.TEXT, null, null);
        Attribute attribute3 = attributeService
                .create(dsl.getId(), 2, "text3", AttributeType.TEXT, null, null);
        Attribute attribute4 = attributeService
                .create(dsl.getId(), 3, "text4", AttributeType.TEXT, null, null);

        Parameter parameter1 = parameterService.create(dataSet1.getId(), attribute1.getId(),
                "1", null, null);
        Parameter parameter2 = parameterService.create(dataSet1.getId(), attribute2.getId(),
                "2", null, null);
        Parameter parameter3 = parameterService.create(dataSet1.getId(), attribute3.getId(),
                "3", null, null);
        Parameter parameter4 = parameterService.create(dataSet1.getId(), attribute4.getId(),
                "4", null, null);

        UiManDataSetList resultBefore = dataSetListService.getAsTree(dsl.getId(), false);
        assertEquals(4, resultBefore.getAttributes().size());

        List<UUID> filter = asList(attribute1.getId(), attribute2.getId());
        UiManDataSetList resultAfter = dataSetListService.getAsTree(dsl.getId(), false, null, filter, null, null,
                false, true);

        assertEquals(2, resultAfter.getAttributes().size());
        assertEquals(attribute1.getId(), resultAfter.getAttributes().get(0).getId());
        assertEquals(attribute2.getId(), resultAfter.getAttributes().get(1).getId());
    }

    @Test
    public void getFilteredDatasets_datasetsWereFilteredByIdAndAttributes() throws DuplicateKeyException {
        DataSetList dsl = dataSetListService.create(vaId, "dsl", null);

        DataSet dataSet1 = dataSetService.create(dsl.getId(), "ds1");
        DataSet dataSet2 = dataSetService.create(dsl.getId(), "ds2");
        DataSet dataSet3 = dataSetService.create(dsl.getId(), "ds3");
        DataSet dataSet4 = dataSetService.create(dsl.getId(), "ds4");

        Attribute attribute1 = attributeService
                .create(dsl.getId(), 0, "text1", AttributeType.TEXT, null, null);
        Attribute attribute2 = attributeService
                .create(dsl.getId(), 1, "text2", AttributeType.TEXT, null, null);
        Attribute attribute3 = attributeService
                .create(dsl.getId(), 2, "text3", AttributeType.TEXT, null, null);
        Attribute attribute4 = attributeService
                .create(dsl.getId(), 3, "text4", AttributeType.TEXT, null, null);

        UiManDataSetList resultBefore = dataSetListService.getAsTree(dsl.getId(), false);
        assertEquals(4, resultBefore.getAttributes().size());
        assertEquals(4, resultBefore.getDataSets().size());


        List<UUID> filterDs = asList(dataSet1.getId(), dataSet2.getId());
        List<UUID> filter = asList(attribute1.getId(), attribute2.getId());

        UiManDataSetList resultAfter = dataSetListService.getAsTree(dsl.getId(), false, filterDs, filter, null, null,
                false, true);
        assertEquals(2, resultAfter.getAttributes().size());
        assertEquals(2, resultAfter.getDataSets().size());
        assertEquals(dataSet1.getId(), resultAfter.getDataSets().get(0).getId());
        assertEquals(dataSet2.getId(), resultAfter.getDataSets().get(1).getId());
        assertEquals(attribute1.getId(), resultAfter.getAttributes().get(0).getId());
        assertEquals(attribute2.getId(), resultAfter.getAttributes().get(1).getId());
    }

    @Test
    public void getAffectedAttributesByDatasetListId_returnResponse() {
        DataSetList toDelete = dataSetListService.create(vaId, "dsl", null);

        DataSetListEntity dsl1 = new DataSetListEntity();
        dsl1.setId(UUID.randomUUID());
        dsl1.setName("dsl 1");

        DataSetListEntity dsl2 = new DataSetListEntity();
        dsl2.setId(UUID.randomUUID());
        dsl2.setName("dsl 2");

        AttributeEntity attribute1 = new AttributeEntity();
        attribute1.setId(UUID.randomUUID());
        attribute1.setName("attr 1");
        attribute1.setDataSetList(dsl1);

        AttributeEntity attribute2 = new AttributeEntity();
        attribute2.setId(UUID.randomUUID());
        attribute2.setName("attr 2");
        attribute2.setDataSetList(dsl2);

        when(attributeRepository.getByTypeDataSetListId(any(), any()))
                .thenReturn(new PageImpl<>(asList(attribute1, attribute2)));

        List<TableResponse> responses =
                dataSetListService.getAffectedAttributes(toDelete.getId(), null, null).getEntities();

        assertThat(responses, hasSize(2));
        assertThat(responses, hasItem(hasProperty("attributeId", equalTo(attribute1.getId()))));
        assertThat(responses, hasItem(hasProperty("attributeId", equalTo(attribute2.getId()))));
    }

    @Test
    public void testGetAffectedDataSetLists_shouldReturnsEmptyList_whenDataSetListIsNotExists() {
        List<AffectedDataSetList> affectedDataSetLists =
                dataSetListService.getAffectedDataSetLists(UUID.randomUUID(), 1, 0);

        assertThat(affectedDataSetLists, hasSize(0));
    }

    @Test
    public void testGetAffectedDataSetLists_shouldReturnsListWithOneElement_whenDataSetListIsExists() {
        DataSetList toDelete = dataSetListService.create(vaId, "dsl", null);

        DataSetList dsl1 = dataSetListService.create(vaId, "dsl1", null);
        DataSetList dsl2 = dataSetListService.create(vaId, "dsl2", null);

        attributeService.create(dsl1.getId(), 0, "dsl", AttributeType.DSL, toDelete.getId(), null);
        attributeService.create(dsl2.getId(), 0, "dsl", AttributeType.DSL, toDelete.getId(), null);

        List<AffectedDataSetList> affectedDataSetLists =
                dataSetListService.getAffectedDataSetLists(toDelete.getId(), 1, 0);

        assertThat(affectedDataSetLists, hasSize(1));
        assertThat(dsl1.getId(), equalTo(affectedDataSetLists.get(0).getDslId()));
        assertThat(dsl1.getName(), equalTo(affectedDataSetLists.get(0).getDslName()));
    }

    @Test
    public void testGetAffectedDataSetLists_shouldReturnsEmptyList_whenDataSetListIsExistsAndOffsetMoreThanCount() {
        DataSetList toDelete = dataSetListService.create(vaId, "dsl", null);

        DataSetList dsl1 = dataSetListService.create(vaId, "dsl1", null);
        DataSetList dsl2 = dataSetListService.create(vaId, "dsl2", null);

        attributeService.create(dsl1.getId(), 0, "dsl", AttributeType.DSL, toDelete.getId(), null);
        attributeService.create(dsl2.getId(), 0, "dsl", AttributeType.DSL, toDelete.getId(), null);

        List<AffectedDataSetList> affectedDataSetLists =
                dataSetListService.getAffectedDataSetLists(toDelete.getId(), 1, 10);

        assertThat(affectedDataSetLists, hasSize(0));
    }

    @Test
    public void testGetAffectedDataSetLists_shouldThrowAnException_whenLimitIsNegative() {
        DataSetList toDelete = dataSetListService.create(vaId, "dsl", null);
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            dataSetListService.getAffectedDataSetLists(toDelete.getId(), -1, 0);
        });
    }

    @Test
    public void testGetAffectedDataSetLists_shouldThrowAnException_whenOffsetIsNegative() {
        DataSetList toDelete = dataSetListService.create(vaId, "dsl", null);
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            dataSetListService.getAffectedDataSetLists(toDelete.getId(), 1, -1);
        });
    }

    @Test
    public void testCopyDslWithDatasets_dslWasCopiedWithoutTestPlan() throws Exception {

        UUID postalCode = dataSetListService.create(vaId, "PostalCode", null).getId();
        DataSet pc1 = dataSetService.create(postalCode, "PC#1");

        UUID postalCode2 = dataSetListService.create(vaId, "PostalCode2", null).getId();
        DataSet pc12 = dataSetService.create(postalCode2, "PC#2");

        Attribute attributeToOverlap = attributeService
                .create(postalCode, 0, "text", AttributeType.TEXT, null, null);
        parameterService.create(dataSet1.getId(), attributeToOverlap.getId(),
                "original", null, null);

        DataSetList dslToCopy = dataSetListService.create(vaId, "toCopy", null);
        DataSet dataSet = dataSetService.create(dslToCopy.getId(), "ds");

        DataSetList dslToCopy2 = dataSetListService.create(vaId, "toCopy2", null);
        DataSet dataSet2 = dataSetService.create(dslToCopy2.getId(), "ds2");
        DataSet dataSet3 = dataSetService.create(dslToCopy2.getId(), "ds3");
        DataSet dataSet4 = dataSetService.create(dslToCopy2.getId(), "ds4");

        Attribute attributeText = attributeService
                .create(dslToCopy.getId(), 1, "textAttr", AttributeType.TEXT, null,
                        null);
        parameterService.create(dataSet.getId(), attributeText.getId(), "Text",
                null, null);
        Attribute pcAttrRef2 = attributeService.create(dslToCopy.getId(), 2, "PostalCodeRef2",
                AttributeType.DSL, postalCode2, null);
        parameterService.create(dataSet.getId(), pcAttrRef2.getId(), null,
                null, pc12.getId());

        Attribute pcAttrRef = attributeService.create(postalCode2, 3, "PostalCodeRef",
                AttributeType.DSL, postalCode, null);
        parameterService.create(pc12.getId(), pcAttrRef.getId(), null,
                null, pc1.getId());

        parameterService.set(dataSet.getId(), attributeToOverlap.getId(), ImmutableList.of(pcAttrRef2.getId()),
                "Overlapped", null, null);

        Map<UUID, Set<UUID>> structureToCopy = new HashMap<>();
        structureToCopy.put(dslToCopy.getId(), new HashSet<>(asList(dataSet.getId())));
        structureToCopy.put(dslToCopy2.getId(), new HashSet<>(asList(dataSet2.getId(), dataSet3.getId())));

        //old ds - pair of new ds, new dsl
        Map<UUID, Pair<UUID, UUID>> result = dataSetListService.copy("prefix", structureToCopy);

        assertNotNull(result);
        assertEquals(3, result.size());

        DataSetList resultDsl1 = dataSetListService.get(result.get(dataSet.getId()).getValue());

        assertEquals(2, dataSetListService.get(result.get(dataSet2.getId()).getValue()).getDataSets().size(),
                "second dsl has only 2 datasets");

        List<Attribute> attrs = resultDsl1.getAttributes();
        assertEquals( "textAttr", attrs.get(0).getName());
        assertEquals(AttributeType.TEXT, attrs.get(0).getType());
        assertEquals("PostalCodeRef2", attrs.get(1).getName());
        assertEquals( AttributeType.DSL, attrs.get(1).getType());

        Assertions.assertNull(resultDsl1.getTestPlan());
    }

    @Test
    public void testExistsById_shouldReturnsFalse_whenDataSetListIsNotExists() {
        boolean exists = dataSetListService.existsById(UUID.randomUUID());

        assertThat(exists, equalTo(false));
    }

    @Test
    public void testExistsById_shouldReturnsTrue_whenDataSetListIsExists() {
        DataSetList dsl = dataSetListService.create(vaId, "dsl", null);

        boolean exists = dataSetListService.existsById(dsl.getId());

        assertThat(exists, equalTo(true));
    }

    @Test
    public void testExistsById_shouldThrowAnException_whenDataSetListIdIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            dataSetListService.existsById(null);
        });
    }

    @Test
    public void testGetDataSetListModifiedWhen_shouldReturnsNull_whenDataSetListIsNotExists() {
        Timestamp modifiedWhen = dataSetListService.getModifiedWhen(UUID.randomUUID());

        assertThat(modifiedWhen, is(nullValue()));
    }

    @Test
    public void testGetDataSetListModifiedWhen_shouldReturnsNull_whenDataSetListModifiedWhenIsNotExists() {
        DataSetList dsl = dataSetListService.create(vaId, "dsl", null);

        Timestamp modifiedWhen = dataSetListService.getModifiedWhen(dsl.getId());

        long modifiedWhenActual = modifiedWhen.getTime();
        long modifiedWhenExpected = dsl.getModifiedWhen().getTime();

        assertThat(modifiedWhenActual, equalTo(modifiedWhenExpected));
    }

    @Test
    public void testGetDataSetListModifiedWhen_shouldThrowAnException_whenDataSetListIdIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            dataSetListService.getModifiedWhen(null);
        });

    }

    @Test
    public void testDelete_shouldRollbackTransaction_whenExceptionIsOccured() {
        doThrow(RuntimeException.class).when(dataSetListSnapshotService).deleteDataSetList(any());
        try {
            dataSetListService.delete(dataSetList.getId());
        } catch (Exception ignored) {}
        assertNotNull(dataSetListService.get(dataSetList.getId()));
    }

    @Test
    public void test_UiManDataSetList_whenMacrosAtpIs() throws DuplicateKeyException {
        ReflectionTestUtils.setField(dataSetListService, "macroFeignUrl", "testUrl");
        VisibilityArea area = visibilityAreaService.create("TestVA");
        DataSetList dsl = dataSetListService.create(area.getId(), "dsl", null);
        DataSet dataSet1 = dataSetService.create(dsl.getId(), "ds1");
        DataSet dataSet2 = dataSetService.create(dsl.getId(), "ds2");
        Attribute attribute1 = attributeService
                .create(dsl.getId(), 0, "macros1",
                AttributeType.TEXT, null, null);
        Attribute attribute2 = attributeService
                .create(dsl.getId(), 1, "macros2",
                        AttributeType.TEXT, null, null);
        Parameter parameter1 = parameterService.create(dataSet1.getId(), attribute1.getId(),
                "32#RANDOMBETWEEN(31,31)", null, null);
        Parameter parameter2 = parameterService.create(dataSet2.getId(), attribute1.getId(),
                "Simple text", null, null);
        Parameter parameter3 = parameterService.create(dataSet1.getId(), attribute2.getId(),
                "Simple text2", null, null);
        Parameter parameter4 = parameterService.create(dataSet2.getId(), attribute2.getId(),
                "69#RANDOMBETWEEN(1,1)", null, null);
        Mockito.when(macrosFeignClient.findAllByProject(any(UUID.class)))
                .thenReturn( new ResponseEntity<>(Collections.singletonList(macrosDto), HttpStatus.OK));
        Mockito.when(macrosCalculator.calculate(any(), any(), any())).thenReturn("OK_MACRO_VALUE");
        Mockito.when(macrosDto.getName()).thenReturn("RANDOMBETWEEN");

        UiManDataSetList dataSetListUi = dataSetListService.getAsTree(
                dsl.getId(), true, null, null, null, null, false, true);

        assertEquals("32OK_MACRO_VALUE", dataSetListUi.getAttributes().get(0).getParameters().get(0).getValue());
        assertEquals("Simple text", dataSetListUi.getAttributes().get(0).getParameters().get(1).getValue());
        assertEquals("Simple text2", dataSetListUi.getAttributes().get(1).getParameters().get(0).getValue());
        assertEquals("69OK_MACRO_VALUE", dataSetListUi.getAttributes().get(1).getParameters().get(1).getValue());

        visibilityAreaService.delete(area.getId());
    }

    @Test
    public void getAsTree_runEvaluateMacros_macrosParameterEvaluated() throws DuplicateKeyException {
        String str = "35#RANDOMBETWEEN(31,31)";
        DataSetList dsl = dataSetListService.create(vaId, "dsl", null);
        DataSet dataSet = dataSetService.create(dsl.getId(), "ds1");
        Attribute attribute = attributeService.create(dsl.getId(), 0, "Test", AttributeType.TEXT, null, null);
        parameterService.create(dataSet.getId(), attribute.getId(), str, null, null);

        UiManDataSetList resultBefore = dataSetListService.getAsTree(dsl.getId(), false);
        UiManDataSetList resultAfter = dataSetListService.getAsTree(dsl.getId(), true);

        assertTrue(resultBefore.getAttributes().get(0).getParameters().get(0).getValue().toString().contains(str));
        assertTrue(resultAfter.getAttributes().get(0).getParameters().get(0).getValue().toString().contains("3531"));
    }

    @Test
    public void mark_setLabelToDatasetList_dataSetListHasLabel() {
        String labelName = "Label";

        dataSetListService.mark(dslId, labelName);
        List<Label> labels = dataSetListService.get(dslId).getLabels();

        assertEquals(labelName, labels.get(0).getName());
    }

    @Test
    public void unmark_deleteLabelFromDatasetList_dataSetListHasNotLabel() {
        String labelName = "Label";
        Label label = dataSetListService.mark(dslId, labelName);

        dataSetListService.unmark(dslId, label.getId());
        List<Label> labels = dataSetListService.get(dslId).getLabels();

        assertEquals(0, labels.size());
    }

    @Test
    public void getAll_getDslOfNotExistedProject_DataSetListIsEmpty() {
        UUID vaUuid = UUID.fromString("31fdbadd-540c-4021-89e5-ad00880ba595");

        assertEquals(0, dataSetListService.getAll(vaUuid, null).size());
    }
}

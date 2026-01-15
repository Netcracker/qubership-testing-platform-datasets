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

package org.qubership.atp.dataset.service.direct.importexport.service;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.dataset.service.direct.importexport.utils.StreamUtils.extractIds;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.db.jpa.Wrapper;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeKeyEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.exception.excel.ExcelImportEmptyExcelException;
import org.qubership.atp.dataset.exception.excel.ExcelImportNotExistingAttributeException;
import org.qubership.atp.dataset.exception.excel.ExcelImportNotExistingRefParameterException;
import org.qubership.atp.dataset.exception.excel.ImportExcelNotEqualsAttributeTypeException;
import org.qubership.atp.dataset.exception.excel.ImportExcelNotSupportedAttributeTypeException;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.AttributeImpl;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.impl.ListValueImpl;
import org.qubership.atp.dataset.model.impl.ParameterImpl;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.importexport.converters.DatasetLinkAttributeImportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.EncryptedAttributeImportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.ListAttributeImportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.TextAttributeImportConverter;
import org.qubership.atp.dataset.service.direct.importexport.converters.XlsxToListConverter;
import org.qubership.atp.dataset.service.direct.importexport.exceptions.ImportFailedException;
import org.qubership.atp.dataset.service.direct.importexport.models.AttributeImportContext;
import org.qubership.atp.dataset.service.direct.importexport.utils.ImportUtils;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.delegates.AbstractObjectWrapper;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableMap;

/*  DSL 2                        DSL 3                        DSL 4
 * ------------------------------------------------------------------------------
 *  Attribute   DS 3    DS 4  |  Attr        DS 5    DS 6  | Attr   DS 7   DS 8 |
 * ---------------------------|----------------------------|--------------------|
 *  a           1       3     |  e           5       6     | f       4      5   |
 *  b           2       4     |  DSL 4 ref   DS 7    DS 8  |                    |
 *  enc         ****    ****  |                            |                    |
 *  country     UA      SP    |                            |                    |
 *  DSL 3 ref   DS 5    DS 6  |                            |                    |
 * ------------------------------------------------------------------------------
 */

//Target summary DSL
/*    DSL 1
 * ----------------------------------------
 *    Attribute         DS 1      DS 2    |
 * ----------------------------------------
 *    text_attr                   land    |
 *    text_attr2                  la      |
 *    text_attr2                  text    |
 *    list_attr         c                 |
 *    list_attr2        c         d       |
 *    enc_attr                    ****    |
 *    DSL 2 ref         DS 4              |
 *       a              1         3       |
 *       b              2         4       |
 *       enc            ****      ****    |
 *       country        UA        SP      |
 *       DSL 3 ref      DS 5      DS 6    |
 *          e           5         6       |
 *          DSL 4 ref   DS 7      DS 8    |
 *             f        4         5       |
 * ----------------------------------------
 */

//Result summary DSL after import Excel file
// Where * - overlap
/*    DSL 1
 * ----------------------------------------
 *    Attribute         DS 1      DS 2    |
 * ----------------------------------------
 *    text_attr                   land    |
 *    text_attr2                  land2   |
 *    text_attr3        text1             |
 *    list_attr         c                 |
 *    list_attr2        d                 |
 *    enc_attr                    ****    |
 *    DSL 2 ref         DS 3      DS 4    |
 *       a              1       * 4444    |
 *       b            * 3333      4       |
 *       enc          * ****    * ****    |
 *       country        UA      * FR      |
 *       DSL 3 ref      DS 5      DS 6    |
 *          e           5       * 666     |
 *          DSL 4 ref   DS 7      DS 8    |
 *             f        4       * 7       |
 * ----------------------------------------
 */

@Isolated
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class DatasetListImportServiceTest {

    @Mock
    private DataSetListImportExportFactory factory;

    @Mock
    private JpaDataSetListService dataSetListService;

    @Mock
    private JpaDataSetService dataSetService;

    @Mock
    private JpaAttributeService attributeService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AttributeImportContext importContext;

    @Mock
    private Attribute attribute;

    @Mock
    private AttributeKey attributeKey;

    @InjectMocks
    private DatasetListImportService importService;

    private UUID targetProjectId;
    private UUID targetDslId;

    private DataSetList dsl1, dsl2, dsl3, dsl4;

    private DataSet ds1, ds2, ds3, ds4, ds5, ds6, ds7, ds8;

    // DSL 1
    private Attribute text_attr, text_attr2, text_attr3, list_attr, list_attr2, enc_attr, dsl2Ref;

    // DSL 2
    private Attribute a, b, enc, country, dsl3Ref;

    // DSL 3
    private Attribute e, dsl4Ref;

    // DSL 4
    private Attribute f;

    private ListValue cListValue, dListValue, uaListValue, frListValue;

    private List<DataSet> dsl1Datasets;
    private List<DataSet> refDatasets;
    private List<DataSetList> refDatasetLists;
    private Set<UUID> refDslDatasetIds;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        ReflectionTestUtils.setField(Wrapper.class, "modelsProvider", new ModelsProvider());
        ReflectionTestUtils.setField(Wrapper.class, "entityManager", entityManager);

        targetProjectId = UUID.randomUUID();
        targetDslId = UUID.randomUUID();

        // Dataset lists
        dsl1 = generateDataSetList(targetDslId, "DSL 1");
        dsl2 = generateDataSetList("DSL 2");
        dsl3 = generateDataSetList("DSL 3");
        dsl4 = generateDataSetList("DSL 4");

        // Datasets
        ds1 = generateDataset("DS 1", dsl1);
        ds2 = generateDataset("DS 2", dsl1);
        ds3 = generateDataset("DS 3", dsl2);
        ds4 = generateDataset("DS 4", dsl2);
        ds5 = generateDataset("DS 5", dsl3);
        ds6 = generateDataset("DS 6", dsl3);
        ds7 = generateDataset("DS 7", dsl4);
        ds8 = generateDataset("DS 8", dsl4);

        dsl1Datasets = asList(ds1, ds2);
        setDatasets(dsl1, ds1, ds2);
        setDatasets(dsl2, ds3, ds4);
        setDatasets(dsl3, ds5, ds6);
        setDatasets(dsl4, ds7, ds8);

        List<DataSetList> projectDatasetLists = asList(dsl1, dsl2, dsl3, dsl4);

        text_attr = generateAttribute("text_attr", dsl1, 1);
        text_attr2 = generateAttribute("text_attr2", dsl1, 1);
        text_attr3 = generateAttribute("text_attr3", dsl1, 1);
        list_attr = generateAttribute("list_attr", dsl1, 3);
        list_attr2 = generateAttribute("list_attr2", dsl1, 3);
        enc_attr = generateAttribute("enc_attr", dsl1, 6);
        dsl2Ref = generateAttribute("DSL 2 ref", dsl1, dsl2, 4);
        List<Attribute> dsl1Attributes = asList(text_attr, text_attr2, text_attr3, list_attr, list_attr2, enc_attr,
                dsl2Ref);
        setAttributes(dsl1.getEntity(), text_attr, text_attr2, text_attr3, list_attr, list_attr2, enc_attr, dsl2Ref);

        a = generateAttribute("a", dsl2, 1);
        b = generateAttribute("b", dsl2, 1);
        enc = generateAttribute("enc", dsl2, 6);
        country = generateAttribute("country", dsl2, 3);
        dsl3Ref = generateAttribute("DSL 3 ref", dsl2, dsl3, 4);
        setAttributes(dsl2.getEntity(), a, b, enc, country, dsl3Ref);
        e = generateAttribute("e", dsl3, 1);
        dsl4Ref = generateAttribute("DSL 4 ref", dsl3, dsl4, 4);
        setAttributes(dsl3.getEntity(), e, dsl4Ref);
        f = generateAttribute("f", dsl4, 1);
        setAttributes(dsl4.getEntity(), f);

        ListValue aListValue = generateListValue("a");
        ListValue bListValue = generateListValue("b");
        cListValue = generateListValue("c");
        dListValue = generateListValue("d");
        uaListValue = generateListValue("UA");
        ListValue spListValue = generateListValue("SP");
        frListValue = generateListValue("FR");
        List<ListValue> list_attrListValues = asList(aListValue, bListValue, cListValue, dListValue);
        List<ListValue> countryListValues = asList(uaListValue, spListValue, frListValue);

        //DSL 1
        //text_attr
        Parameter ds1_text_attr = null; //generateParameter("", ds1, text_attr, null, null);
        Parameter ds2_text_attr = generateParameter("land", ds2, text_attr, null, null);
        Parameter ds1_text_attr2 = generateParameter("", ds1, text_attr2, null, null);
        Parameter ds2_text_attr2 = generateParameter("la", ds2, text_attr2, null, null);
        Parameter ds1_text_attr3 = generateParameter("", ds1, text_attr3, null, null);
        Parameter ds2_text_attr3 = generateParameter("text", ds2, text_attr3, null, null);

        //list_attr
        Parameter ds1_list_attr = generateParameter("c", ds1, list_attr, cListValue, null);
        Parameter ds2_list_attr = generateParameter("", ds2, list_attr, null, null);
        Parameter ds1_list_attr2 = generateParameter("c", ds1, list_attr2, cListValue, null);
        Parameter ds2_list_attr2 = generateParameter("d", ds2, list_attr2, dListValue, null);

        // DSl 2 > DS 3
        Parameter dsl2_ds3_dsl2ref = generateParameter("DSL 2 ref", ds1, dsl2Ref, null, ds4);
        Parameter dsl2_ds3_a = generateParameter("1", ds3, a, null, null);
        Parameter dsl2_ds3_b = generateParameter("2", ds3, b, null, null);
        Parameter dsl2_ds3_country = generateParameter("UA", ds3, country, uaListValue, null);
        // DSl 2 > DS 4
        Parameter dsl2_ds4_dsl2ref = null;
        Parameter dsl2_ds4_a = generateParameter("3", ds4, a, null, null);
        Parameter dsl2_ds4_b = generateParameter("4", ds4, b, null, null);
        Parameter dsl2_ds4_country = generateParameter("SP", ds4, country, spListValue, null);
        // DSl 3 > DS 5
        Parameter dsl3_ds5_dsl3ref = generateParameter("DSL 3 ref", ds3, dsl3Ref, null, ds5);
        Parameter dsl3_ds5_e = generateParameter("5", ds5, e, null, null);
        // DSl 3 > DS 6
        Parameter dsl3_ds6_dsl3ref = generateParameter("DSL 3 ref", ds4, dsl3Ref, null, ds6);
        Parameter dsl3_ds6_e = generateParameter("6", ds6, e, null, null);
        // DSl 4 > DS 7
        Parameter dsl4_ds7_dsl4ref = generateParameter("DSL 4 ref", ds5, dsl4Ref, null, ds7);
        Parameter dsl4_ds7_f = generateParameter("4", ds7, f, null, null);
        // DSl 4 > DS 8
        Parameter dsl4_ds8_dsl4ref = generateParameter("DSL 4 ref", ds6, dsl4Ref, null, ds8);
        Parameter dsl4_ds8_f = generateParameter("5", ds8, f, null, null);

        // DSL 2 parameters
        Set<UUID> dsl2DatasetIds = newHashSet(ds3.getId(), ds4.getId());
        ImmutableMap.of(
                a, asList(dsl2_ds3_a, dsl2_ds4_a),
                b, asList(dsl2_ds3_b, dsl2_ds4_b),
                country, asList(dsl2_ds3_country, dsl2_ds4_country),
                dsl3Ref, asList(dsl3_ds5_dsl3ref, dsl3_ds6_dsl3ref)
        ).forEach((attr, parameters) -> when(
                parameterService.getByAttributeIdAndDatasetIds(attr.getId(), dsl2DatasetIds)).thenReturn(parameters));

        // DSL 3 parameters
        Set<UUID> dsl3DatasetIds = newHashSet(ds5.getId(), ds6.getId());
        ImmutableMap.of(
                e, asList(dsl3_ds5_e, dsl3_ds6_e),
                dsl4Ref, asList(dsl4_ds7_dsl4ref, dsl4_ds8_dsl4ref)
        ).forEach((attr, parameters) -> when(
                parameterService.getByAttributeIdAndDatasetIds(attr.getId(), dsl3DatasetIds)).thenReturn(parameters));

        // DSL 4 parameters
        Set<UUID> dsl4DatasetIds = newHashSet(ds7.getId(), ds8.getId());
        ImmutableMap.of(
                f, asList(dsl4_ds7_f, dsl4_ds8_f)
        ).forEach((attr, parameters) -> when(
                parameterService.getByAttributeIdAndDatasetIds(attr.getId(), dsl4DatasetIds)).thenReturn(parameters));

        when(attributeService.getByDataSetListId(targetDslId)).thenReturn(dsl1Attributes);
        when(dataSetService.getByDataSetListId(targetDslId)).thenReturn(dsl1Datasets);
        when(dataSetListService.getByVisibilityAreaId(targetProjectId)).thenReturn(projectDatasetLists);

        refDatasetLists = asList(dsl2, dsl3, dsl4);
        refDatasets = asList(ds3, ds4, ds5, ds6, ds7, ds8);
        refDslDatasetIds = extractIds(refDatasets, DataSet::getId);

        when(dataSetService.getByDataSetListIdIn(extractIds(refDatasetLists, DataSetList::getId))).thenReturn(
                refDatasets);

        when(attributeService.getListValuesByAttributeId(list_attr.getId())).thenReturn(list_attrListValues);
        when(attributeService.getListValuesByAttributeId(list_attr2.getId())).thenReturn(list_attrListValues);
        when(attributeService.getListValuesByAttributeId(country.getId())).thenReturn(countryListValues);
        //when(attributeService.getListValueByAttributeIdAndValue(list_attr.getId(), cListValue.getText())).thenReturn(cListValue);
        when(attributeService.getListValueByAttributeIdAndValue(list_attr2.getId(), dListValue.getText())).thenReturn(
                dListValue);
        //when(attributeService.getListValueByAttributeIdAndValue(country.getId(), uaListValue.getText())).thenReturn(uaListValue);
        when(attributeService.getListValueByAttributeIdAndValue(country.getId(), frListValue.getText())).thenReturn(
                frListValue);

        when(factory.getAttributeImportConverter(AttributeType.TEXT.getName())).thenReturn(
                new TextAttributeImportConverter(parameterService));
        when(factory.getAttributeImportConverter(AttributeType.LIST.getName())).thenReturn(
                new ListAttributeImportConverter(parameterService, attributeService));
        when(factory.getAttributeImportConverter(AttributeType.DSL.getName())).thenReturn(
                new DatasetLinkAttributeImportConverter(parameterService));
        when(factory.getAttributeImportConverter(AttributeType.ENCRYPTED.getName())).thenReturn(
                new EncryptedAttributeImportConverter(parameterService));

        when(entityManager.find(DataSetListEntity.class, dsl2Ref.getTypeDataSetListId())).thenReturn(dsl2.getEntity());
        when(entityManager.find(DataSetListEntity.class, dsl3Ref.getTypeDataSetListId())).thenReturn(dsl3.getEntity());
        when(entityManager.find(DataSetListEntity.class, dsl4Ref.getTypeDataSetListId())).thenReturn(dsl4.getEntity());

        // DSL 1 text_attr
        when(parameterService.getByDataSetIdAttributeId(ds1.getId(), text_attr.getId())).thenReturn(ds1_text_attr);
        when(parameterService.getByDataSetIdAttributeId(ds2.getId(), text_attr.getId())).thenReturn(ds2_text_attr);
        when(parameterService.getByDataSetIdAttributeId(ds1.getId(), text_attr2.getId())).thenReturn(ds1_text_attr2);
        when(parameterService.getByDataSetIdAttributeId(ds2.getId(), text_attr2.getId())).thenReturn(ds2_text_attr2);
        when(parameterService.getByDataSetIdAttributeId(ds1.getId(), text_attr3.getId())).thenReturn(ds1_text_attr3);
        when(parameterService.getByDataSetIdAttributeId(ds2.getId(), text_attr3.getId())).thenReturn(ds2_text_attr3);
        // DSL 1 list_attr
        when(parameterService.getByDataSetIdAttributeId(ds1.getId(), list_attr.getId())).thenReturn(ds1_list_attr);
        when(parameterService.getByDataSetIdAttributeId(ds2.getId(), list_attr.getId())).thenReturn(ds2_list_attr);
        when(parameterService.getByDataSetIdAttributeId(ds1.getId(), list_attr2.getId())).thenReturn(ds1_list_attr2);
        when(parameterService.getByDataSetIdAttributeId(ds2.getId(), list_attr2.getId())).thenReturn(ds2_list_attr2);
        // DSL 2 ref
        when(parameterService.getByDataSetIdAttributeId(ds1.getId(), dsl2Ref.getId())).thenReturn(dsl2_ds3_dsl2ref);
        when(parameterService.getByDataSetIdAttributeId(ds2.getId(), dsl2Ref.getId())).thenReturn(dsl2_ds4_dsl2ref);

        // DSL 2 ref -> a
        when(parameterService.getByDataSetIdAttributeId(ds3.getId(), a.getId())).thenReturn(dsl2_ds3_a);
        when(parameterService.getByDataSetIdAttributeId(ds4.getId(), a.getId())).thenReturn(dsl2_ds4_a);
        // DSL 2 ref -> b
        when(parameterService.getByDataSetIdAttributeId(ds3.getId(), b.getId())).thenReturn(dsl2_ds3_b);
        when(parameterService.getByDataSetIdAttributeId(ds4.getId(), b.getId())).thenReturn(dsl2_ds4_b);
        // DSL 2 ref -> country
        when(parameterService.getByDataSetIdAttributeId(ds3.getId(), country.getId())).thenReturn(dsl2_ds3_country);
        when(parameterService.getByDataSetIdAttributeId(ds4.getId(), country.getId())).thenReturn(dsl2_ds4_country);
        // DSL 2 ref -> DSL 3 ref
        //when(parameterService.getByDataSetIdAttributeId(ds5.getId(), dsl3Ref.getId())).thenReturn(dsl3_ds5_dsl3ref);
        //when(parameterService.getByDataSetIdAttributeId(ds6.getId(), dsl3Ref.getId())).thenReturn(dsl3_ds6_dsl3ref);
        // DSL 2 ref -> DSL 3 ref -> e
        when(parameterService.getByDataSetIdAttributeId(ds5.getId(), e.getId())).thenReturn(dsl3_ds5_e);
        when(parameterService.getByDataSetIdAttributeId(ds6.getId(), e.getId())).thenReturn(dsl3_ds6_e);
        // DSL 2 ref -> DSL 3 ref -> DSL 4 ref
        //when(parameterService.getByDataSetIdAttributeId(ds7.getId(), dsl4Ref.getId())).thenReturn(dsl4_ds7_dsl4ref);
        //when(parameterService.getByDataSetIdAttributeId(ds8.getId(), dsl4Ref.getId())).thenReturn(dsl4_ds8_dsl4ref);
        // DSL 2 ref -> DSL 3 ref -> DSL 4 ref -> f
        when(parameterService.getByDataSetIdAttributeId(ds7.getId(), f.getId())).thenReturn(dsl4_ds7_f);
        when(parameterService.getByDataSetIdAttributeId(ds8.getId(), f.getId())).thenReturn(dsl4_ds8_f);
    }

    @Test
    public void importDataSetList_ImportExcelFileWhenLockedDataSet_Exception() throws Exception {
        ds1.setLocked(true);
        List<DataSet> lockedDataSets = Collections.singletonList(ds1);
        when(dataSetService.getLockedDataSets(targetDslId)).thenReturn(lockedDataSets);
        when(dataSetListService.getById(targetDslId)).thenReturn(dsl1);

        try {
            Path path = Paths.get("src/test/resources/excel/import/valid_import_file.xlsx");
            importService.importDataSetList(targetProjectId, targetDslId, Files.newInputStream(path), false);
        } catch (ImportFailedException e) {
            Assertions.assertTrue(e.getMessage().contains("Failed to import DataSetList"));
        }
    }

    @Test
    public void importDataSetList() throws Exception {
        File file = Paths.get("src/test/resources/excel/import/valid_import_file.xlsx").toFile();
        importService.importDataSetList(targetProjectId, targetDslId, Files.newInputStream(file.toPath()), false);

        final UUID ds1Id = ds1.getId();
        final UUID ds2Id = ds2.getId();
        // DSL 1 text_attr
        verify(parameterService, never()).setParamSelectJavers(ds1Id, text_attr.getId(), emptyList(), "", null, null,
                false);
        verify(parameterService, never()).setParamSelectJavers(ds2Id, text_attr.getId(), emptyList(), "land", null,
                null,
                false);
        verify(parameterService, never()).setParamSelectJavers(ds1Id, text_attr2.getId(), emptyList(), "", null, null,
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, text_attr2.getId(), emptyList(), "land2", null,
                null,
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds1Id, text_attr3.getId(), emptyList(), "text1", null,
                null,
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, text_attr3.getId(), emptyList(), "", null, null,
                false);

        // DSL 1 list_attr
        verify(parameterService, never()).setParamSelectJavers(ds1Id, list_attr.getId(), emptyList(),
                cListValue.getText(), null, cListValue.getId(),
                false);
        verify(parameterService, never()).deleteParamSelectJavers(list_attr.getId(), ds2Id, targetDslId, emptyList(),
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds1Id, list_attr2.getId(), emptyList(),
                null, null, dListValue.getId(),
                false);
        verify(parameterService, times(1)).deleteParamSelectJavers(list_attr2.getId(), ds2Id, targetDslId, emptyList(),
                false);

        // DSL 1 enc_att
        verify(parameterService, times(1)).setParamSelectJavers(ds1Id, enc_attr.getId(), emptyList(), "", null, null,
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, enc_attr.getId(), emptyList(),
                "cXdlcnR5cXFxcXE=", null, null,
                false);

        // DSL 2 ref
        verify(parameterService, times(1)).setParamSelectJavers(ds1Id, dsl2Ref.getId(), emptyList(), null, ds3.getId(),
                null,
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, dsl2Ref.getId(), emptyList(), null, ds4.getId(),
                null,
                false);

        // DSL 2 ref -> a
        List<UUID> dsl2RefAttributePath = singletonList(dsl2Ref.getId());
        verify(parameterService, never()).setParamSelectJavers(ds1Id, a.getId(), emptyList(), "1", null, null, false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, a.getId(), dsl2RefAttributePath, "4444", null,
                null,
                false);

        // DSL 2 ref -> b
        verify(parameterService, times(1)).setParamSelectJavers(ds1Id, b.getId(), dsl2RefAttributePath, "3333", null,
                null,
                false);
        verify(parameterService, never()).setParamSelectJavers(ds2Id, b.getId(), emptyList(), "4", null, null, false);

        // DSL 2 ref -> enc
        verify(parameterService, times(1)).setParamSelectJavers(ds1Id, enc.getId(), dsl2RefAttributePath, "cXdldw==",
                null, null,
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, enc.getId(), dsl2RefAttributePath, "cXdlcQ==",
                null, null,
                false);

        // DSL 2 ref -> country
        verify(parameterService, never()).setParamSelectJavers(ds1Id, country.getId(), emptyList(),
                uaListValue.getText(), null, uaListValue.getId(),
                false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, country.getId(), dsl2RefAttributePath,
                null, null, frListValue.getId(),
                false);

        // DSL 2 ref -> DSL 3 ref
        verify(parameterService, never()).setParamSelectJavers(ds1Id, dsl3Ref.getId(), emptyList(), null, ds5.getId(),
                null,
                false);
        verify(parameterService, never()).setParamSelectJavers(ds2Id, dsl3Ref.getId(), emptyList(), null, ds6.getId(),
                null,
                false);

        // DSL 2 ref -> DSL 3 ref -> e
        List<UUID> dsl3RefAttributePath = asList(dsl2Ref.getId(), dsl3Ref.getId());
        verify(parameterService, never()).setParamSelectJavers(ds1Id, e.getId(), emptyList(), "5", null, null, false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, e.getId(), dsl3RefAttributePath, "666", null,
                null,
                false);

        // DSL 2 ref -> DSL 3 ref -> DSL 4 ref
        verify(parameterService, never()).setParamSelectJavers(ds1Id, dsl4Ref.getId(), emptyList(), null, ds7.getId(),
                null,
                false);
        verify(parameterService, never()).setParamSelectJavers(ds2Id, dsl4Ref.getId(), emptyList(), null, ds8.getId(),
                null,
                false);

        // DSL 2 ref -> DSL 3 ref -> DSL 4 ref -> f
        List<UUID> dsl4RefAttributePath = asList(dsl2Ref.getId(), dsl3Ref.getId(), dsl4Ref.getId());
        verify(parameterService, never()).setParamSelectJavers(ds1Id, f.getId(), emptyList(), "4", null, null, false);
        verify(parameterService, times(1)).setParamSelectJavers(ds2Id, f.getId(), dsl4RefAttributePath, "7", null, null,
                false);
    }

    @Test
    public void importDataSetList_ThereAreDuplicateDslNames_ErrorWhileImportDataSetList() {
        doThrow( new RuntimeException("There are duplicate DSL names : []")).when(dataSetListService).checkDslNames(targetProjectId);
        File file = Paths.get("src/test/resources/excel/import/valid_import_file.xlsx").toFile();

        assertThrows(Exception.class, () ->
                importService.importDataSetList(targetProjectId, targetDslId, Files.newInputStream(file.toPath()), false));
    }

    @Test
    public void importFileWithNotSupportedAttributeType_validateImportFileSchema_expectImportExcelNotSupportedAttributeTypeException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator(
                "import_file_with_not_supported_attribute_type.xlsx");

        assertThrows(ImportExcelNotSupportedAttributeTypeException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Invalid attribute 'text_attr' type: 'FILE'. Supported import types: [TEXT, LIST, DSL, ENCRYPTED]");
    }

    @Test
    public void importFileWithNotExistedDsRefParamValue_validateImportFileSchema_expectExcelImportNotExistingRefParameterException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator(
                "import_file_with_not_existed_ds_ref_param_value.xlsx");
        String atrName = "DSL 2 ref";
        String textAttrName = "text_attr";
        String listAttrName = "list_attr";
        String encryptAttrName = "enc_attr";
        Map<String, Attribute> attributesNameMap = new HashMap<>();
        attributesNameMap.put(atrName, attribute);
        attributesNameMap.put(textAttrName, attribute);
        attributesNameMap.put(listAttrName, attribute);
        attributesNameMap.put(encryptAttrName, attribute);
        when(importContext.getAttributesNameMap()).thenReturn(attributesNameMap);
        when(attribute.getTypeDataSetListId()).thenReturn(UUID.randomUUID());
        when(attribute.getAttributeType()).thenReturn(AttributeTypeName.TEXT)
                .thenReturn(AttributeTypeName.LIST)
                .thenReturn(AttributeTypeName.ENCRYPTED)
                .thenReturn(AttributeTypeName.DSL);


        assertThrows(ExcelImportNotExistingRefParameterException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Provided referenced [DSL 2 ->  NOT EXISTED DS], DS ' NOT EXISTED DS', in attribute 'DSL 2 ref' parameter doesn't exist");
    }

    @Test
    public void importFileWithNotExistedAttribute_validateImportFileSchema_expectImportExcelNotEqualsAttributeTypeException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator(
                "import_file_with_not_existed_attribute.xlsx");
        String textAttrName = "text_attr";
        Map<String, Attribute> attributesNameMap = new HashMap<>();
        attributesNameMap.put(textAttrName, attribute);
        when(importContext.getAttributesNameMap()).thenReturn(attributesNameMap);
        when(attribute.getAttributeType()).thenReturn(AttributeTypeName.LIST);


        assertThrows(ImportExcelNotEqualsAttributeTypeException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Failed to import data set list: attribute 'some_not_existed_attr' doesn't exist in DSL schema");
    }

    @Test
    public void importFileWithNotExistedAttribute_validateImportFileSchema_expectExcelImportNotExistingAttributeException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator(
                "import_file_with_not_existed_attribute.xlsx");
        String textAttrName = "text_attr";
        Map<String, Attribute> attributesNameMap = new HashMap<>();
        attributesNameMap.put(textAttrName, attribute);
        when(importContext.getAttributesNameMap()).thenReturn(attributesNameMap);
        when(attribute.getAttributeType()).thenReturn(AttributeTypeName.TEXT);

        assertThrows(ExcelImportNotExistingAttributeException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Failed to import data set list: attribute 'some_not_existed_attr' doesn't exist in DSL schema");
    }

    @Test
    public void emptyImportFileImported_validateImportFileSchema_expectImportException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator("empty_import_file.xlsx");

        assertThrows(ExcelImportEmptyExcelException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Failed to import data set list. Reason: empty import file content");
    }

    @Test
    public void emptyHeadingRowImportFileImported_validateImportFileSchema_expectImportException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator("empty_heading_row_import_file.xlsx");

        assertThrows(ExcelImportEmptyExcelException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Failed to import data set list. Reason: empty import file content");
    }

    @Test
    public void invalidImportFileWithoutAttributeHeading_validateImportFileSchema_expectImportException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator(
                "import_file_without_attribute_heading.xlsx");

        assertThrows(ImportFailedException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Required heading 'Attribute' is absent");
    }

    @Test
    public void invalidImportFileIncorrectAttributeHeading_validateImportFileSchema_expectImportException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator(
                "import_file_incorrect_attribute_heading.xlsx");

        assertThrows(ImportFailedException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Required heading 'Attribute' has incorrect name");
    }

    @Test
    public void invalidImportFileIncorrectTypeHeading_validateImportFileSchema_expectImportException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator("import_file_incorrect_type_heading.xlsx");

        assertThrows(ImportFailedException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Required heading 'Type' has incorrect name");
    }

    @Test
    public void invalidImportFileWithoutTypeHeading_validateImportFileSchema_expectImportException() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator("import_file_without_type_heading.xlsx");

        assertThrows(ImportFailedException.class, () ->
                        importService.validateImportFileSchema(targetDslId, rowsIterator, importContext),
                "Required heading 'Type' is absent");
    }

    @Test
    public void importFileWithNotExistedDataset_prepareImportContext_expectCreateDatasetCell() throws Exception {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator(
                "valid_import_file_with_not_existed_datasets.xlsx");

        final ArgumentCaptor<String> datasetNameCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<UUID> datasetDslIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final String ds3Name = "DS 3";
        final String ds4Name = "DS 4";

        when(dataSetService.createDsSelectJavers(ds3Name, targetDslId, false)).thenReturn(generateDataset(ds3Name, dsl1));
        when(dataSetService.createDsSelectJavers(ds4Name, targetDslId, false)).thenReturn(generateDataset(ds4Name, dsl1));

        importService.prepareImportContext(targetProjectId, targetDslId, rowsIterator, false);

        verify(dataSetService, times(2)).createDsSelectJavers(datasetNameCaptor.capture(),
                datasetDslIdCaptor.capture(), eq(false));

        List<String> datasetNames = datasetNameCaptor.getAllValues();
        assertThat(datasetNames, hasSize(2));
        assertThat(datasetNames, containsInAnyOrder(ds3Name, ds4Name));

        Set<UUID> targetDslIds = new HashSet<>(datasetDslIdCaptor.getAllValues());
        assertThat(targetDslIds, hasSize(1));
        assertThat(targetDslIds, hasItem(targetDslId));
    }

    @Test
    public void allNeededEntitiesAreLoadedFromDb_prepareImportContext_expectCorrectContextData() {
        ListIterator<Map<Integer, String>> rowsIterator = getFileRowIterator("valid_import_file.xlsx");

        AttributeImportContext importContext = importService.prepareImportContext(targetProjectId, targetDslId,
                rowsIterator, false);

        Map<UUID, DataSet> datasetsMap = importContext.getDatasetsMap();
        Map<String, UUID> datasetsNameIdMap = importContext.getDatasetsNameIdMap();
        assertThat("Dataset name to id map has incorrect size", datasetsMap.size(), is(dsl1Datasets.size()));
        dsl1Datasets.forEach(dataset -> {
            final String datasetName = dataset.getName();
            final UUID datasetId = dataset.getId();

            final String datasetsMapErrorMsg = String.format("Dataset map should contain dataset: %s", datasetName);
            assertThat(datasetsMapErrorMsg, datasetsMap, IsMapContaining.hasEntry(datasetId, dataset));

            final String datasetsNameIdMapErrorMsg = String.format("Dataset name to id map should contain dataset: %s",
                    datasetName);
            assertThat(datasetsNameIdMapErrorMsg, datasetsNameIdMap, IsMapContaining.hasEntry(datasetName, datasetId));
        });

        Map<String, UUID> refDatasetsNameIdMap = importContext.getRefDatasetsNameIdMap();
        assertThat("Ref dataset name to id map has incorrect size", refDatasetsNameIdMap.size(),
                is(refDatasets.size()));
        refDatasets.forEach(refDataset -> {
            final UUID datasetId = refDataset.getId();
            final String datasetKey = ImportUtils.getDatasetKey(refDataset);

            final String refDatasetsNameIdMapErrorMsg = String.format(
                    "Ref dataset name to id map should contain dataset with key: %s", datasetKey);
            assertThat(refDatasetsNameIdMapErrorMsg, refDatasetsNameIdMap,
                    IsMapContaining.hasEntry(datasetKey, datasetId));
        });

        Map<String, Set<UUID>> refDslDatasetsMap = importContext.getRefDslDatasetIdsMap();
        assertThat("Ref DSL datasets map has incorrect size", refDslDatasetsMap.size(), is(refDatasetLists.size()));
        refDatasetLists.forEach(refDatasetList -> {
            final String refDatasetListName = refDatasetList.getName();

            final String refDslDatasetsErrorMsg = String.format("Ref DSL datasets map should contain DSL with key: %s",
                    refDatasetListName);
            assertThat(refDslDatasetsErrorMsg, refDslDatasetsMap, IsMapContaining.hasKey(refDatasetListName));

            final Set<UUID> dslDatasetIds = refDslDatasetsMap.get(refDatasetListName);
            dslDatasetIds.forEach(dslDatasetId -> {
                final String refDslDatasetsIdErrorMsg = String.format(
                        "Ref DSL datasets map DSL '%s' should contain dataset with id: %s", refDatasetListName,
                        dslDatasetId);
                Assertions.assertTrue(refDslDatasetIds.contains(dslDatasetId), refDslDatasetsIdErrorMsg);
            });
        });
    }

    @Test
    public void allAttributesSet_getDslAttributesMap_getValidDslAttributesMap() {
        Map<String, UUID> resultDslAttributesMap = importService.getDslAttributesMap(targetDslId);

        String errorMessage = "DSL attribute map doesn't contain required key";
        assertThat(errorMessage, resultDslAttributesMap, IsMapContaining.hasKey(text_attr.getName()));
        assertThat(errorMessage, resultDslAttributesMap, IsMapContaining.hasKey(list_attr.getName()));
        assertThat(errorMessage, resultDslAttributesMap, IsMapContaining.hasKey(enc_attr.getName()));
        assertThat(errorMessage, resultDslAttributesMap, IsMapContaining.hasKey(dsl2Ref.getName()));
    }

    @Test
    public void invalidImportFileWithoutDatasetHeadings_getExistedDatasetsCellIndexMap_expectException() {
        Map<Integer, String> headingRow = getFileHeadingRow("import_file_without_dataset_headings.xlsx");

        assertThrows(ImportFailedException.class, () ->
                        importService.getExistedDatasetsCellIndexMap(headingRow),
                "Imported dataset columns are absent");
    }

    @Test
    public void validImportFileWithDatasetHeadings_getExistedDatasetsCellIndexMap_expectValidCellIndexMap() {
        Map<Integer, String> headingRow = getFileHeadingRow("import_file_with_dataset_headings.xlsx");

        Map<String, Integer> resultCellIndexMap = importService.getExistedDatasetsCellIndexMap(headingRow);

        Map<String, Integer> expectedCellIndexMap = new HashMap<>();
        expectedCellIndexMap.put("DS 1", 2);
        expectedCellIndexMap.put("DS 2", 3);
        expectedCellIndexMap.put("DS 3", 4);

        assertThat("Dataset cell index map is incorrect after heading row parsing", resultCellIndexMap,
                is(expectedCellIndexMap));
    }

    private void setDatasets(DataSetList datasetList, DataSet... datasets) {
        List<DataSetEntity> dataSetEntities = Arrays.stream(datasets)
                .map(dataSet -> {
                    DataSetEntity dataSetEntity = new DataSetEntity();
                    dataSetEntity.setId(dataSet.getId());
                    dataSetEntity.setName(dataSet.getName());

                    return dataSetEntity;
                })
                .collect(Collectors.toList());
        DataSetListEntity entity = datasetList.getEntity();
        entity.setDataSets(dataSetEntities);
    }

    private Map<Integer, String> getFileHeadingRow(String filePath) {

        ListIterator<Map<Integer, String>> rowIterator = getFileRowIterator(filePath);

        if (rowIterator.hasNext()) {
            return rowIterator.next();
        } else {
            throw new RuntimeException("Failed to iterate throw tested excel file rows");
        }
    }

    private ListIterator<Map<Integer, String>> getFileRowIterator(String filePath) {
        String locationPrefix = "src/test/resources/excel/import/";
        Path path = Paths.get(locationPrefix + filePath);

        List<Map<Integer, String>> sheetConvertList = new ArrayList<>();
        try {
            OPCPackage pkg = OPCPackage.open(path.toString(), PackageAccess.READ);
            try {
                XlsxToListConverter xlsxToListConverter = new XlsxToListConverter(pkg, sheetConvertList);
                xlsxToListConverter.process();
                return sheetConvertList.listIterator();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load tested excel file rows");
            } finally {
                pkg.revert();
            }
        } catch (InvalidFormatException e) {
            throw new RuntimeException("Failed to load tested excel file rows, cause InvalidFormatException");
        }
    }

    private DataSetList generateDataSetList(String name, Attribute... attributes) {
        return generateDataSetList(UUID.randomUUID(), name);
    }

    private DataSetList generateDataSetList(String name) {
        return generateDataSetList(UUID.randomUUID(), name);
    }

    private DataSetList generateDataSetList(UUID id, String name) {
        DataSetListEntity dataSetListEntity = new DataSetListEntity();
        dataSetListEntity.setId(id);
        dataSetListEntity.setName(name);

        return new DataSetList(dataSetListEntity);
    }

    private void setAttributes(DataSetListEntity dataSetListEntity, Attribute... attributes) {
        if (attributes.length > 0) {
            dataSetListEntity.setAttributes(
                    Arrays.stream(attributes)
                            .map(AbstractObjectWrapper::getEntity)
                            .collect(Collectors.toList())
            );
        }
    }

    private DataSet generateDataset(String name, DataSetList datasetList) {
        DataSetEntity dataSetEntity = new DataSetEntity();
        dataSetEntity.setId(UUID.randomUUID());
        dataSetEntity.setName(name);
        dataSetEntity.setDataSetList(datasetList.getEntity());

        return new DataSet(dataSetEntity);
    }

    private Attribute generateAttribute(String name, DataSetList datasetList, DataSetList refDatasetList,
                                        long attributeTypeId) {
        AttributeEntity attributeEntity = new AttributeEntity();
        attributeEntity.setId(UUID.randomUUID());
        attributeEntity.setName(name);
        attributeEntity.setDataSetList(datasetList.getEntity());
        attributeEntity.setAttributeTypeId(attributeTypeId);
        if (nonNull(refDatasetList)) {
            attributeEntity.setTypeDataSetListId(refDatasetList.getId());
        }

        return new Attribute(attributeEntity);
    }

    private Attribute generateAttribute(String name, DataSetList datasetList, long attributeTypeId) {
        return generateAttribute(name, datasetList, null, attributeTypeId);
    }

    private AttributeKey generateAttributeKey(String key, Attribute attribute, DataSetList datasetList,
                                              DataSet dataSet) {
        AttributeKeyEntity attributeEntity = new AttributeKeyEntity();
        attributeEntity.setId(UUID.randomUUID());
        attributeEntity.setKey(key);
        attributeEntity.setAttribute(attribute.getEntity());
        attributeEntity.setDataSet(dataSet.getEntity());
        attributeEntity.setDataSetList(datasetList.getEntity());
        return new AttributeKey(attributeEntity);
    }

    private ListValue generateListValue(String name) {
        ListValueEntity listValueEntity = new ListValueEntity();
        listValueEntity.setId(UUID.randomUUID());
        listValueEntity.setText(name);

        return new ListValue(listValueEntity);
    }

    private Parameter generateParameter(String text, DataSet dataset, Attribute attribute,
                                        AttributeKey attributeKey, ListValue listValue,
                                        DataSet refDataset) {
        Parameter parameter = new ParameterImpl();
        parameter.setId(UUID.randomUUID());
        parameter.setText(text);

        DataSetList dataSetList = dataset.getDataSetList();
        DataSetListImpl dsl = new DataSetListImpl();
        dsl.setId(dataSetList.getId());
        dsl.setName(dataSetList.getName());

        DataSetImpl ds = new DataSetImpl(dataset.getId(), dataset.getName(), dsl, null, null, false);
        parameter.setDataSet(ds);
        if (nonNull(refDataset)) {
            DataSetImpl refDs = new DataSetImpl(refDataset.getId(), refDataset.getName(), null, null, null, false);
            parameter.setDataSetReference(refDs);
        }

        AttributeImpl attr = new AttributeImpl();
        if (Objects.nonNull(attribute)) {
            attr.setId(attribute.getId());
            attr.setName(attribute.getName());
        } else {
            attr.setId(attributeKey.getId());
            attr.setName(attributeKey.getName());
        }

        parameter.setAttribute(attr);

        if (nonNull(listValue)) {
            ListValueImpl lv = new ListValueImpl();
            lv.setId(listValue.getId());
            lv.setName(listValue.getText());

            parameter.setListValue(lv);
        }

        return parameter;
    }

    private Parameter generateParameter(String text, DataSet dataset, Attribute attribute, ListValue listValue,
                                        DataSet refDataset) {
        return generateParameter(text, dataset, attribute, null, listValue, refDataset);
    }

    private Parameter generateParameter(String text, DataSet dataset, AttributeKey attributeKey, ListValue listValue,
                                        DataSet refDataset) {
        return generateParameter(text, dataset, null, attributeKey, listValue, refDataset);
    }

    private static class Att {

    }
}

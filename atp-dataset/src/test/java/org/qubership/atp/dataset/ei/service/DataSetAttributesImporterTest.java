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

package org.qubership.atp.dataset.ei.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.ei.model.DataSetAttribute;
import org.qubership.atp.dataset.ei.model.DataSetList;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.validation.ValidationType;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;

@Isolated
@ExtendWith(SpringExtension.class)
public class DataSetAttributesImporterTest {

    private ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private DataSetAttributesImporter dataSetAttributesImporter;
    @Mock
    private JpaAttributeService attrService;
    @Mock
    private JpaDataSetListService dslService;
    private DuplicateNameChecker duplicateNameChecker;
    @Mock
    private EntityManagerController entityManagerController;

    private ExportImportData importData;

    @BeforeEach
    public void setUp() throws Exception {
        duplicateNameChecker = new DuplicateNameChecker();
        objectLoaderFromDiskService = new ObjectLoaderFromDiskService();
        dataSetAttributesImporter =
                new DataSetAttributesImporter(objectLoaderFromDiskService, attrService, dslService,
                        duplicateNameChecker, entityManagerController);
        importData = new ExportImportData(null, null, null, false, false, null, new HashMap<>(), new HashMap<>(),
                ValidationType.VALIDATE, false);
    }

    @Test
    public void validateDataSetAttributes_TypeDataSetListNotFound() {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(dslService.getById(any())).thenReturn(null);
        when(attrService.getById(any())).thenReturn(null);
        List<String> result = dataSetAttributesImporter.validateDataSetAttributes(workDir,
                Collections.emptyMap(), false);
        result.contains(
                "Link to DS Attribute 'ref_parameter' will be skipped "
                        + "because it refers to absent Data Set List");
        result.contains(
                "Attribute 'text_value' from Link to DS Attribute group 'ref_parameter' will be skipped "
                        + "because it refers to absent Attribute");
    }

    @Test
    public void importDataSetAttributes_noOneDslImportedDoNothing() throws IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        List<UUID> dataSetLists = new ArrayList<>();
        dataSetAttributesImporter.importDataSetAttributes(workDir, dataSetLists, importData);
        verify(attrService, times(0)).remove(any());
        verify(attrService, times(0)).removeListValuesByAttributeId(any());
    }

    @Test
    public void importDataSetAttributes_1() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        Map<UUID, Path> dataSetListsMap = objectLoaderFromDiskService.getListOfObjects(workDir, DataSetList.class);
        List<UUID> dataSetLists = new ArrayList<>();

        dataSetLists.clear();
        dataSetLists.addAll(dataSetListsMap.keySet());
        UUID dslId = UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75");
        dataSetLists.remove(dslId);

        when(dslService.getById(dslId)).thenReturn(null);
        when(attrService.replicate(any(), any(), any(), any(), any())).thenReturn(mock(Attribute.class));

        dataSetAttributesImporter.importDataSetAttributes(workDir, dataSetLists, importData);

        verify(attrService, times(1)).replicate(any(), any(), any(), any(), any());
        verify(attrService, times(0)).removeListValuesByAttributeId(any());
        verify(attrService, times(0)).replicateListValue(any(), any(), any(), any());
    }

    @Test
    public void importDataSetAttributes_2() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        Map<UUID, Path> dataSetListsMap = objectLoaderFromDiskService.getListOfObjects(workDir, DataSetList.class);
        List<UUID> dataSetLists = new ArrayList<>();

        dataSetLists.clear();
        dataSetLists.addAll(dataSetListsMap.keySet());

        when(attrService.replicate(any(), any(), any(), any(), any())).thenReturn(mock(Attribute.class));
        dataSetAttributesImporter.importDataSetAttributes(workDir, dataSetLists, importData);

        verify(attrService, times(5)).replicate(any(), any(), any(), any(), any());
        verify(attrService, times(4)).replicateListValue(any(), any(), any(), any());
    }

    @Test
    public void importDataSetAttributes_21() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        Map<UUID, Path> dataSetListsMap = objectLoaderFromDiskService.getListOfObjects(workDir, DataSetList.class);
        List<UUID> dataSetLists = new ArrayList<>();

        dataSetLists.clear();
        dataSetLists.addAll(dataSetListsMap.keySet());

        when(attrService.getById(any())).thenReturn(null);
        Attribute existingAttribute = mock(Attribute.class);
        when(attrService.getById(UUID.fromString("f5de3b5b-1d19-490b-a0ef-ecc48762a902")))
                .thenReturn(existingAttribute);
        when(attrService.getListValueById(any())).thenReturn(null);
        ListValue existingListValue = mock(ListValue.class);
        when(attrService.getListValueById(UUID.fromString("47a846d4-c44e-4c92-8dd9-8a0a78829ae6")))
                .thenReturn(existingListValue);

        when(attrService.replicate(any(), any(), any(), any(), any())).thenReturn(mock(Attribute.class));

        dataSetAttributesImporter.importDataSetAttributes(workDir, dataSetLists, importData);

        verify(attrService, times(4)).replicate(any(), any(), any(), any(), any());
        verify(attrService, times(3)).replicateListValue(any(), any(), any(), any());
        verify(attrService, times(1)).save(existingAttribute);
        verify(attrService, times(1)).save(existingListValue);
    }

    @Test
    public void importDataSetAttributeKeys_1() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        when(attrService.getById(any())).thenReturn(null);
        dataSetAttributesImporter.importDataSetAttributeKeys(workDir, importData);

        verify(attrService, times(0)).replicateAttributeKey(
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")),
                eq("2ae01b66-5000-410d-9468-428a418881e0"),
                eq(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")),
                eq(UUID.fromString("8b911573-97ae-41fb-a3b8-d7d1f63d017c")),
                eq(UUID.fromString("e3786dc9-10ba-4251-9a43-7c12cac37ef6")),
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")));
    }

    @Test
    public void importDataSetAttributeKeys_2() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        when(attrService.getById(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")))
                .thenReturn(null);
        dataSetAttributesImporter.importDataSetAttributeKeys(workDir, importData);

        verify(attrService, times(0)).replicateAttributeKey(
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")),
                eq("2ae01b66-5000-410d-9468-428a418881e0"),
                eq(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")),
                eq(UUID.fromString("8b911573-97ae-41fb-a3b8-d7d1f63d017c")),
                eq(UUID.fromString("e3786dc9-10ba-4251-9a43-7c12cac37ef6")),
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")));
    }

    @Test
    public void importDataSetAttributeKeys_3() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        when(attrService.getById(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")))
                .thenReturn(mock(Attribute.class));
        dataSetAttributesImporter.importDataSetAttributeKeys(workDir, importData);

        verify(attrService, times(1)).replicateAttributeKey(
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")),
                eq("2ae01b66-5000-410d-9468-428a418881e0"),
                eq(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")),
                eq(UUID.fromString("8b911573-97ae-41fb-a3b8-d7d1f63d017c")),
                eq(UUID.fromString("e3786dc9-10ba-4251-9a43-7c12cac37ef6")),
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")));
    }

    @Test
    public void importDataSetAttributeKeys_4() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        when(attrService.getById(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")))
                .thenReturn(mock(Attribute.class));
        dataSetAttributesImporter.importDataSetAttributeKeys(workDir, importData);

        verify(attrService, times(1)).replicateAttributeKey(
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")),
                eq("2ae01b66-5000-410d-9468-428a418881e0"),
                eq(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")),
                eq(UUID.fromString("8b911573-97ae-41fb-a3b8-d7d1f63d017c")),
                eq(UUID.fromString("e3786dc9-10ba-4251-9a43-7c12cac37ef6")),
                eq(UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518")));
    }

    @Test
    public void importDataSetAttributeKeys_importOverlapIntoDbWithOverlapWithTheSameParameters_OverlapsInDbShouldBeChangedWithoutDuplication()
            throws IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        when(attrService.getById(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")))
                .thenReturn(mock(Attribute.class));
        AttributeKey attrKey = mock(AttributeKey.class);
        when(attrService.getAttributeKeyByKeyAndDataSetListIdAndDataSetIdAndAttributeId(any(), any(),any(),any()))
                .thenReturn(attrKey);

        dataSetAttributesImporter.importDataSetAttributeKeys(workDir, importData);

        verify(attrKey, times(1)).setKey(eq("2ae01b66-5000-410d-9468-428a418881e0"));
        verify(attrKey, times(1)).setAttribute(eq(UUID.fromString("4400c445-d685-48ce-9817-d6dcebd927d9")));
        verify(attrKey, times(1)).setDataSet(eq(UUID.fromString("8b911573-97ae-41fb-a3b8-d7d1f63d017c")));
        verify(attrKey, times(1)).setDataSetList(eq(UUID.fromString("e3786dc9-10ba-4251-9a43-7c12cac37ef6")));
    }

    @Test
    public void checkAndCorrectName_noDuplicateInBase() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        DataSetAttribute object = new DataSetAttribute();
        object.setName(name);
        object.setId(id);
       
        dataSetAttributesImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }


    @Test
    public void checkAndCorrectName_theSameObject() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        DataSetAttribute object = new DataSetAttribute();
        object.setName(name);
        object.setId(id);

        UUID parentId = UUID.randomUUID();
        object.setDataSetList(parentId);

        DataSetAttribute entity = new DataSetAttribute();
        entity.setName(name);
        entity.setId(id);
        duplicateNameChecker.addToCache(parentId, entity);

        dataSetAttributesImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_theSameButWithOtherNameObject() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        DataSetAttribute object = new DataSetAttribute();
        object.setName(name);
        object.setId(id);

        UUID parentId = UUID.randomUUID();
        object.setDataSetList(parentId);

        DataSetAttribute entity = new DataSetAttribute();
        entity.setName("Object Name 2");
        entity.setId(id);
        duplicateNameChecker.addToCache(parentId, entity);

        dataSetAttributesImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_twoDuplicateInBase() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        DataSetAttribute object = new DataSetAttribute();
        object.setName(name);
        object.setId(id);
        UUID parentId = UUID.randomUUID();
        object.setDataSetList(parentId);

        DataSetAttribute entity = new DataSetAttribute();
        entity.setName(name);
        entity.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity);


        DataSetAttribute entity2 = new DataSetAttribute();
        entity2.setName(name + " Copy");
        entity2.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity2);


        DataSetAttribute entity3 = new DataSetAttribute();
        entity3.setName(name + " Copy _1");
        entity3.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity3);


        dataSetAttributesImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name + " Copy _2");
    }
}

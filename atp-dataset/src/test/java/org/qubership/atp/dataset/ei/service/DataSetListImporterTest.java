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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaVisibilityAreaService;
import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.validation.ValidationType;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;

@Isolated
@ExtendWith(SpringExtension.class)
public class DataSetListImporterTest {

    private ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private DataSetListImporter dataSetListImporter;
    @Mock
    private JpaDataSetListService dslService;
    @Mock
    private JpaVisibilityAreaService vaService;
    private DuplicateNameChecker duplicateNameChecker;
    @Mock
    private EntityManagerController entityManagerController;

    private ExportImportData importData;

    @BeforeEach
    public void setUp() throws Exception {
        duplicateNameChecker = new DuplicateNameChecker();
        objectLoaderFromDiskService = new ObjectLoaderFromDiskService();
        dataSetListImporter =
                new DataSetListImporter(objectLoaderFromDiskService, dslService, vaService, duplicateNameChecker,
                        entityManagerController);
        importData = new ExportImportData(null, null, null, false, false, null, new HashMap<>(), new HashMap<>(),
                ValidationType.VALIDATE, false);
    }

    @Test
    public void importDataSetLists_createNewVa() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206208");
        when(vaService.getById(any())).thenReturn(null);

        List<UUID> result = dataSetListImporter.importDataSetLists(workDir, importData);
        Assertions.assertTrue(result.contains(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")));

        verify(vaService, times(1))
                .replicate(eq(UUID.fromString("9f052227-79d7-4f3f-bd55-aeb2efbcb103")), any());
        verify(dslService, times(1))
                .replicate(eq(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")), eq("export test simple"),
                        eq(UUID.fromString("9f052227-79d7-4f3f-bd55-aeb2efbcb103")),
                        eq(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")),
                        any(), any(), any(), any());
        verify(dslService, times(0)).save(any());
    }

    @Test
    public void importDataSetLists_vaExistsDoNotCreateIt() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206208");
        when(vaService.getById(any())).thenReturn(mock(VisibilityArea.class));
        List<UUID> result = dataSetListImporter.importDataSetLists(workDir, importData);
        Assertions.assertTrue(result.contains(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")));
        verify(vaService, times(0))
                .replicate(eq(UUID.fromString("9f052227-79d7-4f3f-bd55-aeb2efbcb103")), any());
        verify(dslService, times(1))
                .replicate(eq(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")), eq("export test simple"),
                        eq(UUID.fromString("9f052227-79d7-4f3f-bd55-aeb2efbcb103")),
                        eq(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")),
                        any(), any(), any(), any());
        verify(dslService, times(0)).save(any());
    }

    @Test
    public void importDataSetLists_1() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206208");
        when(vaService.getById(any())).thenReturn(mock(VisibilityArea.class));
        when(dslService.getById(any()))
                .thenReturn(mock(DataSetList.class));
        List<UUID> result = dataSetListImporter.importDataSetLists(workDir, importData);
        Assertions.assertTrue(result.contains(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")));
        verify(vaService, times(0))
                .replicate(eq(UUID.fromString("9f052227-79d7-4f3f-bd55-aeb2efbcb103")), any());
        verify(dslService, times(0))
                .replicate(eq(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")), eq("export test simple"),
                        eq(UUID.fromString("9f052227-79d7-4f3f-bd55-aeb2efbcb103")),
                        eq(UUID.fromString("cb6fc56b-392c-456a-82d6-46fc38836a75")),
                        any(), any(), any(), any());
        verify(dslService, times(1)).save(any());
    }


    @Test
    public void checkAndCorrectName_noDuplicateInBase() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSetList object = new org.qubership.atp.dataset.ei.model.DataSetList();
        object.setName(name);
        object.setId(id);

        dataSetListImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_theSameObject() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSetList object = new org.qubership.atp.dataset.ei.model.DataSetList();
        object.setName(name);
        object.setId(id);

        UUID parentId = UUID.randomUUID();
        object.setVisibilityArea(parentId);

        org.qubership.atp.dataset.ei.model.DataSetList entity = new org.qubership.atp.dataset.ei.model.DataSetList();
        entity.setName(name);
        entity.setId(id);

        duplicateNameChecker.addToCache(parentId, entity);

        dataSetListImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_theSameButWithOtherNameObject() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSetList object = new org.qubership.atp.dataset.ei.model.DataSetList();
        object.setName(name);
        object.setId(id);

        UUID parentId = UUID.randomUUID();
        object.setVisibilityArea(parentId);

        org.qubership.atp.dataset.ei.model.DataSetList entity = new org.qubership.atp.dataset.ei.model.DataSetList();
        entity.setName("Object Name 2");
        entity.setId(id);
        duplicateNameChecker.addToCache(parentId, entity);

        dataSetListImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_twoDuplicateInBase() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSetList object = new org.qubership.atp.dataset.ei.model.DataSetList();
        object.setName(name);
        object.setId(id);
        UUID parentId = UUID.randomUUID();
        object.setVisibilityArea(parentId);

        org.qubership.atp.dataset.ei.model.DataSetList entity = new org.qubership.atp.dataset.ei.model.DataSetList();
        entity.setName(name);
        entity.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity);

        org.qubership.atp.dataset.ei.model.DataSetList entity2 = new org.qubership.atp.dataset.ei.model.DataSetList();
        entity2.setName(name + " Copy");
        entity2.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity2);

        org.qubership.atp.dataset.ei.model.DataSetList entity3 = new org.qubership.atp.dataset.ei.model.DataSetList();
        entity3.setName(name + " Copy _1");
        entity3.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity3);

        dataSetListImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name + " Copy _2");
    }
}

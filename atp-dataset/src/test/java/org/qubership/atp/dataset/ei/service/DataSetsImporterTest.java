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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.validation.ValidationType;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import lombok.extern.slf4j.Slf4j;

@Isolated
@ExtendWith(SpringExtension.class)
@Slf4j
public class DataSetsImporterTest {

    private ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private DataSetsImporter dataSetsImporter;
    @Mock
    private JpaDataSetService dsService;
    @Mock
    private JpaDataSetListService dslService;
    private DuplicateNameChecker duplicateNameChecker;
    @Mock
    private EntityManagerController entityManagerController;
    @Mock
    private ClearCacheService clearCacheService;

    private ExportImportData importData;


    @BeforeEach
    public void setUp() {
        duplicateNameChecker = new DuplicateNameChecker();
        objectLoaderFromDiskService = new ObjectLoaderFromDiskService();
        dataSetsImporter =
                new DataSetsImporter(objectLoaderFromDiskService, dsService, dslService, duplicateNameChecker,
                        entityManagerController, clearCacheService);
        DataSetList dslMock =
                mock(DataSetList.class);
        long orderCount = 0;
        when(dslMock.getLastDataSetsOrderNumber()).thenReturn(orderCount++);
        when(dslService.getById(any())).thenReturn(dslMock);
        importData = new ExportImportData(null, null, null, false, false, null, new HashMap<>(), new HashMap<>(),
                ValidationType.VALIDATE, false);
    }

    @Test
    public void importDataSets() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");

        dataSetsImporter.importDataSets(workDir, importData);

        verify(dsService, times(2)).replicate(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void importDataSets_2() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(dsService.getById(any())).thenReturn(mock(DataSet.class));
        dataSetsImporter.importDataSets(workDir, importData);

        verify(dsService, times(0)).replicate(any(), any(), any(), any(), any(), any());
        verify(dsService, times(2)).save(any());
    }

    @Test
    public void checkAndCorrectName_noDuplicateInBase() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSet object = new org.qubership.atp.dataset.ei.model.DataSet();
        object.setName(name);
        object.setId(id);

        dataSetsImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_theSameObject() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSet object = new org.qubership.atp.dataset.ei.model.DataSet();
        object.setName(name);
        object.setId(id);

        UUID parentId = UUID.randomUUID();
        object.setDataSetList(parentId);

        org.qubership.atp.dataset.ei.model.DataSet entity = new org.qubership.atp.dataset.ei.model.DataSet();
        entity.setName(name);
        entity.setId(id);
        duplicateNameChecker.addToCache(parentId, entity);

        dataSetsImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_theSameButWithOtherNameObject() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSet object = new org.qubership.atp.dataset.ei.model.DataSet();
        object.setName(name);
        object.setId(id);

        UUID parentId = UUID.randomUUID();
        object.setDataSetList(parentId);

        org.qubership.atp.dataset.ei.model.DataSet entity = new org.qubership.atp.dataset.ei.model.DataSet();
        entity.setName("Object Name 2");
        entity.setId(id);
        duplicateNameChecker.addToCache(parentId, entity);

        dataSetsImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name);
    }

    @Test
    public void checkAndCorrectName_twoDuplicateInBase() {
        String name = "Object Name";
        UUID id = UUID.randomUUID();
        org.qubership.atp.dataset.ei.model.DataSet object = new org.qubership.atp.dataset.ei.model.DataSet();
        object.setName(name);
        object.setId(id);

        UUID parentId = UUID.randomUUID();
        object.setDataSetList(parentId);

        org.qubership.atp.dataset.ei.model.DataSet entity = new org.qubership.atp.dataset.ei.model.DataSet();
        entity.setName(name);
        entity.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity);

        org.qubership.atp.dataset.ei.model.DataSet entity2 = new org.qubership.atp.dataset.ei.model.DataSet();
        entity2.setName(name + " Copy");
        entity2.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity2);


        org.qubership.atp.dataset.ei.model.DataSet entity3 = new org.qubership.atp.dataset.ei.model.DataSet();
        entity3.setName(name + " Copy _1");
        entity3.setId(UUID.randomUUID());
        duplicateNameChecker.addToCache(parentId, entity3);

        dataSetsImporter.checkAndCorrectName(object);

        Assertions.assertEquals(object.getName(), name + " Copy _2");
    }
}

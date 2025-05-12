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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.validation.ValidationType;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;

@Isolated
@ExtendWith(SpringExtension.class)
public class DataSetParametersImporterTest {

    private ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private DataSetParametersImporter dataSetParametersImporter;
    @Mock
    private JpaParameterService paramService;
    @Mock
    private JpaAttributeService attrService;
    @Mock
    private JpaDataSetService dsService;
    @Mock
    private EntityManagerController entityManagerController;
    @Mock
    private ClearCacheService clearCacheService;
    private ExportImportData importData;

    @BeforeEach
    public void setUp() throws Exception {
        objectLoaderFromDiskService = new ObjectLoaderFromDiskService();
        dataSetParametersImporter =
                new DataSetParametersImporter(objectLoaderFromDiskService, paramService, attrService, dsService,
                        entityManagerController, clearCacheService);
        importData = new ExportImportData(null, null, null, false, false, null, new HashMap<>(), new HashMap<>(),
                ValidationType.VALIDATE, false);
    }

    @Test
    public void validateDataSetParameters() {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(dsService.getById(any())).thenReturn(null);
        List<String> result = dataSetParametersImporter.validateDataSetParameters(workDir,
                Collections.emptyMap(), false);
        Assertions.assertTrue(result.contains("Some Link to Data Set Attribute refers to absent Data Set."));

        when(dsService.getById(any())).thenReturn(mock(DataSet.class));
        result = dataSetParametersImporter.validateDataSetParameters(workDir, Collections.emptyMap(), false);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void importDataSetParameters_1() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(attrService.getById(any())).thenReturn(null);
        when(attrService.getAttributeKeyById(any())).thenReturn(null);
        dataSetParametersImporter.importDataSetParameters(workDir, importData);
        verify(paramService, times(0)).replicate(any(), any(), any(), any());
    }

    @Test
    public void importDataSetParameters_2() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(attrService.getById(any())).thenReturn(mock(Attribute.class));
        when(paramService.replicate(any(), any(), any(), any())).thenReturn(mock(Parameter.class));
        dataSetParametersImporter.importDataSetParameters(workDir, importData);
        verify(paramService, times(5)).replicate(any(), any(), any(), any());
    }

    @Test
    public void importDataSetParameters_21() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(attrService.getById(any())).thenReturn(null);
        when(attrService.getAttributeKeyById(any())).thenReturn(mock(AttributeKey.class));
        when(paramService.replicate(any(), any(), any(), any())).thenReturn(mock(Parameter.class));

        dataSetParametersImporter.importDataSetParameters(workDir, importData);
        verify(paramService, times(5)).replicate(any(), any(), any(), any());
    }

    @Test
    public void importDataSetParameters_22() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(attrService.getById(any())).thenReturn(mock(Attribute.class));
        UUID absentAttributeId = UUID.fromString("1c80d1bd-614e-4b01-9def-9fb9b9e79518");
        when(attrService.getById(absentAttributeId)).thenReturn(null);
        when(attrService.getAttributeKeyById(absentAttributeId)).thenReturn(null);
        when(paramService.replicate(any(), any(), any(), any())).thenReturn(mock(Parameter.class));

        dataSetParametersImporter.importDataSetParameters(workDir, importData);
        verify(paramService, times(4)).replicate(any(), any(), any(), any());
    }

    @Test
    public void importDataSetParameters_3() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(attrService.getById(any())).thenReturn(mock(Attribute.class));
        Parameter parameter = mock(Parameter.class);
        when(paramService.replicate(any(), any(), any(), any())).thenReturn(parameter);

        UUID dataSetReferenceValue = UUID.fromString("5bbb5c00-ef9a-4b02-81cc-7bd7cf91a7ce");
        when(dsService.getById(dataSetReferenceValue))
                .thenReturn(mock(DataSet.class));

        dataSetParametersImporter.importDataSetParameters(workDir, importData);
        verify(parameter, times(1)).setDataSetReferenceId(dataSetReferenceValue);
        verify(paramService, times(5)).replicate(any(), any(), any(), any());
    }

    @Test
    public void importDataSetParameters_4() throws DataSetServiceException, IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206209");
        when(attrService.getById(any())).thenReturn(mock(Attribute.class));

        Parameter existingParameter = mock(Parameter.class);
        when(paramService.getById(UUID.fromString("67094fbd-d205-4893-ac7a-a33877e7c14b")))
                .thenReturn(existingParameter);

        Parameter parameter = mock(Parameter.class);
        when(paramService.replicate(any(), any(), any(), any())).thenReturn(parameter);

        UUID dataSetReferenceValue = UUID.fromString("5bbb5c00-ef9a-4b02-81cc-7bd7cf91a7ce");
        when(dsService.getById(dataSetReferenceValue))
                .thenReturn(mock(DataSet.class));

        dataSetParametersImporter.importDataSetParameters(workDir, importData);
        verify(parameter, times(1)).setDataSetReferenceId(dataSetReferenceValue);
        verify(paramService, times(4)).replicate(any(), any(), any(), any());
        verify(paramService, times(1)).save(existingParameter);
        verify(paramService, times(5)).save(any());
    }
}

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

package org.qubership.atp.dataset.ei;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.qubership.atp.dataset.ei.service.DataSetAttributesImporter;
import org.qubership.atp.dataset.ei.service.DataSetFileImporter;
import org.qubership.atp.dataset.ei.service.DataSetListImporter;
import org.qubership.atp.dataset.ei.service.DataSetParametersImporter;
import org.qubership.atp.dataset.ei.service.DataSetsImporter;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.dto.validation.ValidationType;

@Isolated
@ExtendWith(SpringExtension.class)
public class DataSetImportExecutorTest {

    private DataSetImportExecutor dataSetImportExecutor;
    @Mock
    private DataSetListImporter dataSetListImporter;
    @Mock
    private DataSetsImporter dataSetsImporter;
    @Mock
    private DataSetAttributesImporter dataSetAttributesImporter;
    @Mock
    private DataSetParametersImporter dataSetParametersImporter;
    @Mock
    private DataSetFileImporter dataSetFileImporter;

    @BeforeEach
    public void setUp() throws Exception {
        dataSetImportExecutor = new DataSetImportExecutor(
                dataSetListImporter,
                dataSetsImporter,
                dataSetAttributesImporter,
                dataSetParametersImporter,
                dataSetFileImporter);
    }

    @Test
    public void validateData_1() throws Exception {
        ExportImportData importData =
                new ExportImportData(UUID.randomUUID(), null, null, false, false, null, new HashMap<>(),
                        new HashMap<>(), ValidationType.VALIDATE, false);
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");

        ValidationResult result =
                dataSetImportExecutor.validateData(importData, workDir);

        Assertions.assertTrue(result.isValid());
        Assertions.assertNotNull(result.getDetails());
        Assertions.assertTrue(result.getDetails().isEmpty());
    }


    @Test
    public void validateData_2() throws Exception {
        UUID projectId = UUID.randomUUID();
        ExportImportData importData =
                new ExportImportData(projectId, null, null, false, false, null, new HashMap<>(),
                        new HashMap<>(), ValidationType.VALIDATE, false);
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");

        when(dataSetAttributesImporter.validateDataSetAttributes(workDir, Collections.emptyMap(), false))
                .thenReturn(Arrays.asList("There is a problem"));

        ValidationResult result =
                dataSetImportExecutor.validateData(importData, workDir);

        Assertions.assertFalse(result.isValid());
        Assertions.assertNotNull(result.getDetails());
        Assertions.assertEquals(1, result.getDetails().size());
    }

    @Test
    public void importData() throws Exception {
        ExportImportData importData = mock(ExportImportData.class);
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        List<UUID> importedDsl = new ArrayList();

        when(dataSetListImporter.importDataSetLists(workDir, importData)).thenReturn(importedDsl);

        dataSetImportExecutor.importData(importData, workDir);

        verify(dataSetListImporter, times(1)).importDataSetLists(workDir, importData);
        verify(dataSetsImporter, times(1)).importDataSets(workDir, importData);
        verify(dataSetAttributesImporter, times(1)).importDataSetAttributes(workDir,
                importedDsl, importData);
        verify(dataSetAttributesImporter, times(1)).importDataSetAttributeKeys(workDir, importData);
        verify(dataSetParametersImporter, times(1)).importDataSetParameters(workDir, importData);
        verify(dataSetFileImporter, times(1)).importFiles(workDir, importData);
    }
}

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.validation.ValidationType;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;

@Isolated
@ExtendWith(SpringExtension.class)
public class DataSetFileImporterTest {

    private ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private DataSetFileImporter dataSetFileImporter;
    @Mock
    private GridFsRepository gridFsRepoProvider;

    @BeforeEach
    public void setUp() throws Exception {
        objectLoaderFromDiskService = new ObjectLoaderFromDiskService();
        dataSetFileImporter =
                new DataSetFileImporter(objectLoaderFromDiskService, gridFsRepoProvider);

    }

    @Test
    public void importFiles() throws IOException {
        Path workDir = Paths.get("src/test/resources/ei/import/1d554fe3-4a15-4e1c-964a-1585e3206210");
        ExportImportData exportImportData =
                new ExportImportData(null, null, null, false, false, null, new HashMap<>(), new HashMap<>(),
                        ValidationType.VALIDATE, false);
        dataSetFileImporter.importFiles(workDir, exportImportData);

        verify(gridFsRepoProvider, times(3)).save(any(), any());
    }
}

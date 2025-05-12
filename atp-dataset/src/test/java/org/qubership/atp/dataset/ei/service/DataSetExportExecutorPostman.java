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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.ei.Constants;
import org.qubership.atp.dataset.ei.DataSetExportExecutor;
import org.qubership.atp.dataset.service.jpa.service.AbstractJpaTest;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;

@Isolated
@SpringBootTest
@ContextConfiguration(classes = {TestConfiguration.class})
@ExtendWith(SpringExtension.class)
    public class DataSetExportExecutorPostman extends AbstractJpaTest {

    @Autowired
    DataSetExportExecutor dataSetExportExecutor;


    @Test
    @Sql(scripts = {"classpath:test_data/sql/postman.export/clear_postman_export.sql",
            "classpath:test_data/sql/postman.export/postman_dsl.sql"})

    public void exportToFolder_PostmanFormat_successfulExport() throws Exception {
        String path = "src/test/resources/ei/postman";
        Path workDir = Paths.get(path);
        Files.createDirectories(workDir);
        UUID visibilityAreaId = UUID.fromString("46094b3a-dc76-46f6-9a38-e3af2a8cc9fb");
        ExportImportData exportData = new ExportImportData(visibilityAreaId, new ExportScope(), ExportFormat.POSTMAN);
        String dsl = "1ca198cf-6b55-4a59-899c-bd9808bd5ad6";
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASET_STORAGE, Collections.singleton(dsl));
        Mockito.when(macrosFeignClient.findAllByProject(any())).thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        dataSetExportExecutor.exportToFolder(exportData, workDir);

        String expectedFile1 = getFileValue(
                "src/test/resources/test_data/postman.expected/dataset/postman_test_dsl.postman_ds1.dataset.json");
        String expectedFile2 = getFileValue(
                "src/test/resources/test_data/postman.expected/dataset/postman_test_dsl.postman_ds2.dataset.json");
        String actualFileValue1 = getFileValue(
                "src/test/resources/ei/postman/dataset/postman_test_dsl.postman_ds1.dataset.json");
        String actualFileValue2 = getFileValue(
                "src/test/resources/ei/postman/dataset/postman_test_dsl.postman_ds2.dataset.json");
        File folder = new File(path + "/dataset");
        int count = folder.list().length;

        Assertions.assertEquals(2, count);
        Assertions.assertEquals(expectedFile1, actualFileValue1);
        Assertions.assertEquals(expectedFile2, actualFileValue2);

        FileUtils.deleteDirectory(new File(path));
    }


    public String getFileValue (String stringPath) throws IOException {
        Path path = Paths.get(stringPath);
      return   new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}

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

package org.qubership.atp.dataset.service.direct;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.AbstractTest;

@Isolated
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class GridFsServiceTest extends AbstractTest {

    @Autowired
    private GridFsRepository repository;

    @TempDir
    private Path tempDir;

    private File file;
    private UUID parameterUuid;

    @BeforeEach
    public void setUp() throws Exception {
//        file = folder.newFile();
        file = File.createTempFile("junit", (String)null, tempDir.toFile());
//        file = Files.createFile(tempDir).toFile();
        parameterUuid = UUID.randomUUID();
    }

    @AfterEach
    public void tearDown() {
        gridFsService.delete(parameterUuid);
    }

    @Test
    public void testUploadToGridFs_FileIsPresentInGridFs() throws IOException {
        saveFileToGridFs();
        Assertions.assertTrue(gridFsService.get(parameterUuid).isPresent());
    }

    @Test
    public void testDeleteFromGridFs_FileIsDisappearInGridFs() throws IOException {
        saveFileToGridFs();
        gridFsService.delete(parameterUuid);
        Assertions.assertFalse(gridFsService.get(parameterUuid).isPresent());
    }

    @Test
    public void testGetFromGridFs_ReturnOneFileWithExpectedContent() throws IOException {
        String er = "<sz>Test</sz>";
        Files.write(file.toPath(), er.getBytes());
        saveFileToGridFs();
        Optional<InputStream> stream = gridFsService.get(parameterUuid);
        String output = getFileContent(
                stream.orElseThrow(
                        () -> new AssertionError("File not found in gridfs by id: " + parameterUuid)));
        Assertions.assertEquals(er, output);
    }

    private String getFileContent(InputStream stream) throws IOException {
        return IOUtils.toString(stream, Charset.defaultCharset());
    }

    @Test
    public void testCopyFile_CopyOfFile_isAppeared() throws IOException {
        String er = "<sz>Test</sz>";
        Files.write(file.toPath(), er.getBytes());
        saveFileToGridFs();
        UUID targetParameterUuid = UUID.randomUUID();
        gridFsService.copy(parameterUuid, targetParameterUuid, false);
        Optional<InputStream> stream = gridFsService.get(targetParameterUuid);
        String output = getFileContent(
                stream.orElseThrow(
                        () -> new AssertionError("File not found in gridfs by id: " + targetParameterUuid)));
        Assertions.assertEquals(er, output);
    }

    @Test
    public void testGetFileInfo_It_Has_FileName_Type_ContentType() throws IOException {
        saveFileToGridFs();
        FileData fileInfo = gridFsService.getFileInfo(parameterUuid);
        Assertions.assertEquals("xml", fileInfo.getContentType());
        Assertions.assertEquals("tmp", fileInfo.getFileType());
        Assertions.assertEquals(file.getName(), fileInfo.getFileName());
    }

    private void saveFileToGridFs() throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            gridFsService.save(new FileData(file.getName(), parameterUuid, "xml"),
                    inputStream, false);
        }
    }
}

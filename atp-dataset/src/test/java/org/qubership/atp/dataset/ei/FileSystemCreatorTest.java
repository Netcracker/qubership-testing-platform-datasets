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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.ei.node.exceptions.ExportException;

@Isolated
public class FileSystemCreatorTest {

    FileSystemCreator fileSystemCreator = new FileSystemCreator();


    @TempDir
    private Path tempDir;

    @Test
    public void create_invokesWithException_exceptionInvoked() throws IOException, ExportException {
        Path temporaryPath = Files.createDirectories(tempDir.resolve("dsExport"));
        Assertions.assertThrows(ExportException.class, () ->
                fileSystemCreator.create(temporaryPath, path -> Files.createFile(path)));
    }

    @Test
    public void create_createDirectory_directoryCreatesProperly() throws ExportException, IOException {
        Path temporaryPath = tempDir.resolve("test");
        DataSet dataSet = new DataSetImpl();
        dataSet.setName("dataSet");
        fileSystemCreator.create(temporaryPath, path -> Files.createDirectory(path));
        Assertions.assertTrue(Files.exists(temporaryPath));
    }
}

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
import java.nio.file.Path;

import org.qubership.atp.dataset.model.utils.CheckedConsumer;
import org.qubership.atp.ei.node.exceptions.ExportException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileSystemCreator {

    /**
     * Creates file or directory due to {@link CheckedConsumer} logic.
     *
     * @param path     which is needed to create file/directory
     * @param consumer {@link CheckedConsumer} for creation of file or directory.
     * @throws ExportException the export exception
     */
    public void create(Path path, CheckedConsumer<Path, IOException> consumer) throws ExportException {
        try {
            consumer.accept(path);
        } catch (IOException e) {
            log.error("Can not create directory/file {} ", path.toString(), e);
            ExportException.throwException("Can not create directory/file {} ", path.toString(), e);
        }
    }
}

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

package org.qubership.atp.dataset.db;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;

import com.mongodb.client.gridfs.model.GridFSFile;


public interface GridFsRepository {

    /**
     * Remove attachment.
     *
     * @param attachmentUuid for delete.
     */
    void remove(UUID attachmentUuid);

    /**
     * Method saves file to gridfs. Overrides file, if it exist.
     *
     * @param fileData        is {@link FileData} which contains meta information for file storage.
     * @param fileInputStream file to save.
     */
    void save(FileData fileData, InputStream fileInputStream);

    /**
     * Get GridFSFile file information.
     */
    Optional<GridFSFile> getGridFsFile(UUID attachmentUuid);

    /**
     * Get file from GridFs as {@link Optional} of {@link InputStream} In case file not found,
     * return {@link Optional#empty()}.
     *
     * @param parameterUuid of {@link Parameter}
     * @return file as {@link Optional} of {@link InputStream}
     */
    Optional<InputStream> get(UUID parameterUuid);

    /**
     * Get files from GridFs as {@link Map} of {@link Optional} input stream mapped to {@link UUID}.
     *
     * @param parametersUuids {@link List} uuids of {@link Parameter}
     * @return files as {@link Map} of {@link Optional} input stream mapped to {@link UUID}
     */
    Map<UUID, Optional<InputStream>> getAll(List<UUID> parametersUuids);

    /**
     * Returns metadata of File in storage. FileName, ParameterUUID, Type(text/binary),
     * ContentType(html, sql, json,..).
     *
     * @param parameterUuid {@link UUID} of  {@link Parameter}
     * @return an {@link Optional#empty()} in case file not found or metadata is empty.
     */
    Optional<FileData> getFileInfo(UUID parameterUuid);

    void onDeleteCascade(List<UUID> parameters);

    void dropLocalThreadCache();
}


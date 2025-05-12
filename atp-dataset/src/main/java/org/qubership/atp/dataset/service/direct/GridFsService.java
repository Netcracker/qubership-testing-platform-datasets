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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;

public interface GridFsService {

    void save(FileData fileData, InputStream file, boolean isTestPlan);

    void saveAll(List<FileData> filesData, MultipartFile file) throws IOException;

    void delete(UUID parameterUuid);

    Optional<InputStream> get(UUID parameterUuid);

    void copy(UUID sourceParameterUuid, UUID targetParameterUuid, boolean isTestPlan);

    void copyIfExist(UUID sourceParameterUuid, UUID targetParameterUuid, boolean isTestPlan);

    FileData getFileInfo(UUID parameterUuid);

    /**
     * Get files from GridFs as {@link Map} of {@link Optional} input stream mapped to {@link UUID}.
     *
     * @param parametersUuids {@link List} uuids of {@link Parameter}
     * @return files as {@link Map} of {@link Optional} input stream mapped to {@link UUID}
     */
    Map<UUID, Optional<InputStream>> getAll(List<UUID> parametersUuids);

    /**
     * Get GridFSFile file information.
     */
    GridFSFile getGridFsFile(UUID attachmentUuid);
}

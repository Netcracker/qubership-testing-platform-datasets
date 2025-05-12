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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.springframework.beans.factory.annotation.Autowired;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridFsRepositoryImpl implements GridFsRepository {

    private final GridFSBucket gridFsBucket;

    private ThreadLocal<Map<UUID, Optional<FileData>>> cachedFileInfo =
            ThreadLocal.withInitial(() -> new ConcurrentHashMap<>());

    @Autowired
    public GridFsRepositoryImpl(GridFSBucket gridFsBucket) {
        this.gridFsBucket = gridFsBucket;
    }

    /**
     * Remove attachment.
     *
     * @param attachmentUuid for delete.
     */
    @Override
    public void remove(UUID attachmentUuid) {
        Optional<GridFSFile> res = getGridFsFile(attachmentUuid);
        if (res.isPresent()) {
            gridFsBucket.delete(res.get().getObjectId());
            cachedFileInfo.get().remove(attachmentUuid);
        } else {
            log.error("Can not found gridFs file with id: {}", attachmentUuid);
        }
    }

    /**
     * Get GridFS file.
     */
    public Optional<GridFSFile> getGridFsFile(UUID attachmentUuid) {
        Document filter = new Document().append("metadata.attachmentUuid", attachmentUuid);
        GridFSFile file = gridFsBucket.find(filter).first();
        if (file != null) {
            return Optional.of(file);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Method saves file to gridfs. Overrides file, if it exist.
     *
     * @param fileData        is {@link FileData} which contains meta information for file storage.
     * @param fileInputStream file to save.
     */
    @Override
    public void save(FileData fileData, InputStream fileInputStream) {
        remove(fileData.getParameterUuid());
        GridFSUploadOptions uploadOptions = buildOptions(fileData);
        gridFsBucket.uploadFromStream(fileData.getParameterUuid().toString(), fileInputStream, uploadOptions);
        cachedFileInfo.get().put(fileData.getParameterUuid(), Optional.of(fileData));
    }

    private GridFSUploadOptions buildOptions(FileData fileData) {
        return new GridFSUploadOptions().chunkSizeBytes(1024)
                .metadata(new Document("type", fileData.getFileType())
                        .append("uploadDate", LocalDateTime.now().toString())
                        .append("attachmentUuid", fileData.getParameterUuid())
                        .append("contentType", fileData.getContentType())
                        .append("fileName", fileData.getFileName())
                        .append("url", fileData.getUrl())
                );
    }

    /**
     * Get file from GridFs as {@link Optional} of {@link InputStream} In case file not found,
     * return {@link Optional#empty()}.
     *
     * @param parameterUuid of {@link Parameter}
     * @return file as {@link Optional} of {@link InputStream}
     */
    @Override
    public Optional<InputStream> get(UUID parameterUuid) {
        GridFSFile attachment = gridFsBucket.find(filter(parameterUuid)).first();
        if (attachment == null) {
            return Optional.empty();
        }
        return Optional.of(gridFsBucket.openDownloadStream(attachment.getId()));
    }

    /**
     * Get files from GridFs as {@link Map} of {@link Optional} input stream mapped to {@link UUID}.
     *
     * @param parametersUuids {@link List} uuids of {@link Parameter}
     * @return files as {@link Map} of {@link Optional} input stream mapped to {@link UUID}
     */
    @Override
    public Map<UUID, Optional<InputStream>> getAll(List<UUID> parametersUuids) {
        List<GridFSFile> files = new ArrayList<>();
        gridFsBucket.find(Filters.in("metadata.attachmentUuid", parametersUuids)).into(files);
        Map<UUID, Optional<InputStream>> fileToUuid = new HashMap<>();
        files.forEach(file -> {
            UUID id = (UUID) Objects.requireNonNull(file.getMetadata()).get("attachmentUuid");
            fileToUuid.put(id, Optional.of(gridFsBucket.openDownloadStream(file.getId())));
        });
        return fileToUuid;
    }

    /**
     * Returns metadata of File in storage. FileName, ParameterUUID, Type(text/binary),
     * ContentType(html, sql, json,..).
     *
     * @param parameterUuid {@link UUID} of  {@link Parameter}
     * @return an {@link Optional#empty()} in case file not found or metadata is empty.
     */
    @Override
    public Optional<FileData> getFileInfo(UUID parameterUuid) {
        Optional<FileData> fileInfo =
                cachedFileInfo.get().computeIfAbsent(parameterUuid, id -> findFileInfoInStore(id));
        return fileInfo;
    }

    private Optional<FileData> findFileInfoInStore(UUID parameterUuid) {
        GridFSFile attachment = gridFsBucket.find(filter(parameterUuid)).first();
        if (attachment == null) {
            return Optional.empty();
        }
        Document metadata = attachment.getMetadata();
        if (Objects.isNull(metadata)) {
            return Optional.empty();
        }
        return Optional.of(getMetaDataInfo(metadata));
    }

    private FileData getMetaDataInfo(Document metadata) {
        String contentType = metadata.getString("contentType");
        String fileName = metadata.getString("fileName");
        UUID parameterUuid = metadata.get("attachmentUuid", UUID.class);
        return new FileData(fileName, parameterUuid, contentType);
    }

    private Document filter(UUID attachmentUuid) {
        return new Document().append("metadata.attachmentUuid", attachmentUuid);
    }

    @Override
    public void onDeleteCascade(List<UUID> parameters) {
        parameters.forEach(this::remove);
    }

    public void dropLocalThreadCache() {
        cachedFileInfo.remove();
    }
}

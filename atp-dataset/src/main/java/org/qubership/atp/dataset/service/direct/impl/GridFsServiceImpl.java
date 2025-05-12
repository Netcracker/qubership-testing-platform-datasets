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

package org.qubership.atp.dataset.service.direct.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.db.ParameterRepository;
import org.qubership.atp.dataset.exception.file.FileDsCopyException;
import org.qubership.atp.dataset.exception.file.FileDsNotFoundException;
import org.qubership.atp.dataset.exception.file.FileDsNotFoundToCopyException;
import org.qubership.atp.dataset.exception.file.FileDsSaveException;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.direct.ClearCacheService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
@Service
public class GridFsServiceImpl implements GridFsService {

    private final GridFsRepository repository;
    private final ParameterRepository parameterRepository;
    private final DataSetListSnapshotService commitEntityService;
    private final ClearCacheService clearCacheService;

    @Override
    public void save(FileData fileData, InputStream file, boolean isTestPlan) {
        log.debug("start save(fileData: {}, InputStream file)", fileData);
        repository.save(fileData, file);
        if (!isTestPlan) {
            Parameter parameter = parameterRepository.getById(fileData.getParameterUuid());
            if (parameter != null) {
                UUID dataSetListId = parameter.getDataSet().getDataSetList().getId();
                commitEntityService.findAndCommitIfExists(dataSetListId);
                clearCacheService.evictDatasetListContextCache(parameter.getDataSet().getId());
                clearCacheService.evictParameterCache(parameter.getId());
            }
        }
    }

    @Override
    public GridFSFile getGridFsFile(UUID attachmentUuid) {
        Optional<GridFSFile> file = repository.getGridFsFile(attachmentUuid);
        if (file.isPresent()) {
            return file.get();
        } else {
            log.error("Can not found gridFs file with id: {}", attachmentUuid);
            return null;
        }
    }

    @Override
    public void saveAll(List<FileData> filesData, MultipartFile file) {
        log.info("start saveAll(filesData: {}, file: {})", filesData, file.getOriginalFilename());
        filesData.forEach(fileData -> {
            try {
                repository.save(fileData, file.getInputStream());
                Parameter parameter = parameterRepository.getById(fileData.getParameterUuid());
                clearCacheService.evictDatasetListContextCache(parameter.getDataSet().getId());
                clearCacheService.evictParameterCache(parameter.getId());
            } catch (IOException e) {
                log.error("Cannot bulk save file resource. fileData: {}, file: {}", fileData,
                        file.getOriginalFilename(), e);
                throw new FileDsSaveException();
            }
        });
    }

    @Override
    public void delete(UUID parameterUuid) {
        repository.remove(parameterUuid);
        Parameter parameter = parameterRepository.getById(parameterUuid);
        if (parameter != null) {
            if (parameter.isOverlap()) {
                ParameterOverlap parameterOverlap = parameter.asOverlap();
                parameterRepository.delete(parameterOverlap);
            }
            DataSet ds = parameter.getDataSet();
            UUID dataSetListId = ds.getDataSetList().getId();
            commitEntityService.findAndCommitIfExists(dataSetListId);
            clearCacheService.evictDatasetListContextCache(ds.getId());
        }
    }

    @Override
    public Optional<InputStream> get(UUID parameterUuid) {
        return repository.get(parameterUuid);
    }

    /**
     * Get files from GridFs as {@link Map} of {@link Optional} input stream mapped to {@link UUID}.
     *
     * @param parametersUuids {@link List} uuids of {@link Parameter}
     * @return files as {@link Map} of {@link Optional} input stream mapped to {@link UUID}
     */
    @Override
    public Map<UUID, Optional<InputStream>> getAll(List<UUID> parametersUuids) {
        return repository.getAll(parametersUuids);
    }

    @Override
    public void copy(UUID sourceParameterUuid, UUID targetParameterUuid, boolean isTestPlan) {
        Optional<InputStream> streamOptional = repository.get(sourceParameterUuid);
        Optional<FileData> parameterInfo = repository.getFileInfo(sourceParameterUuid);
        InputStream stream = streamOptional.orElseThrow(() -> {
            log.error("Can not create copy of file with id: " + sourceParameterUuid + "' file doesn't exist.");
            return new FileDsNotFoundToCopyException();
        });
        FileData fileData = parameterInfo.orElseThrow(() -> {
            log.error("Can not create copy of file with UUID '" + sourceParameterUuid + "', has no information about"
                    + " [Type, FileName, AttachmentUUid, ContentType].");
            return new FileDsCopyException();
        });
        fileData.setParameterUuid(targetParameterUuid);
        save(fileData, stream, isTestPlan);
    }

    /**
     * Copies file by source parameter id to target parameter.
     */
    public void copyIfExist(UUID sourceParameterUuid, UUID targetParameterUuid, boolean isTestPlan) {
        log.debug("copyIfExist (sourceParameterId: {}, targetParameterId: {}, isTestPlan: {})",
                sourceParameterUuid, targetParameterUuid, isTestPlan);
        InputStream inputStream = repository.get(sourceParameterUuid).orElse(null);
        FileData fileData = repository.getFileInfo(sourceParameterUuid).orElse(null);
        if (Objects.nonNull(inputStream) && Objects.nonNull(fileData)) {
            log.debug("File found on source parameter - copying");
            fileData.setParameterUuid(targetParameterUuid);
            save(fileData, inputStream, isTestPlan);
        }
    }

    @Override
    public FileData getFileInfo(UUID parameterUuid) {
        return repository.getFileInfo(parameterUuid)
                .orElseThrow(() -> {
                    log.error("File not found by id: " + parameterUuid);
                    return new FileDsNotFoundException();
                });
    }
}

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

package org.qubership.atp.dataset.service.rest.facade;

import static org.qubership.atp.dataset.service.jpa.impl.MetricsService.ATP_MAX_SIZE_DOWNLOAD_FILE_PER_PROJECT_TOTAL;
import static org.qubership.atp.dataset.service.jpa.impl.MetricsService.ATP_MAX_SIZE_UPLOAD_FILE_PER_PROJECT_TOTAL;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.jpa.impl.MetricsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.google.common.base.Preconditions;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AttachmentControllerFacade {
    private final GridFsService gridFsService;
    private final ParameterService parameterService;
    private final ConcurrentModificationService concurrentModificationService;
    private final MetricsService metricsService;

    /**
     * Download file by parameter id.
     */
    public ResponseEntity<InputStreamResource> getAttachmentByParameterId(UUID parameterUuid) {
        UUID projectId = parameterService.get(parameterUuid).getDataSet().getDataSetList().getVisibilityArea().getId();
        Optional<InputStream> optional = gridFsService.get(parameterUuid);
        FileData info = gridFsService.getFileInfo(parameterUuid);

        if (!optional.isPresent()) {
            return ResponseEntity.noContent().build();
        }
        InputStreamResource inputStreamResource = new InputStreamResource(optional.get());
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(info.getFileName(), StandardCharsets.UTF_8)
                .build();
        GridFSFile attachment = gridFsService.getGridFsFile(parameterUuid);
        metricsService.registerMetricFileSize(attachment.getLength(), projectId,
                ATP_MAX_SIZE_DOWNLOAD_FILE_PER_PROJECT_TOTAL);
        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(info.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(inputStreamResource);
    }

    /**
     * Download file by attribute and dataset id.
     */
    public ResponseEntity<InputStreamResource> getAttachmentByAttributeIdAndDatasetId(
            UUID attributeId, UUID datasetId) {
        Parameter parameter = parameterService.getByDataSetIdAttributeId(datasetId, attributeId);
        Preconditions.checkNotNull(parameter.getId(), "Could not get parameters id");
        return getAttachmentByParameterId(parameter.getId());
    }

    /**
     * Upload file by parameter id.
     */
    public FileData uploadByParameterId(UUID parameterUuid, String contentType, String fileName, InputStream file) {
        UUID projectId = parameterService.get(parameterUuid).getDataSet().getDataSetList().getVisibilityArea().getId();
        FileData fileData = parameterService.upload(parameterUuid, contentType, fileName, file);
        GridFSFile attachment = gridFsService.getGridFsFile(parameterUuid);
        metricsService.registerMetricFileSize(attachment.getLength(), projectId,
                ATP_MAX_SIZE_UPLOAD_FILE_PER_PROJECT_TOTAL);
        return fileData;
    }

    /**
     * Upload file and store it to GridFs.
     */
    public ResponseEntity<FileData> uploadByAttributeIdAndDatasetId(UUID attributeId, UUID datasetId,
                                                                    String contentType, String fileName,
                                                                    List<UUID> attrPathIds, UUID dataSetListId,
                                                                    Long modifiedWhen, InputStream file) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        FileData fileData = parameterService.upload(datasetId, attributeId, attrPathIds,
                contentType, fileName, file);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(fileData)
                : ResponseEntity.ok(fileData);
    }

    /**
     * Delete attachment from parameter.
     */
    public ResponseEntity<Void> deleteByParameterId(@PathVariable("parameterUuid") UUID parameterId) {
        parameterService.clearAttachment(parameterId);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete attachment from parameter.
     */
    public ResponseEntity<Void> deleteByAttributeIdAndDatasetId(UUID attributeId, UUID datasetId) {
        Parameter parameter = parameterService.getByDataSetIdAttributeId(datasetId, attributeId);
        Preconditions.checkNotNull(parameter.getId(), "Could not get parameters id");
        return deleteByParameterId(parameter.getId());
    }
}

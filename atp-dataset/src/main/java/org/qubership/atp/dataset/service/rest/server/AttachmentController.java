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

package org.qubership.atp.dataset.service.rest.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.rest.facade.AttachmentControllerFacade;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attachment")
@RequiredArgsConstructor
public class AttachmentController /*extends AttachmentControllerApi*/ {

    private final AttachmentControllerFacade attachmentControllerFacade;

    /**
     * Download file by parameter id.
     *
     * @param parameterUuid mapped to file in gridFs.
     * @return file;
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@parameterServiceImpl.get(#parameterUuid).getDataSet().getDataSetList().getVisibilityArea().getId(),"
            + "'READ')")
    @AuditAction(auditAction = "Get file by parameter: {{#parameterUuid}}")
    @GetMapping("/{parameterUuid}")
    public ResponseEntity<InputStreamResource> getAttachmentByParameterId(
            @PathVariable("parameterUuid") UUID parameterUuid) {
        return attachmentControllerFacade.getAttachmentByParameterId(parameterUuid);
    }

    /**
     * Download file by attribute and dataset id.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@dataSetServiceImpl.get(#datasetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @AuditAction(auditAction = "Get file by attribute: {{#attributeId}} and dataset: {{#datasetId}}")
    @GetMapping("/attributeId/{attributeId}/dataset/{datasetId}")
    public ResponseEntity<InputStreamResource> getAttachmentByAttributeIdAndDatasetId(
            @PathVariable("attributeId") UUID attributeId,
            @PathVariable("datasetId") UUID datasetId) {
        return attachmentControllerFacade.getAttachmentByAttributeIdAndDatasetId(attributeId, datasetId);
    }

    /**
     * Upload file and store it to GridFS.
     *
     * @param parameterUuid target {@link Parameter#getId()}
     * @param contentType   content type in http request format
     * @param fileName      target file name
     * @param file          InputStream of file
     * @return transfer object of {@link FileData}
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@parameterServiceImpl.get(#parameterUuid).getDataSet().getDataSetList().getVisibilityArea().getId(),"
            + "'UPDATE')")
    @AuditAction(auditAction = "Upload file by parameter: {{#parameterUuid}}")
    @PostMapping("/{parameterUuid}")
    public FileData uploadByParameterId(@PathVariable("parameterUuid") UUID parameterUuid,
                           @RequestParam("type") String contentType,
                           @RequestParam("fileName") String fileName,
                           InputStream file) throws IOException {
        return attachmentControllerFacade.uploadByParameterId(parameterUuid, contentType, fileName, file);
    }

    /**
     * Upload file and store it to GridFS.
     *
     * @param contentType content type in http request format
     * @param fileName    target file name
     * @param file        InputStream of file
     * @return transfer object of {@link FileData}
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@dataSetServiceImpl.get(#datasetId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @AuditAction(auditAction = "Upload file by attribute: {{#attributeId}} and dataset: {{#datasetId}}")
    @PostMapping("/attributeId/{attributeId}/dataset/{datasetId}")
    public ResponseEntity<FileData> uploadByAttributeIdAndDatasetId(@PathVariable("attributeId") UUID attributeId,
                                           @PathVariable("datasetId") UUID datasetId,
                                           @RequestParam("type") String contentType,
                                           @RequestParam("fileName") String fileName,
                                           @RequestParam(value = "attrPath", required = false) List<UUID> attrPathIds,
                                           @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
                                           @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
                                           InputStream file) {
        return attachmentControllerFacade.uploadByAttributeIdAndDatasetId(attributeId, datasetId, contentType,
                fileName, attrPathIds, dataSetListId, modifiedWhen, file);
    }

    /**
     * Delete attachment from parameter.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@parameterServiceImpl.get(#parameterId).getDataSet().getDataSetList().getVisibilityArea().getId(),"
            + "'DELETE')")
    @AuditAction(auditAction = "Delete file by parameter: {{#parameterUuid}}")
    @DeleteMapping("/{parameterUuid}")
    public ResponseEntity<Void> deleteByParameterId(@PathVariable("parameterUuid") UUID parameterId) {
        return attachmentControllerFacade.deleteByParameterId(parameterId);
    }

    /**
     * Delete attachment from parameter.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@dataSetServiceImpl.get(#datasetId).getDataSetList().getVisibilityArea().getId(),'DELETE')")
    @AuditAction(auditAction = "Delete file by attribute: {{#attributeId}} and dataset: {{#datasetId}}")
    @DeleteMapping("/attributeId/{attributeId}/dataset/{datasetId}")
    public ResponseEntity<Void> deleteByAttributeIdAndDatasetId(@PathVariable("attributeId") UUID attributeId,
                                 @PathVariable("datasetId") UUID datasetId) {
        return attachmentControllerFacade.deleteByAttributeIdAndDatasetId(attributeId, datasetId);
    }
}

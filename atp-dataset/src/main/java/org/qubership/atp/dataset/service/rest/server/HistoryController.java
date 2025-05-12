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

import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemResponseDto;
import org.qubership.atp.dataset.service.rest.dto.versioning.UiManDataSetListJDto;
import org.qubership.atp.dataset.versioning.service.JaversHistoryService;
import org.qubership.atp.dataset.versioning.service.RestoreService;
import org.qubership.atp.dataset.versioning.service.RevisionDetailsService;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class HistoryController implements HistoryControllerApi {

    private final JaversHistoryService javersHistoryService;
    private final RevisionDetailsService revisionDetailsService;
    private final RestoreService restoreService;

    @PreAuthorize("@entityAccess.checkAccess(#projectId,'READ')")
    @AuditAction(auditAction = "Get all history for project: {{#projectId}}")
    @Override
    public ResponseEntity<HistoryItemResponseDto> getAllHistory(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("id") UUID id,
            @Valid @RequestParam(value = "offset", required = false, defaultValue = "0")  Integer offset,
            @Valid @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        HistoryItemResponseDto response = javersHistoryService.getAllHistory(id, offset, limit);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'READ')")
    @AuditAction(auditAction = "Get revision details for project: {{#projectId}}")
    @Override
    public ResponseEntity<UiManDataSetListJDto> getRevisionDetails(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("entityId") UUID entityId,
            @PathVariable("revision") Integer revision) {
        UiManDataSetListJDto revisionDetails = revisionDetailsService.getRevisionDetails(revision, entityId);
        return ResponseEntity.ok(revisionDetails);
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectId,'UPDATE')")
    @AuditAction(auditAction = "Restore to revision: {{#revisionId}}")
    @Override
    public ResponseEntity<Void> restoreToRevision(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("id") UUID id,
            @PathVariable("revisionId") Integer revisionId) {
        restoreService.restore(id, revisionId);
        return ResponseEntity.ok().build();
    }
}

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

package org.qubership.atp.dataset.service.rest.server.v2;

import java.net.URI;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.api.ParameterRequest;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v2/parameter")
@RequiredArgsConstructor
public class ParameterControllerV2 {

    private final ParameterService parameterService;
    private final DataSetService dsService;
    private final JpaParameterService jpaParameterService;
    private final ConcurrentModificationService concurrentModificationService;

    /**
     * Updates value of the selected parameter.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/ds/{dataSetId}/attribute/{attributeId}")
    @AuditAction(auditAction = "Update parameter value for dataset: {{#dataSetId}} and attribute: {{#attributeId}}")
    @Operation(summary = "Updates value of the selected parameter.")
    public ResponseEntity<Parameter> update(
            @PathVariable("dataSetId") UUID dataSetId,
            @PathVariable("attributeId") UUID attributeId,
            @RequestBody ParameterRequest requestBody) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(requestBody.getDataSetListId(),
                                                                            requestBody.getModifiedWhen());
        Parameter parameter = parameterService.set(dataSetId, attributeId, requestBody.getAttrPathIds(),
                requestBody.getValue(), requestBody.getDataSetReference(), requestBody.getListValueReference());
        dsService.evictAllAffectedDatasetsFromContextCacheByDsId(dataSetId);
        return HttpStatus.IM_USED.equals(httpStatus) ? ResponseEntity.status(httpStatus).body(parameter)
                                                     : ResponseEntity.ok(parameter);
    }

    /**
     * Creates new parameter with provided text.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'CREATE')")
    @PutMapping("/ds/{dataSetId}/attribute/{attributeId}")
    @AuditAction(auditAction = "Create new parameter for dataset: {{#dataSetId}} and attribute: {{#attributeId}}")
    @Operation(summary = "Creates new parameter with provided text.")
    public ResponseEntity<UUID> create(
            @PathVariable("dataSetId") UUID dataSetId,
            @PathVariable("attributeId") UUID attributeId,
            @RequestBody ParameterRequest requestBody,
            HttpServletRequest request) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(requestBody.getDataSetListId(),
                requestBody.getModifiedWhen());
        UUID parameterId = jpaParameterService.createParameter(dataSetId, attributeId, requestBody.getValue(),
                        requestBody.getDataSetReference(), requestBody.getListValueReference()).getId();
        dsService.evictAllAffectedDatasetsFromContextCacheByDsId(dataSetId);
        String url = request.getRequestURL().append("/").append(parameterId.toString()).toString();
        URI uri = URI.create(url);
        return HttpStatus.IM_USED.equals(httpStatus) ? ResponseEntity.status(httpStatus).location(uri).body(parameterId)
                : ResponseEntity.created(uri).body(parameterId);
    }

    /**
     * Bulk update value of parameters v2.
     * Receive UUID dataSetId - for bulk updating
     * Receive List UUID listIdsParametersToChange - for deleting one list parameter and replacing its value
     * in other Parameters
     */
    @PreAuthorize("@entityAccess.checkAccess(#dataSetListId,'UPDATE')")
    @PostMapping("/update/bulk")
    @AuditAction(auditAction = "Bulk update value of parameters v2")
    @Operation(summary = "Updates value of the selected parameters. Please do not use for files or overlaps! v2")
    public ResponseEntity<Void> updateParameters(
            @RequestBody ParameterRequest requestBody) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(requestBody.getDataSetListId(),
                                                                            requestBody.getModifiedWhen());
        boolean updated = jpaParameterService.bulkUpdateValues(requestBody.getValue(),
                requestBody.getDataSetReference(), requestBody.getListValueReference(),
                requestBody.getDataSetListId(), requestBody.getDataSetId(), requestBody.getListIdsParametersToChange());
        dsService.evictAllAffectedDatasetsFromContextCacheByDslId(requestBody.getDataSetListId());
        return updated ? ResponseEntity.status(httpStatus).build()
                       : ResponseEntity.status(HttpStatus.NOT_MODIFIED.value()).build();
    }
}

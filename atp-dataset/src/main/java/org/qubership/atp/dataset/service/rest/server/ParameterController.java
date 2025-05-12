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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.qubership.atp.dataset.exception.dataset.DataSetNotFoundException;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.rest.QueryParamFlag;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/parameter")
@RequiredArgsConstructor
public class ParameterController {

    private final ParameterService parameterService;
    private final DataSetService dsService;
    private final JpaParameterService jpaParameterService;
    private final ConcurrentModificationService concurrentModificationService;

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
            @RequestParam(value = "value", required = false) String value,
            @RequestParam(value = "dataSetReference", required = false) UUID dataSetReference,
            @RequestParam(value = "listValueReference", required = false) UUID listValueReference,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
            HttpServletRequest request) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        UUID parameterId = jpaParameterService
                .createParameter(dataSetId, attributeId, value, dataSetReference, listValueReference).getId();
        dsService.evictAllAffectedDatasetsFromContextCacheByDsId(dataSetId);
        String url = request.getRequestURL().append("/").append(parameterId.toString()).toString();
        URI uri = URI.create(url);
        return HttpStatus.IM_USED.equals(httpStatus) ? ResponseEntity.status(httpStatus).location(uri).body(parameterId)
                                                     : ResponseEntity.created(uri).body(parameterId);
    }

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
            @RequestParam(value = "value", required = false) String value,
            @RequestParam(value = "dataSetReference", required = false) UUID dataSetReference,
            @RequestParam(value = "listValueReference", required = false) UUID listValueReference,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
            @RequestBody(required = false) List<UUID> attrPathIds) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        Parameter parameter =
                parameterService.set(dataSetId, attributeId, attrPathIds, value, dataSetReference, listValueReference);
        dsService.evictAllAffectedDatasetsFromContextCacheByDsId(dataSetId);
        return HttpStatus.IM_USED.equals(httpStatus) ? ResponseEntity.status(httpStatus).body(parameter)
                                                     : ResponseEntity.ok(parameter);
    }

    /**
     * Bulk update value of parameters.
     * Receive UUID dataSetId - for bulk updating
     * Receive List UUID listIdsParametersToChange - for deleting one list parameter and replacing its value
     * in other Parameters
     */
    @PreAuthorize("@entityAccess.checkAccess(#dataSetListId,'UPDATE')")
    @PostMapping("/update/bulk")
    @AuditAction(auditAction = "Bulk update value of parameters")
    @Operation(summary = "Updates value of the selected parameters. Please do not use for files or overlaps!")
    public ResponseEntity<Void> updateParameters(
            @RequestParam(value = "value", required = false) String stringValue,
            @RequestParam(value = "dataSetReference", required = false) UUID dataSetReference,
            @RequestParam(value = "listValueReference", required = false) UUID listValueReference,
            @RequestParam(value = "dataSetListId") UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
            @RequestParam(value = "dataSetId", required = false) UUID dataSetId,
            @RequestBody(required = false) List<UUID> listIdsParametersToChange) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        boolean updated = jpaParameterService.bulkUpdateValues(stringValue, dataSetReference, listValueReference,
                                                               dataSetListId, dataSetId, listIdsParametersToChange);
        dsService.evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        return updated ? ResponseEntity.status(httpStatus).build()
                       : ResponseEntity.status(HttpStatus.NOT_MODIFIED.value()).build();
    }

    /**
     * Bulk update value of parameters.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@attributeServiceImpl.get(#attributeId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping(value = "/bulk/attribute/{attributeId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Bulk update value of parameters in attribute: {{#attributeId}}")
    public ResponseEntity<List<Object>> bulkUpdateAttribute(@PathVariable("attributeId") UUID attributeId,
                                                      @RequestParam(value = "dataSetListId") UUID dataSetListId,
                                                      @RequestParam(value = "value", required = false) String value,
                                                      @RequestParam(value = "file", required = false)
                                                              MultipartFile file,
                                                      @RequestParam(value = "dataSetsIds", required = false)
                                                              List<UUID> dataSetsIds,
                                                      @RequestParam(value = "modifiedWhen", required = false)
                                                              Long modifiedWhen,
                                                      @RequestParam(value = "attrPathIds",required = false)
                                                              List<UUID> attrPathIds) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        List<Object> result =
                parameterService.bulkUpdate(dataSetListId, attrPathIds, dataSetsIds, attributeId, value, file);
        dsService.evictAllAffectedDatasetsFromContextCacheByDslId(dataSetListId);
        return HttpStatus.IM_USED.equals(httpStatus) ? ResponseEntity.status(httpStatus).body(result)
                                                     : ResponseEntity.ok().body(result);
    }

    /**
     * Deletes value of the selected parameter.
     * @param dataSetId   dataSetId
     * @param attributeId attributeId
     * @return HttpStatus
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'DELETE')")
    @DeleteMapping
    @AuditAction(auditAction = "Delete parameter value for dataset: {{#dataSetId}} and attribute: {{#attributeId}}")
    @Operation(summary = "Deletes value of the selected parameter.")
    public ResponseEntity<Void> delete(
            @RequestParam("dataSetId") UUID dataSetId,
            @RequestParam("attributeId") UUID attributeId,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        boolean deleted = parameterService.delete(attributeId, dataSetId, dataSetListId, null);
        dsService.evictAllAffectedDatasetsFromContextCacheByDsId(dataSetId);
        return deleted
                ? HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_MODIFIED.value()).build();
    }

    /**
     * Returns all parameters which uses provided {@link ListValue}.
     * @param listValueId {@link UUID} of {@link ListValue#getId()}
     * @return all affected {@link Parameter} which uses {@link ListValue}
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/affected/by/")
    @AuditAction(auditAction = "Get all parameters which uses provided ListValue")
    @Operation(summary = "Get all parameters which uses provided ListValue")
    public List<?> getAffectedParametersByListValue(
            @RequestParam(value = "listValueId", required = false) UUID listValueId,
            @RequestParam(value = "full", required = false) QueryParamFlag full) {
        boolean withDsl = QueryParamFlag.isPresent(full);
        return parameterService.getParametersAffectedByListValue(listValueId, withDsl);
    }

    /**
     * Returns all parameters which uses provided {@link ListValue}.
     *
     * @param listValueIds list of {@link UUID} of {@link ListValue#getId()}
     * @return all affected data which uses {@link ListValue}
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/affected/by/listValues")
    @AuditAction(auditAction = "Get all parameters which uses provided ListValue")
    @Operation(summary = "Get all parameters which use provided ListValue")
    public List<TableResponse> getAffectedParametersByListValues(
            @RequestBody(required = false) List<UUID> listValueIds) {
        return parameterService.getParametersAffectedByListValues(listValueIds);
    }

    /**
     * Returns the parameters of selected DSL and DS.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/ds/{dataSetId}")
    @AuditAction(auditAction = "Get parameters value in dataset: {{#dataSetId}}")
    @Operation(
            summary = "Returns the parameters of selected DS.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Parameter.class)))))
    public List<Parameter> getDataSetParameters(@PathVariable("dataSetId") UUID dataSetId) {
        DataSet dataSet = dsService.get(dataSetId);
        if (dataSet != null) {
            return dataSet.getParameters();
        }
        return Collections.emptyList();
    }

    /**
     * Method returns original value of overridden parameter.
     *
     * @param dataSetId    - dataset with overridden value
     * @param targetAttrId - !!!datasetId!!! which parameter has been overridden
     * @param attrPathIds  - path of attributes to overridden value
     * @return - original value overridden parameter.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @PostMapping("/get/original")
    @AuditAction(auditAction = "Get original parameter value for dataset: {{#dataSetId}} "
            + "and attribute: {{#targetAttrId}}")
    public ResponseEntity<Parameter> getOriginalParameter(@RequestParam("dataSetId") UUID dataSetId,
                                                          @RequestParam("targetAttrId") UUID targetAttrId,
                                                          @RequestParam(value = "dataSetListId", required = false)
                                                                  UUID dataSetListId,
                                                          @RequestParam(value = "modifiedWhen", required = false)
                                                                  Long modifiedWhen,
                                                          @RequestBody(required = false) List<UUID> attrPathIds) {
        DataSet dataSet = dsService.get(dataSetId);
        if (dataSet == null) {
            log.error("Data Set not found. Id='" + dataSetId + "'");
            throw new DataSetNotFoundException();
        }
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        OverlapIterator overlapIterator = OverlapIterator.create(dataSet, targetAttrId, attrPathIds);
        overlapIterator.next(); //OverlapParameter
        OverlapItem target = overlapIterator.next();//Underneath overlap parameter.
        Parameter parameter = target.getParameter().orElse(null);
        return ResponseEntity.status(httpStatus).body(parameter);
    }
}

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

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.qubership.atp.dataset.exception.dataset.DataSetContextParseException;
import org.qubership.atp.dataset.exception.dataset.DataSetIdNotSpecifiedException;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.api.CompareDsRequest;
import org.qubership.atp.dataset.model.api.CompareDsResponse;
import org.qubership.atp.dataset.model.api.CopyDsAttributeBulkRequest;
import org.qubership.atp.dataset.model.api.CopyDsAttributeRequest;
import org.qubership.atp.dataset.model.api.CopyDsAttributeResponse;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsRequest;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsResponse;
import org.qubership.atp.dataset.model.enums.CompareStatus;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.CheckedConsumer;
import org.qubership.atp.dataset.model.utils.ObjectShortResponse;
import org.qubership.atp.dataset.service.direct.CompareService;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetTree;
import org.qubership.atp.dataset.service.rest.PaginationResponse;
import org.qubership.atp.dataset.service.rest.QueryParamFlag;
import org.qubership.atp.dataset.service.rest.dto.manager.AbstractEntityResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mysema.commons.lang.Pair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ds")
@RequiredArgsConstructor
public class DataSetController {

    private final DataSetService dsService;
    private final ObjectMapper jsonObjectMapper;
    private final ConcurrentModificationService concurrentModificationService;
    private final JpaDataSetService dataSetService;
    private final CompareService compare;

    /**
     * Creates new DS with name provided.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(), 'CREATE')")
    @PutMapping("/{dataSetListId}/ds")
    @AuditAction(auditAction = "Creates new dataset with name: {{#name}} in dataset list: {{#dataSetListId}}")
    @Operation(summary = "Creates new DS with name provided.")
    public ResponseEntity<UUID> create(@PathVariable("dataSetListId") UUID dataSetListId,
                                       @RequestParam("name") String name,
                                       HttpServletRequest request,
                                       @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        UUID dsId = dataSetService.create(name, dataSetListId).getId();

        String url = request.getRequestURL().append("/").append(dsId.toString()).toString();
        URI uri = URI.create(url);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).location(uri).body(dsId)
                : ResponseEntity.created(uri).body(dsId);
    }

    /**
     * Deletes selected DS.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'DELETE')")
    @DeleteMapping("/{dataSetId}")
    @AuditAction(auditAction = "Delete dataset: {{#dataSetId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes selected DS.")
    public ResponseEntity<Void> delete(@PathVariable("dataSetId") UUID dataSetId,
                                       @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
                                       @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        dsService.delete(dataSetId);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * Returns all dataSets.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping
    @AuditAction(auditAction = "Get all dataSets")
    @Operation(
            summary = "Returns all dataSets.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSet.class)))))
    public List<DataSet> getDataSets() {
        return dsService.getAll();
    }

    /**
     * Returns all dataSets names with pagination.
     * Filter by projectId (visibility area id), name
     *
     * @return list of dataset ids and names
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "#visibilityAreaId, 'READ')")
    @GetMapping("/find-name-pagination")
    @AuditAction(auditAction = "Get dataset: {{#name}} in project: {{#visibilityAreaId}}")
    @Operation(
            summary = "Returns all dataSets.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSet.class)))))
    public Page<AbstractEntityResponse> getDataSetsByName(@RequestParam("name") String name,
                                                          @RequestParam("projectId") UUID visibilityAreaId,
                                                          @ParameterObject @PageableDefault(sort = {"name"},
                                                                  direction = Sort.Direction.ASC) Pageable pageable) {
        return dataSetService.getDatasetsIdNamesPageByNameAndVaId(name, visibilityAreaId, pageable);
    }

    /**
     * Returns dataSet by id.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetId}")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}}")
    @Operation(summary = "Returns dataSet by id.")
    public Object getDataSetById(@PathVariable("dataSetId") UUID dataSetId) {
        return dsService.get(dataSetId);
    }

    /**
     * See {@link DataSetService#getInItfFormat(MixInId)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId.getUuid()).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @GetMapping("/{dataSetId}/legacy/itf")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ITF format")
    @Operation(summary = "Returns dataSet by id in ITF format: "
            + "{attribute = parameter, attribute2 : {attribute = parameter2}}.")
    public Object getInItfFormat(@PathVariable("dataSetId") MixInId dataSetId) {
        return dsService.getInItfFormat(dataSetId);
    }

    /**
     * Returns dataSet by id in ATP format + atp macros.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId.getUuid()).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/legacy/atp")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ITF format + atp macros")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    public ResponseEntity<StreamingResponseBody> getDataSetsForAtp(
            @PathVariable("dataSetId") MixInId dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestBody(required = false) String atpContext) {
        Map<String, String> context = null;
        if (!Strings.isNullOrEmpty(atpContext)) {
            try {
                context = jsonObjectMapper.readValue(atpContext, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                log.error("Cannot parse ATP Context for DataSet id='" + dataSetId.getUuid() + "'");
                throw new DataSetContextParseException();
            }
        }
        CheckedConsumer<OutputStream, IOException> streamConsumer = dsService.writeInAtpFormat(dataSetId,
                context,
                QueryParamFlag.isPresent(evaluate));
        if (streamConsumer == null) {
            return ResponseEntity.noContent().build();
        }
        StreamingResponseBody stream = streamConsumer::accept;
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_STREAM_JSON)
                .body(stream);
    }

    /**
     * Returns dataSet by id in ATP format + atp macros. Table model - if DS is not selected,
     * that means all it's parameters is empty.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/atp")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ATP format + atp macros")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    public DataSetTree getAtpContextFull(
            @PathVariable("dataSetId") UUID dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestBody(required = false) String atpContext) {
        boolean isEvaluate = QueryParamFlag.isPresent(evaluate);
        return dataSetService.getDataSetTreeInAtpFormat(dataSetId, isEvaluate, atpContext, ContextType.FULL);
    }

    /**
     * Gets atp context full.
     * Bulk method.
     * For decrease amount of call to data set for context set countOfEvaluates = N to get the N copies
     * with unique values for macros.
     *
     * @param dataSetId the data set id
     * @param evaluate the evaluate
     * @param countOfEvaluates the count of evaluates
     * @param atpContext the atp context
     * @return the atp context full 2
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/atp/bulk")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ATP format + atp macros")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    public List<JSONObject> getAtpContextFull(
            @PathVariable("dataSetId") UUID dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestParam(value = "countOfEvaluates", required = false, defaultValue = "1") Integer countOfEvaluates,
            @RequestBody(required = false) String atpContext) {
        boolean isEvaluate = QueryParamFlag.isPresent(evaluate);
        log.info("Request for Data Set in ATP format '{}' with evaluate={}", dataSetId, isEvaluate);
        return dataSetService
                .getDataSetTreeInAtpFormat(dataSetId, isEvaluate, atpContext, ContextType.FULL, countOfEvaluates);
    }

    /**
     * Returns dataSet by id in ATP format + atp macros. Table model - if DS is not selected,
     * that means there is no parameters at all.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/atp/object")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ATP format + atp macros")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    public DataSetTree getAtpContextObject(
            @PathVariable("dataSetId") UUID dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestBody(required = false) String atpContext) {
        boolean isEvaluate = QueryParamFlag.isPresent(evaluate);
        log.info("Request for Data Set in ATP format '{}' with evaluate={}", dataSetId, isEvaluate);
        return dataSetService.getDataSetTreeInAtpFormat(
                dataSetId, QueryParamFlag.isPresent(evaluate), atpContext, ContextType.OBJECT
        );
    }

    /**
     * Returns dataSet by id in ATP format + atp macros. Table model - if DS is not selected,
     * that means there is no parameters at all.
     * Bulk method.
     * For decrease amount of call to data set for context set countOfEvaluates = N to get the N copies
     * with unique values for macros.
     *
     * @param dataSetId the data set id
     * @param evaluate the evaluate
     * @param countOfEvaluates the count of evaluates
     * @param atpContext the atp context
     * @return the atp context object
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/atp/object/bulk")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ATP format + atp macros")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    public List<JSONObject> getAtpContextObject(
            @PathVariable("dataSetId") UUID dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestParam(value = "countOfEvaluates", required = false, defaultValue = "1") Integer countOfEvaluates,
            @RequestBody(required = false) String atpContext) {
        boolean isEvaluate = QueryParamFlag.isPresent(evaluate);
        log.info("Request for Data Set in ATP format '{}' with evaluate={}", dataSetId, isEvaluate);
        return dataSetService.getDataSetTreeInAtpFormat(dataSetId, QueryParamFlag.isPresent(evaluate), atpContext,
                ContextType.OBJECT, countOfEvaluates);
    }

    /**
     * Returns dataSet by id in ITF format + atp macros. Table model - if DS is not selected,
     * that means there is no parameters at all, except root DSL overlaps.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/atp/optimized")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ATP format + atp macros")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    public DataSetTree getAtpContextOptimized(
            @PathVariable("dataSetId") UUID dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestBody(required = false) String atpContext) {
        boolean isEvaluate = QueryParamFlag.isPresent(evaluate);
        log.info("Request for Data Set in ATP format optimized '{}' with evaluate={}", dataSetId, isEvaluate);
        return dataSetService.getDataSetTreeInAtpFormat(
                dataSetId, QueryParamFlag.isPresent(evaluate), atpContext, ContextType.NO_NULL_VALUES);
    }

    /**
     * Returns dataSet by id in ITF format + atp macros. Table model - if DS is not selected,
     * that means there is no parameters at all, except root DSL overlaps.
     * Bulk method.
     * For decrease amount of call to data set for context set countOfEvaluates = N to get the N copies
     * with unique values for macros.
     *
     * @param dataSetId the data set id
     * @param evaluate the evaluate
     * @param countOfEvaluates the count of evaluates
     * @param atpContext the atp context
     * @return the atp context optimized
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/atp/optimized/bulk")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ATP format + atp macros")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    public List<JSONObject> getAtpContextOptimized(
            @PathVariable("dataSetId") UUID dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestParam(value = "countOfEvaluates", required = false, defaultValue = "1") Integer countOfEvaluates,
            @RequestBody(required = false) String atpContext) {
        boolean isEvaluate = QueryParamFlag.isPresent(evaluate);
        log.info("Request for Data Set in ATP format optimized '{}' with evaluate={}", dataSetId, isEvaluate);
        return dataSetService.getDataSetTreeInAtpFormat(dataSetId, QueryParamFlag.isPresent(evaluate), atpContext,
                ContextType.NO_NULL_VALUES, countOfEvaluates);
    }

    /**
     * Returns dataSet by id in ATP format + atp macros. Table model - if DS is not selected,
     * that means there is no parameters at all, except root DSL overlaps.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @PostMapping("/{dataSetId}/atp/objectExtended")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ATP format + atp macros")
    @Operation(summary = "Returns dataSet by id in ATP format + atp macros")
    public DataSetTree getAtpContextObjectExtended(
            @PathVariable("dataSetId") UUID dataSetId,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestBody(required = false) String atpContext) {
        boolean isEvaluate = QueryParamFlag.isPresent(evaluate);
        log.info("Request for Data Set in ATP format object extended '{}' with evaluate={}", dataSetId, isEvaluate);
        return dataSetService.getDataSetTreeInAtpFormat(dataSetId, QueryParamFlag.isPresent(evaluate), atpContext,
                ContextType.OBJECT_EXTENDED);
    }

    /**
     * Returns dataSet by id in ATP format + atp macros.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(), 'READ')")
    @Operation(summary = "Returns dataSet by id in ITF format + atp macros")
    @GetMapping(value = "/{dataSetId}/itf", produces = MediaType.APPLICATION_JSON_VALUE)
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} in ITF format")
    public String getItfContext(@PathVariable("dataSetId") UUID dataSetId) {
        return dataSetService.getDataSetTreeInItfFormat(dataSetId);
    }

    /**
     * Renames DS.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/{dataSetId}")
    @AuditAction(auditAction = "Rename dataset: {{#dataSetId}} on {{#name}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Renames DS.")
    public ResponseEntity<Void> rename(@PathVariable("dataSetId") UUID dataSetId,
                                       @RequestParam("name") String name,
                                       @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
                                       @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        dsService.rename(dataSetId, name);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * See {@link DataSetService#getParametersOnAttributePath(UUID, List, boolean)}}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @PostMapping("/{dataSetId}/parameters")
    @AuditAction(auditAction = "Get attribute of dataset: {{#dataSetId}}")
    public UiManAttribute getUiAttribute(@PathVariable("dataSetId") UUID dataSetId,
                                         @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
                                         @RequestBody List<UUID> attrPathIds) {
        if (dataSetId != null && attrPathIds != null) {
            return dsService.getParametersOnAttributePath(dataSetId, attrPathIds, QueryParamFlag.isPresent(evaluate));
        }
        return null;
    }

    /**
     * Get datasets or TableResponse with overridden parameters.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/affected/by")
    @AuditAction(auditAction = "Get dataset: {{#dataSetId}} or TableResponse with overridden parameters")
    @Operation(
            summary = "Returns all affected datasets by changes at attribute.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSet.class)))))
    public List<?> getDatasetsOrTableResponseAffectedByChangeParameterValue(
            @RequestParam("dataSetId") UUID dataSetId,
            @RequestParam("attributeId") UUID attrId,
            @RequestParam(value = "full", required = false) QueryParamFlag full) {
        boolean withInfo = QueryParamFlag.isPresent(full);
        return dsService.getOverlapContainers(dataSetId, attrId, withInfo);
    }

    /**
     * Reset to default value overridden parameters.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetsIds.get(0)).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/reset/affected/by")
    @AuditAction(auditAction = "Reset to default value overridden parameters in dataset list: {{#dataSetListId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reset all selected datasets, at changed attribute")
    public ResponseEntity<Void> restAffectedDatasetsByChangesAttribute(
            @RequestParam(value = "attributeId", required = false) UUID attributeId,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
            @RequestBody(required = false) List<UUID> dataSetsIds) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        dsService.deleteAllParameterOverlaps(attributeId, dataSetsIds);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * Copy DS.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'CREATE')")
    @PutMapping("/{dataSetId}/copy")
    @AuditAction(auditAction = "Copy dataset: {{#dataSetId}} with name: {{#name}}")
    @Operation(summary = "Copy DS with name provided.")
    public UUID copy(@PathVariable("dataSetId") UUID dataSetId, @RequestParam("name") String name) {
        return dsService.copy(dataSetId, name).getId();
    }

    /**
     * Path to DS.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dsId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/parent/path")
    @AuditAction(auditAction = "Get path to dataset: {{#dsId}}")
    @Operation(
            summary = "Returns visibility area and data set list ids",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(schema = @Schema(implementation = Pair.class))))
    public Pair<UUID, UUID> getPath(@RequestParam("dsId") UUID dsId) {
        DataSet dataSet = dsService.get(dsId);
        Preconditions.checkNotNull(dataSet, "DataSet with ID" + dsId + " not found");
        return Pair.of(dataSet.getDataSetList().getVisibilityArea().getId(), dataSet.getDataSetList().getId());
    }

    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/dsReferenceId/affected/by")
    @AuditAction(auditAction = "Get all affected dataSets by changes at reference: {{#dsReferenceId}}")
    @Operation(
            summary = "Returns all affected dataSets by changes at reference value.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSet.class)))))
    public List<?> getAffectedDataSetsByChangesDataSetReference(
            @RequestParam("dsReferenceId") UUID dsReferenceId,
            @RequestParam(value = "full", required = false) QueryParamFlag full) {
        boolean deleteDs = QueryParamFlag.isPresent(full);
        return dsService.getAffectedDataSetsByChangesDataSetReference(dsReferenceId, deleteDs);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetId}/affectedDatasets")
    @AuditAction(auditAction = "Get all affected dataSets by changes at reference: {{#dataSetId}} with pagination")
    @Operation(summary = "Returns all affected dataSets by specified reference with pagination support.")
    public PaginationResponse<TableResponse> getAffectedDataSets(@PathVariable UUID dataSetId,
                                                                 @RequestParam Integer page,
                                                                 @RequestParam Integer size) {
        return dsService.getAffectedDataSets(dataSetId, page, size);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetId}/affectedDatasetsCount")
    @Operation(summary = "Returns all affected dataSets count by specified reference.")
    @AuditAction(auditAction = "Get count of affected dataSets by changes at reference: {{#dataSetId}}")
    public Long getAffectedDataSetsCount(@PathVariable UUID dataSetId) {
        return dsService.getAffectedDataSetsCount(dataSetId);
    }

    /**
     * See {@link DataSetService#deleteParameterOverlap(UUID, UUID, List)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/reset/by")
    @Operation(summary = "Reset all selected datasets, at changed attribute. "
            + "dataSetIds is ids of datasets in which we want to reset overridden parameters with attributeId provided")
    @AuditAction(auditAction = "Reset overridden parameter in dataset: {{#dataSetId}} and attribute: {{#targetAttrId}}")
    public ResponseEntity<Parameter> resetOverriddenParameter(
            @RequestParam("dataSetId") UUID dataSetId,
            @RequestParam("targetAttrId") UUID targetAttrId,
            @RequestBody List<UUID> attrPathIds,
            @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        Parameter parameter = dsService.deleteParameterOverlap(dataSetId, targetAttrId, attrPathIds);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(parameter)
                : ResponseEntity.ok(parameter);
    }

    /**
     * Adds new dataSetLabel with name provided.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PutMapping("/{dataSetId}/label")
    @AuditAction(auditAction = "Add label: {{#name}} to dataset: {{#dataSetId}}")
    public ResponseEntity<Label> addLabel(@PathVariable("dataSetId") UUID dataSetId,
                                          @RequestParam("name") String name,
                                          @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
                                          @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        Label label = dsService.mark(dataSetId, name);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(label)
                : ResponseEntity.ok(label);
    }

    /**
     * Returns dataSetLabels.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetId}/label")
    @AuditAction(auditAction = "Get labels for dataset: {{#dataSetId}}")
    public List<Label> getLabels(@PathVariable("dataSetId") UUID dataSetId) {
        return dsService.getLabels(dataSetId);
    }

    /**
     * Deletes dataSetLabel.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @DeleteMapping("/{dataSetId}/label/{labelId}")
    @AuditAction(auditAction = "Delete label: {{#labelId}} for dataset: {{#dataSetId}}")
    public ResponseEntity<Boolean> deleteLabel(@PathVariable("dataSetId") UUID dataSetId,
                                               @PathVariable("labelId") UUID labelId,
                                               @RequestParam(value = "dataSetListId", required = false)
                                               UUID dataSetListId,
                                               @RequestParam(value = "modifiedWhen", required = false)
                                               Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        boolean isUnmark = dsService.unmark(dataSetId, labelId);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(isUnmark)
                : ResponseEntity.ok(isUnmark);
    }

    /**
     * See {@link DataSetService#restore(JsonNode)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetJson.get(\"id\").asText()).getDataSetList().getVisibilityArea().getId(),"
            + "'UPDATE')")
    @PostMapping("/restore")
    @AuditAction(auditAction = "Restore dataset in dataset list: {{#dataSetListId}}")
    public ResponseEntity<Boolean> restore(@RequestBody JsonNode dataSetJson,
                                           @RequestParam(value = "dataSetListId", required = false) UUID dataSetListId,
                                           @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        boolean isRestored = dsService.restore(dataSetJson);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(isRestored)
                : ResponseEntity.ok(isRestored);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/short")
    @AuditAction(auditAction = "Get datasets short in dataset list: {{#dataSetListId}}")
    @Operation(
            summary = "Returns list of Datasets (id + name) for selected DSL.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = ObjectShortResponse.class)))))
    public List<ObjectShortResponse> getDataSetsShort(@PathVariable("dataSetListId") UUID dataSetListId) {
        return dsService.getByParentId(dataSetListId);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetId).getDataSetList().getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/{dataSetId}/position")
    @AuditAction(auditAction = "Set position of dataset: {{#dataSetId}}")
    @Operation(summary = "Creates new list values for provided attribute by text.")
    public void setPosition(@PathVariable("dataSetId") UUID dataSetId,
                            @RequestBody Integer position) {
        dataSetService.setPosition(dataSetId, position);
    }

    /**
     * Lock DS from change.
     */
    @PreAuthorize("#isLock "
            + "? @entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(), 'LOCK') "
            + ": @entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(), 'UNLOCK')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/lock/update")
    @AuditAction(auditAction = "Lock datasets: {{#dataSetListId}} in dataset list: {{#dataSetId}}")
    @Operation(summary = "Lock")
    public ResponseEntity<Void> lock(@RequestParam("dataSetListId") UUID dataSetListId,
                                     @RequestParam boolean isLock,
                                     @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen,
                                     @RequestBody List<UUID> dataSetsIds) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        dsService.lock(dataSetListId, dataSetsIds, isLock);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).build()
                : ResponseEntity.noContent().build();
    }

    /**
     * Returns DS-s by ID-s.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#dataSetsIds.get(0)).getDataSetList().getVisibilityArea().getId(),'READ')")
    @PostMapping("/listOfDatasets")
    @Operation(summary = "Returns dataSets by id-s.")
    public List<DataSet> getDataSetsByIds(@RequestBody List<UUID> dataSetsIds) {
        return dsService.getAll(dataSetsIds);
    }

    /**
     * Compare two datasets by id.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/compare")
    @Operation(summary = "Compare Datasets from different test cases")
    public CompareDsResponse compareDsAtp(@RequestBody CompareDsRequest request) {
        UUID leftDataSetId = request.getLeftDataSetId();
        UUID rightDataSetId = request.getRightDataSetId();
        if (isNull(leftDataSetId) || isNull(rightDataSetId)) {
            throw new DataSetIdNotSpecifiedException();
        }
        CompareStatus status = compare.compare(leftDataSetId, rightDataSetId);
        return new CompareDsResponse(status);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#request.getLeftDatasetId()).getDataSetList().getVisibilityArea().getId(),"
            + "'READ')")
    @PostMapping("/compare/detailed")
    @Operation(summary = "Detailed comparison datasets")
    public DetailedComparisonDsResponse detailedComparisonDsAtp(@RequestBody DetailedComparisonDsRequest request) {
        return compare.detailedComparison(request);
    }

    /**
     * Copies attribute from one dataset to another dataset.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#request.getTargetDataSetId()).getDataSetList().getVisibilityArea().getId(),"
            + "'UPDATE')")
    @PostMapping("/compare/copy")
    @Operation(summary = "Copy attribute from source dataset to target dataset")
    public CopyDsAttributeResponse copyDsAttributes(@RequestBody CopyDsAttributeRequest request) {
        UUID targetAttributeId = dataSetService.copyDsAttributeValue(
                request.getSourceDataSetId(), request.getTargetDataSetId(),
                request.getSourceAttributeId(), request.getTargetAttributeId());
        return new CopyDsAttributeResponse(targetAttributeId);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetServiceImpl.get(#request.getTargetDataSetId()).getDataSetList().getVisibilityArea().getId(),"
            + "'UPDATE')")
    @PostMapping("/compare/copy/bulk")
    @Operation(summary = "Copy all attributes from source dataset to target dataset")
    public void copyDsAttributesBulk(@RequestBody CopyDsAttributeBulkRequest request) {
        dataSetService.copyDsAttributeValueBulk(request.getSourceDataSetId(), request.getTargetDataSetId());
    }

}

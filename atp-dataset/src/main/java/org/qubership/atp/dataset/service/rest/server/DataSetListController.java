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

import java.io.File;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.dataset.db.jpa.entities.AttributesSortType;
import org.qubership.atp.dataset.db.jpa.entities.UserSettingsEntity;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.impl.FlatDataImpl;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.model.utils.DatasetResponse;
import org.qubership.atp.dataset.model.utils.FilterPair;
import org.qubership.atp.dataset.model.utils.HttpUtils;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.importexport.models.DatasetListImportResponse;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListExportService;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListImportService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListCheckService;
import org.qubership.atp.dataset.service.jpa.model.CyclesCheckResult;
import org.qubership.atp.dataset.service.jpa.model.DataSetListDependencyNode;
import org.qubership.atp.dataset.service.rest.CopyDataSetListsRequest;
import org.qubership.atp.dataset.service.rest.PaginationResponse;
import org.qubership.atp.dataset.service.rest.QueryParamFlag;
import org.qubership.atp.dataset.service.rest.View;
import org.qubership.atp.dataset.service.rest.dto.manager.AffectedDataSetList;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
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

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/dsl")
@RequiredArgsConstructor
@Slf4j
public class DataSetListController {

    private final DataSetListService dslService;
    private final DataSetListCheckService dataSetListCheckService;
    private final ConcurrentModificationService concurrentModificationService;
    private final JpaDataSetListService jpaDataSetListService;
    private final AttributeService attributeService;
    private final Provider<UserInfo> userInfoProvider;
    private final DatasetListExportService datasetListExportService;
    private final DatasetListImportService importService;

    /**
     * Creates new DSL with name provided.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "#vaId,'CREATE')")
    @PutMapping("/va/{vaId}")
    @ApiOperation(value = "Creates new DSL with name provided.")
    @Operation(summary = "Creates new DSL with name provided.")
    public ResponseEntity<UUID> create(@PathVariable("vaId") UUID vaId,
                                       @RequestParam("name") String name,
                                       @RequestParam(value = "testPlan", required = false) UUID testPlanId,
                                       HttpServletRequest request) {
        dslService.checkOnDuplicate(vaId, name);
        UUID dslId = dslService.create(vaId, name, testPlanId).getId();
        String url = request.getRequestURL().append("/").append(dslId.toString()).toString();
        URI uri = URI.create(url);
        return ResponseEntity.created(uri).body(dslId);
    }

    /**
     * Deletes selected DSL.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'DELETE')")
    @DeleteMapping("/{dataSetListId}")
    @AuditAction(auditAction = "Delete dataset list: {{#dataSetListId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes selected DSL.")
    public void delete(@PathVariable("dataSetListId") UUID dataSetListId) {
        DataSetList dataSetList = dslService.get(dataSetListId);
        String dslName = dataSetList.getName();
        UUID projectId = dataSetList.getVisibilityArea().getId();
        dslService.delete(dataSetListId);
        log.info(userInfoProvider.get().getFullName() + " deleted the DSL \"" + dslName + "\" in project " + projectId);
    }

    /**
     * Returns all dataSetLists. // Method is UNUSED
     */
    @GetMapping
    @AuditAction(auditAction = "Get all Dataset lists")
    @Operation(
            summary = "Returns all dataSetLists.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSetList.class)))))
    @JsonView(View.CreatedModified.class)
    public List<DataSetList> getDataSetLists() {
        return dslService.getAll();
    }

    /**
     * Returns all dataSetLists for selected visibility area.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "#visibilityArea,'READ')")
    @GetMapping("/va/{vaId}")
    @AuditAction(auditAction = "Get dataset list for project: {{#visibilityArea}}")
    @Operation(
            summary = "Returns all dataSetLists for selected visibility area.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSetList.class)))))
    @JsonView(View.CreatedModified.class)
    public List<DataSetList> getDataSetLists(@PathVariable("vaId") UUID visibilityArea,
                                             @RequestParam(value = "label", required = false) String labelName) {
        return dslService.getAll(visibilityArea, labelName);
    }

    /**
     * Returns {@link FlatDataImpl} if flag is specified <br/>or {@link DataSetList} otherwise.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}")
    @AuditAction(auditAction = "Get information about dataset list: {{#dataSetListId}}")
    @Operation(summary = "Returns information about the selected DSL.")
    public Object getDataSetListInfo(
            @PathVariable("dataSetListId") UUID dataSetListId,
            @Parameter(in = ParameterIn.QUERY)
            @PathVariable(value = "filterDs", required = false) List<UUID> filterByDatasets,
            @Parameter(in = ParameterIn.QUERY)
            @PathVariable(value = "filterAttr", required = false) List<UUID> filterByAttributes,
            @RequestParam(value = "flat", required = false) QueryParamFlag flat,
            @RequestParam(value = "new", required = false) QueryParamFlag newOne,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate) {
        boolean doEvaluate = QueryParamFlag.isPresent(evaluate);
        if (QueryParamFlag.isPresent(newOne)) {
            return dslService
                    .getAsTree(dataSetListId, doEvaluate, filterByDatasets, filterByAttributes, null, null,
                            false, true);
        }
        if (QueryParamFlag.isPresent(flat)) {
            return dslService.getAsFlat(dataSetListId, doEvaluate);
        }
        return dslService.get(dataSetListId);
    }

    /**
     * Returns {@link DataSetList} information with filters.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @PostMapping("/{dataSetListId}/filters")
    @Operation(summary = "Returns information about the selected DSL.")
    @AuditAction(auditAction = "Get information about dataset list: {{#dataSetListId}}")
    public Object getDataSetListInfo(
            @PathVariable("dataSetListId") UUID dataSetListId,
            @RequestBody(required = false) @Nullable FilterPair<List<UUID>, List<UUID>> body,
            @RequestParam(value = "evaluate", required = false) QueryParamFlag evaluate,
            @RequestParam(value = "startIndex", required = false) Integer startIndex,
            @RequestParam(value = "endIndex", required = false) Integer endIndex,
            @RequestParam(value = "sort", required = false) Boolean isSortEnabled,
            @RequestParam(value = "expandAll", required = false, defaultValue = "true") boolean expandAll) {
        boolean doEvaluate = QueryParamFlag.isPresent(evaluate);
        List<UUID> filterByDatasets = null;
        List<UUID> filterByAttributes = null;
        if (body != null) {
            filterByDatasets = CollectionUtils.isEmpty(body.getDatasets()) ? null : body.getDatasets();
            filterByAttributes = CollectionUtils.isEmpty(body.getAttributes()) ? null : body.getAttributes();
        }
        UUID userId = userInfoProvider.get().getId();
        if (isSortEnabled != null) {
            attributeService.saveAttributeSortConfigurationForUser(userId, isSortEnabled);
            return dslService.getAsTree(dataSetListId, doEvaluate, filterByDatasets, filterByAttributes,
                    startIndex, endIndex, isSortEnabled, expandAll);
        }
        UserSettingsEntity sortEnabledEntity = attributeService.getAttributeSortConfigurationForUser(userId);
        boolean isSortConfigured = sortEnabledEntity != null
                                  && AttributesSortType.SORT_BY_NAME.equals(sortEnabledEntity.getAttributesSortType());
        return dslService.getAsTree(dataSetListId, doEvaluate, filterByDatasets, filterByAttributes,
                startIndex, endIndex, isSortConfigured, expandAll);
    }

    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/ds/{ds}")
    @AuditAction(auditAction = "Get information about dataset: {{#dsId}} in dataset list: {{#dataSetListId}}")
    @Operation(summary = "Returns information about the selected DS under DSL.")
    public Object getDataSetInfoByDataSetListIdAndId(@PathVariable("dataSetListId") UUID dataSetListId,
                                                     @PathVariable("ds") UUID dsId) {
        return dslService.getAsTree(dataSetListId, true, Collections.singletonList(dsId), false);
    }

    /**
     * Returns datasets full information of the selected DSL.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/full")
    @AuditAction(auditAction = "Get information about datasets in dataset list: {{#dataSetListId}}")
    @Operation(
            summary = "Returns datasets full information of the selected DSL.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSet.class)))))
    public List<DataSet> getDataSetListFullInfo(@PathVariable("dataSetListId") UUID dataSetListId) {
        DataSetList dataSetList = dslService.get(dataSetListId);
        if (dataSetList == null) {
            return Collections.emptyList();
        }
        return dataSetList.getDataSets();
    }

    /**
     * See {@link DataSetListService#getChildren(UUID, boolean, String)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/ds")
    @AuditAction(auditAction = "Get datasets in dataset list: {{#dataSetListId}}")
    @Operation(
            summary = "Returns datasets id/name information of the selected DSL.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataSet.class)))))
    @JsonView(View.CreatedModified.class)
    public List<DataSet> getDataSets(@PathVariable("dataSetListId") UUID dataSetListId,
                                     @RequestParam(value = "skipEvaluate", required = false)
                                             QueryParamFlag skipEvaluate,
                                     @RequestParam(value = "label", required = false) String labelName) {
        return dslService.getChildren(dataSetListId, !(Objects.isNull(skipEvaluate) || skipEvaluate.isPresent()),
                labelName);
    }

    /**
     * List of dataset with DataSetId, Name, DataSetListId.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/ds/all")
    @AuditAction(auditAction = "Get information about datasets in dataset lists: {{#dataSetListId}}")
    public List<DatasetResponse> getDataSetsWithNameAndDataSetList(@RequestBody List<UUID> dataSetListIds) {
        return dslService.getListOfDsIdsAndNameAndDslId(dataSetListIds);
    }

    /**
     * Modify dsl: rename or add test plan.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'UPDATE')")
    @PostMapping("/{dataSetListId}")
    @AuditAction(auditAction = "Rename dataset list: {{#dataSetListId}}")
    @Operation(summary = "Renames selected DSL.")
    public ResponseEntity<Boolean> rename(@PathVariable("dataSetListId") UUID dataSetListId,
                                          @RequestParam(value = "name", required = false) String name,
                                          @RequestParam(value = "testPlan", required = false) UUID testPlanId,
                                          @RequestParam(value = "clearTestPlan", required = false)
                                                  QueryParamFlag clearTestPlan,
                                          @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        boolean isModify = dslService.modify(dataSetListId, name, testPlanId, QueryParamFlag.isPresent(clearTestPlan));
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(isModify)
                : ResponseEntity.ok(isModify);
    }

    /**
     * Adds new dataSetListLabel with name provided.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'UPDATE')")
    @PutMapping("/{dataSetListId}/label")
    @AuditAction(auditAction = "Add label for dataset list: {{#dataSetListId}}")
    public ResponseEntity<Label> addLabel(@PathVariable("dataSetListId") UUID dataSetListId,
                                          @RequestParam("name") String name,
                                          @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        Label label = dslService.mark(dataSetListId, name);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(label)
                : ResponseEntity.ok(label);
    }

    /**
     * Returns dataSetListLabels.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/label")
    @AuditAction(auditAction = "Get labels for dataset list: {{#dataSetListId}}")
    public List<Label> getLabels(@PathVariable("dataSetListId") UUID dataSetListId) {
        return dslService.getLabels(dataSetListId);
    }

    /**
     * Deletes dataSetListLabel.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'UPDATE')")
    @DeleteMapping("/{dataSetListId}/label/{labelId}")
    @AuditAction(auditAction = "Delete label: {{#labelId}} for dataset list: {{#dataSetListId}}")
    public ResponseEntity<Boolean> deleteLabel(
            @PathVariable("dataSetListId") UUID dataSetListId,
            @PathVariable("labelId") UUID labelId,
            @RequestParam(value = "modifiedWhen", required = false) Long modifiedWhen) {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetListId, modifiedWhen);
        boolean isUnmark = dslService.unmark(dataSetListId, labelId);
        return HttpStatus.IM_USED.equals(httpStatus)
                ? ResponseEntity.status(httpStatus).body(isUnmark)
                : ResponseEntity.ok(isUnmark);
    }

    /**
     * Create copy of dataSetList.
     *
     * @param dataSetListId - dsl id
     * @param name - new dsl name
     * @param withData - true - with, false - without
     * @param vaId - visibility area id
     * @return -  id of new dsl
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "#vaId,'CREATE')")
    @PutMapping("/va/{vaId}/dsl/{dataSetListId}/copy")
    @AuditAction(auditAction = "Copy dataset list: {{#dataSetListId}} with provided name: {{#name}}")
    @Operation(summary = "Copy DSL with name provided.")
    public UUID copy(@PathVariable("dataSetListId") UUID dataSetListId,
                     @PathVariable("vaId") UUID vaId,
                     @RequestParam("name") String name,
                     @RequestParam(value = "type", required = false) Boolean withData,
                     @RequestParam(value = "testPlan", required = false) UUID testPlanId) throws Exception {
        log.info("Requested copying data set list {}", dataSetListId);
        return dslService.copy(vaId, dataSetListId, name, withData, testPlanId).getId();
    }

    /**
     * Create copies of dataSetLists.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/copy")
    @AuditAction(auditAction = "Copy dataset lists: {{#request.getDataSetListIds()}} with provided name")
    @Operation(summary = "Copy DSL with name provided.")
    public List<CopyDataSetListsResponse> copy(@RequestBody CopyDataSetListsRequest request) {
        return jpaDataSetListService.copyDataSetLists(request.getDataSetListIds(), request.isUpdateReferences(),
                request.getPostfix(), request.getPrevNamePattern());
    }

    /**
     * Create copy of dataSetLists and dataSets in it.
     *
     * @param name - new DSL name
     * @param data - DSL to copy with it's DSs to copy
     * @return - structure which contains mapping of old DS and pair of new DS and DSL
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/ds/clone/bulk")
    @AuditAction(auditAction = "Copy dataset lists: {{#data.keySet()}} and datasets with provided name: {{#name}}")
    @Operation(summary = "Copy DSLs and DSs. Uses for cloning of test plan on Catalog")
    public Map<UUID, Pair<UUID, UUID>> copyBulk(@RequestParam("name") String name,
                                                @RequestBody Map<UUID, Set<UUID>> data) {
        return dslService.copy(name, data);
    }

    /**
     * Get affected attribute by dsl.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/getAffected")
    @AuditAction(auditAction = "Get affected attribute by deleting dataset list: {{#dataSetListId}}")
    @Operation(summary = "Get affected attribute by deleting dsl( data set storage).")
    public PaginationResponse<TableResponse> getAffectedAttributes(@PathVariable("dataSetListId") UUID dataSetListId,
                                                                   @RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size) {
        return dslService.getAffectedAttributes(dataSetListId, page, size);
    }

    /**
     * Get affected attribute count by dsl.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).ATTRIBUTE.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/getAffectedCount")
    @AuditAction(auditAction = "Get affected attribute count by deleting dataset list: {{#dataSetListId}}")
    @Operation(summary = "Get affected attributes count by deleting dsl( data set storage).")
    public Long getAffectedAttributesCount(@PathVariable("dataSetListId") UUID dataSetListId) {
        return dslService.getAffectedAttributesCount(dataSetListId);
    }

    /**
     * See {@link DataSetListService#getAffectedDataSetLists(UUID, Integer, Integer)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/getAffectedDSL")
    @AuditAction(auditAction = "Get affected dataset list by deleting dataset list: {{#dataSetListId}}")
    @ResponseStatus(HttpStatus.PARTIAL_CONTENT)
    @Operation(summary = "Get affected dsl by deleting dsl( data set storage).")
    public ResponseEntity<List<AffectedDataSetList>> getAffectedDataSetLists(
            @PathVariable("dataSetListId") UUID dataSetListId,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
        List<AffectedDataSetList> affectedDataSetLists =
                dslService.getAffectedDataSetLists(dataSetListId, limit, offset);
        if (affectedDataSetLists.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(affectedDataSetLists);
    }

    /**
     * See {@link DataSetListService#getModifiedWhen(UUID)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/modifiedWhen")
    @AuditAction(auditAction = "Get dataset list: {{#dataSetListId}} modifiedWhen")
    @Operation(summary = "Get dsl modifiedWhen.")
    public ResponseEntity<Timestamp> getModifiedWhen(@PathVariable("dataSetListId") UUID dataSetListId) {
        Timestamp modifiedWhen = jpaDataSetListService.getModifiedWhen(dataSetListId);
        return ResponseEntity.ok(modifiedWhen);
    }

    /**
     * Checks DSL on cycles and returns results.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/cyclesCheck")
    @AuditAction(auditAction = "Get affected attribute by deleting dataset list: {{#dataSetListId}}")
    @Operation(summary = "Get affected attribute by deleting dsl( data set storage).")
    public CyclesCheckResult getCyclesCheck() {
        return dataSetListCheckService.checkOnCyclesAll();
    }

    /**
     * Get dsl dependencies tree.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/dependencies")
    @AuditAction(auditAction = "Get dependencies trees for dataset lists: {{#dataSetListIds}}")
    @Operation(summary = "Get dataset lists dependencies trees")
    public List<DataSetListDependencyNode> getDependencies(@RequestBody List<UUID> dataSetListIds) {
        return jpaDataSetListService.getDependencies(dataSetListIds);
    }

    /**
     * See {@link DataSetListService#existsById(UUID)}.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}/exists")
    @AuditAction(auditAction = "Check if dsl exists")
    @Operation(summary = "Check if dsl exists.")
    public ResponseEntity<Void> existsById(@PathVariable("dataSetListId") UUID dataSetListId) {
        return dslService.existsById(dataSetListId) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Get dsl dependencies tree.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/dependenciesRecursive")
    @AuditAction(auditAction = "Get dependencies trees for dataset lists: {{#dataSetListIds}}")
    @Operation(summary = "Get dataset lists dependencies trees")
    public List<DataSetListDependencyNode> getDependenciesRecursive(@RequestBody List<UUID> dataSetListIds) {
        return jpaDataSetListService.getDependenciesRecursive(dataSetListIds);
    }

    /**
     * Export data set list to excel file.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @PostMapping(path = "/{dataSetListId}/export/excel")
    @AuditAction(auditAction = "Export dataset lists: {{#dataSetListId}} to excel file")
    @Operation(summary = "Export dataset list to excel file")
    public ResponseEntity<InputStreamResource> exportDataSetList(@PathVariable("dataSetListId") UUID dataSetListId) {
        File fileExcel = datasetListExportService.exportDataSetList(dataSetListId);
            return HttpUtils.buildFileResponseEntity(fileExcel,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * Import data set list from excel file.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'UPDATE')")
    @PostMapping(value = "/{dataSetListId}/import/excel")
    @AuditAction(auditAction = "Import dataset lists: {{#dataSetListId}} to excel file")
    @Operation(summary = "Import dataset list from excel file")
    public DatasetListImportResponse importDataSetList(@RequestParam UUID projectId,
                                                       @PathVariable UUID dataSetListId,
                                                       HttpServletRequest request,
                                                       @RequestParam(value = "versioning", required = false,
                                                               defaultValue = "false") Boolean isJavers)
            throws Exception {
        return importService.importDataSetList(projectId, dataSetListId, request.getInputStream(), isJavers);
    }
}

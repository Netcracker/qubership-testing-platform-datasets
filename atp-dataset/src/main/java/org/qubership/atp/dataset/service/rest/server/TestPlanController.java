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
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.service.direct.TestPlanService;
import org.qubership.atp.dataset.service.rest.View;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;
import com.mysema.commons.lang.Pair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/testplan")
public class TestPlanController {

    private final TestPlanService testPlanService;

    /**
     * test plan service.
     */
    @Autowired
    public TestPlanController(TestPlanService testPlanService) {
        this.testPlanService = testPlanService;
    }

    /**
     * Creates new test plan with name provided.
     */
    @PreAuthorize("@entityAccess.checkAccess(#vaId,'CREATE')")
    @PutMapping("/va/{vaId}")
    @AuditAction(auditAction = "Creates new Test Plan with name: {{#name}} in project: {{#vaId}}")
    @Operation(summary = "Creates new Test Plan with name provided.")
    public ResponseEntity<UUID> create(@PathVariable("vaId") UUID vaId,
                                 @RequestParam ("name") String name,
                                 HttpServletRequest request) {
        Pair<TestPlan, Boolean> result = testPlanService.create(vaId, name);
        UUID testPlanId = result.getFirst().getId();
        boolean created = result.getSecond();
        String url = request.getRequestURL().append("/").append(testPlanId.toString()).toString();
        URI uri = URI.create(url);
        return created ? ResponseEntity.created(uri).body(testPlanId) :
                ResponseEntity.status(HttpStatus.OK).location(uri).body(testPlanId);
    }

    /**
     * Deletes test plan with provided name.
     */
    @PreAuthorize("@entityAccess.checkAccess(#vaId,'DELETE')")
    @DeleteMapping("/va/{vaId}")
    @AuditAction(auditAction = "Delete Test Plan with name: {{#name}} in project: {{#vaId}}")
    @Operation(summary = "Deletes Test Plan with provided name.")
    public ResponseEntity<Void> delete(@PathVariable("vaId") UUID vaId,
                                 @RequestParam ("name") String name) {
        boolean deleted = testPlanService.delete(vaId, name);
        return deleted ? ResponseEntity.noContent().build() :
                ResponseEntity.status(HttpStatus.NOT_MODIFIED.value()).build();
    }

    /**
     * Get all test plans under va.
     */
    @PreAuthorize("@entityAccess.checkAccess(#visibilityArea,'READ')")
    @GetMapping("/va/{vaId}")
    @AuditAction(auditAction = "Get Test Plan for project: {{#vaId}}")
    @Operation(
            summary = "Returns all test plans for selected visibility area.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TestPlan.class)))))
    public List<TestPlan> getTestPlans(@PathVariable("vaId") UUID visibilityArea) {
        return testPlanService.getAll(visibilityArea);
    }

    /**
     * Returns dataSetLists with selected Test Plan.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/{testPlanId}/dsl")
    @AuditAction(auditAction = "Get dataSet Lists for Test Plan: {{#testPlanId}}")
    @Operation(summary = "Returns dataSetLists with selected Test Plan")
    @JsonView(View.IdNameLabelsTestPlan.class)
    public List<DataSetList> getDataSetListsUnderTestPlan(@PathVariable("testPlanId") UUID testPlanId) {
        return testPlanService.getChildren(testPlanId);
    }
}

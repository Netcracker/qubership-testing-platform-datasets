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

import org.qubership.atp.dataset.service.jpa.JpaVisibilityAreaService;
import org.qubership.atp.dataset.service.jpa.model.VisibilityAreaFlatModel;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/va")
public class VisibilityAreaController {

    @Autowired
    protected JpaVisibilityAreaService visibilityAreaService;

    /**
     * Creates new visibility area with provided name and order.
     */
    @PreAuthorize("@entityAccess.isAdmin()")
    @PutMapping
    @AuditAction(auditAction = "Create new visibility area with name : {{#name}}")
    @Operation(summary = "Creates new visibility area with provided name and order.")
    public ResponseEntity<UUID> create(@RequestParam("name") String name,
                                       HttpServletRequest request) {
        UUID vaId = visibilityAreaService.getOrCreateWithName(name);
        String url = request.getRequestURL().append("/").append(vaId.toString()).toString();
        URI uri = URI.create(url);
        return ResponseEntity.created(uri).body(vaId);
    }

    /**
     * Deletes selected VA.
     */
    @PreAuthorize("@entityAccess.isAdmin()")
    @DeleteMapping("/{visibilityAreaId}")
    @AuditAction(auditAction = "Delete visibility area: {{#visibilityAreaId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes selected VA.")
    public void delete(@PathVariable("visibilityAreaId") UUID visibilityAreaId) {
        visibilityAreaService.deleteById(visibilityAreaId);
    }

    /**
     * Returns all visibility areas.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping
    @AuditAction(auditAction = "Get all visibility areas")
    @Operation(
            summary = "Returns all visibility areas.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = VisibilityAreaFlatModel.class)))))
    public List<VisibilityAreaFlatModel> getVisibilityAreas() {
        return visibilityAreaService.getAll();
    }

    /**
     * Returns all visibility areas sorted by name.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/sorted")
    @AuditAction(auditAction = "Get all visibility areas sorted by name")
    @Operation(
            summary = "Returns all visibility areas sorted by name.",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = VisibilityAreaFlatModel.class)))))
    public List<VisibilityAreaFlatModel> getVisibilityAreasSorted() {
        return visibilityAreaService.getAllSortedByNameAsc();
    }

    /**
     * Renames selected VA.
     */
    @PreAuthorize("@entityAccess.isAdmin()")
    @PostMapping("/{visibilityAreaId}")
    @AuditAction(auditAction = "Rename visibility area for ptoject: {{#visibilityAreaId}}")
    @Operation(summary = "Renames selected VA.")
    public boolean rename(@PathVariable("visibilityAreaId") UUID visibilityAreaId,
                          @RequestParam("name") String name) {
        return visibilityAreaService.setName(visibilityAreaId, name);
    }

    /**
     * Create copies of dataSetLists.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "#sourceAreaId, 'CREATE')")
    @PostMapping("/{visibilityAreaId}/copyTo")
    @AuditAction(auditAction = "Copy DSL with name provided for project: {{#visibilityAreaId}}")
    @Operation(summary = "Copy DSL with name provided.")
    public void copyToVisibilityArea(@PathVariable("visibilityAreaId") UUID sourceAreaId,
                                     @RequestBody UUID targetAreaId) {
        visibilityAreaService.copyDataSetListsToVisibilityArea(sourceAreaId, targetAreaId);
    }
}

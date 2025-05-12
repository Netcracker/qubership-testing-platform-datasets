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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.model.Filter;
import org.qubership.atp.dataset.service.direct.FilterService;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Preconditions;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/filter")
public class LabelFilterController {

    private FilterService filterService;

    @Autowired
    public LabelFilterController(FilterService filterService) {
        this.filterService = filterService;
    }

    /**
     * Creates new filter by labels.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PostMapping("/create")
    @AuditAction(auditAction = "Create new filter for project: {{#vaId}}")
    @Operation(summary = "Create new filter")
    public Filter createFilter(
            @RequestParam("name") String name,
            @RequestParam("vaId") UUID vaId,
            @RequestBody(required = false) Map<String, List<UUID>> labels) {
        List<UUID> dsLabels = getLabelsOrEmptyList(labels, "dsLabels");
        List<UUID> dslLabels = getLabelsOrEmptyList(labels, "dslLabels");
        Preconditions.checkNotNull(name, "Query parameter 'name' should not be null");
        Preconditions.checkNotNull(name, "Query parameter 'vaId' should not be null");
        return filterService.create(name, vaId, dsLabels, dslLabels);
    }

    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/get")
    @AuditAction(auditAction = "Get filters for project: {{#vaId}}")
    @Operation(summary = "Returns all filter under visibility area")
    public List<Filter> getFilters(@RequestParam(value = "vaId", required = false) UUID vaId) {
        return filterService.getAll(vaId);
    }

    /**
     * Update filter name and filter labels.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @PutMapping("/update")
    @AuditAction(auditAction = "Update filter: {{#filterId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update filter name and filter labels")
    public void update(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "filterId", required = false) UUID filterId,
            @RequestBody(required = false) Map<String, List<UUID>> labels) {
        List<UUID> dsLabels = getLabelsOrEmptyList(labels, "dsLabels");
        List<UUID> dslLabels = getLabelsOrEmptyList(labels, "dslLabels");
        filterService.update(filterId, name, dsLabels, dslLabels);
    }

    private List<UUID> getLabelsOrEmptyList(Map<String, List<UUID>> labels, String dslLabels) {
        return labels.computeIfAbsent(dslLabels, key -> Collections.emptyList());
    }

    @PreAuthorize("@entityAccess.isAuthenticated()")
    @DeleteMapping("/delete")
    @AuditAction(auditAction = "Delete filter: {{#filterId}}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete filter by id")
    public void delete(@RequestParam("filterId") UUID filterId) {
        filterService.delete(filterId);
    }
}

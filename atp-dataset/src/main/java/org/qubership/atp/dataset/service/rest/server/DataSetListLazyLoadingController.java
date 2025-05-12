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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.model.dsllazyload.dsl.DataSetListFlat;
import org.qubership.atp.dataset.service.jpa.model.dsllazyload.referencedcontext.RefDataSetListFlat;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/dsl/lazy")
public class DataSetListLazyLoadingController {
    @Autowired
    protected JpaDataSetListService dataSetListService;

    /**
     * Top level DSl.
     * */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(),'READ')")
    @GetMapping("/{dataSetListId}")
    @AuditAction(auditAction = "Get information about dataset lists: {{#dataSetListId}}")
    @Operation(summary = "Returns information about the selected DSL.")
    public DataSetListFlat getDataSetListFlat(@PathVariable("dataSetListId") UUID dataSetListId) {
        return dataSetListService.getDataSetListFlat(dataSetListId);
    }

    /**
     * Referenced DSL by path.
     * */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "#dataSetListId,'READ')")
    @GetMapping("/{dataSetListId}/ReferenceByPath/{attributePath}")
    @AuditAction(auditAction = "Get information about referenced dataset lists by attr path")
    @Operation(summary = "Returns information about the selected DSL.")
    public RefDataSetListFlat getReferencedDataSetList(@PathVariable("dataSetListId") UUID dataSetListId,
                                                       @PathVariable("attributePath") String attributePath,
                                                       @PageableDefault(size = 15) Pageable pageable,
                                                       @RequestBody(required = false) List<UUID> dataSetIds) {
        String[] split = attributePath.split("@");
        return dataSetListService.getReferencedDataSetListFlat(
                dataSetListId,
                UUID.fromString(split[split.length - 1]),
                attributePath.replaceAll("@", ","),
                dataSetIds,
                pageable);
    }

    /**
     * Referenced DSL by path.
     * */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "@dataSetListServiceImpl.get(#dataSetListId).getVisibilityArea().getId(), 'READ')")
    @GetMapping("/{dataSetListId}/ReferenceByPath/rows/{attributePath}")
    @AuditAction(auditAction = "Get information about referenced dataset lists by attr path")
    @Operation(summary = "Returns information about the selected DSL.")
    public RefDataSetListFlat getReferencedDataSetListRows(@PathVariable("dataSetListId") UUID dataSetListId,
                                                           @PathVariable("attributePath") String attributePath,
                                                           @PageableDefault(size = 15) Pageable pageable,
                                                           @RequestBody(required = false) List<UUID> dataSetIds) {
        String[] split = attributePath.split("@");
        return dataSetListService.getReferencedDataSetListFlatRows(
                dataSetListId,
                UUID.fromString(split[split.length - 1]),
                attributePath.replaceAll("@", ","),
                dataSetIds,
                pageable);
    }
}

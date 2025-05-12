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

import javax.validation.Valid;

import org.qubership.atp.dataset.model.api.saga.requests.CopyDataSetListsRequest;
import org.qubership.atp.dataset.model.api.saga.requests.RevertRequest;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/saga")
@RequiredArgsConstructor
public class SagaController {

    private final JpaDataSetListService jpaDataSetListService;
    private final DataSetListService dataSetListService;

    static final String X_SAGA_SESSION_ID_HEADER = "X-Saga-Session-Id";

    /**
     * Create copies of dataSetLists.
     */
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "#request.getProjectId(),'CREATE')")
    @PostMapping("/dsl/copy")
    @Operation(summary = "Copy DSL with name provided.")
    public List<CopyDataSetListsResponse> copyDataSetLists(@RequestHeader(name = X_SAGA_SESSION_ID_HEADER)
                                                               UUID sagaSessionId,
                                                           @RequestBody @Valid CopyDataSetListsRequest request) {
        return jpaDataSetListService.copyDataSetLists(request.getDataSetListIds(), request.isUpdateReferences(),
                request.getProjectId(), request.getPostfix(), request.getPrevNamePattern(), sagaSessionId);
    }


    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.dataset.model.UserManagementEntities).DATASET_LIST.getName(),"
            + "#request.getProjectId(),'DELETE')")
    @PostMapping("/dsl/revert")
    public void revertDataSetLists(@RequestHeader(name = X_SAGA_SESSION_ID_HEADER) UUID sagaSessionId,
                                   @RequestBody @Valid RevertRequest request) {
        dataSetListService.revert(sagaSessionId, request);
    }
}

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

import org.qubership.atp.dataset.db.DBConfig;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class VersionController {

    private DBConfig dbConfig;

    @Autowired
    public VersionController(DBConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Returns atp-dataSet service version and db information such as driverClass.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/version")
    @AuditAction(auditAction = "Get current version")
    @Operation(summary = "gets current version")
    public String version() {
        String infoString;
        String serviceVersion = getClass().getPackage().getImplementationVersion();
        infoString = "DataSet Version: " + (serviceVersion == null ? "UNKNOWN" : serviceVersion) + "\n";
        String dbInfo = dbConfig.getDriverClassName();
        infoString += "DB Info: " + dbInfo;
        return infoString;
    }
}

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

import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.dataset.db.jpa.entities.AttributesSortType;
import org.qubership.atp.dataset.db.jpa.entities.UserSettingsEntity;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.rest.dto.manager.UserSettingsResponse;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AttributeService attributeService;
    private final Provider<UserInfo> userInfoProvider;

    /**
     * Gets current user settings.
     */
    @PreAuthorize("@entityAccess.isAuthenticated()")
    @GetMapping("/current/settings")
    @AuditAction(auditAction = "Get current user settings")
    @Operation(summary = "Gets current user settings.")
    public UserSettingsResponse getUserSettings() {
        UUID userId = userInfoProvider.get().getId();
        UserSettingsEntity sortEnabledEntity = attributeService.getAttributeSortConfigurationForUser(userId);
        boolean isSortEnabled =
                sortEnabledEntity != null && AttributesSortType.SORT_BY_NAME
                        .equals(sortEnabledEntity.getAttributesSortType());
        return new UserSettingsResponse(isSortEnabled);
    }
}

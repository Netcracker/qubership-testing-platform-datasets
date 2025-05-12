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

package org.qubership.atp.dataset.service.direct.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import javax.inject.Provider;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DateAuditorService;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DateAuditorServiceImpl implements DateAuditorService {

    private final Provider<DataSetListService> dslServiceProvider;
    private final org.qubership.atp.auth.springbootstarter.ssl.Provider<UserInfo> userInfoProvider;

    @Override
    public void updateModifiedFields(UUID dataSetListId) {
        UUID modifiedBy = userInfoProvider.get().getId();
        Timestamp modifiedWhen = Timestamp.from(Instant.now());
        dslServiceProvider.get().updateModifiedFields(dataSetListId, modifiedBy, modifiedWhen);
    }
}

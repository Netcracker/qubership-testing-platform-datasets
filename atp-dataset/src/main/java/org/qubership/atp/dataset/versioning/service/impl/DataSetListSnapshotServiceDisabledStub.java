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

package org.qubership.atp.dataset.versioning.service.impl;

import java.util.UUID;

import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(prefix = "atp-dataset", name = "javers.enabled", havingValue = "false")
@Slf4j
public class DataSetListSnapshotServiceDisabledStub implements DataSetListSnapshotService {

    private static final String DISABLED_MESSAGE =
            "Javers is disabled. If you want to enable javers, change the JAVERS_ENABLED property to 'true' value";

    @Override
    public void commitEntity(UUID dataSetListId) {
        log.warn(DISABLED_MESSAGE);
    }

    @Override
    public void findAndCommitIfExists(UUID id) {
        log.warn(DISABLED_MESSAGE);
    }

    @Override
    public void findAndCommitRestored(UUID id, Integer revisionId) {
        log.warn(DISABLED_MESSAGE);
    }

    @Override
    public void deleteDataSetList(UUID id) {
        log.warn(DISABLED_MESSAGE);
    }

    @Override
    public DataSetListSnapshot findDataSetListSnapshot(UUID id, Integer revisionId) {
        return null;
    }
}

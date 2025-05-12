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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.javers.core.Javers;
import org.javers.core.commit.Commit;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.GlobalIdDTO;
import org.javers.repository.jql.InstanceIdDTO;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.service.JaversAuthorProvider;
import org.qubership.atp.dataset.versioning.service.JaversCommitService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JaversCommitServiceImpl implements JaversCommitService {

    private static final String USERNAME_PROPERTY_NAME = "Username";

    private final Javers javers;
    private final JaversAuthorProvider authorProvider;

    @Override
    public Commit commit(Object currentVersion) {
        return javers.commit(getAuthor(), currentVersion, getCommitProperties());
    }

    @Override
    public Commit commitWithExtraProperties(Object currentVersion, Map<String, String> extraProperties) {
        Map<String, String> commitProperties = getCommitProperties();
        commitProperties.putAll(extraProperties);
        return javers.commit(getAuthor(), currentVersion, commitProperties);
    }

    @Override
    public CompletableFuture<Commit> commitAsync(Object currentVersion, Executor executor) {
        return javers.commitAsync(getAuthor(), currentVersion, getCommitProperties(), executor);
    }

    @Override
    public Commit commitShallowDelete(Object deleted) {
        DataSetListSnapshot obj = (DataSetListSnapshot) deleted;
        Optional<CdoSnapshot> latestSnapshot =
                javers.getLatestSnapshot(obj.getId(), DataSetListSnapshot.class);
        return latestSnapshot
            .map(cdoSnapshot -> javers.commitShallowDelete(getAuthor(), deleted, getCommitProperties()))
            .orElse(null);
    }

    @Override
    public Commit commitShallowDeleteById(GlobalIdDTO globalId) {
        InstanceIdDTO instanceId = (InstanceIdDTO) globalId;
        Optional<CdoSnapshot> latestSnapshot =
            javers.getLatestSnapshot(instanceId.getCdoId(), instanceId.getEntity());
        return latestSnapshot
            .map(cdoSnapshot -> javers.commitShallowDeleteById(getAuthor(), globalId, getCommitProperties()))
            .orElse(null);
    }

    private String getAuthor() {
        return authorProvider.provide();
    }

    private Map<String, String> getCommitProperties() {
        Map<String, String> commitProperties = new HashMap<>();
        commitProperties.put(USERNAME_PROPERTY_NAME, authorProvider.getUsername());
        return commitProperties;
    }
}

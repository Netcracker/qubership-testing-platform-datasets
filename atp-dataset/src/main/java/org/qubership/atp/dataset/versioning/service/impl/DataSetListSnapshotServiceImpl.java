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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.javers.core.Javers;
import org.javers.repository.jql.InstanceIdDTO;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.versioning.exception.DataSetListIdNotFound;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.qubership.atp.dataset.versioning.service.JaversCommitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Service
@ConditionalOnProperty(prefix = "atp-dataset", name = "javers.enabled", matchIfMissing = true)
@Slf4j
public class DataSetListSnapshotServiceImpl implements DataSetListSnapshotService {

    @Autowired
    private final ModelsProvider modelsProvider;
    @Autowired
    private final JaversCommitService javersCommitService;
    @Autowired
    private final Javers javers;

    @Override
    @Transactional
    public void commitEntity(UUID dataSetListId) {
        commitWithProperties(
                modelsProvider.getDataSetListById(dataSetListId),
                Collections.emptyMap()
        );
    }

    @Override
    public void findAndCommitIfExists(UUID id) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(id);
        if (dataSetList != null) {
            commitEntity(id);
        }
    }

    @Override
    public void findAndCommitRestored(UUID id, Integer revisionId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(id);
        if (dataSetList != null) {
            Map<String, String> properties = ImmutableMap.of(RESTORED_TO_PROPERTY, revisionId.toString());
            commitWithProperties(dataSetList, properties);
        }
    }

    @Override
    public void deleteDataSetList(UUID id) {
        javersCommitService.commitShallowDeleteById(InstanceIdDTO.instanceId(id, DataSetListSnapshot.class));
    }

    @Override
    public DataSetListSnapshot findDataSetListSnapshot(UUID id, Integer revisionId) {
        JqlQuery query = QueryBuilder
                .byInstanceId(id, DataSetListSnapshot.class)
                .withVersion(revisionId)
                .build();
        Object result = javers.findShadows(query)
                .stream()
                .findFirst()
                .orElseThrow(DataSetListIdNotFound::new)
                .get();
        return (DataSetListSnapshot) result;
    }

    private void commitWithProperties(DataSetList dsl, Map<String, String> properties) {
        log.debug("Preparing DataSetList for commit: {}", dsl.getId());
        DataSetListSnapshot dataSetListSnapshot = new DataSetListSnapshot(dsl);
        javersCommitService.commitWithExtraProperties(dataSetListSnapshot, properties);
        log.debug("Snapshot of DataSetList committed: {}", dataSetListSnapshot);
    }
}

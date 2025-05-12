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

package org.qubership.atp.dataset.service.jpa.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.model.copy.DataSetListCopyData;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataSetListAsyncService {

    @Autowired
    protected ModelsProvider modelsProvider;

    private final DataSetListSnapshotService commitEntityService;

    public DataSetListAsyncService(DataSetListSnapshotService commitEntityService) {
        this.commitEntityService = commitEntityService;
    }

    /**
     * Copy DSL with same Name with postfix (N)
     * where N - is copy number.
     */
    @Async("asyncCopyTaskExecutor")
    @Transactional
    public Future<DataSetListCopyData> duplicate(UUID dataSetListId, @Nullable String postfix,
                                                 String prevNamePattern, @Nullable UUID sagaSessionId) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        log.info("Start copy Dataset List with id {} and name {}.", dataSetListId, dataSetList.getName());
        DataSetListCopyData duplicateData = dataSetList.duplicate(postfix, prevNamePattern, sagaSessionId);
        commitEntityService.findAndCommitIfExists(duplicateData.getCopyId());
        DataSetList dataSetListCopy = duplicateData.getDataSetListCopy();
        log.info("Finish copy Dataset List with id {} and name {}. New id {} and new name {}",
                dataSetListId, dataSetList.getName(), dataSetListCopy.getId(), dataSetListCopy.getName());
        return new AsyncResult<>(duplicateData);
    }

    /**
     * Updates overlaps keys.
     */
    @Async("asyncCopyTaskExecutor")
    @Transactional
    public Future<?> updateOverlaps(DataSetList dataSetList, Map<UUID, DataSetListCopyData> copiesData) {
        log.info("Update overlaps for Dataset List {}", dataSetList.getId());
        dataSetList.updateOverlaps(copiesData);
        return new AsyncResult<>(null);
    }
}
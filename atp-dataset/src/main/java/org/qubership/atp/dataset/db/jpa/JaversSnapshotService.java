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

package org.qubership.atp.dataset.db.jpa;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.javers.core.metamodel.object.SnapshotType;
import org.qubership.atp.dataset.db.dto.JaversCountResponse;
import org.qubership.atp.dataset.db.jpa.entities.JvSnapshotEntity;
import org.qubership.atp.dataset.db.jpa.repositories.JpaJvCommitPropertyRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaJvCommitRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaJvGlobalIdRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaJvSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JaversSnapshotService {

    private static final Integer FIRST_PAGE = 0;

    private final JpaJvSnapshotRepository snapshotRepository;
    private final JpaJvGlobalIdRepository globalIdRepository;
    private final JpaJvCommitRepository commitRepository;
    private final JpaJvCommitPropertyRepository commitPropertyRepository;

    @Value("${atp-dataset.archive.job.bulk-delete-count}")
    private Integer bulkDeleteCount;

    @Value("${atp-dataset.archive.job.page-size}")
    private Integer size;

    /**
     * Constructor.
     *
     * @param snapshotRepository snapshotRepository
     * @param globalIdRepository globalIdRepository
     * @param commitRepository   commitRepository
     * @param commitPropertyRepository commitPropertyRepository
     */
    @Autowired
    public JaversSnapshotService(JpaJvSnapshotRepository snapshotRepository,
                                 JpaJvGlobalIdRepository globalIdRepository,
                                 JpaJvCommitRepository commitRepository,
                                 JpaJvCommitPropertyRepository commitPropertyRepository) {
        this.snapshotRepository = snapshotRepository;
        this.globalIdRepository = globalIdRepository;
        this.commitRepository = commitRepository;
        this.commitPropertyRepository = commitPropertyRepository;
    }

    /**
     * Get globalId and number of old objects.
     *
     * @param lastRevisionCount number of the last revisions.
     * @return {@link List} of {@link JaversCountResponse}
     */
    public List<JaversCountResponse> findGlobalIdAndCount(Long lastRevisionCount) {
        List<JaversCountResponse> globalIdAndCount =
                snapshotRepository.findGlobalIdAndCountGreaterThan(lastRevisionCount);
        log.debug("Number of unique globalId '{}'", globalIdAndCount.size());
        return globalIdAndCount;
    }

    /**
     * Get old snapshots by globalId and count.
     *
     * @param globalId globalId
     * @param count    count
     * @return {@link List} of {@link JvSnapshotEntity}
     */
    public List<JvSnapshotEntity> findOldSnapshots(Long globalId, Long count) {
        PageRequest pageRequest = PageRequest.of(0, Math.toIntExact(count));
        List<JvSnapshotEntity> oldSnapshots =
                snapshotRepository.findAllByGlobalIdOrderByVersionAsc(globalId, pageRequest);
        log.debug("Number of old snapshots '{}' for globalId '{}'", oldSnapshots.size(), globalId);
        return oldSnapshots;
    }

    /**
     * Get the oldest snapshot and update snapshot type with INITIAL value.
     *
     * @param globalId globalId
     * @return {@link JvSnapshotEntity} entity;
     */
    public JvSnapshotEntity findTheOldestSnapshotByGlobalIdAndUpdateTypeAsInitial(Long globalId) {
        JvSnapshotEntity snapshot = snapshotRepository.findFirstByGlobalIdOrderByVersionAsc(globalId);
        if (isNull(snapshot)) {
            return null;
        }
        snapshot.setType(SnapshotType.INITIAL);
        return snapshotRepository.save(snapshot);
    }

    /**
     * Delete old snapshots, commit properties and commits.
     * And update the oldest snapshot as initial.
     *
     * @param globalId  globalId
     * @param snapshots old snapshots
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOldAndUpdateAsInitial(Long globalId, List<JvSnapshotEntity> snapshots) {
        snapshots.forEach(snapshot -> deleteOldAndUpdateAsInitial(globalId, snapshot));
        findTheOldestSnapshotByGlobalIdAndUpdateTypeAsInitial(globalId);
    }

    private void deleteOldAndUpdateAsInitial(Long globalId, JvSnapshotEntity snapshot) {
        Long commitId = snapshot.getCommitId();
        Long version = snapshot.getVersion();
        snapshotRepository.deleteByVersionAndGlobalIdAndCommitId(version, globalId, commitId);
        log.debug("Deleted snapshots with version '{}', globalId '{}', commitId '{}'",
                version, globalId, commitId);
        Long commitCount = snapshotRepository.countByCommitId(commitId);
        if (commitCount.equals(0L)) {
            commitPropertyRepository.deleteByIdCommitId(commitId);
            commitRepository.deleteById(commitId);
            log.debug("Deleted commit properties and commits with commitId '{}'", commitId);
        }
    }

    /**
     * Delete terminated snapshots, globalIds, commits and commit properties.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTerminatedSnapshots() {
        while (true) {
            Page<JvSnapshotEntity> page =
                    snapshotRepository.findAllByTypeIs(SnapshotType.TERMINAL, PageRequest.of(FIRST_PAGE, size));
            deleteTerminatedSnapshots(page);
            if (!page.hasNext()) {
                break;
            }
        }
    }

    private void deleteTerminatedSnapshots(Page<JvSnapshotEntity> page) {
        List<JvSnapshotEntity> terminalSnapshots = page.getContent();
        Set<Long> globalIds = getIds(terminalSnapshots, JvSnapshotEntity::getGlobalId);
        log.debug("Number of terminal globalIds '{}'", globalIds.size());
        List<JvSnapshotEntity> snapshots = new ArrayList<>();
        doAction(globalIds, ids -> snapshots.addAll(snapshotRepository.findAllByGlobalIdIn(ids)));
        Set<Long> commitIds = getIds(snapshots, JvSnapshotEntity::getCommitId);
        log.debug("Number of terminal commitIds '{}'", commitIds.size());
        doAction(globalIds, snapshotRepository::deleteByGlobalIdIn);
        log.debug("Terminated snapshots deleted");
        doAction(globalIds, globalIdRepository::deleteByIdIn);
        log.debug("Terminated globalIds deleted");
        doAction(commitIds, commitPropertyRepository::deleteByIdCommitIdIn);
        log.debug("Terminated commit properties deleted");
        doAction(commitIds, commitRepository::deleteByIdIn);
        log.debug("Terminated commits deleted");
    }

    private <T> void doAction(Collection<T> collection, Consumer<? super List<T>> action) {
        Iterators.partition(collection.iterator(), bulkDeleteCount).forEachRemaining(action);
    }

    private <T, R> Set<R> getIds(List<T> snapshots, Function<T, R> function) {
        return snapshots.stream()
                .map(function)
                .collect(Collectors.toSet());
    }
}

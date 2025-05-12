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

import java.util.List;

import org.qubership.atp.dataset.db.jpa.entities.JvSnapshotEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class JobRunner {

    private static final String UTC_TIMEZONE = "UTC";

    private final JaversSnapshotService javersSnapshotService;
    private final ThreadPoolTaskExecutor archiveJobExecutor;

    @Value("${atp-dataset.last.revision.count}")
    private Long lastRevisionCount;

    @Autowired
    public JobRunner(JaversSnapshotService javersSnapshotService,
                     ThreadPoolTaskExecutor archiveJobExecutor) {
        this.javersSnapshotService = javersSnapshotService;
        this.archiveJobExecutor = archiveJobExecutor;
    }

    /**
     * Job that removes irrelevant data from the change history.
     */
    @Scheduled(cron = "${atp-dataset.archive.cron.expression}", zone = UTC_TIMEZONE)
    @SchedulerLock(name = "${atp-dataset.archive.job.name}", lockAtMostFor = "12h", lockAtLeastFor = "2h")
    public void run() {
        javersSnapshotService.deleteTerminatedSnapshots();
        javersSnapshotService.findGlobalIdAndCount(lastRevisionCount).stream()
                .<Runnable>map(response -> () -> {
                    Long globalId = response.getId();
                    long numberOfOldSnapshots = response.getCount() - lastRevisionCount;
                    List<JvSnapshotEntity> oldSnapshots =
                            javersSnapshotService.findOldSnapshots(globalId, numberOfOldSnapshots);
                    javersSnapshotService.deleteOldAndUpdateAsInitial(globalId, oldSnapshots);
                }).forEach(archiveJobExecutor::execute);
    }
}

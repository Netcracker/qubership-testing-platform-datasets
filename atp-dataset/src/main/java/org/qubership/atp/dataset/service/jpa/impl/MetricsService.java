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

import java.util.HashMap;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MetricsService {

    public static final String ATP_MAX_SIZE_DOWNLOAD_FILE_PER_PROJECT_TOTAL =
            "atp.data.sets.max.size.download.file.per.project.total";
    public static final String ATP_MAX_SIZE_UPLOAD_FILE_PER_PROJECT_TOTAL =
            "atp.data.sets.max.size.upload.file.per.project.total";
    private static final String PROJECT_ID = "projectId";

    private final Counter.Builder uploadFileCounter = Counter.builder(ATP_MAX_SIZE_UPLOAD_FILE_PER_PROJECT_TOTAL)
            .description("Counter for all requests");
    private final Counter.Builder downloadFileCounter = Counter.builder(ATP_MAX_SIZE_DOWNLOAD_FILE_PER_PROJECT_TOTAL)
            .description("Counter for run collections");
    private final MeterRegistry meterRegistry;
    private HashMap<String, HashMap<String, Long>> storage = new HashMap<>();

    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Increment counter by tags.
     */
    private void incrementByBuilderCounterAndTags(Counter.Builder counter, String metricName,
                                                  long fileSize, String projectId) {
        Long size = calculateSize(fileSize);
        if (!storage.containsKey(metricName)) {
            HashMap<String, Long> map = new HashMap<>();
            map.put(projectId, size);
            storage.put(metricName, map);

            registerMetricSize(counter, size, projectId);
        } else {
            HashMap<String, Long> map = storage.get(metricName);
            if (!map.containsKey(projectId)) {
                HashMap<String, Long> projectMapWithSize = new HashMap<>();
                projectMapWithSize.put(projectId, size);

                registerMetricSize(counter, size, projectId);
            } else {
                Long existSize = map.get(projectId);
                if (existSize <= size) {
                    Long differenceSize = size - existSize;
                    existSize += differenceSize;
                    storage.get(metricName).replace(projectId, existSize);

                    registerMetricSize(counter, differenceSize, projectId);
                }
            }
        }
    }

    private void registerMetricSize(Counter.Builder counter, long fileSize, String projectId) {
        counter.tag(PROJECT_ID, projectId)
                .register(meterRegistry)
                .increment(fileSize);
    }

    private void incrementByTypeTag(long size, @NonNull UUID projectId, @NonNull String counterType) {
        switch (counterType) {
            case ATP_MAX_SIZE_UPLOAD_FILE_PER_PROJECT_TOTAL:
                incrementByBuilderCounterAndTags(uploadFileCounter,
                        ATP_MAX_SIZE_UPLOAD_FILE_PER_PROJECT_TOTAL, size,
                         projectId.toString());
                break;
            case ATP_MAX_SIZE_DOWNLOAD_FILE_PER_PROJECT_TOTAL:
                incrementByBuilderCounterAndTags(downloadFileCounter,
                        ATP_MAX_SIZE_DOWNLOAD_FILE_PER_PROJECT_TOTAL, size,
                        projectId.toString());
                break;
            default:
                break;
        }
    }

    public void registerMetricFileSize(long size, UUID projectId, String metricName) {
        incrementByTypeTag(size, projectId, metricName);
    }

    private Long calculateSize(long bytes) {
        return bytes / 1024 / 1024;
    }
}

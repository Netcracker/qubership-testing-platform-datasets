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

package org.qubership.atp.dataset.kafka.handlers.project.strategies;

import org.qubership.atp.dataset.constants.CacheEnum;
import org.qubership.atp.dataset.kafka.entities.project.EventType;
import org.qubership.atp.dataset.kafka.entities.project.ProjectEvent;
import org.qubership.atp.dataset.kafka.handlers.project.ProcessingStrategy;
import org.qubership.atp.dataset.service.direct.VisibilityAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "kafka.enable",
        matchIfMissing = false
)
public class DeleteProjectProcessingStrategy implements ProcessingStrategy<ProjectEvent, EventType> {

    @Autowired
    private VisibilityAreaService visibilityAreaService;

    @Override
    @CacheEvict(value = CacheEnum.Constants.PROJECT_CACHE, key = "#projectEvent.getProjectId()")
    public void process(ProjectEvent projectEvent) {
        visibilityAreaService.delete(projectEvent.getProjectId());
    }

    @Override
    public EventType getIdentifier() {
        return EventType.DELETE;
    }
}

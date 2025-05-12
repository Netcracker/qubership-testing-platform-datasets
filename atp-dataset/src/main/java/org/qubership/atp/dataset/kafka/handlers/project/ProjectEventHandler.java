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

package org.qubership.atp.dataset.kafka.handlers.project;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.qubership.atp.dataset.kafka.entities.project.EventType;
import org.qubership.atp.dataset.kafka.entities.project.ProjectEvent;
import org.qubership.atp.dataset.kafka.handlers.KafkaEventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "kafka.enable",
        matchIfMissing = false
)
public class ProjectEventHandler implements KafkaEventHandler<ProjectEvent> {

    private Map<EventType, ProcessingStrategy> processingStrategyMap;

    /**
     * ProjectEventHandler constructor.
     *
     * @param handleStrategySet is set of processing strategies for a project event.
     */
    @Autowired
    public ProjectEventHandler(Set<ProcessingStrategy<ProjectEvent,EventType>> handleStrategySet) {

        processingStrategyMap = new HashMap<>();
        handleStrategySet.forEach(x -> processingStrategyMap.put(x.getIdentifier(), x));
    }

    @Override
    public void handle(ProjectEvent projectEvent) {
        processingStrategyMap.get(projectEvent.getType()).process(projectEvent);
    }
}

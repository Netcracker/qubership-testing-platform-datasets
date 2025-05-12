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

package org.qubership.atp.dataset.kafka.listeners;

import java.io.IOException;

import org.qubership.atp.dataset.config.KafkaConfiguration;
import org.qubership.atp.dataset.kafka.entities.project.ProjectEvent;
import org.qubership.atp.dataset.kafka.handlers.KafkaEventHandler;
import org.qubership.atp.integration.configuration.mdc.MdcField;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(
        value = "kafka.enable",
        matchIfMissing = false
)
@RequiredArgsConstructor
public class ProjectEventListener {

    private static final String KAFKA_LISTENER_ID = "datasetProjectEvent";

    private final ObjectMapper objectMapper;
    private final KafkaEventHandler<ProjectEvent> projectEventHandler;

    @Autowired
    public ProjectEventListener(KafkaEventHandler<ProjectEvent> projectEventHandler,
                                @Qualifier("ignoreUnknownPropertiesMapper") ObjectMapper objectMapper) {
        this.projectEventHandler = projectEventHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen project-event kafka topic.
     */
    @KafkaListener(id = KAFKA_LISTENER_ID, topics = "${kafka.project.event.consumer.topic.name}",
            containerFactory = KafkaConfiguration.KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME)
    public void listen(@Payload String event) throws IOException {
        MDC.clear();
        ProjectEvent projectEvent = objectMapper.readValue(event, ProjectEvent.class);
        MdcUtils.put(MdcField.PROJECT_ID.toString(), projectEvent.getProjectId());
        projectEventHandler.handle(projectEvent);
    }
}

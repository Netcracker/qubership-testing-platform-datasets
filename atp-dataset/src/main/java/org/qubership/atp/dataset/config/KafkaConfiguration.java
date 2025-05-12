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

package org.qubership.atp.dataset.config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.qubership.atp.dataset.kafka.entities.project.ProjectEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
@ConditionalOnProperty(
        value = "kafka.enable",
        matchIfMissing = false
)
@ComponentScan("org.qubership.atp.dataset.kafka")
public class KafkaConfiguration {

    public static final String KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME = "projectEventContainerFactory";

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServer;

    /**
     * Factory for kafka project event topic listener.
     */
    @Bean(KAFKA_PROJECT_EVENT_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<?> projectEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, ProjectEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(projectEventConsumerFactory());
        factory.setErrorHandler((e, consumerRecord) -> {
            throw new RuntimeException("Error during event processing.", e);
        });

        return factory;
    }

    /**
     * Custom kafka consumer factory.
     */
    @Bean
    public ConsumerFactory projectEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory(consumerConfigs());
    }

    /**
     * Kafka consumer configuration.
     */
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return props;
    }
}

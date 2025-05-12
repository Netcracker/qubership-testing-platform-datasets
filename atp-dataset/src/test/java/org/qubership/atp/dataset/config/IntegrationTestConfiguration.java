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

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import com.querydsl.sql.SQLQueryFactory;

@Configuration
@Import({TestConfiguration.class})
public class IntegrationTestConfiguration {

    private static final String KEYCLOAK_AUTH_SERVER_URL = "KEYCLOAK_AUTH_SERVER_URL";

    @Bean("serverContext")
    public ApplicationContext serverContext() {
        Map<String, Object> props = new HashMap<>();
        props.put(KEYCLOAK_AUTH_SERVER_URL, "");
        return new SpringApplicationBuilder(OverrideOriginBeanConfig.class)
                .properties(props).run();
    }

    @Bean
    public Environment serverEnvironment() {
        return serverContext().getEnvironment();
    }

    @Bean
    public SQLQueryFactory queryFactory() {
        return serverContext().getBean(SQLQueryFactory.class);
    }
}

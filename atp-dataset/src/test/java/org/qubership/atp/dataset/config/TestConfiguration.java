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

import static org.mockito.Mockito.mock;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.repository.api.JaversRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.qubership.atp.auth.springbootstarter.provider.impl.DisableSecurityUserProvider;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.client.MacrosFeignClient;
import liquibase.integration.spring.SpringLiquibase;

@org.springframework.boot.test.context.TestConfiguration
@Import({MetricsConfiguration.class, ServiceConfiguration.class})
public class TestConfiguration {

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setShouldRun(false);
        return liquibase;
    }

    /**
     * By default, without specified {@link JaversRepository}, Javers will use in-memory database
     *
     * @return instance for {@link Javers} bean
     */
    @Bean
    public Javers javers() {
        return JaversBuilder
                .javers()
                .build();
    }

    @Bean
    public DisableSecurityUserProvider userInfoProvider() {
        return new DisableSecurityUserProvider();
    }

    @Bean
    public MacrosFeignClient macrosFeignClient() {
        return mock(MacrosFeignClient.class);
    }

    @Bean
    public MacrosCalculator macrosCalculator() {
        return mock(MacrosCalculator.class);
    }
}

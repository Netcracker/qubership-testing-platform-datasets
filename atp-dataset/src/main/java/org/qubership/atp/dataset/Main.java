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

package org.qubership.atp.dataset;

import org.qubership.atp.auth.springbootstarter.security.oauth2.client.config.annotation.EnableOauth2FeignClientInterceptor;
import org.qubership.atp.common.lock.annotation.EnableAtpLockManager;
import org.qubership.atp.common.probes.annotation.EnableProbes;
import org.qubership.atp.crypt.config.annotation.AtpCryptoEnable;
import org.qubership.atp.crypt.config.annotation.AtpDecryptorEnable;
import org.qubership.atp.dataset.config.AtpSwaggerConfig;
import org.qubership.atp.dataset.config.CacheConfiguration;
import org.qubership.atp.dataset.config.ConverterConfig;
import org.qubership.atp.dataset.config.DsSecurityConfiguration;
import org.qubership.atp.dataset.config.ExportNodeConfiguration;
import org.qubership.atp.dataset.config.HttpsConfig;
import org.qubership.atp.dataset.config.KafkaConfiguration;
import org.qubership.atp.dataset.config.LocaleResolverConfiguration;
import org.qubership.atp.dataset.config.ServiceConfiguration;
import org.qubership.atp.dataset.config.SpringLiquibaseConfig;
import org.qubership.atp.dataset.db.migration.MigrationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;

@Import({
        ServiceConfiguration.class,
        HttpsConfig.class,
        DsSecurityConfiguration.class,
        ConverterConfig.class,
        KafkaConfiguration.class,
        ExportNodeConfiguration.class,
        AtpSwaggerConfig.class,
        LocaleResolverConfiguration.class,
        SpringLiquibaseConfig.class,
        CacheConfiguration.class
})
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = MongoAutoConfiguration.class)
@AtpCryptoEnable
@AtpDecryptorEnable
@EnableFeignClients(basePackages = {
        "org.qubership.atp.integration.configuration.feign",
        "org.qubership.atp.macros.core",
        "org.qubership.atp.dataset"
})
@EnableOauth2FeignClientInterceptor
@EnableDiscoveryClient
@EnableAsync
@Slf4j
@EnableAtpLockManager
@EnableProbes
public class Main {

    /**
     * Entry point class for Dataset application.
     *
     * @param args CL arguments.
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(Main.class, args);
        configurableApplicationContext.addApplicationListener(new ApplicationPidFileWriter("application.pid"));
        MigrationRunner migrationRunner = configurableApplicationContext.getBean(MigrationRunner.class);
        migrationRunner.runMigration();
    }
}

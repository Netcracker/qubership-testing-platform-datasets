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

package org.qubership.atp.dataset.db.migration;

import java.sql.SQLException;

import org.qubership.atp.dataset.db.migration.config.DbConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import liquibase.exception.LiquibaseException;
import lombok.extern.slf4j.Slf4j;

@Import({DbConfiguration.class})
@Configuration
@ComponentScan("org.qubership.atp.dataset.db.migration")
@Slf4j
public class Main {

    /**
     * Entry point class for Dataset migration.
     *
     * @param args CL arguments.
     */
    public static void main(String[] args) throws SQLException, LiquibaseException {
        log.info("Started migration runner in Main Migration module");
        ConfigurableApplicationContext configurableApplicationContext = SpringApplication.run(Main.class, args);
        MigrationRunner migrationRunner = configurableApplicationContext.getBean(MigrationRunner.class);
        migrationRunner.runMigration();
    }
}

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

@ContextConfiguration(classes = {MigrationRunnerTest.TestConfiguration.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:test.properties")
public class MigrationRunnerTest {

    @Autowired
    private MigrationRunner migrationRunner;

    @Autowired
    private LiquibaseFactory liquibaseFactory;

    @Configuration
    @ComponentScan("org.qubership.atp.dataset.db.migration")
    public static class TestConfiguration {

        @Bean
        public DataSource dataSource() {
            return mock(DataSource.class);
        }

        @Bean
        public LiquibaseFactory liquibaseFactory() {
            return mock(LiquibaseFactory.class);
        }

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @Test
    public void runMigrationTest_successfullyMigrated() throws SQLException, LiquibaseException {
        // given
        String liquibaseConfigXml = "config.xml";
        Database database = mock(Database.class);
        DatabaseConnection databaseConnection = mock(DatabaseConnection.class);
        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
        Liquibase liquibase = mock(Liquibase.class);
        // when
        when(liquibaseFactory.create(any())).thenReturn(liquibase);
        when(liquibaseFactory.create(any(), any())).thenReturn(liquibase);
        when(liquibase.getDatabase()).thenReturn(database);
        when(database.getConnection()).thenReturn(databaseConnection);
        migrationRunner.runMigration();
        // then
        verify(liquibase, times(2)).update(any(Contexts.class), any(LabelExpression.class));
    }
}

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

import static java.util.concurrent.TimeUnit.SECONDS;

import javax.sql.DataSource;

import org.qubership.atp.dataset.config.listeners.ConnectionClosedListener;
import org.qubership.atp.dataset.db.DBConfig;
import org.qubership.atp.dataset.db.DbConfigImpl;
import org.qubership.atp.dataset.service.direct.helper.DbCreationFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spring.SpringExceptionTranslator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

@org.springframework.context.annotation.Configuration
@ComponentScan("org.qubership.atp.dataset.db")
@EnableJpaRepositories("org.qubership.atp.dataset.db.jpa.repositories")
public class DbConfiguration {

    /**
     * creates DBConfig from provided properties.
     *
     * @return created DBConfig
     */
    @Bean
    public DBConfig dbConfig() {
        return new DbConfigImpl();
    }

    /**
     * creates data source from DBConfig.
     */

    @Bean
    public DataSource dataSource(DBConfig dbconfig) {
        HikariConfig config = new HikariConfig();
        config.setLeakDetectionThreshold(SECONDS.toMillis(dbconfig.getLeakDetectionThreshold()));
        config.setJdbcUrl(dbconfig.getJdbcUrl());
        config.setUsername(dbconfig.getUsername());
        config.setPassword(dbconfig.getPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMinimumIdle(dbconfig.getMinIdle());
        config.setMaximumPoolSize(dbconfig.getMaxPoolSize());
        config.setRegisterMbeans(dbconfig.isDebug());
        return new HikariDataSource(config);
    }

    /**
     * Transaction manager bean.
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Get query dsl configuration.
     */
    @Bean
    public Configuration qdslConfig(DataSource dataSource) {
        SQLTemplates templates = PostgreSQLTemplates.builder().build();
        Configuration configuration = new Configuration(templates);
        configuration.setExceptionTranslator(new SpringExceptionTranslator());
        configuration.addListener(new ConnectionClosedListener(dataSource));
        return configuration;
    }

    @Bean
    public SQLQueryFactory queryFactory(DataSource dataSource, Configuration qdslConfig) {
        return new SQLQueryFactory(qdslConfig, new TransactionAwareDataSourceProxy(dataSource));
    }

    @Bean
    public DbCreationFacade dbCreationFacade() {
        return new DbCreationFacade();
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}

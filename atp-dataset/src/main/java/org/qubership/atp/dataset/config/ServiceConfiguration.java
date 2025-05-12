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

import java.util.ArrayList;
import java.util.Properties;

import javax.script.ScriptEngineManager;
import javax.sql.DataSource;
import javax.validation.Validator;

import org.javers.core.Javers;
import org.javers.repository.api.JaversRepository;
import org.javers.spring.jpa.TransactionalJpaJaversBuilder;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.dataset.model.utils.FlatSerializerModifier;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.service.ws.WebSocketConfig;
import org.qubership.atp.dataset.versioning.service.JaversAuthorProvider;
import org.qubership.atp.dataset.versioning.service.impl.HistoryItemMixin;
import org.qubership.atp.dataset.versioning.service.impl.JaversAuthorProviderImpl;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.calculator.ScriptMacrosCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableWebSocket

@ComponentScan({
        "org.qubership.atp.dataset.macros",
        "org.qubership.atp.dataset.service",
        "org.qubership.atp.dataset.config.interceptors",
        "org.qubership.atp.dataset.versioning.service",
})
@Import({DbConfiguration.class, GridFsConfiguration.class, WebSocketConfig.class,
        SchedulerConfig.class, ThreadPoolConfig.class})
@EnableAspectJAutoProxy
@EnableTransactionManagement
public class ServiceConfiguration {

    /**
     * Create custom json serializer.
     */
    @Bean
    @Primary
    public ObjectMapper jsonObjectMapper() {
        ArrayList<Module> modules = new ArrayList<>();

        SimpleModule serializerModule = new SimpleModule();
        serializerModule.setSerializerModifier(new FlatSerializerModifier());

        modules.add(serializerModule);

        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(modules)
                .build();
        objectMapper.enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID);
        objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        objectMapper.addMixIn(HistoryItemDto.class, HistoryItemMixin.class);
        return objectMapper;
    }

    /**
     * Object mapper configuration that ignores unknown properties.
     */
    @Bean
    public ObjectMapper ignoreUnknownPropertiesMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    /**
     * Custom entity manager for hibernate.
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.qubership.atp.dataset.db.jpa.entities");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());
        return em;
    }

    @Bean
    public Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL94Dialect");
        properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", "true");
        properties.setProperty("hibernate.show_sql", "false"); //Set as 'true' to enable verbose query logging
        properties.setProperty("hibernate.format_sql", "false"); //Set as 'true' to enable pretty format
        properties.setProperty("spring.jpa.show-sql", "false");
        properties.setProperty(
                "hibernate.current_session_context_class",
                "org.hibernate.context.internal.ThreadLocalSessionContext"
        );
        properties.setProperty("hibernate.generate_statistics", "true");
        properties.setProperty("hibernate.session.events.log", "false");
        return properties;
    }

    /**
     * Custom transaction manager for hibernate.
     */
    @Bean
    public PlatformTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean entityManagerFactoryBean
    ) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactoryBean.getObject());
        return transactionManager;
    }

    @Bean
    public JaversAuthorProvider javersAuthorProvider(Provider<UserInfo> userInfoProvider) {
        return new JaversAuthorProviderImpl(userInfoProvider);
    }

    @Bean
    public MacrosCalculator macrosCalculator(ScriptEngineManager scriptEngineManager) {
        return new ScriptMacrosCalculator(scriptEngineManager);
    }

    /**
     * Create Nashorn Script Engine Factory for calculate ATP macroses.
     */
    @Bean
    public ScriptEngineManager scriptEngineManager() {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        scriptEngineManager.registerEngineName("javascript", new NashornScriptEngineFactory());
        return scriptEngineManager;
    }

    /**
     * Javers transactions.
     */
    @Bean
    public Javers javers(JaversRepository javersRepository, PlatformTransactionManager txManager) {
        return TransactionalJpaJaversBuilder
                .javers()
                .withTxManager(txManager)
                .registerJaversRepository(javersRepository)
                .build();
    }
}

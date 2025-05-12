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

package org.qubership.atp.dataset.migration.config;

import javax.inject.Provider;

import org.qubership.atp.dataset.config.ServiceConfiguration;
import org.qubership.atp.dataset.db.CacheRepository;
import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.ListValueRepository;
import org.qubership.atp.dataset.db.ParameterRepository;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.migration.repo.OrderedAttributeRepository;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.VisibilityAreaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.querydsl.sql.SQLQueryFactory;

@Configuration
@Import(ServiceConfiguration.class)
@ComponentScan("org.qubership.atp.dataset.migration.repo")
public class MigrationConfig {

    @Bean
    public DsServicesFacade dsServicesFacade(VisibilityAreaService va,
                                             DataSetListService dsl,
                                             OrderedAttributeRepository attrRepo,
                                             DataSetService ds,
                                             ParameterService param,
                                             AttributeService attr,
                                             GridFsService fs) {
        return new DsServicesFacade(va, dsl, attrRepo, ds, param, attr, fs);
    }

    @Bean
    public OrderedAttributeRepository attributeRepository(SQLQueryFactory queryFactory,
                                                          Provider<DataSetListRepository> dslRepo,
                                                          Provider<ParameterRepository> paramRepo,
                                                          Provider<ListValueRepository> lvRepo,
                                                          CacheRepository cacheRepo) {
        return new OrderedAttributeRepository(queryFactory, dslRepo, paramRepo, lvRepo, cacheRepo);
    }
}

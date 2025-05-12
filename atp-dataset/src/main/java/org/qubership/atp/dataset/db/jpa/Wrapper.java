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

package org.qubership.atp.dataset.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.qubership.atp.dataset.db.jdbc.JdbcTemplates;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Wrapper {
    protected static EntityManager entityManager;
    protected static JdbcTemplates jdbc;
    protected static ModelsProvider modelsProvider;
    protected static GridFsService gridFsService;

    @PersistenceContext
    private void injectEntityManager(EntityManager entityManager) {
        Wrapper.entityManager = entityManager;
    }

    @Autowired
    private void setJdbc(JdbcTemplates jdbc) {
        Wrapper.jdbc = jdbc;
    }

    @Autowired
    private void setModelsProvider(ModelsProvider modelsProvider) {
        Wrapper.modelsProvider = modelsProvider;
    }

    @Autowired
    private void setGridFsService(GridFsService gridFsService) {
        Wrapper.gridFsService = gridFsService;
    }
}

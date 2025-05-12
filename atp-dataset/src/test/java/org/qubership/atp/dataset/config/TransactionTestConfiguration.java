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

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetListService;

@Configuration
@Import({TestConfiguration.class})
public class TransactionTestConfiguration {
    @Bean
    public DbUtils dbUtils() {
        return new DbUtils();
    }

    public static class DbUtils {

        @Autowired
        private AttributeService attributeService;
        @Autowired
        private DataSetListService dataSetListService;


        @Transactional
        public void operationsInTransaction(UUID attributeId, UUID dslId, String dslNameRenamed) {
            attributeService.delete(attributeId);
            dataSetListService.rename(dslId, dslNameRenamed);
        }
    }
}

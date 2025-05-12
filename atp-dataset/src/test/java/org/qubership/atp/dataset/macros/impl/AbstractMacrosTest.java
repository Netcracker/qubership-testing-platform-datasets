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

package org.qubership.atp.dataset.macros.impl;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.dataset.macros.processor.Evaluator;
import org.qubership.atp.dataset.service.direct.impl.ClearCacheServiceImpl;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "jdbc.leak.detection.threshold=10",
        "atp-dataset.last.revision.count=200",
        "atp-dataset.archive.job.bulk-delete-count=1000",
        "atp-dataset.archive.cron.expression=0 0 0 * * ?",
        "atp-dataset.archive.job.name=atp-dataset-archive-job",
        "atp-dataset.archive.job.page-size=50",
        "atp-dataset.archive.job.thread.max-pool-size=5",
        "atp-dataset.archive.job.thread.core-pool-size=5",
        "atp-dataset.archive.job.thread.queue-capacity=100"
})
public abstract class AbstractMacrosTest {
    @MockBean
    Encryptor encryptor;
    @MockBean
    Decryptor decryptor;
    @Autowired
    Evaluator evaluator;
    @MockBean
    ClearCacheServiceImpl clearCacheService;
}

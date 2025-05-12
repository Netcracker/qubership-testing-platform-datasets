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

package org.qubership.atp.dataset.service.jpa.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.dataset.service.jpa.JpaAttributeService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.jpa.JpaVisibilityAreaService;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.qubership.atp.macros.core.calculator.MacrosCalculator;
import org.qubership.atp.macros.core.client.MacrosFeignClient;

@Disabled
@Isolated
public class AbstractJpaTest {
    @Autowired
    protected JpaDataSetListService dataSetListService;
    @Autowired
    protected JpaDataSetService dataSetService;
    @Autowired
    protected JpaAttributeService attributeService;
    @Autowired
    protected JpaParameterService parameterService;
    @Autowired
    protected JpaVisibilityAreaService visibilityAreaService;
    @Autowired
    protected DataSetListSnapshotService snapshotService;
    @MockBean
    protected Encryptor encryptor;
    @MockBean
    protected Decryptor decryptor;
    @MockBean
    protected MacrosFeignClient macrosFeignClient;
    @MockBean
    protected MacrosCalculator scriptMacrosCalculator;
}

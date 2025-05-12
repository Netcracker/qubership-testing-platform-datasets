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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import org.qubership.atp.dataset.config.MockJaversCommitEntityServiceConfiguration;
import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

@Isolated
@ContextConfiguration(classes = {TestMacrosConfiguration.class, MockJaversCommitEntityServiceConfiguration.class})
public class ReferenceMacrosTest extends AbstractTest {

    @Autowired
    private EvaluationService evaluationService;

    @Test
    public void testReference() throws Exception {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String alias = wrapperService.wrapToAlias("#REF(DS.DS_ExternalDS.ExternalAttr)", d.va, d.dsl);
            String evaluate = evaluator.evaluateText(d.ds, alias);
            Assertions.assertEquals(d.expectedValue, evaluate);
        }
    }

    @Test
    public void testReferenceToDsName() throws Exception {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String alias = wrapperService.wrapToAlias("#REF(DS.DS_ExternalDS)", d.va, d.dsl);
            String evaluate = evaluator.evaluateText(d.ds, alias);
            Assertions.assertEquals(d.externalDs.getName(), evaluate);
        }
    }
}

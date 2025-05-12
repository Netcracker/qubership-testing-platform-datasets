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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

@Isolated
@ContextConfiguration(classes = {TestMacrosConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class ReferenceToDslMacrosTest extends AbstractTest {
    @Autowired
    private EvaluationService evaluationService;

    @Test
    public void testReferenceToDsl() throws EvalException {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String inputText = wrapperService.wrapToAlias("#REF_DSL(ExternalDSL.ExternalDS.ExternalAttr)", d.va, d.dsl);
            String result = evaluator.evaluateText(d.ds, inputText);
            assertEquals(d.expectedValue, result);
        }
    }

    @Test
    public void testReferenceDslHasSameValueAfterReEvaluation() throws EvalException {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);

        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            parameterService.update(d.param.getId(), "#UUID()");
            String inputText = wrapperService.wrapToAlias("#REF_DSL(ExternalDSL.ExternalDS.ExternalAttr)", d.va, d.dsl);
            //for DSL: referenceDslId eval value must be cached and should be same.
            String result = evaluator.evaluateText(d.ds, inputText);
            String result2 = evaluator.evaluateText(d.ds, inputText);
            assertEquals(result, result2, "Values must be equals");
        }
    }
}

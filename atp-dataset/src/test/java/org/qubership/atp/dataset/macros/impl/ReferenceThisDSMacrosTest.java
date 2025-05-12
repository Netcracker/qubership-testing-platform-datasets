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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.base.Splitter;
import org.qubership.atp.dataset.config.MockJaversCommitEntityServiceConfiguration;
import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

/**
 * atp-dataset-aggregator
 */
@Isolated
@ContextConfiguration(classes = {TestMacrosConfiguration.class, MockJaversCommitEntityServiceConfiguration.class})
public class ReferenceThisDSMacrosTest extends AbstractTest {

    @Autowired
    private EvaluationService evaluationService;

    @Test
    public void wrapToAlias_ReplaceNamesWithIdAndAlias_TypesObtainedByAliasEqualToExpectedTypes() {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);
        String alias = wrapperService.wrapToAlias("#REF_THIS(DS_ExternalDS.ExternalAttr)", d.va, d.dsl);
        assertAliasPathEquals(alias, d.externalDsRef.getAttribute().getId(), d.externalParam.getAttribute().getId());
    }

    @Test
    public void wrapToAlias_ReplaceNamesWithIdAndAliasForDslAttr_TypesObtainedByAliasEqualToExpectedTypes() {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);
        String alias = wrapperService.wrapToAlias("#REF_THIS(DS_ExternalDS)", d.va, d.dsl);
        assertAliasPathEquals(alias, d.externalDsRef.getAttribute().getId());
    }

    private void assertAliasPathEquals(String wrappedMacros, UUID... expected) {
        String actualAliasPath = wrappedMacros.substring(wrappedMacros.indexOf("(") + 1, wrappedMacros.length() - 1);
        List<UUID> ar = StreamSupport.stream(Splitter.on('.').split(actualAliasPath).spliterator(), false)
                .map(UUID::fromString)
                .collect(Collectors.toList());
        List<UUID> er = Arrays.asList(expected);
        assertThat(ar, hasSize(er.size()));
        Iterator<UUID> arIter = ar.iterator();
        for (Iterator<UUID> erIter = er.iterator(); erIter.hasNext(); ) {
            UUID erItemId = erIter.next();
            UUID arItemId = arIter.next();
            Assertions.assertEquals(erItemId, arItemId);
        }
    }

    @Test
    public void unWrapAlias_UnWrapToAliasAndCompareWithExpectedNames_ReturnStringEqualToExpected() {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);
        String expected = "dasdasd #REF_THIS(DS_ExternalDS.ExternalAttr)";
        String wrapped = wrapperService.wrapToAlias(expected, d.va, d.dsl);
        String unwrapped = wrapperService.unWrapAlias(wrapped);
        Assertions.assertEquals(expected, unwrapped);
    }

    @Test
    public void evaluate_EvaluateReferenceThis_ReturnValueOfAttributeEqualToExpectedValue() throws EvalException {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);

        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String alias = wrapperService.wrapToAlias("#REF_THIS(DS_ExternalDS.ExternalAttr)", d.va, d.dsl);
            String evaluate = evaluator.evaluateText(d.ds, alias);
            Assertions.assertEquals(d.expectedValue, evaluate);
        }
    }

    @Test
    public void evaluate_RefToTargetParamWithoutGroup_Resolved() throws EvalException {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);

        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String alias = wrapperService.wrapToAlias("#REF_THIS(Attr)", d.va, d.dsl);
            String evaluate = evaluator.evaluateText(d.ds, alias);
            Assertions.assertEquals(d.expectedValue, evaluate);
        }
    }

    @Test
    public void evaluate_RefToDSLAttr_EqualsDsName() throws EvalException {
        ReferenceMacrosTestData d = createTestDataInstance(ReferenceMacrosTestData::new);

        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String alias = wrapperService.wrapToAlias("#REF_THIS(DS_ExternalDS)", d.va, d.dsl);
            String evaluate = evaluator.evaluateText(d.ds, alias);
            Assertions.assertEquals("ExternalDS", evaluate);
        }
    }
}

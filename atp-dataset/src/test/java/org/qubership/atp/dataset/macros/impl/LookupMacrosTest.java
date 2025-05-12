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

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import org.qubership.atp.dataset.config.MockJaversCommitEntityServiceConfiguration;
import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.macros.processor.AbstractMacroProcessorTest;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.model.utils.PhoneCallTestData;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.direct.EvaluationService;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

@Isolated
@ContextConfiguration(classes = {TestMacrosConfiguration.class, MockJaversCommitEntityServiceConfiguration.class})
public class LookupMacrosTest extends AbstractTest {

    @Autowired
    private EvaluationService evaluationService;

    @Test
    public void refDslToRefThis_ChildRefPointsToUnreachableAttribute_FoundInParentRefContext() throws Exception {
        RefToRefTestData.RefDslChildRefTargetAttrIsUnreachable data = createTestDataInstance(RefToRefTestData
                .RefDslChildRefTargetAttrIsUnreachable::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            //validate parent ref
            Optional<String> result = evaluator.apply(data.parentRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
        //new cache
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            //validate child ref
            Exception expected = null;
            try {
                evaluator.apply(data.childRef);
            } catch (Exception e) {
                expected = e;
            }
            Assertions.assertNotNull(expected, "Exception expected");
        }
    }

    @Test
    public void refDslToRefThis_ChildRefPointsToUninitializedParameter_GotEmptyString() throws Exception {
        RefToRefTestData.RefDslChildRefTargetParamIsUninitialized data = createTestDataInstance(RefToRefTestData
                .RefDslChildRefTargetParamIsUninitialized::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> result = evaluator.apply(data.parentRef);
            Assertions.assertEquals("", result.get());
            result = evaluator.apply(data.childRef);
            Assertions.assertEquals("", result.get());
        }
    }

    @Test
    public void refDslToRefThis_ChildRefPointsToInitializedParameter_GotParameterText() throws Exception {
        RefToRefTestData.RefDslChildRefTargetParamIsInitialized data = createTestDataInstance(RefToRefTestData
                .RefDslChildRefTargetParamIsInitialized::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> result = evaluator.apply(data.parentRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
            result = evaluator.apply(data.childRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
    }

    @Test
    public void refDslToRefThis_ChildRefPointsToParameterWithBrackets_GotParameterText() throws Exception {
        RefToRefTestData.RefDslChildRefTargetParamHasBrackets data = createTestDataInstance(RefToRefTestData
                .RefDslChildRefTargetParamHasBrackets::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> result = evaluator.apply(data.parentRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
            result = evaluator.apply(data.childRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
    }

    @Test
    public void refThisToRefThis_ChildRefPointsToUnreachableAttribute_FoundInParentRefContext() throws Exception {
        RefToRefTestData.RefThisChildRefTargetAttrIsUnreachable data = createTestDataInstance(RefToRefTestData
                .RefThisChildRefTargetAttrIsUnreachable::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            OverlapItem.Reachable parentRef = OverlapIterator.create(data.currentCase, data.parentRef.getAttribute()
                    .getId(), null)
                    .next()
                    .asReachable();
            Optional<String> result = evaluator.apply(parentRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
            OverlapItem.Reachable childRef = OverlapIterator.create(data.currentCase, data.childRef.getAttribute()
                    .getId(), Collections.singletonList(data.currentCaseIntoBelgium.getAttribute().getId()))
                    .next()
                    .asReachable();
            result = evaluator.apply(childRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
    }

    @Test
    public void refThisToRefThis_ChildRefPointsToUninitializedParameter_GotEmptyString() throws Exception {
        RefToRefTestData.RefThisChildRefTargetParamIsUninitialized data = createTestDataInstance(RefToRefTestData
                .RefThisChildRefTargetParamIsUninitialized::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            OverlapItem.Reachable parentRef = OverlapIterator.create(data.currentCase,
                    data.parentRef.getAttribute().getId(), null)
                    .next()
                    .asReachable();
            Optional<String> result = evaluator.apply(parentRef);
            Assertions.assertEquals("", result.get());
            OverlapItem.Reachable childRef = OverlapIterator.create(data.currentCase,
                    data.childRef.getAttribute().getId(),
                    Collections.singletonList(data.currentCaseIntoBelgium.getAttribute().getId()))
                    .next()
                    .asReachable();
            result = evaluator.apply(childRef);
            Assertions.assertEquals("", result.get());
        }
    }

    @Test
    public void refThisToRefThis_ChildRefPointsToInitializedParameter_GotParameterText() throws Exception {
        RefToRefTestData.RefThisChildRefTargetParamIsInitialized data = createTestDataInstance(RefToRefTestData
                .RefThisChildRefTargetParamIsInitialized::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> result = evaluator.apply(data.parentRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
    }

    @Test
    public void refThis_RefTargetIsOverlapped_ContextOfOverlapIsUsed() throws Exception {
        RefToRefTestData.RefTargetParamIsOverlapped data = createTestDataInstance(RefToRefTestData
                .RefTargetParamIsOverlapped::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            OverlapItem.Reachable childRefInCurrentCaseCtx = OverlapIterator.create(data.currentCase,
                    data.childRef.getAttribute().getId(),
                    Collections.singletonList(data.currentCaseIntoBelgium.getAttribute().getId()))
                    .next()
                    .asReachable();
            Optional<String> result = evaluator.apply(childRefInCurrentCaseCtx);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
    }

    @Test
    public void refThisToRefThis_RefTargetIsOverlapped_ContextOfOverlapIsUsed() throws Exception {
        RefToRefTestData.ChildRefTargetParamIsOverlapped data = createTestDataInstance(RefToRefTestData
                .ChildRefTargetParamIsOverlapped::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> result = evaluator.apply(data.parentRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
    }

    @Test
    public void refThisToRefThis_ChildRefIsOverlapped_ContextOfDefaultIsUsed() throws Exception {
        RefToRefTestData.ChildRefIsOverlapped data = createTestDataInstance(RefToRefTestData.ChildRefIsOverlapped::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> result = evaluator.apply(data.parentRef);
            Assertions.assertEquals(data.brussels.getText(), result.get());
        }
    }

    @Test
    public void vLookup_RefDslUsingRefThis_ResolvedProperly() throws Exception {
        PhoneCallTestData data = createTestDataInstance(PhoneCallTestData::new);
        String vlookupMacro = String.format("#REF_DSL(%s.#REF_THIS(%s.%s).#REF_THIS(%s.%s))",
                data.internationalRateCost.getName(),
                data.voiceCallIntoOriginCountryRef.getAttribute().getName(),
                data.countryZone.getName(),
                data.voiceCallIntoDestinationCountryRef.getAttribute().getName(),
                data.countryZone.getName());
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String alias = wrapperService.wrapToAlias(vlookupMacro, data.va, data.voiceCall);
            String evaluate = evaluator.evaluateText(data.defaultVoiceCall, alias);
            Assertions.assertEquals(data.rateCost.getText(), evaluate);
        }
    }

    @Test
    public void vLookup_RefToDslWithOverlap_ResolvedProperly() throws Exception {
        RefToRefTestData.VoiceCaseRefToDslWithOverlap data = createTestDataInstance(RefToRefTestData
                .VoiceCaseRefToDslWithOverlap::new);
        //#REF_DSL(International Voice Rates.#REF_THIS(Subscription.TariffName).#REF_THIS(UsageType).#REF_THIS
        // (DestinationCountry.IntenationalZone))
        String vlookupMacro = String.format("#REF_DSL(%s.#REF_THIS(%s.%s).#REF_THIS(%s).#REF_THIS(%s.%s))",
                data.internationalVoiceRates.getName(),
                data.belToUsaIntoSubscriptionRef.getAttribute().getName(),
                data.tariffName.getAttribute().getName(),
                data.usageType.getAttribute().getName(),
                data.belToUsaIntoCountryRef.getAttribute().getName(),
                data.internationalZone.getAttribute().getName());
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            String alias = wrapperService.wrapToAlias(vlookupMacro, data.va, data.voiceCases);
            String evaluate = evaluator.evaluateText(data.belToUsa, alias);
            Assertions.assertEquals(data.belgiumToUsaRatingOverlap.getText(), evaluate);
        }
    }

    /**
     * You can find expected evaluation order in {@link AbstractMacroProcessorTest#macroReturnsAnotherMacro_complexRootMacro_AllAreEvaluated()}.
     */
    @Test
    public void vLookup_RefToRefWithOverlap_ResolvedProperly() throws Exception {
        RefToRefTestData.VoiceCaseRefToRefWithOverlap data = createTestDataInstance(RefToRefTestData
                .VoiceCaseRefToRefWithOverlap::new);
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> result = evaluator.apply(data.targetParameter);
            Assertions.assertEquals(data.belgiumToWorld2RatingOverlap.getText(), result.get());
        }
    }

    @Test
    public void TwoRefDsl_ToTheSameRandom_IsEqual() throws Exception {
        RefToRefTestData.TwoRefsToTheSameRandom d = createTestDataInstance(facade -> new RefToRefTestData
                .TwoRefsToTheSameRandom(facade, wrapperService));
        try (DsEvaluator evaluator = evaluationService.getEvaluator(true, false)) {
            Optional<String> keyA = evaluator.apply(d.keyA);
            Optional<String> keyB = evaluator.apply(d.keyB);
            Assertions.assertEquals(keyA.get(), keyB.get());
        }
    }
}


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

package org.qubership.atp.dataset.macros.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import org.qubership.atp.dataset.macros.EvalContextImpl;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.cache.MacroCacheKey;
import org.qubership.atp.dataset.macros.cache.NoCache;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.impl.RefToRefTestData;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.model.utils.OverlapIterator;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;
import org.qubership.atp.dataset.service.direct.macros.EvaluateDsParamStrategyImpl;

@Isolated
public class EvalContextImplTest {
    private static final CreationFacade FACADE = SimpleCreationFacade.INSTANCE;
    private static final MacroRegistry REGISTRY = Mockito.mock(MacroRegistry.class);
    private static final AliasWrapperService ALIAS_WRAPPER_SVC = Mockito.mock(AliasWrapperService.class);
    private static final TestEvalStrategy EVAL = new TestEvalStrategy(REGISTRY, ALIAS_WRAPPER_SVC);


    private static OverlapItem.Reachable goTo(DataSet ds, Attribute targetAttribute) {
        return OverlapIterator.create(ds, targetAttribute.getId(), Collections.emptyList())
                .next()
                .asReachable();
    }

    private static OverlapItem.Reachable goTo(DataSet ds, Attribute targetAttribute, List<Attribute> attributePath) {
        return OverlapIterator.create(ds, targetAttribute.getId(),
                attributePath.stream().map(Identified::getId).collect(Collectors.toList()))
                .next()
                .asReachable();
    }

    private static OverlapItem.Reachable goTo(DataSet ds, Attribute targetAttribute, Attribute... attributePath) {
        return goTo(ds, targetAttribute, Arrays.asList(attributePath));
    }

    private static TestEvalContext rootErContext(@Nonnull DataSet ds) {
        return erContext(null, ds, true, null, Collections.emptyList());
    }

    private static TestEvalContext erContext(@Nullable TestEvalContext parent,
                                             @Nonnull DataSet ds,
                                             boolean parentIsStrict,
                                             @Nullable Attribute targetAttrFromParent,
                                             Attribute... pathFromParent) {
        return erContext(parent, ds, parentIsStrict, targetAttrFromParent, Arrays.asList(pathFromParent));
    }

    private static TestEvalContext erContext(@Nullable TestEvalContext parent,
                                             @Nonnull DataSet ds,
                                             boolean parentIsStrict,
                                             @Nullable Attribute targetAttrFromParent,
                                             @Nonnull List<Attribute> pathFromParent) {
        return new TestEvalContext(parent, REGISTRY, ALIAS_WRAPPER_SVC, ds.getDataSetList(), ds,
                parentIsStrict, pathFromParent, targetAttrFromParent);
    }

    @BeforeEach
    public void reset() {
        EVAL.reset();
    }


    @Test
    public void refDslToRefThis_ChildRefPointsToUninitializedParameter_GotEmptyString() throws Exception {
        RefToRefTestData.RefDslChildRefTargetParamIsUninitialized data = new RefToRefTestData.RefDslChildRefTargetParamIsUninitialized(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext parentRefCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        nextParam = goTo(data.belgium, data.childRef.getAttribute());
        TestEvalContext childRefCtx = erContext(parentRefCtx, data.belgium, false, data.childRef.getAttribute(), Collections.emptyList());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(parentRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }

    @Test
    public void refDslToRefThis_ChildRefPointsToUnreachableAttribute_FoundInParentRefContext() throws Exception {
        RefToRefTestData.RefDslChildRefTargetAttrIsUnreachable data = new RefToRefTestData.RefDslChildRefTargetAttrIsUnreachable(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext parentRefCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        nextParam = goTo(data.belgium, data.childRef.getAttribute());
        TestEvalContext childRefCtx = erContext(parentRefCtx, data.belgium, false, data.childRef.getAttribute(), Collections.emptyList());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.capital);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(parentRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }

    @Test
    public void refDslToRefThis_ChildRefPointsToInitializedParameter_GotTwoStrictContexts() throws Exception {
        RefToRefTestData.RefDslChildRefTargetParamIsInitialized data = new RefToRefTestData.RefDslChildRefTargetParamIsInitialized(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext parentRefCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        nextParam = goTo(data.belgium, data.childRef.getAttribute());
        TestEvalContext childRefCtx = erContext(parentRefCtx, data.belgium, false, data.childRef.getAttribute(), Collections.emptyList());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.belgium, data.brussels.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(parentRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }

    @Test
    public void refThisToRefThis_ChildRefPointsToUnreachableAttribute_FoundInParentRefContext() throws Exception {
        RefToRefTestData.RefThisChildRefTargetAttrIsUnreachable data = new RefToRefTestData.RefThisChildRefTargetAttrIsUnreachable(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext parentRefCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        TestEvalContext childRefCtx = erContext(parentRefCtx, data.currentCase, true, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.capital);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx, parentRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }

    @Test
    public void refThisToRefThis_ChildRefPointsToUninitializedParameter_GotEmptyString() throws Exception {
        RefToRefTestData.RefThisChildRefTargetParamIsUninitialized data = new RefToRefTestData.RefThisChildRefTargetParamIsUninitialized(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext parentRefCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        TestEvalContext childRefCtx = erContext(parentRefCtx, data.currentCase, true, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.capital, data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx, parentRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }

    @Test
    public void refThisToRefThis_ChildRefIsOverlapped_ContextOfDefaultIsUsed() throws Exception {
        RefToRefTestData.ChildRefIsOverlapped data = new RefToRefTestData.ChildRefIsOverlapped(FACADE);

        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext parentRefCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        TestEvalContext childRefCtx = erContext(parentRefCtx, data.currentCase, true, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.capital, data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx, parentRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }

    @Test
    public void refThisToRefThis_ChildRefPointsToInitializedParameter_GotThreeStrictContexts() throws Exception {
        RefToRefTestData.RefThisChildRefTargetParamIsInitialized data = new RefToRefTestData.RefThisChildRefTargetParamIsInitialized(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext parentRefCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(parentRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        TestEvalContext childRefCtx = erContext(parentRefCtx, data.currentCase, true, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.brussels.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx, parentRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }

    @Test
    public void refThis_RefTargetIsOverlapped_ContextOfOverlapIsUsed() throws Exception {
        RefToRefTestData.RefTargetParamIsOverlapped data = new RefToRefTestData.RefTargetParamIsOverlapped(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        TestEvalContext childRefCtx = erContext(null, data.currentCase, true, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.capital, data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));

    }

    @Test
    public void refThisToRefThis_RefTargetIsOverlapped_ContextOfOverlapIsUsed() throws Exception {
        RefToRefTestData.ChildRefTargetParamIsOverlapped data = new RefToRefTestData.ChildRefTargetParamIsOverlapped(FACADE);
        OverlapItem.Reachable nextParam;
        TestEvalContext curCtx;

        nextParam = goTo(data.currentCase, data.parentRef.getAttribute());
        TestEvalContext caseCtx = rootErContext(data.currentCase);
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(caseCtx, curCtx);

        nextParam = goTo(data.currentCase, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        TestEvalContext childRefCtx = erContext(caseCtx, data.currentCase, true, data.childRef.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        nextParam = goTo(data.currentCase, data.brussels.getAttribute(), data.currentCaseIntoBelgium.getAttribute());
        curCtx = EVAL.next(nextParam);
        Assertions.assertEquals(childRefCtx, curCtx);

        Assertions.assertEquals(Lists.newArrayList(childRefCtx, caseCtx), Lists.newArrayList(childRefCtx.getStrictContexts()));
        Assertions.assertEquals(Lists.newArrayList(childRefCtx), Lists.newArrayList(childRefCtx.getNonStrictContexts()));
    }


    private static class TestEvalStrategy extends EvaluateDsParamStrategyImpl {
        private TestEvalContext last = null;

        public TestEvalStrategy(@Nonnull MacroRegistry registry,
                                @Nonnull AliasWrapperService wrapperService) {
            super(registry, wrapperService, NoCache.INSTANCE, false);
        }

        @Nonnull
        @Override
        protected EvalContextImpl createContext(@Nonnull DataSetList dsl, @Nonnull DataSet ds,
                                                @Nonnull List<Attribute> pathFromParent,
                                                @Nullable Attribute targetAttrFromParent) {
            if (last == null) {
                last = new TestEvalContext(null,
                        registry, wrapperService,
                        dsl, ds, true, pathFromParent, targetAttrFromParent);
            }
            return last;
        }

        public TestEvalContext next(OverlapItem.Reachable item) {
            apply(item);
            return TestEvalContext.leaf;
        }

        void reset() {
            last = null;
        }
    }

    private static class TestEvalContext extends EvalContextImpl {
        private static TestEvalContext leaf;

        public TestEvalContext(@Nullable EvalContextImpl parent,
                               @Nonnull MacroRegistry registry,
                               @Nonnull AliasWrapperService wrapperService,
                               @Nonnull DataSetList dsl,
                               @Nonnull DataSet ds,
                               boolean parentIsStrict,
                               @Nonnull List<Attribute> pathFromParent,
                               @Nullable Attribute targetAttrFromParent) {
            super(parent, registry, wrapperService, NoCache.INSTANCE, dsl, ds, parentIsStrict, pathFromParent, targetAttrFromParent);
        }

        @Nonnull
        @Override
        protected EvalContextImpl createChild(@Nonnull DataSetList dsl,
                                              @Nonnull DataSet ds,
                                              boolean parentIsStrict,
                                              @Nonnull List<Attribute> pathFromParent,
                                              @Nonnull Attribute targetAttrFromParent) {
            return new TestEvalContext(this, registry, wrapperService, dsl, ds, parentIsStrict,
                    pathFromParent, targetAttrFromParent);
        }

        @Nonnull
        @Override
        protected String evaluate(@Nonnull String inputText, @Nonnull MacroCacheKey cacheKey) throws CtxEvalException {
            leaf = this;
            return inputText;
        }
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.macros.impl.LookupMacrosTest;
import org.qubership.atp.dataset.macros.parser.ParsingStateTest;

public class AbstractMacroProcessorTest {

    private static void validate(String inputMacro, String overallEr, EvaluationStep... steps) throws EvalException {
        Iterator<EvaluationStep> er = Arrays.asList(steps).iterator();
        TestMacroProcessor p = new TestMacroProcessor(inputMacro) {
            @Override
            protected void validate(@Nonnull TestMacroContext context) throws CtxEvalException {
                if (!er.hasNext()) {
                    throw new IllegalStateException("\nGot unexpected iteration:\n"
                            + new EvaluationStep(context.macro, context.args.toString(), null));
                }
                EvaluationStep erItem = er.next();
                context.text.append(erItem.evaluate(context));
            }
        };
        String ar = p.evaluateAll();
        if (er.hasNext()) {
            String extra = StreamSupport.stream(Spliterators.spliteratorUnknownSize(er, Spliterator.ORDERED), false)
                    .map(Objects::toString)
                    .collect(Collectors.joining("\n"));
            throw new IllegalStateException("Additional iteration(s) expected:\n" + extra);
        }
        assertEquals(overallEr, ar, "Overall evaluation result is wrong");
    }

    @Test
    public void macroReturnsMacroPart_MacroUnionInsideAnotherMacro_MacroPartEvaluated() throws EvalException {
        String vlookupMacro = "#REF_DSL(InternationalRateCost.#REF_#REF_THIS(OriginCountry.Zone).#REF_THIS(DestinationCountry.Zone))";
        validate(vlookupMacro, "end",
                new EvaluationStep("REF_THIS", "OriginCountry.Zone", "DSL(Countries.EU.From)"),
                new EvaluationStep("REF_THIS", "DestinationCountry.Zone", "#REF_DSL(Countries.EU.To)"),
                new EvaluationStep("REF_DSL", "Countries.EU.From", "from"),
                new EvaluationStep("REF_DSL", "Countries.EU.To", "to"),
                new EvaluationStep("REF_DSL", "InternationalRateCost.from.to", "end"));
    }

    @Test
    public void macroReturnsAnotherMacro_MacroUnionInsideAnotherMacro_BothAreEvaluated() throws EvalException {
        String vlookupMacro = "#REF_DSL(InternationalRateCost.#REF_THIS(OriginCountry.Zone).#REF_THIS(DestinationCountry.Zone))";
        validate(vlookupMacro, "end",
                new EvaluationStep("REF_THIS", "OriginCountry.Zone", "#REF_DSL(Countries.EU.Any)"),
                new EvaluationStep("REF_THIS", "DestinationCountry.Zone", "to"),
                new EvaluationStep("REF_DSL", "Countries.EU.Any", "from"),
                new EvaluationStep("REF_DSL", "InternationalRateCost.from.to", "end"));
    }

    @Test
    public void macroReturnsAnotherMacro_MacroUnionWithDifferentArgsCountInsideAnotherMacro_BothAreEvaluated() throws EvalException {
        String vlookupMacro = "#REF_DSL(National Rates.#REF_THIS(Subscription.TariffName).#REF_THIS(UsageType))";
        validate(vlookupMacro, "0",
                new EvaluationStep("REF_THIS", "Subscription.TariffName", "Pro Contact"),
                new EvaluationStep("REF_THIS", "UsageType", "SMS"),
                new EvaluationStep("REF_DSL", "National Rates.Pro Contact.SMS", "0"));
    }

    @Test
    public void macroReturnsAnotherMacro_insideAnotherMacro_AllAreEvaluated() throws EvalException {
        String vlookupMacro = "#REF_DSL(#REF_THIS(OriginCountry.Zone))";
        validate(vlookupMacro, "end",
                new EvaluationStep("REF_THIS", "OriginCountry.Zone", "#REF_DSL(Countries.EU.Any)"),
                new EvaluationStep("REF_DSL", "Countries.EU.Any", "from"),
                new EvaluationStep("REF_DSL", "from", "end"));
    }

    @Test
    public void macroReturnsAnotherMacro_rootMacro_AllAreEvaluated() throws EvalException {
        String vlookupMacro = "#REF_THIS(OriginCountry.Zone)";
        validate(vlookupMacro, "end",
                new EvaluationStep("REF_THIS", "OriginCountry.Zone", "#REF_DSL(Countries.EU.Any)"),
                new EvaluationStep("REF_DSL", "Countries.EU.Any", "end"));
    }

    /**
     * You can find integration test in {@link LookupMacrosTest#vLookup_RefToRefWithOverlap_ResolvedProperly()}.
     */
    @Test
    public void macroReturnsAnotherMacro_complexRootMacro_AllAreEvaluated() throws EvalException {
        String macro = "#REF_DSL(Roaming SMS Rates.#REF_THIS(Account.AccountType).#REF_THIS(OriginCountry.Zone).#REF_THIS(DestinationCountry.Zone))";
        String secondMacro = "#REF_DSL(International Voice Rates.#REF_THIS(Subscription.TariffName).#REF_THIS(UsageType).#REF_THIS(DestinationCountry.IntenationalZone))";
        validate(macro, "0.33",
                new EvaluationStep("REF_THIS", "Account.AccountType", "B2B"),
                new EvaluationStep("REF_THIS", "OriginCountry.Zone", "Belgium"),
                new EvaluationStep("REF_THIS", "DestinationCountry.Zone", "Magreb"),
                new EvaluationStep("REF_DSL", "Roaming SMS Rates.B2B.Belgium.Magreb", secondMacro),
                new EvaluationStep("REF_THIS", "Subscription.TariffName", "Pro Contact"),
                new EvaluationStep("REF_THIS", "UsageType", "SMS"),
                new EvaluationStep("REF_THIS", "DestinationCountry.IntenationalZone", "World 2"),
                new EvaluationStep("REF_DSL", "International Voice Rates.Pro Contact.SMS.World 2", "0.33"));
    }

    @Test
    public void macroUnion_InsideAnotherMacro_ThreeEvaluationStepsOccurred() throws EvalException {
        String vlookupMacro = "#REF_DSL(InternationalRateCost.#REF_THIS(OriginCountry.Zone).#REF_THIS(DestinationCountry.Zone))";
        validate(vlookupMacro, "end",
                new EvaluationStep("REF_THIS", "OriginCountry.Zone", "1"),
                new EvaluationStep("REF_THIS", "DestinationCountry.Zone", "2"),
                new EvaluationStep("REF_DSL", "InternationalRateCost.1.2", "end"));
    }

    @Test
    public void noMacro_textWithSpecialSymbols_NoEvaluationStepsOccurred() throws EvalException {
        String macro = "#just_a text #";
        validate(macro, macro);
    }

    @Test
    public void singleMacro_MacroBetweenTextParts_OneEvaluationStepsOccurred() throws EvalException {
        String macro = "#just_a text#REF_DSL(1.2.3)#";
        validate(macro, "#just_a text #",
                new EvaluationStep("REF_DSL", "1.2.3", " "));
    }

    private abstract static class TestMacroProcessor extends AbstractMacroProcessor<TestMacroProcessor.TestMacroContext> {

        protected TestMacroProcessor(String inputMacro) {
            super(new StringBuilder(inputMacro), ParsingStateTest.MACRO_FINDER);
            state = new TestMacroContext();
        }

        protected abstract void validate(@Nonnull TestMacroContext context) throws CtxEvalException;

        @Nonnull
        @Override
        protected String getArguments(@Nonnull TestMacroContext context) {
            return context.args.toString();
        }

        @Nonnull
        @Override
        protected String getText(@Nonnull TestMacroContext context) {
            return context.text.toString();
        }

        @Override
        protected void dropArguments(@Nonnull TestMacroContext context) {
            context.args.setLength(0);
        }

        @Override
        protected void dropText(@Nonnull TestMacroContext testMacroContext) {
            testMacroContext.text.setLength(0);
        }

        @Override
        protected TestMacroContext createChildContext(@Nonnull TestMacroContext parent, @Nonnull String macro) {
            return new TestMacroContext(parent, macro);
        }

        public final class TestMacroContext extends MacroContextBase {

            private StringBuilder args;
            private StringBuilder text = new StringBuilder();
            private String macro;

            public TestMacroContext() {
                super();
            }

            public TestMacroContext(@Nonnull MacroContextBase parent, @Nonnull String macro) {
                super(parent, macro);
                initialize(macro);
            }

            @Override
            protected void pushArguments(@Nonnull String args) {
                this.args.append(args);
            }

            @Override
            public void initialize(@Nonnull String macro) {
                super.initialize(macro);
                this.macro = macro;
                this.args = new StringBuilder();
            }

            @Override
            protected void evaluate() throws CtxEvalException {
                getProcessor().validate(this);
            }

            @Override
            protected void pushText(@Nonnull String text) {
                this.text.append(text);
            }

            @Nonnull
            @Override
            public TestMacroProcessor getProcessor() {
                return TestMacroProcessor.this;
            }
        }
    }

    private static class EvaluationStep {

        public final String macroName;
        public final String macroArgs;
        public final String result;

        private EvaluationStep(String macroName, String macroArgs, String result) {
            this.macroName = macroName;
            this.macroArgs = macroArgs;
            this.result = result;
        }

        public String evaluate(TestMacroProcessor.TestMacroContext context) throws CtxEvalException {
            String macroName = context.macro;
            String macroArgs = context.args.toString();
            assertEquals(this,
                    new EvaluationStep(macroName, macroArgs, result), "Unexpected evaluation step");
            return result;
        }

        @Override
        public String toString() {
            return "#" + macroName + "(" + macroArgs + ")=" + result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EvaluationStep)) {
                return false;
            }
            EvaluationStep that = (EvaluationStep) o;
            return Objects.equals(macroName, that.macroName) &&
                    Objects.equals(macroArgs, that.macroArgs) &&
                    Objects.equals(result, that.result);
        }

        @Override
        public int hashCode() {
            return Objects.hash(macroName, macroArgs, result);
        }
    }
}

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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.Position;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.DotSeparatedArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArgFactory;
import org.qubership.atp.dataset.macros.args.MacroArgsFactory;
import org.qubership.atp.dataset.macros.args.SignatureArg;
import org.qubership.atp.dataset.macros.args.TextArg;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.macros.parser.ParsingStateTest;

public class RefAliasProcessorTest {

    public static final MacroRegistry MACRO_FINDER = new MacroRegistry() {
        @Override
        public boolean fullyEquals(String to) {
            return ParsingStateTest.MACRO_FINDER.fullyEquals(to);
        }

        @Override
        public boolean partiallyEquals(String to) {
            return ParsingStateTest.MACRO_FINDER.partiallyEquals(to);
        }

        @Nonnull
        @Override
        public Macros getMacros(@Nonnull String key) {
            return new Macros(key.toUpperCase()) {

                @Override
                public Object evaluate(Stream input, EvaluationContext context) throws Exception {
                    throw new UnsupportedOperationException("Mock method");
                }

                @Override
                public ArgsParser createArgsParser() {
                    return new DotSeparatedArgsParser(new MacroArgsFactory()) {
                        @Override
                        protected SignatureArg createArg(int index, @Nonnull MacroArgFactory args) throws Exception {
                            return args.text();
                        }
                    };
                }

                @Override
                public boolean doCache() {
                    return false;
                }
            };
        }
    };

    private static void validate(String inputMacro, TrackedStep... steps) throws EvalException {
        Iterator<TrackedStep> er = Arrays.asList(steps).iterator();
        RefAliasProcessor p = new RefAliasProcessor(MACRO_FINDER, inputMacro) {
            @Override
            protected void notifyRefsFound(RefAliasContext context, ArgsParser.Result args) throws CtxEvalException {
                if (!er.hasNext()) {
                    throw new IllegalStateException("\nGot unexpected iteration:\n"
                            + new TrackedStep(context.macros.getDefinition(), args));
                }
                TrackedStep erItem = er.next();
                erItem.validate(context.macros.getDefinition(), args, context.offset);
            }
        };
        p.evaluateAll();
        if (er.hasNext()) {
            String extra = StreamSupport.stream(Spliterators.spliteratorUnknownSize(er, Spliterator.ORDERED), false)
                    .map(Objects::toString)
                    .collect(Collectors.joining("\n"));
            throw new IllegalStateException("Additional iteration(s) expected:\n" + extra);
        }
    }

    private static SignatureArg text(String text, int start, int end) {
        return TextArg.of(new Position(start, end), text);
    }

    @Test
    public void macroArgs_MacroUnionInsideAnotherMacro_BestEffortMacroParsing() throws EvalException {
        String vlookupMacro = "#REF_DSL(InternationalRateCost.#REF_#REF_THIS(OriginCountry.Zone).#REF_THIS(DestinationCountry.Zone))";
        validate(vlookupMacro,
                new TrackedStep("REF_THIS", "OriginCountry.Zone",
                        "", text("OriginCountry", 46, 59), text("Zone", 60, 64)),
                new TrackedStep("REF_THIS", "DestinationCountry.Zone",
                        "", text("DestinationCountry", 76, 94), text("Zone", 95, 99)),
                new TrackedStep("REF_DSL", "InternationalRateCost.#REF_",
                        "#REF_", text("InternationalRateCost", 9, 30)));
    }

    @Test
    public void macroArgs_MacroInTheMiddleOfAnotherMacro_BestEffortMacroParsing() throws EvalException {
        String vlookupMacro = "#REF_DSL(InternationalRateCost.#REF_#REF_THIS(OriginCountry.Zone).target)";
        validate(vlookupMacro,
                new TrackedStep("REF_THIS", "OriginCountry.Zone",
                        "", text("OriginCountry", 46, 59), text("Zone", 60, 64)),
                new TrackedStep("REF_DSL", "InternationalRateCost.#REF_",
                        "#REF_", text("InternationalRateCost", 9, 30)));
    }

    @Test
    public void macroArgs_ContainsBrackets_Parsed() throws Exception {
        String vlookupMacro = "#REF_DSL(InternationalRateCost(brackets).#REF_#REF_THIS(OriginCountry(brackets).Zone(brackets)).target(brackets))";
        validate(vlookupMacro,
                new TrackedStep("REF_THIS", "OriginCountry(brackets).Zone(brackets)",
                        "", text("OriginCountry(brackets)", 56, 79), text("Zone(brackets)", 80, 94)),
                new TrackedStep("REF_DSL", "InternationalRateCost(brackets).#REF_",
                        "#REF_", text("InternationalRateCost(brackets)", 9, 40)));
    }

    @Test
    public void singleMacro_MacroBetweenTextParts_BestEffortMacroParsing() throws EvalException {
        String macro = "#just_a text#REF_DSL(1.2.3)#";
        validate(macro,
                new TrackedStep("REF_DSL", "1.2.3", "",
                        text("1", 21, 22),
                        text("2", 23, 24),
                        text("3", 25, 26)));
    }

    private static class TrackedStep {

        private final String macroName;
        private final String macroArgs;
        private final String unparsed;
        private final List<SignatureArg> parsed;

        private TrackedStep(String macroName, ArgsParser.Result result) {
            this(macroName, result.toString(), result.getUnparsed().orElse(""), result.getParsed());
        }

        private TrackedStep(String macroName, String macroArgs, String unparsed, SignatureArg... parsed) {
            this(macroName, macroArgs, unparsed, Arrays.asList(parsed));
        }

        private TrackedStep(String macroName, String macroArgs, String unparsed, List<SignatureArg> parsed) {
            this.macroName = macroName;
            this.macroArgs = macroArgs;
            this.unparsed = unparsed;
            this.parsed = parsed;
        }

        public void validate(String macroName, ArgsParser.Result result, int argsEndPosition) {
            Optional<Exception> error = result.getError();
            if (error.isPresent()) {
                throw new RuntimeException("Step evaluated with error: " + new TrackedStep(macroName, result), error.get());
            }
            //adjust offset to attributes start
            int argsStartPosition = argsEndPosition - result.toString().length();
            for (SignatureArg macroArg : result.getParsed()) {
                Position position = macroArg.getSignature().getPosition();
                //adjust position from attributes start to position from macro start
                position.setStart(position.getStart() + argsStartPosition);
                position.setEnd(position.getEnd() + argsStartPosition);
            }
            Assertions.assertEquals(this,
                    new TrackedStep(macroName, result), "Unexpected step");
        }

        @Override
        public String toString() {
            return "#" + macroName + "(" + macroArgs + ") -> #" + macroName + "(parsed:"
                    + parsed.stream()
                    .map(arg -> arg.getText() + arg.asSignature()
                            .map(sig -> sig.getSignature().getPosition().toString())
                            .orElse("null"))
                    .collect(Collectors.joining("|"))
                    + " unparsed:" + unparsed + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TrackedStep)) {
                return false;
            }
            TrackedStep that = (TrackedStep) o;
            return Objects.equals(macroName, that.macroName) &&
                    Objects.equals(macroArgs, that.macroArgs) &&
                    Objects.equals(unparsed, that.unparsed) &&
                    Objects.equals(parsed, that.parsed);
        }

        @Override
        public int hashCode() {
            return Objects.hash(macroName, macroArgs, unparsed, parsed);
        }
    }
}

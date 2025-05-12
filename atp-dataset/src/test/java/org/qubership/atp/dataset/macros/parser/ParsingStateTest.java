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

package org.qubership.atp.dataset.macros.parser;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.Macros;

public class ParsingStateTest {

    public static final Set<String> MACROSES = ImmutableSet.of("REF_DSL", "SUM", "REF_ALIAS", "REF_THIS");
    public static final MacroRegistry MACRO_FINDER = new MacroRegistry() {
        @Override
        public boolean fullyEquals(String to) {
            to = to.toUpperCase();
            for (String macro : MACROSES) {
                if (to.equals(macro)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean partiallyEquals(String to) {
            to = to.toUpperCase();
            for (String macro : MACROSES) {
                if (macro.startsWith(to)) {
                    return true;
                }
            }
            return false;
        }

        @Nonnull
        @Override
        public Macros getMacros(@Nonnull String key) {
            throw new UnsupportedOperationException("Mock method");
        }
    };

    public static void validate(String inputMacro, Map.Entry<TokenType, String>... entries) {
        ValidateState testFacade = new ValidateState(inputMacro, Arrays.asList(entries).iterator());
        testFacade.validate();
    }

    @Test
    public void hasNestedMacro_NoExtraArgs_ContainsOnlyFormulas() throws Exception {
        validate("#REF_DSL(#sum(1,2,3))",
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_START, "sum"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1,2,3"),
                new ImmutablePair<>(TokenType.FORMULA_END, ""));
    }

    @Test
    public void text_EndedWithSharp_IsProbablyFormula() throws Exception {
        validate("123#",
                new ImmutablePair<>(TokenType.PROBABLY_FORMULA, "123#"));
    }

    @Test
    public void text_WithSharpInTheMiddle_IsText() throws Exception {
        validate("123#456",
                new ImmutablePair<>(TokenType.TEXT_PART, "123#456"));
    }

    @Test
    public void hasNestedMacro_HasExtraArgs_ContainsTextPart() throws Exception {
        validate("#REF_DSL(1,#sum(1,#2,#),3)",
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.TEXT_PART, "1,"),
                new ImmutablePair<>(TokenType.FORMULA_START, "sum"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1,#2,#"),
                new ImmutablePair<>(TokenType.FORMULA_END, ",3"));
    }

    @Test
    public void hasTwoNestedMacro_HasExtraArgs_ContainsTextParts() throws Exception {
        validate("#REF_DSL(1,#sum(1,#2,#),3#sum(1,#2,#))",
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.TEXT_PART, "1,"),
                new ImmutablePair<>(TokenType.FORMULA_START, "sum"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1,#2,#"),
                new ImmutablePair<>(TokenType.TEXT_PART, ",3"),
                new ImmutablePair<>(TokenType.FORMULA_START, "sum"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1,#2,#"),
                new ImmutablePair<>(TokenType.FORMULA_END, ""));
    }

    @Test
    public void macroUnion_NoSeparator_ContainsOnlyFormulas() throws Exception {
        validate("#REF_DSL(1.2.3)#REF_DSL(4.5.6)",
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1.2.3"),
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "4.5.6"));
    }

    @Test
    public void macroUnion_HasSeparators_ContainsTextParts() throws Exception {
        validate("#REF_DSL(1.2.3)##REF_DSL(4.5.6)#",
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1.2.3"),
                new ImmutablePair<>(TokenType.TEXT_PART, "#"),
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "4.5.6"),
                new ImmutablePair<>(TokenType.PROBABLY_FORMULA, "#"));
    }

    @Test
    public void macro_SurroundedWithText_EndsWithTextPart() throws Exception {
        validate("first.#REF_DSL(1.2.3).third",
                new ImmutablePair<>(TokenType.TEXT_PART, "first."),
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1.2.3"),
                new ImmutablePair<>(TokenType.TEXT_PART, ".third"));
    }

    private static class ValidateState {

        private final Iterator<Map.Entry<TokenType, String>> er;
        private final StringBuilder toParse;
        StringBuilder parsed = new StringBuilder();
        ParsingState parsingState = new ParsingState(MACRO_FINDER) {
            @Override
            protected void pushToken(Strategy entry) {
                super.pushToken(entry);
                iterate(entry);
            }
        };

        private ValidateState(String inputMacro, Iterator<Map.Entry<TokenType, String>> er) {
            toParse = new StringBuilder(inputMacro);
            this.er = er;
        }

        public void validate() {
            while (toParse.length() != 0) {
                char c = toParse.charAt(0);
                toParse.deleteCharAt(0);
                parsed.append(c);
                parsingState.read(c);
            }
            if (!parsingState.currentStrategy.consumed) {
                iterate(parsingState.currentStrategy);
            }
            if (er.hasNext()) {
                String extra = StreamSupport.stream(Spliterators.spliteratorUnknownSize(er, Spliterator.ORDERED), false)
                        .map(Objects::toString)
                        .collect(Collectors.joining("\n"));
                throw new IllegalStateException("Additional iteration(s) expected:\n" + extra);
            }
        }

        private void iterate(Strategy entry) {
            String currentIteration = parsed + " | " + toParse + "   " + entry.getKey() + "   " + entry.getValue();
            if (!er.hasNext()) {
                throw new IllegalStateException("\nGot unexpected iteration:\n" + currentIteration);
            }
            Map.Entry<TokenType, String> expectedEntry = er.next();
            Assertions.assertEquals(expectedEntry, entry, "\nFailed iteration:\n" + currentIteration);
            System.out.println(currentIteration);
        }
    }
}

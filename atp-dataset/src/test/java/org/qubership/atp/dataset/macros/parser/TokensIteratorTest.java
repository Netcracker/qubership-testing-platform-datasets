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
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokensIteratorTest {

    private static void validate(String inputMacro, Map.Entry<TokenType, String>... entries) {
        TokensIterator iterator = new TokensIterator(ParsingStateTest.MACRO_FINDER, new StringBuilder(inputMacro));
        List<Map.Entry<TokenType, String>> er = Arrays.asList(entries);
        List<Map.Entry<TokenType, String>> ar = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        Assertions.assertEquals(er, ar);
    }

    private static void testUnionBack(String inputMacro) {
        TokensIterator iterator = new TokensIterator(ParsingStateTest.MACRO_FINDER, new StringBuilder(inputMacro));
        String ar = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .map(Token::getSource)
                .collect(Collectors.joining());
        Assertions.assertEquals(inputMacro, ar);
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
    public void hasTextWithBraces_insideAMacro_ParsedBracesAsText() throws Exception {
        validate("#REF_DSL(Standard(24h))",
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "Standard(24h)"));
    }

    @Test
    public void hasTextWithBraces_aroundMacro_ParsedBracesAsText() throws Exception {
        validate("Standard(#REF_DSL(Standard(24h)))",
                new ImmutablePair<>(TokenType.TEXT_PART, "Standard("),
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "Standard(24h)"),
                new ImmutablePair<>(TokenType.TEXT_PART, ")"));
    }

    @Test
    public void hasTextWithBraces_doubleInsideAMacro_ParsedBracesAsText() throws Exception {
        validate("#REF_DSL(Standard(24h),((24h)))",
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "Standard(24h),((24h))"));
    }

    @Test
    public void hasTextWithBraces_aroundDoubleMacro_ParsedBracesAsText() throws Exception {
        validate("Standard(#REF_DSL(Standard(24h)),#REF_DSL(Standard(24h)))",
                new ImmutablePair<>(TokenType.TEXT_PART, "Standard("),
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "Standard(24h)"),
                new ImmutablePair<>(TokenType.TEXT_PART, ","),
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "Standard(24h)"),
                new ImmutablePair<>(TokenType.TEXT_PART, ")"));
    }

    @Test
    public void text_EndedWithSharp_IsProbablyFormula() throws Exception {
        validate("123#",
                new ImmutablePair<>(TokenType.PROBABLY_FORMULA, "123#"));
    }

    @Test
    public void macro_SurroundedWithSharp_EndsWithProbablyFormula() throws Exception {
        validate("##REF_DSL(1.2.3)#",
                new ImmutablePair<>(TokenType.TEXT_PART, "#"),
                new ImmutablePair<>(TokenType.FORMULA_START, "REF_DSL"),
                new ImmutablePair<>(TokenType.FORMULA_END, "1.2.3"),
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

    @Test
    public void hasNestedMacro_NoExtraArgs_UnitedCorrectly() throws Exception {
        testUnionBack("#REF_DSL(#sum(1,2,3))");
    }

    @Test
    public void text_EndedWithSharp_UnitedCorrectly() throws Exception {
        testUnionBack("123#");
    }

    @Test
    public void macro_SurroundedWithSharp_UnitedCorrectly() throws Exception {
        testUnionBack("##REF_DSL(1.2.3)#");
    }

    @Test
    public void macro_SurroundedWithText_UnitedCorrectly() throws Exception {
        testUnionBack("first.#REF_DSL(1.2.3).third");
    }
}

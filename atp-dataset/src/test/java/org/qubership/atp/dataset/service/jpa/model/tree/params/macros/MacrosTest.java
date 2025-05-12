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

package org.qubership.atp.dataset.service.jpa.model.tree.params.macros;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;

@Isolated
public class MacrosTest extends AbstractMacroTest {

    @Test
    public void TestMacro_InnMacro_result() {
        String parameterValue = "#INN()";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Pattern pattern = Pattern.compile("[0-9]{10}");
        Assertions.assertTrue(pattern.matcher(result.toString()).matches());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_RefThisMacro_result() {
        String parameterValue = "#REF_THIS(dsl_reference.attribute_id)";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(MACRO_RESULT, result.toString());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_RefMacro_result() {
        String parameterValue = "#REF(dsl_reference.attribute_id)";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(MACRO_RESULT, result.toString());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_RefDslMacro_result() {
        String parameterValue = "#REF_DSL(dsl_id.dsl_reference.attribute_id)";
        List<AbstractTextParameter> parseEvaluatedResult = parser.parse(parameterValue, true);
        StringBuilder evaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseEvaluatedResult) {
            evaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(MACRO_RESULT, evaluatedResult.toString());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_UuidMacro_result() {
        String parameterValue = "#UUID()";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertTrue(Utils.isUuid(result.toString()));

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_UnknownMacro_result() {
        String parameterValue = "#DUNNO()";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, result.toString());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_CharsMacro_result() {
        String parameterValue = "#CHARS(10)";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Pattern pattern = Pattern.compile("[a-z]{10}");
        Assertions.assertTrue(pattern.matcher(result.toString()).matches());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_CharsUpperMacro_result() {
        String parameterValue = "#CHARS_UPPERCASE(10)";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Pattern pattern = Pattern.compile("[A-Z]{10}");
        Assertions.assertTrue(pattern.matcher(result.toString()).matches());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_RandomBetween_result() {
        String parameterValue = "#RANDBETWEEN(100,1000)";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, result.toString());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }

    @Test
    public void TestMacro_Rand_result() {
        String parameterValue = "#RAND('5')";
        List<AbstractTextParameter> parseResult = parser.parse(parameterValue, true);
        StringBuilder result = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseResult) {
            result.append(abstractTextParameter.getValue());
        }
        Pattern pattern = Pattern.compile("[0-9]{5}");
        Assertions.assertTrue(pattern.matcher(result.toString()).matches());

        List<AbstractTextParameter> parseUnevaluatedResult = parser.parse(parameterValue, false);
        StringBuilder unevaluatedResult = new StringBuilder();
        for (AbstractTextParameter abstractTextParameter : parseUnevaluatedResult) {
            unevaluatedResult.append(abstractTextParameter.getValue());
        }
        Assertions.assertEquals(parameterValue, unevaluatedResult.toString());
    }
}

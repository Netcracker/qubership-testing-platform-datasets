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

import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getCharsMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getCharsUpperCaseMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getContextMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getCustomMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getDateMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getInnMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getRandomBetweenMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getShiftDayMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getSumMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getTrNameMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getUuidMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.TestUtils.getUuidUpperCaseMacros;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isChars;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isCharsUpperCase;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isDate;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isInn;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDate;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isNaN;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isUuid;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isUuidUpperCase;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.math.NumberUtils.isParsable;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.TextParameter;
import org.qubership.atp.macros.core.calculator.ScriptMacrosCalculator;
import org.qubership.atp.macros.core.model.Macros;

@Isolated
@ExtendWith(SpringExtension.class)
public class AtpMacroTest {

    private ParameterPositionContext parameterPositionContext;
    private MacroContext macroContext;
    private Macros uuidMacros;
    private Macros customMacros;
    private Macros contextMacros;
    private Macros innMacros;
    private Macros uuidUpperCaseMacros;
    private Macros randomBetweenMacros;
    private Macros dateMacros;
    private Macros charsMacros;
    private Macros charsUpperCaseMacros;
    private Macros sumMacros;
    private Macros trNameMacros;
    private Macros shiftDayMacros;

    @BeforeEach
    public void setUp() {
        macroContext = new MacroContext();
        uuidMacros = getUuidMacros();
        customMacros = getCustomMacros();
        contextMacros = getContextMacros();
        innMacros = getInnMacros();
        uuidUpperCaseMacros = getUuidUpperCaseMacros();
        randomBetweenMacros = getRandomBetweenMacros();
        dateMacros = getDateMacros();
        charsMacros = getCharsMacros();
        charsUpperCaseMacros = getCharsUpperCaseMacros();
        sumMacros = getSumMacros();
        trNameMacros = getTrNameMacros();
        shiftDayMacros = getShiftDayMacros();
        List<Macros> macrosList = asList(uuidMacros, customMacros, contextMacros, innMacros, uuidUpperCaseMacros,
                randomBetweenMacros, dateMacros, charsMacros, charsUpperCaseMacros, sumMacros, trNameMacros,
                shiftDayMacros);
        macroContext.setMacros(macrosList);
        macroContext.setMacrosCalculator(new ScriptMacrosCalculator(new ScriptEngineManager()));
        parameterPositionContext = new ParameterPositionContext(Collections.emptyList(), 0, null, 0L, null);
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnUuidValue_whenMacrosIsUuid() {
        AbstractMacro macro = new AtpMacro(uuidMacros.getName(), macroContext, parameterPositionContext, uuidMacros);
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isUuid());
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnCustomValue_whenMacrosIsCustom() {
        AbstractMacro macro =
            new AtpMacro(customMacros.getName(), macroContext, parameterPositionContext, customMacros);
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, is("CUSTOM_VALUE"));
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnContextValue_whenMacrosIsContext() {
        macroContext.addAtpDataSetContext("{'VAR1': '12345'}");
        AbstractMacro macro =
            new AtpMacro(contextMacros.getName(), macroContext, parameterPositionContext, contextMacros);
        macro.addTextParameter(new TextParameter("VAR1", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, is("12345"));
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnUnknownValue_whenMacrosIsContext() {
        macroContext.addAtpDataSetContext("{'VAR1': '12345'}");
        AbstractMacro macro =
            new AtpMacro(contextMacros.getName(), macroContext, parameterPositionContext, contextMacros);
        macro.addTextParameter(new TextParameter("VAR2", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, is("Unknown VAR2"));
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnInnValue_whenMacrosIsInn() {
        AbstractMacro macro = new AtpMacro(innMacros.getName(), macroContext, parameterPositionContext, innMacros);
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isInn());
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnUuidUpperCaseValue_whenMacrosIsUuidUpperCase() {
        AbstractMacro macro =
            new AtpMacro(uuidUpperCaseMacros.getName(), macroContext, parameterPositionContext, uuidUpperCaseMacros);
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isUuidUpperCase());
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnRandomBetweenValue_whenMacrosIsRandomBetween() {
        AbstractMacro macro =
            new AtpMacro(randomBetweenMacros.getName(), macroContext, parameterPositionContext, randomBetweenMacros);
        macro.addTextParameter(new TextParameter("11", parameterPositionContext));
        macro.addTextParameter(new TextParameter("20", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertTrue(isParsable(evaluatedValue));
        assertThat(Integer.parseInt(evaluatedValue),
                is(both(greaterThanOrEqualTo(11)).and(lessThanOrEqualTo(20))));
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnNaNValue_whenMacrosIsRandomBetween() {
        AbstractMacro macro =
            new AtpMacro(randomBetweenMacros.getName(), macroContext, parameterPositionContext, randomBetweenMacros);
        macro.addTextParameter(new TextParameter("abc", parameterPositionContext));
        macro.addTextParameter(new TextParameter("def", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isNaN());
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnDateValue_whenMacrosIsDate() {
        AbstractMacro macro = new AtpMacro(dateMacros.getName(), macroContext, parameterPositionContext, dateMacros);
        macro.addTextParameter(new TextParameter("yyyy-MM-dd\\'T\\'HH:mm:ss.SSSZ", parameterPositionContext));
        macro.addTextParameter(new TextParameter("GMT+2", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isIsoDate());
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnCharsValue_whenMacrosIsChars() {
        AbstractMacro macro = new AtpMacro(charsMacros.getName(), macroContext, parameterPositionContext, charsMacros);
        macro.addTextParameter(new TextParameter("200", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isChars(200));
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnCharsUpperCaseValue_whenMacrosIsCharsUpperCase() {
        AbstractMacro macro = new AtpMacro(
            charsUpperCaseMacros.getName(), macroContext, parameterPositionContext, charsUpperCaseMacros);
        macro.addTextParameter(new TextParameter("200", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isCharsUpperCase(200));
    }

    @Test
    public void testGetEvaluatedValue_shouldThrowAnException_whenMacrosIsSum() {
        AbstractMacro macro = new AtpMacro(sumMacros.getName(), macroContext, parameterPositionContext, sumMacros);
        macro.addTextParameter(new TextParameter("abc", parameterPositionContext));
        macro.addTextParameter(new TextParameter("123", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isNaN());
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnSumValue_whenMacrosIsSum() {
        AbstractMacro macro = new AtpMacro(sumMacros.getName(), macroContext, parameterPositionContext, sumMacros);
        macro.addTextParameter(new TextParameter("11", parameterPositionContext));
        macro.addTextParameter(new TextParameter("20", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertTrue(isParsable(evaluatedValue));
    }

    @Test
    public void testGetEvaluatedValue_whenTestRunNameUnknown_shouldReturnUnevaluatedValue() {
        AbstractMacro macro = new AtpMacro(trNameMacros.getName(), macroContext, parameterPositionContext, trNameMacros);
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        Assertions.assertEquals(macro.getUnevaluatedValue(""), evaluatedValue);
    }

    @Test
    public void testGetEvaluatedValue_shouldReturnShiftDayValue_whenMacrosIsShiftDay() {
        AbstractMacro macro =
            new AtpMacro(shiftDayMacros.getName(), macroContext, parameterPositionContext, shiftDayMacros);
        macro.addTextParameter(new TextParameter("57", parameterPositionContext));
        macro.addTextParameter(new TextParameter("05.11.2020", parameterPositionContext));
        macro.addTextParameter(new TextParameter("dd.MM.yyyy", parameterPositionContext));
        String evaluatedValue = macro.getEvaluatedValue(macro.getArguments());
        assertThat(evaluatedValue, isDate());
    }
}

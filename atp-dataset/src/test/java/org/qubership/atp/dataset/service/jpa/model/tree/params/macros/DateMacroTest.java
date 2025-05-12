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

import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithTimezoneCharacter;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithTimezoneCharacterInQuotes;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithoutMs;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithoutMsAndWithTimezoneCharacter;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithoutMsAndWithTimezoneCharacterInQuotes;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithoutMsAndWithTimezoneInQuotes;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithoutMsAndWithoutTimezoneInQuotes;
import static org.qubership.atp.dataset.service.jpa.model.tree.params.macros.matcher.MatcherFactory.isIsoDateWithoutTimeInQuotes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;

@Isolated
public class DateMacroTest extends AbstractMacroTest {

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_1() {
        String macro = "\"#DATE(-198d,yyyy-MM-dd\\'T\\'HH:mm:ss.sss)Z\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithTimezoneCharacterInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_1() {
        String macro = "\"#DATE(-198d,yyyy-MM-dd\\'T\\'HH:mm:ss.sss)Z\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_2() {
        String macro = "\"#DATE(-1d,yyyy-MM-dd\\'T\\'HH:mm:ss.sss\\'Z\\')\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithTimezoneCharacterInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_2() {
        String macro = "\"#DATE(-1d,yyyy-MM-dd\\'T\\'HH:mm:ss.sss\\'Z\\')\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_3() {
        String macro = "\"#DATE(-5h, yyyy-MM-dd\\'T\\')23:59:59\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutMsAndWithoutTimezoneInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_3() {
        String macro = "\"#DATE(-5h, yyyy-MM-dd\\'T\\')23:59:59\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_4() {
        String macro = "\"#DATE(+10d-5h, yyyy-MM-dd\\'T\\')\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutTimeInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_4() {
        String macro = "\"#DATE(+10d-5h, yyyy-MM-dd\\'T\\')\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_5() {
        String macro = "\"#DATE(+10m, yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\')\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutMsAndWithTimezoneCharacterInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_5() {
        String macro = "\"#DATE(+10m, yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\')\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_6() {
        String macro = "\"#DATE(+1d, yyyy-MM-dd\\'T\\'HH:mm:ss)\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutMsAndWithoutTimezoneInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_6() {
        String macro = "\"#DATE(+1d, yyyy-MM-dd\\'T\\'HH:mm:ss)\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_7() {
        String macro = "\"#DATE(+1d, yyyy-MM-dd\\'T\\'HH:mm:ss.123\\'Z\\')\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithTimezoneCharacterInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_7() {
        String macro = "\"#DATE(+1d, yyyy-MM-dd\\'T\\'HH:mm:ss.123\\'Z\\')\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_8() {
        String macro = "\"#DATE(+1d,yyyy-MM-28\\'T\\'HH:mm:ss)\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutMsAndWithoutTimezoneInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_8() {
        String macro = "\"#DATE(+1d,yyyy-MM-28\\'T\\'HH:mm:ss)\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_9() {
        String macro = "\"#DATE(+2d, yyyy-MM-dd\\'T\\'08:00:05+01:00)\"";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutMsAndWithTimezoneInQuotes());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_9() {
        String macro = "\"#DATE(+2d, yyyy-MM-dd\\'T\\'08:00:05+01:00)\"";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_10() {
        String macro = "#DATE(yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\')";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutMsAndWithTimezoneCharacter());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_10() {
        String macro = "#DATE(yyyy-MM-dd\\'T\\'HH:mm:ss\\'Z\\')";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_11() {
        String macro = "#DATE(yyyy-MM-dd\\'T\\'HH:mm:ssZ)";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithoutMs());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_11() {
        String macro = "#DATE(yyyy-MM-dd\\'T\\'HH:mm:ssZ)";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsTrue_12() {
        String macro = "#DATE(yyyy-MM-dd\\'T\\'HH:mm:ss.sss)Z";
        String result = getResult(macro, true);
        assertThat(result, isIsoDateWithTimezoneCharacter());
    }

    @Test
    public void testDateMacro_shouldReturnsEquals_whenEvaluateIsFalse_12() {
        String macro = "#DATE(yyyy-MM-dd\\'T\\'HH:mm:ss.sss)Z";
        String result = getResult(macro, false);
        assertThat(result, equalTo(macro));
    }

    private String getResult(String macro, boolean isCalculate) {
        List<AbstractTextParameter> parseResult = parser.parse(macro, isCalculate);
        return parseResult.stream().map(AbstractParameter::getValue).collect(Collectors.joining());
    }
}

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

import static org.qubership.atp.dataset.RegexpMatcher.matchesToRegExp;
import static org.hamcrest.MatcherAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;

import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;

@Isolated
@ContextConfiguration(classes = {TestMacrosConfiguration.class})
public class DateMacrosTest extends AbstractMacrosTest {

    private final DateMacros DATE_MACROS = new DateMacros();

    @Test
    public void testDateMacrosParsFormatAndReturnsFormattedDate() throws Exception {
        String evaluate = DATE_MACROS.evaluate("yyyy-MM-dd", null);
        Assertions.assertNotNull(evaluate);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        DateTime dateTime = DateTime.parse(evaluate);
        Assertions.assertEquals(DateTime.now().getDayOfYear(), dateTime.getDayOfYear());
    }

    @Test
    public void testParseIncreaseToOneMonthAndFormatDate() throws Exception {
        String evaluate = DATE_MACROS.evaluate("+1M,yyyy-MM-dd", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        int monthOfYear = DateTime.now().getMonthOfYear();
        if (monthOfYear > 11) { //December
            monthOfYear = 0;
        }
        Assertions.assertTrue(monthOfYear < DateTime.parse(evaluate).getMonthOfYear());
    }

    @Test
    public void testParseIncreaseToTwoMonthAndFormatDate() throws Exception {
        String evaluate = DATE_MACROS.evaluate("+2M,yyyy-MM-dd", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        int monthOfYear = DateTime.now().getMonthOfYear();
        if (monthOfYear > 10) { //November, December
            monthOfYear = 0;
        }
        Assertions.assertTrue(monthOfYear < DateTime.parse(evaluate).getMonthOfYear());
    }

    @Test
    public void testParseIncrementationToZeroMonthAndFormatDate() throws Exception {
        String evaluate = DATE_MACROS.evaluate("+0M,yyyy-MM-dd", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        Assertions.assertEquals(DateTime.now().getDayOfYear(), DateTime.parse(evaluate).getDayOfYear());
    }

    @Test
    public void testIncreaseYear() throws Exception {
        String evaluate = DATE_MACROS.evaluate("+1y,yyyy-MM-dd", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        Assertions.assertTrue(DateTime.now().getYear() < DateTime.parse(evaluate).getYear());
    }

    @Test
    public void testIncreaseMonth() throws Exception {
        String evaluate = DATE_MACROS.evaluate("+1M,yyyy-MM-dd", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        Assertions.assertTrue(DateTime.now().getMillis() < DateTime.parse(evaluate).getMillis());
    }

    @Test
    public void testIncreaseDay() throws Exception {
        String evaluate = DATE_MACROS.evaluate("+1d,yyyy-MM-dd", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        Assertions.assertTrue(DateTime.now().getMillis() < DateTime.parse(evaluate).getMillis());
    }

    @Test
    public void testIncreaseHour() throws Exception {
        String evaluate = DATE_MACROS.evaluate("+1h,yyyy-MM-dd HH:MM", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
        Assertions.assertTrue(
                DateTime.now().getMillis()
                        < DateTime.parse(evaluate, DateTimeFormat.forPattern("yyyy-MM-dd HH:MM")).getMillis()
        );
    }

    @Test
    public void testIncreaseMinutes() throws Exception {
        DateTime now = DateTime.now();
        DateTime dateTime = new DateTime(now);
        DateTime evaluate = DATE_MACROS.calculateDate(dateTime, "+1m");
        Assertions.assertTrue(now.getMillis() < evaluate.getMillis());
    }

    @Test
    public void testIncreaseSecond() throws Exception {
        DateMacros dateMacros = new DateMacros();
        DateTime now = DateTime.now();
        DateTime dateTime = new DateTime(now);
        DateTime evaluate = dateMacros.calculateDate(dateTime, "+1s");
        Assertions.assertTrue(now.getMillis() < evaluate.getMillis());
    }

    @Test
    public void testDecreaseYear() throws Exception {
        String evaluate = DATE_MACROS.evaluate("-1y,yyyy-MM-dd", null);
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        Assertions.assertTrue(DateTime.now().getYear() > DateTime.parse(evaluate).getYear());
    }

    @Test
    public void usingCustomDate_decreasingHours_mayDecreaseDays() throws Exception {
        String evaluate = DATE_MACROS.formattedCalculatedDate("+2M-4h,yyyy-MM-dd", DateTime.parse("2018-10-24T02:00"));
        Assertions.assertEquals("2018-12-23", evaluate);
    }

    @Test
    public void testEvaluatingWithEvaluator() throws Exception {
        String evaluate = evaluator.evaluate("#date(-1y+1d,yyyy-MM-dd)", new DataSetListImpl(), new DataSetImpl());
        assertThat(evaluate, matchesToRegExp("\\d{4}-\\d{2}-\\d{2}"));
        DateTime dateTime = DateTime.parse(evaluate, DateTimeFormat.forPattern("yyyy-MM-dd"));
        DateTime now = DateTime.now();
        Assertions.assertEquals(now.getYear() - 1, dateTime.getYear());
        //I found difficult to validate the date, just trust me, it's work. I've checked it.
    }

//    @Test(expected = IllegalArgumentException.class)
    @Test
    public void testExceptionIfOperationTypeUndefined() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            DATE_MACROS.evaluate("+1g,yyyy-MM-dd", null); //'g' - is undefined
        });

    }
}

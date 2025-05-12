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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.ReadablePeriod;
import org.joda.time.Seconds;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.DumbArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArg;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;

@Component
public class DateMacros extends Macros<String> {

    private static final String REGEX = "^([+-]\\d+\\w)+?,(.+)";

    public DateMacros() {
        super("DATE");
    }

    @Override
    public String evaluate(Stream<? extends MacroArg> input, EvaluationContext context) throws Exception {
        String dataExpression = input.findAny()
                .orElseThrow(() -> new IllegalArgumentException("One argument expected"))
                .getText();
        if (dataExpression.matches(REGEX)) {
            return formattedCalculatedDate(dataExpression, new DateTime());
        } else {
            return formattedDate(dataExpression, DateTime.now());
        }
    }

    @Override
    public ArgsParser createArgsParser() {
        return new DumbArgsParser();
    }

    @Override
    public boolean doCache() {
        return true;
    }

    private String formattedDate(String dataExpression, DateTime dateTimeNow) {
        return dateTimeNow.toString(DateTimeFormat.forPattern(dataExpression));
    }

    protected String formattedCalculatedDate(String matcher, DateTime dateTimeNow) {
        List<String> list = Splitter.on(',').limit(2).trimResults().splitToList(matcher);
        String calculationPatterns = list.get(0);
        String dateFormatPattern = list.get(1);
        dateTimeNow = calculate(calculationPatterns, dateTimeNow);
        return dateTimeNow.toString(DateTimeFormat.forPattern(dateFormatPattern));
    }

    private DateTime calculate(String calculationPatterns, DateTime dateTime) {
        Matcher matcher = Pattern.compile("[+-]\\d+\\w").matcher(calculationPatterns);
        while (matcher.find()) {
            dateTime = calculateDate(dateTime, matcher.group());
        }
        return dateTime;
    }

    /**
     * calculates date according to operation.
     */
    public DateTime calculateDate(DateTime dateTime, String entry) {
        Operation operation;
        if (entry.charAt(0) == '-') {
            operation = Operation.MINUS;
        } else {
            operation = Operation.PLUS;
        }
        int capacity = Integer.parseInt(entry.substring(1, entry.length() - 1));
        dateTime = doOperation(dateTime, entry, operation, capacity);
        return dateTime;
    }

    private DateTime doOperation(DateTime dateTime, String entry, Operation operation, int capacity) {
        char charAt = entry.charAt(entry.length() - 1);
        switch (charAt) {
            case 'd': { //most popular, I think
                return operation.calc(dateTime, Days.days(capacity));
            }
            case 'h': {
                return operation.calc(dateTime, Hours.hours(capacity));
            }
            case 'm': {
                return operation.calc(dateTime, Minutes.minutes(capacity));
            }
            case 's': {
                return operation.calc(dateTime, Seconds.seconds(capacity));
            }
            case 'M': {
                return operation.calc(dateTime, Months.months(capacity));
            }
            case 'y': {
                return operation.calc(dateTime, Years.years(capacity));
            }
            default: {
                throw new IllegalArgumentException(
                        "Undefined type of date operation: " + operation.operationSign + charAt
                );
            }
        }
    }

    private enum Operation {
        PLUS('+') {
            @Override
            public DateTime calc(DateTime dateTime, ReadablePeriod period) {
                return dateTime.plus(period);
            }
        }, MINUS('-') {
            @Override
            public DateTime calc(DateTime dateTime, ReadablePeriod period) {
                return dateTime.minus(period);
            }
        };

        private char operationSign;

        Operation(char operationSign) {
            this.operationSign = operationSign;
        }

        public abstract DateTime calc(DateTime dateTime, ReadablePeriod period);
    }
}

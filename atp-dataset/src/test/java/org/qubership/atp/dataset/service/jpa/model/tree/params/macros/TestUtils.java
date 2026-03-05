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

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;

import java.util.UUID;

import org.qubership.atp.macros.core.model.Macros;
import org.qubership.atp.macros.core.model.MacrosParameter;

public class TestUtils {

    private static final String EMPTY = "";
    private static final String ENGINE = "javascript";

    public static Macros getUuidMacros() {
        return getMacros("UUID",
                """
                function main() {
                    var result = java.util.UUID.randomUUID();
                    return result.toString();
                }\
                """);
    }

    public static Macros getCustomMacros() {
        return getMacros("CUSTOM_VALUE",
                """
                function main() {
                    return 'CUSTOM_VALUE';
                }\
                """);
    }

    public static Macros getContextMacros() {
        Macros context = getMacros("CONTEXT",
                """
                function main(variableName) {
                    return (contextMap != null && contextMap.get(variableName) != null)\s
                      ? contextMap.get(variableName)\s
                      : 'Unknown ' + variableName;
                }\
                """);
        context.setParameters(of(
                getMacrosParameter("variableName", null))
        );
        return context;
    }

    public static Macros getInnMacros() {
        return getMacros("INN",
                """
                function main() {
                    var factors = [2, 4, 10, 3, 5, 9, 4, 6, 8];
                    var result = '';
                    var last = 0;
                    for (var i = 0; i < 9; i++) {
                        var floor = Math.floor(Math.random() * (10 - 1) + 1);
                        result += floor;
                        last += factors[i] * floor;
                    }
                    last = last % 11 % 10;
                    result += last;
                    return result;
                }\
                """);
    }

    public static Macros getUuidUpperCaseMacros() {
        return getMacros("UUID_UPPERCASE",
                """
                function main() {
                    var result = java.util.UUID.randomUUID();
                    return result.toString().toUpperCase();
                }\
                """);
    }

    public static Macros getRandomBetweenMacros() {
        Macros macros = getMacros("RANDOMBETWEEN",
                """
                function main(min, max) {
                    min = parseInt(min);
                    max = parseInt(max);
                    if (max < min) {
                        var temp = max;
                        max = min;
                        min = temp;
                    }
                    var result = Math.floor(Math.random() * (max - min + 1)) + min;
                    return result.toString();
                }\
                """);
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("min", "1"),
                getMacrosParameter("max", "100"))));
        return macros;
    }

    public static Macros getDateMacros() {
        Macros macros = getMacros("DATE",
                """
                function main(format, timeZone) {
                    var date = new Packages.java.util.Date();
                    if (format.equals('millis')) {
                        return date.getTime().toString();
                    }
                    var print = new Packages.java.text.SimpleDateFormat(format);
                    if (timeZone != null) {
                        var timezone = Packages.java.util.TimeZone.getTimeZone(timeZone);
                        print.setTimeZone(timezone);
                    }
                    return print.format(date);
                }\
                """);
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("format", "dd.MM.yyyy hh:mm:ss"),
                getMacrosParameter("timeZone", null))));
        return macros;
    }

    public static Macros getCharsMacros() {
        Macros macros = getMacros("CHARS",
                """
                function main(count) {
                    var result = '';
                    var characters = 'abcdefghijklmnopqrstuvwxyz';
                    var length = characters.length;
                    for (var i = 0; i < count; i++) {
                        result += characters.charAt(Math.floor(Math.random() * length));
                    }
                    return result;
                }\
                """);
        macros.setParameters(of(
                getMacrosParameter("count", null)));
        return macros;
    }

    public static Macros getCharsUpperCaseMacros() {
        Macros macros = getMacros("CHARS_UPPERCASE",
                """
                function main(count) {
                    var result = '';
                    var characters = 'abcdefghijklmnopqrstuvwxyz';
                    var length = characters.length;
                    for (var i = 0; i < count; i++) {
                        result += characters.charAt(Math.floor(Math.random() * length));
                    }
                    return result.toUpperCase();
                }\
                """);
        macros.setParameters(of(
                getMacrosParameter("count", null)));
        return macros;
    }

    public static Macros getSumMacros() {
        Macros macros = getMacros("SUM",
                """
                function main(first, second) {
                    return parseInt(first) + parseInt(second);
                }\
                """);
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("first", null),
                getMacrosParameter("second", null))));
        return macros;
    }

    public static Macros getShiftDayMacros() {
        Macros macros = getMacros("SHIFT_DAY",
                """
                function main(value, date, format) {
                    var calendar = Packages.java.util.Calendar.getInstance();
                    if (format.equals('millis')) {
                        calendar.setTimeInMillis(Packages.java.lang.Long.parseLong(date));
                        var newDay = calendar.get(Packages.java.util.Calendar.DAY_OF_MONTH) + Packages\
                .java.lang.Integer.parseInt(value);
                        calendar.set(Packages.java.util.Calendar.DAY_OF_MONTH, newDay);
                        return calendar.getTimeInMillis();
                    } else {
                        var sdf = new Packages.java.text.SimpleDateFormat(format);
                        var date1 = sdf.parse(date);
                        calendar.setTimeInMillis(date1.getTime());
                        var newDay = calendar.get(Packages.java.util.Calendar.DAY_OF_MONTH) + Packages\
                .java.lang.Integer.parseInt(value);
                        calendar.set(Packages.java.util.Calendar.DAY_OF_MONTH, newDay);
                        return sdf.format(calendar.getTime());
                    }
                }\
                """);
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("value", null),
                getMacrosParameter("date", null),
                getMacrosParameter("format", null))));
        return macros;
    }

    public static Macros getRandMacros() {
        Macros macros = getMacros("RAND",
            """
            function main(digit) {
                digit = Math.abs(parseInt(digit));
                min = Math.pow(10, digit - 1);
                max = Math.pow(10, digit) - 1;
                var result = Math.floor(Math.random() * (max - min + 1)) + min;
                return result.toString();
            }\
            """);
        macros.setParameters(of(
            getMacrosParameter("digit", "1")));
        return macros;
    }

    public static Macros getTrNameMacros() {
        Macros macros = getMacros("TEST_CASE_SHORT_NAME",
                """
                function main() {   \s
                	return (contextMap != null && contextMap.get('TEST_CASE_SHORT_NAME') != null)\s
                		? contextMap.get('TEST_CASE_SHORT_NAME') : 'Unknown TEST_CASE_SHORT_NAME';
                }\
                """);
        return macros;
    }

    private static Macros getMacros(String name, String content) {
        Macros macros = new Macros();
        macros.setEngine(ENGINE);
        macros.setContent(content);
        macros.setUuid(UUID.randomUUID());
        macros.setName(name);
        return macros;
    }

    private static MacrosParameter getMacrosParameter(String name, String defaultValue) {
        boolean value = nonNull(defaultValue);
        MacrosParameter parameter = new MacrosParameter();
        parameter.setUuid(UUID.randomUUID());
        parameter.setName(name);
        parameter.setDefaultValue(value ? defaultValue : EMPTY);
        parameter.setOptional(value);
        parameter.setDescription(EMPTY);
        return parameter;
    }
}

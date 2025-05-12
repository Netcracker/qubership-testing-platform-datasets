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
                "function main() {\n"
                        + "    var result = java.util.UUID.randomUUID();\n"
                        + "    return result.toString();\n"
                        + "}");
    }

    public static Macros getCustomMacros() {
        return getMacros("CUSTOM_VALUE",
                "function main() {\n"
                        + "    return 'CUSTOM_VALUE';\n"
                        + "}");
    }

    public static Macros getContextMacros() {
        Macros context = getMacros("CONTEXT",
                "function main(variableName) {\n"
                        + "    return (contextMap != null && contextMap.get(variableName) != null) \n"
                        + "      ? contextMap.get(variableName) \n"
                        + "      : 'Unknown ' + variableName;\n"
                        + "}");
        context.setParameters(of(
                getMacrosParameter("variableName", null))
        );
        return context;
    }

    public static Macros getInnMacros() {
        return getMacros("INN",
                "function main() {\n"
                        + "    var factors = [2, 4, 10, 3, 5, 9, 4, 6, 8];\n"
                        + "    var result = '';\n"
                        + "    var last = 0;\n"
                        + "    for (var i = 0; i < 9; i++) {\n"
                        + "        var floor = Math.floor(Math.random() * (10 - 1) + 1);\n"
                        + "        result += floor;\n"
                        + "        last += factors[i] * floor;\n"
                        + "    }\n"
                        + "    last = last % 11 % 10;\n"
                        + "    result += last;\n"
                        + "    return result;\n"
                        + "}");
    }

    public static Macros getUuidUpperCaseMacros() {
        return getMacros("UUID_UPPERCASE",
                "function main() {\n"
                        + "    var result = java.util.UUID.randomUUID();\n"
                        + "    return result.toString().toUpperCase();\n"
                        + "}");
    }

    public static Macros getRandomBetweenMacros() {
        Macros macros = getMacros("RANDOMBETWEEN",
                "function main(min, max) {\n"
                        + "    min = parseInt(min);\n"
                        + "    max = parseInt(max);\n"
                        + "    if (max < min) {\n"
                        + "        var temp = max;\n"
                        + "        max = min;\n"
                        + "        min = temp;\n"
                        + "    }\n"
                        + "    var result = Math.floor(Math.random() * (max - min + 1)) + min;\n"
                        + "    return result.toString();\n"
                        + "}");
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("min", "1"),
                getMacrosParameter("max", "100"))));
        return macros;
    }

    public static Macros getDateMacros() {
        Macros macros = getMacros("DATE",
                "function main(format, timeZone) {\n"
                        + "    var date = new Packages.java.util.Date();\n"
                        + "    if (format.equals('millis')) {\n"
                        + "        return date.getTime().toString();\n"
                        + "    }\n"
                        + "    var print = new Packages.java.text.SimpleDateFormat(format);\n"
                        + "    if (timeZone != null) {\n"
                        + "        var timezone = Packages.java.util.TimeZone.getTimeZone(timeZone);\n"
                        + "        print.setTimeZone(timezone);\n"
                        + "    }\n"
                        + "    return print.format(date);\n"
                        + "}");
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("format", "dd.MM.yyyy hh:mm:ss"),
                getMacrosParameter("timeZone", null))));
        return macros;
    }

    public static Macros getCharsMacros() {
        Macros macros = getMacros("CHARS",
                "function main(count) {\n"
                        + "    var result = '';\n"
                        + "    var characters = 'abcdefghijklmnopqrstuvwxyz';\n"
                        + "    var length = characters.length;\n"
                        + "    for (var i = 0; i < count; i++) {\n"
                        + "        result += characters.charAt(Math.floor(Math.random() * length));\n"
                        + "    }\n"
                        + "    return result;\n"
                        + "}");
        macros.setParameters(of(
                getMacrosParameter("count", null)));
        return macros;
    }

    public static Macros getCharsUpperCaseMacros() {
        Macros macros = getMacros("CHARS_UPPERCASE",
                "function main(count) {\n"
                        + "    var result = '';\n"
                        + "    var characters = 'abcdefghijklmnopqrstuvwxyz';\n"
                        + "    var length = characters.length;\n"
                        + "    for (var i = 0; i < count; i++) {\n"
                        + "        result += characters.charAt(Math.floor(Math.random() * length));\n"
                        + "    }\n"
                        + "    return result.toUpperCase();\n"
                        + "}");
        macros.setParameters(of(
                getMacrosParameter("count", null)));
        return macros;
    }

    public static Macros getSumMacros() {
        Macros macros = getMacros("SUM",
                "function main(first, second) {\n"
                        + "    return parseInt(first) + parseInt(second);\n"
                        + "}");
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("first", null),
                getMacrosParameter("second", null))));
        return macros;
    }

    public static Macros getShiftDayMacros() {
        Macros macros = getMacros("SHIFT_DAY",
                "function main(value, date, format) {\n"
                        + "    var calendar = Packages.java.util.Calendar.getInstance();\n"
                        + "    if (format.equals('millis')) {\n"
                        + "        calendar.setTimeInMillis(Packages.java.lang.Long.parseLong(date));\n"
                        + "        var newDay = calendar.get(Packages.java.util.Calendar.DAY_OF_MONTH) + Packages"
                        + ".java.lang.Integer.parseInt(value);\n"
                        + "        calendar.set(Packages.java.util.Calendar.DAY_OF_MONTH, newDay);\n"
                        + "        return calendar.getTimeInMillis();\n"
                        + "    } else {\n"
                        + "        var sdf = new Packages.java.text.SimpleDateFormat(format);\n"
                        + "        var date1 = sdf.parse(date);\n"
                        + "        calendar.setTimeInMillis(date1.getTime());\n"
                        + "        var newDay = calendar.get(Packages.java.util.Calendar.DAY_OF_MONTH) + Packages"
                        + ".java.lang.Integer.parseInt(value);\n"
                        + "        calendar.set(Packages.java.util.Calendar.DAY_OF_MONTH, newDay);\n"
                        + "        return sdf.format(calendar.getTime());\n"
                        + "    }\n"
                        + "}");
        macros.setParameters(unmodifiableList(asList(
                getMacrosParameter("value", null),
                getMacrosParameter("date", null),
                getMacrosParameter("format", null))));
        return macros;
    }

    public static Macros getRandMacros() {
        Macros macros = getMacros("RAND",
            "function main(digit) {\n"
                + "    digit = Math.abs(parseInt(digit));\n"
                + "    min = Math.pow(10, digit - 1);\n"
                + "    max = Math.pow(10, digit) - 1;\n"
                + "    var result = Math.floor(Math.random() * (max - min + 1)) + min;\n"
                + "    return result.toString();\n"
                + "}");
        macros.setParameters(of(
            getMacrosParameter("digit", "1")));
        return macros;
    }

    public static Macros getTrNameMacros() {
        Macros macros = getMacros("TEST_CASE_SHORT_NAME",
                "function main() {    \n"
                        + "\treturn (contextMap != null && contextMap.get('TEST_CASE_SHORT_NAME') != null) \n"
                        + "\t\t? contextMap.get('TEST_CASE_SHORT_NAME') : 'Unknown TEST_CASE_SHORT_NAME';\n"
                        + "}");
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

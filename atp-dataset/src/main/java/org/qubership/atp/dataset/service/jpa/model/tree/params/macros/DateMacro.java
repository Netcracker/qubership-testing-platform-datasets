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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DateMacro extends AbstractMacro {
    public static final String MACRO_NAME = "DATE";
    @JsonIgnore
    private static final String GROUP_SHIFT_REGEX = "([\\+\\-])([0-9]*)?([a-zA-Z])(.*)";
    @JsonIgnore
    private static final Pattern groupShiftPattern = Pattern.compile(GROUP_SHIFT_REGEX);
    @JsonIgnore
    private static final String SHIFT_REGEX =
            "([\\+\\-][0-9]*d)?([\\+\\-][0-9]*M)?([\\+\\-][0-9]*y)?"
                    + "([\\+\\-][0-9]*h)?([\\+\\-][0-9]*m)?([\\+\\-][0-9]*s)?(.*)";
    @JsonIgnore
    private static final Pattern shiftPattern = Pattern.compile(SHIFT_REGEX);

    public DateMacro(
            String realMacroName,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext) {
        super(realMacroName, macroContext, parameterPositionContext);
    }

    @Override
    public String getEvaluatedValue(List<String> arguments) {
        if (arguments != null) {
            switch(arguments.size()) {
                case 1 : return makeDateString(null, new SimpleDateFormat(arguments.get(0).trim()));
                case 2 : return makeDateString(arguments.get(0).trim(), new SimpleDateFormat(arguments.get(1).trim()));
                default: throw new IllegalArgumentException("Wrong argument count");
            }
        }
        throw new IllegalArgumentException("List of arguments is null");
    }

    private String makeDateString(String dateShift, SimpleDateFormat dateFormat) {
        Date date = new Date();
        if (StringUtils.isEmpty(dateShift)) {
            return dateFormat.format(date);
        }
        Matcher matcher = shiftPattern.matcher(dateShift);
        if (matcher.matches()) {
            for (int i = 1; i < matcher.groupCount(); i++) {
                String shiftGroup = matcher.group(i);
                if (StringUtils.isNotEmpty(shiftGroup)) {
                    Matcher groupMatcher = groupShiftPattern.matcher(shiftGroup);
                    if (groupMatcher.matches()) {
                        int val = getValue(groupMatcher.group(1), groupMatcher.group(2));
                        switch (groupMatcher.group(3)) {
                            case "d":
                                date = DateUtils.addDays(date, val);
                                break;
                            case "M":
                                date = DateUtils.addMonths(date, val);
                                break;
                            case "y":
                                date = DateUtils.addYears(date, val);
                                break;
                            case "h":
                                date = DateUtils.addHours(date, val);
                                break;
                            case "m":
                                date = DateUtils.addMinutes(date, val);
                                break;
                            case "s":
                                date = DateUtils.addSeconds(date, val);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        return dateFormat.format(date);
    }

    private int getValue(String sign, String val) {
        return Integer.parseInt(sign + Optional.ofNullable(val).orElse("1"));
    }
}

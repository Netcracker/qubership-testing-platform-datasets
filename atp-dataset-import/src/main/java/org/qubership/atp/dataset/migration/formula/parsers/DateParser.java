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

package org.qubership.atp.dataset.migration.formula.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;

public class DateParser extends RegexpFormulaAdapter {

    private static final String DATE_REGEX = "CONCATENATE\\(TEXT\\(([^,]+),\"yyyy-MM-dd\"\\),"
            + "CONCATENATE\\(\"T\",CONCATENATE\\(TEXT\\(([^\"]+),\"HH:mm:ss\"\\),\"([^\"]+)\"\\)\\)\\)";
    private static final Pattern DATE_PATTERN = Pattern.compile(DATE_REGEX);

    private static final Pattern DATE_DELAY = Pattern.compile("TODAY\\(\\)\\s*([+-]\\d+)");
    private static final Pattern TIME_EXPRESSION =
            Pattern.compile("TIME\\((\\d{1,2}),\\s*(\\d{1,2}),\\s*(\\d{1,2})\\)");

    @Override
    protected Pattern getPattern() {
        return DATE_PATTERN;
    }

    @Override
    protected String getFormulaDescription() {
        return DATE_REGEX + " (custom excel date formula)";
    }

    @Override
    public FormulaType getType() {
        return FormulaType.DATE;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        Matcher matcher = getMatcher(cellData);
        String date = matcher.group(1);
        String time = matcher.group(2);
        String suffix = matcher.group(3);
        date = transformDate(date);
        time = transformTime(time);
        suffix = transformSuffix(suffix);
        return String.format("#DATE(%s'T'%s%s)", date, time, suffix);
    }

    private String transformSuffix(String suffix) {
        //all chars in suffix of date (should be escaped with ' single quotas
        // to avoid its transformation during formulas calculation
        //e.g. we have 123z in excel, it should be transformed to 123'z' , to be calculated to 123z
        //if 123z is transfromed to 123z, it is calculated to 123TIMEZONE (e.g. 123MSK) - which is unexpected
        return suffix.replaceAll("([a-zA-Z]+)", "'$1'");//' is not supported in excel formulas for now
    }

    private String transformDate(String date) {
        if ("NOW()".equalsIgnoreCase(date) || "TODAY()".equalsIgnoreCase(date)) {
            date = "yyyy-MM-dd";
        }
        final Matcher dateDelayMatcher = DATE_DELAY.matcher(date);//TODAY() +- number of days
        if (dateDelayMatcher.matches()) {
            date = dateDelayMatcher.group(1) + "d, yyyy-MM-dd";//calculate days delay
        }
        return date;
    }

    private String transformTime(String time) {
        if ("NOW()".equalsIgnoreCase(time) || "TODAY()".equalsIgnoreCase(time)) {
            time = "HH:mm:ss";
        }
        final Matcher timeFormatMatcher = TIME_EXPRESSION.matcher(time);
        if (timeFormatMatcher.matches()) {
            Integer hours = Integer.valueOf(timeFormatMatcher.group(1));
            Integer minutes = Integer.valueOf(timeFormatMatcher.group(2));
            Integer seconds = Integer.valueOf(timeFormatMatcher.group(3));
            time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return time;
    }
}

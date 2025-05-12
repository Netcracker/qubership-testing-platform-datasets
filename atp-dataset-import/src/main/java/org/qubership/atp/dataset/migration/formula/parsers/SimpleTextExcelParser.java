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

public class SimpleTextExcelParser extends RegexpFormulaAdapter {

    private static final String TEXT_REGEXP = "\"([^\"]*)\"";
    private static final Pattern SIMPLE_VALUE_PATTERN = Pattern.compile(TEXT_REGEXP);

    @Override
    public FormulaType getType() {
        return FormulaType.CONSTANT_TEXT_VALUE;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        Matcher matcher = getMatcher(cellData);
        return matcher.group(1);
    }

    @Override
    protected Pattern getPattern() {
        return SIMPLE_VALUE_PATTERN;
    }

    @Override
    protected String getFormulaDescription() {
        return TEXT_REGEXP + " (simple text in cell \"sometext\")";
    }
}

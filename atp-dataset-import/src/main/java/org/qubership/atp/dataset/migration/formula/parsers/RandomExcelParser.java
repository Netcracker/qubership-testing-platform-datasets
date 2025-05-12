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

public class RandomExcelParser extends RegexpFormulaAdapter {

    private static final String RANDOM_REGEXP = "RANDBETWEEN\\(([+-]?\\d+),\\s*([+-]?\\d+)\\)";
    private static final Pattern random = Pattern.compile(RANDOM_REGEXP);

    @Override
    public FormulaType getType() {
        return FormulaType.RANDOM;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        final Matcher matcher = getMatcher(cellData);
        return String.format("#RANDOMBETWEEN(%s, %s)", matcher.group(1), matcher.group(2));
    }

    @Override
    protected Pattern getPattern() {
        return random;
    }

    @Override
    protected String getFormulaDescription() {
        return RANDOM_REGEXP;
    }
}

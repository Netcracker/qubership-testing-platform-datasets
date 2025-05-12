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
import org.qubership.atp.dataset.migration.formula.model.ExcelFormulaAdapter;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;

public class ExternalReferenceParser implements ExcelFormulaAdapter {

    private static final Pattern externalReferenceIF =
            Pattern.compile("IF\\(\\[1][^!]+!\\$?\\w+\\d+=\"\",\"\",\\[1][^!]+!\\$?\\w+\\d+\\)");
    private static final Pattern externalReferenceT =
            Pattern.compile("T\\(\\[1][^!]+!\\$\\w+\\d+\\)");

    @Override
    public FormulaType getType() {
        return FormulaType.EXTERNAL_REFERENCE;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        if (!matches(cellData)) {
            throw new TransformationException("Text '" + cellData + "' is not external reference");
        }
        String text = cellData.getFormula();
        Matcher matcher = externalReferenceIF.matcher(text);//should be same logic to matches method
        if (!matcher.matches()) {
            matcher = externalReferenceT.matcher(text);
        }
        return "${" + matcher.group() + "}";
    }

    @Override
    public boolean matches(CellData cellData) {
        String text = cellData.getFormula();
        return externalReferenceIF.matcher(text).matches()
                || externalReferenceT.matcher(text).matches();
    }
}

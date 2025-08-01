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

import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.ExcelFormulaAdapter;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;

public class RandomCharParser implements ExcelFormulaAdapter {

    @Override
    public FormulaType getType() {
        return FormulaType.RANDOM_CHAR;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        return "#CHARS_UPPERCASE(1)";
    }

    @Override
    public boolean matches(CellData cellData) {
        return cellData.getFormula().equals("CHAR(RANDBETWEEN(65,90))");
    }
}

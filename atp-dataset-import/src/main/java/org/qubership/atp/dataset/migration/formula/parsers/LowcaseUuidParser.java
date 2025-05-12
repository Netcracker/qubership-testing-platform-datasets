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

public class LowcaseUuidParser implements ExcelFormulaAdapter {

    private static boolean isFormula_lowcase_Uuid(String formula) {
        return ("LOWER(CONCATENATE(DEC2HEX(RANDBETWEEN(0,4294967295),8),\"-\",DEC2HEX(RANDBETWEEN(0,65535),4),"
                + "\"-\",DEC2HEX(RANDBETWEEN(16384,20479),4),\"-\",DEC2HEX(RANDBETWEEN(32768,49151),4),"
                + "\"-\",DEC2HEX(RANDBETWEEN(0,65535),4),DEC2HEX(RANDBETWEEN(0,4294967295),8)))").equals(formula);
    }

    @Override
    public FormulaType getType() {
        return FormulaType.LOWCASE_UUID;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        String text = cellData.getFormula();
        if (!isFormula_lowcase_Uuid(text)) {
            throw new TransformationException("Text '" + text + "' is not lowcase UUID");
        }
        return "#UUID()";
    }

    @Override
    public boolean matches(CellData text) {
        return isFormula_lowcase_Uuid(text.getFormula());
    }
}

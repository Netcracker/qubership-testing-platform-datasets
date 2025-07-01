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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellType;
import org.qubership.atp.dataset.migration.formula.ExcelFormulasEvaluator;
import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.ExcelFormulaAdapter;
import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;

public class ConcatenateIntReferenceParser implements ExcelFormulaAdapter {

    private final ExcelFormulasEvaluator evaluator;

    /**
     * creates parser instance for formulas evaluation.
     *
     * @param evaluator used to calculate internal functions in formulas
     */
    public ConcatenateIntReferenceParser(ExcelFormulasEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public FormulaType getType() {
        return FormulaType.CONCATANATION_INTERNAL_REFERENCES;
    }

    @Override
    public String transform(CellData cellData) throws TransformationException {
        String text = cellData.getFormula();
        String[] formulaParts = text.split("&");
        return Arrays.stream(formulaParts)
                .map(String::trim)
                .map(formulaPart
                        -> new CellData(formulaPart, cellData.getValue(), cellData.getLocation(), CellType.FORMULA))
                .map(evaluator::getFormula)
                .map(Formula::getDatasetValue)
                .collect(Collectors.joining(""));
    }

    @Override
    public boolean matches(CellData cellData) {
        return cellData.getFormula().contains("&");
    }
}

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

package org.qubership.atp.dataset.migration.formula;

import java.util.HashSet;
import java.util.Set;

import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.ExcelFormulaAdapter;
import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.TransformationException;
import org.qubership.atp.dataset.migration.formula.parsers.ConcatenateIntReferenceParser;
import org.qubership.atp.dataset.migration.formula.parsers.DateParser;
import org.qubership.atp.dataset.migration.formula.parsers.ExternalReferenceParser;
import org.qubership.atp.dataset.migration.formula.parsers.InternalReferenceParser;
import org.qubership.atp.dataset.migration.formula.parsers.ListValueFormulaParser;
import org.qubership.atp.dataset.migration.formula.parsers.LowcaseUuidParser;
import org.qubership.atp.dataset.migration.formula.parsers.RandomCharParser;
import org.qubership.atp.dataset.migration.formula.parsers.RandomExcelParser;
import org.qubership.atp.dataset.migration.formula.parsers.SimpleTextExcelParser;
import org.qubership.atp.dataset.migration.formula.parsers.UuidParser;
import org.qubership.atp.dataset.migration.model.FalloutReport;

public class ExcelFormulasEvaluator {

    private final FalloutReport report;
    private Set<ExcelFormulaAdapter> adapters = new HashSet<>();

    /**
     * creates evaluator with default formulas parsers.
     *
     * @param report to store calculation failures
     */
    public ExcelFormulasEvaluator(FalloutReport report) {
        this(true, report);
    }

    /**
     * creates evaluator.
     *
     * @param fillWithDefaultParsers default formulas parser is used
     * @param report                 to store calculation failures
     */
    public ExcelFormulasEvaluator(boolean fillWithDefaultParsers, FalloutReport report) {
        this.report = report;
        if (fillWithDefaultParsers) {
            add(new ConcatenateIntReferenceParser(this));
            add(new RandomExcelParser());
            add(new SimpleTextExcelParser());
            add(new ExternalReferenceParser());
            add(new LowcaseUuidParser());
            add(new UuidParser());
            add(new InternalReferenceParser());
            add(new DateParser());
            add(new RandomCharParser());
            add(new ListValueFormulaParser());
        }
    }

    public void add(ExcelFormulaAdapter parsers) {
        adapters.add(parsers);
    }

    /**
     * tries to find formula by input formula from excel.
     *
     * @param cellData data from excel cell
     * @return calculated formula or UNKNOWN formula if formula is not implemented yet
     */
    public Formula getFormula(CellData cellData) {
        String excelFormula = cellData.getFormula();
        for (ExcelFormulaAdapter adapter : adapters) {
            if (adapter.matches(cellData)) {
                try {
                    String datasetValue = adapter.transform(cellData);
                    return new Formula(cellData, adapter.getType(), datasetValue, null);
                } catch (TransformationException e) {
                    report.report(cellData.getLocation(), excelFormula,
                            String.format("failed to convert formula to '%s'", adapter.getType()), e.getMessage());
                }
            }
        }
        //if no one formula is found - return unknown one
        report.report(cellData.getLocation(), excelFormula, "unknown formula", "");
        return new Formula(cellData, FormulaType.UNKNOWN, "UNKNOWN: " + excelFormula, null);
    }
}

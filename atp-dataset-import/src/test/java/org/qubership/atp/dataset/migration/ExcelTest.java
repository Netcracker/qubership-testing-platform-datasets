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

package org.qubership.atp.dataset.migration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import org.qubership.atp.dataset.migration.formula.ExcelFormulasEvaluator;
import org.qubership.atp.dataset.migration.formula.model.CellData;
import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.model.ExcelEvaluator;
import org.qubership.atp.dataset.migration.model.FalloutReport;

@Disabled
public class ExcelTest {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelTest.class);
    private ExcelFormulasEvaluator evaluator;

    @BeforeEach
    public void before() throws IOException {
        evaluator = new ExcelFormulasEvaluator(new FalloutReport("fallout.test.tsv"));
    }

    /**
     * read all excel files from the specified folder, gets all formulas, save its to the log file
     * for future processing after test move or copy log files to folder test_excel
     */
    @Test
    @Disabled//ignored because it is not required to perform such parsing each time
    public void get_all_formulas() throws IOException, InvalidFormatException {
        final List<Path> files = Files.find(Paths.get("TEST DATA\\test data"), 1,
                (path, basicFileAttributes) -> basicFileAttributes.isRegularFile()
                        && !path.startsWith("~")
                        && path.getFileName().toString().toLowerCase().matches(".+\\.xlsx?"))
                .collect(Collectors.toList());
        //YA - can be removed from here and calculated in the formula parsing test
        final int[] totalFormulas = {0};
        for (Path file : files) {
            final Path test_excel = Paths.get("test_excel", file.getFileName().toString() + ".txt");
            Files.deleteIfExists(test_excel);
            ExcelEvaluator excelEvaluator = new ExcelEvaluator(files, evaluator);
            Path formulasFile = Files.createFile(test_excel);
            BufferedWriter writer = new BufferedWriter(new FileWriter(formulasFile.toFile()));
            try {
                final Workbook workbook;
                try {
                    workbook = excelEvaluator.register(file);
                } catch (OutOfMemoryError e) {
                    System.gc();
                    LOG.error("FAILED TO PROCESS FILE - not enough memory : " + file.toString());
                    continue;
                }
                workbook.forEach(sheet -> sheet.forEach(row -> {
                    if (excelEvaluator.getValue(row.getCell(0)).isEmpty()
                            && excelEvaluator.getValue(row.getCell(1)).isEmpty()) {
                        return;
                    }
                    row.forEach(cell -> {
                        if (cell.getCellTypeEnum() == CellType.FORMULA) {
                            //log all formulas for future processing
                            try {
                                writer.write(sheet.getSheetName() + "#" + row
                                        .getRowNum() + ":" +
                                        cell.getColumnIndex() + " : " + cell.getCellFormula());
                                writer.newLine();
                            } catch (IOException e) {
                                LOG.error("failed to write formula to file: " + formulasFile + " " +
                                        "Error: " + e.getMessage());
                            }
                            totalFormulas[0]++;
                        }
                    });
                    try {
                        writer.flush();
                    } catch (IOException ignored) {
                        //ignored
                    }
                }));
            } catch (RuntimeException e) {
                LOG.error("FAILED TO PROCESS file " + file + ". Error: " + e.getMessage());
            } finally {
                writer.flush();
                writer.close();
            }
        }
    }

    /**
     * read all logs files from test_excel folder print all distinct unknown formulas
     */
    @Test
    public void show_unknown_formulas() throws IOException {
        getFormulasStream()
                .map(s -> evaluator.getFormula(new CellData(s, "unknown")))
                .filter(f -> f.getFormulaType() == FormulaType.UNKNOWN)
                .map(Formula::getExcelFormulaText)
                .distinct()
                .sorted()
                .forEach(LOG::info);
    }

    @Test
    public void get_formulas_types() throws IOException {
        Map<?, ?> result = getFormulasStream()
                .map(s -> evaluator.getFormula(new CellData(s, "unknown")).getFormulaType())
                .collect(Collectors.groupingBy(
                        FormulaType::name, Collectors.counting()));
        LOG.error("Error: ", result);
    }

    private Stream<String> getFormulasStream() throws IOException {
        return Files.find(Paths.get("test_excel"), 1,
                (p, a) -> a.isRegularFile() && p.getFileName().toString().endsWith(".txt"))
                .flatMap(path -> {
                    try {
                        return Files.lines(path);
                    } catch (IOException e) {
                        LOG.error(Throwables.getStackTraceAsString(e));
                        return Stream.empty();
                    }
                })
                .filter(s -> s.matches("^.+#\\d+:\\d+ :.+"))
                .map(s -> s.split("#\\d+:\\d+ : ", 2)[1]);
    }
}

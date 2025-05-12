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

package org.qubership.atp.dataset.migration.model;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.model.ExternalLinksTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qubership.atp.dataset.migration.formula.ExcelFormulasEvaluator;
import org.qubership.atp.dataset.migration.formula.model.AttachedFiles;
import org.qubership.atp.dataset.migration.formula.model.Formula;
import org.qubership.atp.dataset.migration.formula.model.FormulaType;
import org.qubership.atp.dataset.migration.formula.model.ParameterAssociation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

public class ExcelEvaluator {

    private static final Pattern ATP_MACROS_PATTERN = Pattern.compile("\\$(ENV_VARIABLE|RAND)\\(([^)]*)\\)");
    private static final Logger LOG = LoggerFactory.getLogger(ExcelEvaluator.class);
    private final List<Path> refFiles;
    private Map<XSSFWorkbook, FormulaEvaluator> evaluators = new HashMap<>();
    private Map<Path, XSSFWorkbook> filesToExcel = new HashMap<>();
    private Map<XSSFWorkbook, XSSFWorkbook> childParentMapping = new HashMap<>();
    private ExcelFormulasEvaluator formulaEvaluator;

    public ExcelEvaluator(List<Path> refFiles, ExcelFormulasEvaluator formulaEvaluator) {
        this.refFiles = refFiles;
        this.formulaEvaluator = formulaEvaluator;
    }

    private static Set<String> getRefs(@Nonnull XSSFWorkbook workbook) {
        List<ExternalLinksTable> links = workbook.getExternalLinksTable();
        if (links == null || links.isEmpty()) {
            return Sets.newHashSet();
        }
        Set<String> paths = Sets.newHashSetWithExpectedSize(links.size());
        for (ExternalLinksTable t : links) {
            String fileName = t.getLinkedFileName();
            if (fileName == null) {
                continue;
            }
            String path = PackagingURIHelper.decodeURI(URI.create(fileName));
            //java.nio.file.InvalidPathException: Illegal char <:> at index 4:
            // file://///mockingbird-tst/mockingbird_app_26.08.2016/DAO/dataset/WS Parent.xlsx
            Paths.get(path);
            paths.add(path);
        }
        return paths;
    }

    /**
     * register file can be used in external formulas evaluation.
     *
     * @param file excel file to be registered
     * @return excel workbook
     * @throws IOException            file can not be read
     * @throws InvalidFormatException if excel file has more than one reference to external files
     */
    public XSSFWorkbook register(Path file) throws IOException, InvalidFormatException {
        if (filesToExcel.get(file) != null) {
            return filesToExcel.get(file);
        }
        LOG.info("FILE: " + file);
        XSSFWorkbook book = (XSSFWorkbook) WorkbookFactory.create(file.toFile(), null, true);
        filesToExcel.put(file, book);
        FormulaEvaluator childBookEvaluator = book.getCreationHelper().createFormulaEvaluator();
        evaluators.put(book, childBookEvaluator);
        // Track the workbook references
        Map<String, FormulaEvaluator> workbooks = new HashMap<>();
        // Add this workbook
        workbooks.put(PackagingURIHelper.encode(file.getFileName().toString()), childBookEvaluator);
        //for each reference - register book one more time
        final Set<String> refs = getRefs(book);
        if (refs.size() > 1) {
            throw new InvalidFormatException("File should have only one references to external excel files: "
                    + file + ". But has " + refs.size() + " : " + refs);
        }
        if (!refs.isEmpty()) {
            String reference = refs.iterator().next();//refs now contains only one reference
            Optional<Path> s = refFiles.stream().filter(path -> path.getFileName().toString().equals(reference))
                    .findFirst();
            if (s.isPresent()) {
                XSSFWorkbook parent = register(s.get());
                childParentMapping.put(book, parent);
                workbooks.put(PackagingURIHelper.encode(reference), evaluators.get(parent));
            }
        }
        // Attach them
        childBookEvaluator.setupReferencedWorkbooks(workbooks);
        return book;
    }

    /**
     * get string value of cell.
     *
     * @param cell to be calculated
     * @return calculated value
     */
    @Nonnull
    public String getValue(Cell cell) {
        try {
            return getValueUnSafe(cell);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Calculate formula value.
     *
     * @param parameter to be calculated
     * @return calculated formula
     */
    public Formula getFormulaValue(ParameterAssociation parameter, List<Ole10Native> files) {
        if (parameter.data.getCellType() == CellType.FORMULA) {
            return formulaEvaluator.getFormula(parameter.data);
        } else {
            Optional<Ole10Native> file = AttachedFiles.findFileFromList(files, parameter.parameterSup.getAttrName(),
                    parameter.parameterSup.getDs().getName());
            if (file.isPresent()) {
                return new Formula(parameter.data, FormulaType.FILE, file.get().getLabel(), file.get());
            }
            FormulaType formulaType = FormulaType.TEXT;
            Matcher matcher = ATP_MACROS_PATTERN.matcher(parameter.data.getValue());
            StringBuffer valueSb = new StringBuffer();
            while (matcher.find()) {
                formulaType = FormulaType.ATP_MACROS;
                String convertedValue;
                String macroName = matcher.group(1);
                if (macroName.equalsIgnoreCase("ENV_VARIABLE")) {
                    String macroArgs = matcher.group(2);
                    convertedValue = "#CONTEXT(" + macroArgs + ")";
                } else if (macroName.equalsIgnoreCase("RAND")) {
                    String macroArg = matcher.group(2).replaceAll("'", "");
                    String parameters = transformIntToRange(macroArg);
                    convertedValue = "#RANDOMBETWEEN(" + parameters + ")";
                } else {
                    //do not convert macros from atp to context anymore, but if smth happens - leave as it is without $
                    convertedValue = parameter.data.getValue();
                }
                matcher.appendReplacement(valueSb, convertedValue);
            }
            matcher.appendTail(valueSb);
            return new Formula(parameter.data, formulaType, valueSb.toString(), null);
        }
    }

    /**
     * Transforms macroArg, which is number of digits in number, to range.
     *
     * @param macroArg number of digits in number
     * @return range for macros #RANDOMBETWEEN(range)
     */
    private String transformIntToRange(String macroArg) {
        int number = Integer.parseInt(macroArg);
        long firstArg = 1;
        long secondArg = 9;
        if (number >= 2) {
            for (int i = 1; i < number; i++) {
                firstArg *= 10;
                secondArg *= 10;
                secondArg += 9;
            }
            return firstArg + "," + secondArg;
        }
        return "0,9";
    }

    /**
     * Returns value of the current excel-cell.
     *
     * @return value of the current excel-cell
     * @throws IllegalArgumentException if current excell-cell contains invalid format data
     */
    private String getValueUnSafe(Cell cell) throws IllegalArgumentException {
        DataFormatter dataFormatter = new DataFormatter(Locale.ENGLISH);//assume english locale is default
        String result = "";//return empty string by default
        if (cell != null) {
            try {
                FormulaEvaluator evaluator = evaluators.get(cell.getSheet().getWorkbook());
                result = dataFormatter.formatCellValue(cell, evaluator); // better in most situations
            } catch (Exception e) {
                if (cell.getCellTypeEnum() == CellType.ERROR) {
                    result = cell instanceof XSSFCell ? ((XSSFCell) cell).getErrorCellString() : "####";
                }
                throw new RuntimeException("Cell parsing report"
                        + "\tSheet: " + cell.getSheet().getSheetName() + ", "
                        + "\tRow: " + (cell.getRowIndex() + 1) + ", "
                        + "\tCell: " + (cell.getColumnIndex() + 1) + ", "
                        + "\tValue: " + result, e);
            }
        }
        return result == null ? "" : result.trim();
    }

    /**
     * register all referenced files.
     *
     * @throws IOException            file can not be read
     * @throws InvalidFormatException if excel file has more than one reference to external files
     */
    public void registerAll() throws IOException, InvalidFormatException {
        for (Path p : refFiles) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            register(p);
            LOG.info(p + " = " + stopwatch.stop().elapsed(TimeUnit.SECONDS));
        }
    }

    /**
     * release excel resources.
     */
    public void release() {
        filesToExcel.forEach((path, workbook) -> {
            try {
                workbook.close();
            } catch (IOException ignored) {
                //ignored
            }
        });
        filesToExcel = new HashMap<>();
    }

    public Workbook getParentBook(XSSFWorkbook childBook) {
        return childParentMapping.get(childBook);
    }
}


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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qubership.atp.dataset.migration.formula.ExcelFormulasEvaluator;
import org.qubership.atp.dataset.migration.repo.DsServicesFacade;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * Encapsulates all resources used in import process.
 */
public class ImportResources implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ImportResources.class);

    public final DsServicesFacade services;
    public final ExcelEvaluator excelEvaluator;
    public final FalloutReport falloutReport;
    public final String bookName;
    public final String groupDataSetName;
    public final XSSFWorkbook book;
    public final VisibilityArea va;

    /**
     * See {@link ImportResources}.
     */
    public ImportResources(DsServicesFacade services,
                           ExcelEvaluator excelEvaluator,
                           FalloutReport falloutReport,
                           String bookName,
                           String groupDataSetName, XSSFWorkbook book,
                           VisibilityArea va) {
        this.services = services;
        this.excelEvaluator = excelEvaluator;
        this.falloutReport = falloutReport;
        this.bookName = bookName;
        this.groupDataSetName = groupDataSetName;
        this.book = book;
        this.va = va;
    }

    /**
     * create parent dataset.
     *
     * @param services           services
     * @param visibilityAreaName visibility area where data sets are created
     * @param excelDataFolder    - location with xlsx files
     * @param bookName           - target excel file name
     * @param groupDataSetName   - default data set for group parameter with default values(parent);
     *                           name for all groups datasets(child)
     * @throws IOException - in case IO errors occurred,
     * @throws InvalidFormatException - in case invalid Excel File format.
     */
    public static ImportResources create(DsServicesFacade services,
                                         String visibilityAreaName,
                                         String excelDataFolder,
                                         String bookName,
                                         String groupDataSetName) throws IOException, InvalidFormatException {
        FalloutReport falloutReport = new FalloutReport(bookName + ".fallout.tsv");
        ExcelEvaluator excelEvaluator = new ExcelEvaluator(Files.find(Paths.get(excelDataFolder), Integer.MAX_VALUE,
                (path, basicFileAttributes) -> basicFileAttributes.isRegularFile()
                        && !path.startsWith("~$")
                        && path.getFileName().toString().toLowerCase().matches(".+\\.xlsx"))
                .collect(Collectors.toList()), new ExcelFormulasEvaluator(falloutReport));
        XSSFWorkbook book = excelEvaluator.register(Paths.get(bookName));
        VisibilityArea va = services.get_VA_ByNameOrCreate(visibilityAreaName);
        return new ImportResources(services, excelEvaluator, falloutReport, bookName, groupDataSetName, book, va);
    }

    @Override
    public void close() {
        try {
            book.close();
        } catch (IOException e) {
            LOG.error("FAILED TO CLOSE EXCEL BOOK : " + bookName);
            LOG.error(Throwables.getStackTraceAsString(e));
        }
        excelEvaluator.release();
        try {
            falloutReport.close();
        } catch (Exception e) {
            LOG.error("FAILED TO CLOSE FALLOUT REPORT : " + falloutReport.toString());
            LOG.error(Throwables.getStackTraceAsString(e));
        }
    }
}

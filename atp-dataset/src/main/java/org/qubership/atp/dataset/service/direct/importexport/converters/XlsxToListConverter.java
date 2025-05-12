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

package org.qubership.atp.dataset.service.direct.importexport.converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import lombok.extern.slf4j.Slf4j;

/**
 * A rudimentary XLSX -&gt; ListMap processor modeled on the
 * POI sample program XlsxToListConverter from the package
 * org.apache.poi.hssf.eventusermodel.examples.
 * Read using a SAX parser to keep the
 * memory footprint relatively small, so this should be
 * able to read enormous workbooks.  The styles table and
 * the shared-string table must be kept in memory.  The
 * standard POI styles table class is used, but a custom
 * (read-only) class is used for the shared string table
 * because the standard POI SharedStringsTable grows very
 * quickly with the number of unique strings.
 */
@Slf4j
public class XlsxToListConverter {

    private final OPCPackage xlsxPackage;

    private final List<Map<Integer, String>> sheetConvertList;

    /**
     * Creates a new XLSX -&gt; ListMap converter.
     * output. The sheetConvertList to output the ListMap to.
     *
     * @param pkg The XLSX package to process
     * @param sheetConvertList The XLSX converted to ListMap
     */
    public XlsxToListConverter(OPCPackage pkg, List<Map<Integer, String>> sheetConvertList) {
        this.xlsxPackage = pkg;
        this.sheetConvertList = sheetConvertList;
    }

    /**
     * Parses and shows the content of one sheet
     * using the specified styles and shared-strings tables.
     *
     * @param styles The table of styles that may be referenced by cells in the sheet
     * @param strings The table of strings that may be referenced by cells in the sheet
     * @param sheetInputStream The stream to read the sheet-data from.
     * @throws java.io.IOException An IO exception from the parser,
     *         possibly from a byte stream or character stream
     *         supplied by the application.
     * @throws SAXException if parsing the XML data fails.
     */
    public void processSheet(Styles styles, SharedStrings strings, SheetContentsHandler sheetHandler,
                             InputStream sheetInputStream) throws IOException, SAXException {
        InputSource sheetSource = new InputSource(sheetInputStream);
        try {
            XMLReader sheetParser = XMLHelper.newXMLReader();
            ContentHandler handler = new XSSFSheetXMLHandler(styles, strings, sheetHandler, false);
            sheetParser.setContentHandler(handler);
            sheetParser.parse(sheetSource);
        } catch (ParserConfigurationException e) {
            String error = "SAX parser appears to be broken - " + e.getMessage();
            log.error(error);
            throw new RuntimeException(error);
        }
    }

    /**
     * Initiates the processing of the XLS workbook file to List.
     *
     * @throws IOException If reading the data from the package fails.
     * @throws SAXException if parsing the XML data fails.
     */
    public void process() throws IOException, OpenXML4JException, SAXException {
        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(this.xlsxPackage);
        XSSFReader xssfReader = new XSSFReader(this.xlsxPackage);
        StylesTable styles = xssfReader.getStylesTable();
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        if (iter.hasNext()) {
            try (InputStream stream = iter.next()) {
                processSheet(styles, strings, new SheetXmlConverter(), stream);
            }
        }
    }

    ////////////////////////////////////////////////
    /**
     * Uses the XSSF Event SAX helpers to do most of the work
     * of parsing the Sheet XML, and outputs the contents
     * as a (basic) List.
     */
    private class SheetXmlConverter implements SheetContentsHandler {

        private int currentRow = -1;
        private int currentCol = -1;

        @Override
        public void startRow(int rowNum) {
            // Prepare for this row
            currentRow = rowNum;
            currentCol = -1;
            sheetConvertList.add(new LinkedHashMap<>());
        }

        @Override
        public void endRow(int rowNum) {
            log.debug("End of row then do nothing");
        }

        @Override
        public void cell(String cellReference, String formattedValue,
                         XSSFComment comment) {

            if (cellReference == null) {
                log.debug("gracefully handle missing CellRef here in a similar way as XSSFCell does");
                cellReference = new CellAddress(currentRow, currentCol).formatAsString();
            }

            int thisCol = new CellReference(cellReference).getCol();

            if (formattedValue == null) {
                log.debug("no need to append anything if we do not have a value");
                return;
            }

            currentCol = thisCol;
            sheetConvertList.get(currentRow).put(thisCol, formattedValue);
        }
    }
}

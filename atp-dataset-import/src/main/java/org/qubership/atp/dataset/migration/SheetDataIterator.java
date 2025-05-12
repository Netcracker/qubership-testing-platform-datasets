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

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.qubership.atp.dataset.migration.model.ExcelEvaluator;

import com.google.common.collect.AbstractIterator;

public class SheetDataIterator extends AbstractIterator<DsRow> {

    public final ExcelEvaluator excelEvaluator;
    private final Iterator<Row> sheetData;
    private final int valuesColumnIndex;
    private final State state = new State();

    /**
     * Iterates over significant cells in sheet.
     *
     * @param sheetData         should not include header row
     * @param valuesColumnIndex index of current dataSet column
     * @param excelEvaluator    used to acquire group cell, parameter key cell values.
     */
    public SheetDataIterator(Iterator<Row> sheetData, int valuesColumnIndex, ExcelEvaluator excelEvaluator) {
        this.sheetData = sheetData;
        this.valuesColumnIndex = valuesColumnIndex;
        this.excelEvaluator = excelEvaluator;
    }

    /**
     * Creates iterator to traverse all rows from supplied sheet except the header one.
     */
    public static SheetDataIterator create(Sheet sheet, int valuesColumnIndex, ExcelEvaluator excelEvaluator) {
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();//skip header row
        return new SheetDataIterator(rowIterator, valuesColumnIndex, excelEvaluator);
    }

    /**
     * Sets group to initial value on the first invoke. Sets new value if it is differ from the old
     * one.
     */
    private void setGroup(@Nonnull Cell newGroupCell) {
        String cellValue = getCellValue(newGroupCell);
        String group = cellValue.trim().isEmpty() ? null : cellValue;
        state.gotNewGroup = group != null && !group.equals(state.group);
        if (state.gotNewGroup) {
            state.group = group;
        }
        state.groupCell = newGroupCell;
    }

    private void setValue(@Nonnull Cell keyCell, @Nonnull Cell valueCell) {
        state.parameterKeyCell = keyCell;
        state.parameterValueCell = valueCell;
        String cellValue = getCellValue(state.parameterKeyCell);
        state.parameterKey = cellValue.trim().isEmpty() ? null : cellValue;
    }

    @Nonnull
    private String getCellValue(@Nullable Cell cell) {
        return excelEvaluator.getValue(cell);
    }

    @Override
    protected DsRow computeNext() {
        while (sheetData.hasNext()) {
            Row row = sheetData.next();
            setGroup(row.getCell(0, CREATE_NULL_AS_BLANK));
            setValue(row.getCell(1, CREATE_NULL_AS_BLANK),
                    row.getCell(valuesColumnIndex, CREATE_NULL_AS_BLANK));
            if (state.gotNewGroup || state.parameterKey != null) {
                return state;
            }
            //it is parameter without key
            //not acceptable
            //continue
        }
        return endOfData();
    }

    private static class State implements DsRow {
        private String group = null;
        private Cell groupCell;
        private boolean gotNewGroup;
        private String parameterKey;
        private Cell parameterKeyCell;
        private Cell parameterValueCell;

        @Override
        public boolean gotNewGroup() {
            return gotNewGroup;
        }

        @Nonnull
        @Override
        public Cell getGroupCell() {
            return groupCell;
        }

        @Nullable
        @Override
        public String getGroup() {
            return group;
        }

        @Nonnull
        @Override
        public Cell getParameterKeyCell() {
            return parameterKeyCell;
        }

        @Nullable
        @Override
        public String getParameterKey() {
            return parameterKey;
        }

        @Nonnull
        @Override
        public Cell getParameterValueCell() {
            return parameterValueCell;
        }

        /**
         * Returns 'SHEET_NAME|GROUP_CELL_ADDRESS' info, or 'UNKNOWN' if no group entered yet.
         */
        @Nonnull
        @Override
        public String getCurrentLocationInfo() {
            if (groupCell == null) {
                //not initialized yet
                return "UNKNOWN";
            }
            return groupCell.getSheet().getSheetName() + "|" + groupCell.getAddress().formatAsString();
        }
    }
}

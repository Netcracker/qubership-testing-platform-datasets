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

package org.qubership.atp.dataset.migration.formula.model;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.qubership.atp.dataset.migration.model.ExcelEvaluator;

import com.google.common.base.MoreObjects;

public class CellData {

    private CellType cellType = CellType._NONE;
    private String formula = "";
    private String value = "";
    private String location = "";

    /**
     * creates object to store formula information.
     *
     * @param formula  formula value to transform
     * @param location formula location
     */
    public CellData(String formula, String location) {
        this(formula, "", location, CellType._NONE);
    }

    /**
     * creates object to store formula information.
     *
     * @param formula  formula value to transform
     * @param value    calculated formula value
     * @param location formula location
     * @param cellType type of cell
     */
    public CellData(String formula, String value, String location, CellType cellType) {
        this.formula = formula;
        this.value = value;
        this.location = location;
        this.cellType = cellType;
    }

    /**
     * creates object to store formula information.
     *
     * @param excelEvaluator cell value calculator used to calculate cell value
     * @param cell           cell to be stored
     */
    public CellData(ExcelEvaluator excelEvaluator, Cell cell) {
        this.location = cell.getSheet().getSheetName() + "!" + cell.getAddress().formatAsString();
        this.cellType = cell.getCellTypeEnum();
        this.value = excelEvaluator.getValue(cell);
        if (cell.getCellTypeEnum() == CellType.FORMULA) {
            this.formula = cell.getCellFormula();
        }
    }

    public String getFormula() {
        return formula;
    }

    public String getValue() {
        return value;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cellType", cellType)
                .add("formula", formula)
                .add("value", value)
                .toString();
    }

    public CellType getCellType() {
        return cellType;
    }
}

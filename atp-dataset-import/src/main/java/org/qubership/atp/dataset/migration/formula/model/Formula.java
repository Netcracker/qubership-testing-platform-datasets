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

import org.apache.poi.poifs.filesystem.Ole10Native;

import com.google.common.base.MoreObjects;

public class Formula {

    private final CellData data;
    private final FormulaType formulaType;
    private final String datasetValue;
    private final Ole10Native file;

    /**
     * formula stores calculated value.
     *
     * @param data         from which dataset value has been acquired
     * @param formulaType  formula type
     * @param datasetValue result data set value
     */
    public Formula(CellData data, FormulaType formulaType, String datasetValue, Ole10Native file) {
        this.data = data;
        this.formulaType = formulaType;
        this.datasetValue = datasetValue;
        this.file = file;
    }

    public String getLocation() {
        return data.getLocation();
    }

    public String getExcelFormulaValue() {
        return data.getValue();
    }

    public FormulaType getFormulaType() {
        return formulaType;
    }

    public String getExcelFormulaText() {
        return data.getFormula();
    }

    public String getDatasetValue() {
        return datasetValue;
    }

    public Ole10Native getFile() {
        return file;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("data", data)
                .add("formulaType", formulaType)
                .add("datasetValue", datasetValue)
                .add("file", getFileName())
                .toString();
    }

    private String getFileName() {
        return file == null ? "" : file.getFileName();
    }
}

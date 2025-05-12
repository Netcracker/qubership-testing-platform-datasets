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

import org.qubership.atp.dataset.migration.apply.CreateParam;
import org.qubership.atp.dataset.migration.apply.Overlap;
import org.qubership.atp.dataset.migration.apply.SetParam;

public enum FormulaType {
    TEXT(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.TEXT_OR_LIST_VALUE),

    ATP_MACROS(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.TEXT_ONLY),

    FILE(
            CreateParam.FILE,
            SetParam.FILE,
            Overlap.FILE),

    REFERENCE(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY),

    EXTERNAL_REFERENCE(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.SKIP),

    RANDOM(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY),

    CONSTANT_TEXT_VALUE(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.TEXT_OR_LIST_VALUE),

    UPCASE_UUID(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY),

    LOWCASE_UUID(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY),

    CONCATANATION_INTERNAL_REFERENCES(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY),

    DATE(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY),

    RANDOM_CHAR(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY),

    LIST_VALUE(
            CreateParam.LIST_VALUE,
            SetParam.LIST_VALUE,
            Overlap.TEXT_OR_LIST_VALUE),

    UNKNOWN(
            CreateParam.TEXT,
            SetParam.TEXT,
            Overlap.MACROS_AS_TEXT_ONLY);//may be too ugly to overlap list values

    public final CreateParam createParam;
    public final SetParam setParam;
    public final Overlap overlap;

    FormulaType(CreateParam createParam, SetParam setParam, Overlap overlap) {
        this.createParam = createParam;
        this.setParam = setParam;
        this.overlap = overlap;
    }
}

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

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;

/**
 * Representation of a group with 1) incoming attribute ref from user defined DSL. 2) parameter
 * incoming ref from DS with name from EXCEL in user defined DSL.
 */
public interface OverlapParamContainer extends ParamContainer {

    /**
     * Data set list with user defined name from {@link Settings#getDslName()}.
     */
    default DataSetList getDsl() {
        return getRefToDsl().getDataSetList();
    }

    /**
     * Data set under {@link #getDsl()} with name of the ds column in the excel.
     */
    default DataSet getDs() {
        return getRefToDs().getDataSet();
    }

    /**
     * A ref from {@link #getDsl()} to the {@link #getGroupDsl()}.
     */
    default Attribute getRefToDsl() {
        return getRefToDs().getAttribute();
    }

    /**
     * A ref from {@link #getDs()} to the {@link #getGroupDs()}.
     */
    Parameter getRefToDs();
}

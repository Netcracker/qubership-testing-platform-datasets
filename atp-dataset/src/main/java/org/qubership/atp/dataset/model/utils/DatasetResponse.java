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

package org.qubership.atp.dataset.model.utils;

import java.util.UUID;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatasetResponse {
    private UUID dataSetId;
    private String dataSetName;
    private UUID dataSetListId;
    private String dataSetListName;

    /**
     * DatasetResponse constructor.
     *
     * @param dataSet data set
     * @param dataSetList data set list
     */
    public DatasetResponse(DataSet dataSet, DataSetList dataSetList) {
        this.dataSetId = dataSet.getId();
        this.dataSetName = dataSet.getName();
        this.dataSetListId = dataSetList.getId();
        this.dataSetListName = dataSetList.getName();
    }
}

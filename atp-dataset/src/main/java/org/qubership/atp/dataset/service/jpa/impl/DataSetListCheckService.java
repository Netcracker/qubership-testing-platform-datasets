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

package org.qubership.atp.dataset.service.jpa.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.datasetlist.DataSetListCycleException;
import org.qubership.atp.dataset.exception.datasetlist.DataSetListNotFoundException;
import org.qubership.atp.dataset.service.jpa.JpaVisibilityAreaService;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.CyclesCheckResult;
import org.qubership.atp.dataset.service.jpa.model.VisibilityAreaFlatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataSetListCheckService {

    @Autowired
    protected DataSetListContextService dataSetListContextService;
    @Autowired
    protected ModelsProvider modelsProvider;
    @Autowired
    protected JpaVisibilityAreaService visibilityAreaService;

    /**
     * Returns DSL cycle check result.
     */
    public CyclesCheckResult checkOnCyclesAll() {
        CyclesCheckResult result = new CyclesCheckResult();
        for (VisibilityAreaFlatModel visibilityAreaFlatModel : visibilityAreaService.getAll()) {
            for (UUID dataSetListId : visibilityAreaFlatModel.getDataSetLists()) {
                try {
                    DataSetList dataSetListById = modelsProvider.getDataSetListById(dataSetListId);
                    if (!dataSetListById.getDataSets().isEmpty()) {
                        dataSetListContextService.getDataSetListContext(
                                dataSetListId,
                                Collections.emptyList(),
                                Collections.emptyList(),
                                null);
                        result.getResults().put(dataSetListId, "OK");
                    } else {
                        result.getResults().put(dataSetListId, "NO_DATASETS");
                    }
                } catch (DataSetListNotFoundException e) {
                    result.getResults().put(dataSetListId, e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * Returns DSL cycle check result.
     */
    public void checkOnCyclesThrowException(DataSetList dataSetList) {
        List<UUID> level = new LinkedList<>();
        checkRecursively(dataSetList, level);
    }

    private void checkRecursively(DataSetList dataSetList, List<UUID> level) {
        if (level.contains(dataSetList.getId())) {
            throw new DataSetListCycleException();
        }
        level.add(dataSetList.getId());
        for (Attribute dslAttribute : dataSetList.getAttributes()) {
            if (dslAttribute.getAttributeType() == AttributeTypeName.DSL) {
                checkRecursively(dslAttribute.getTypeDataSetList(), level);
            }
        }
        level.remove(dataSetList.getId());
    }
}

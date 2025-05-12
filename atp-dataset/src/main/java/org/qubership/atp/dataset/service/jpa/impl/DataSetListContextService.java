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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.exception.datasetlist.DataSetListNotFoundException;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DataSetListContextService {
    protected ThreadLocal<List<DataSetListContext>> threadLocalValue = new ThreadLocal<>();
    @Autowired
    protected ModelsProvider modelsProvider;

    private DataSetListContext getCachedDataSetListContext(
            UUID dataSetListId, List<Integer> loadedColumns, List<AttributeTypeName> attributeTypesToLoad) {
        List<DataSetListContext> dataSetListContexts = threadLocalValue.get();
        if (dataSetListContexts != null) {
            for (DataSetListContext dataSetListContext : dataSetListContexts) {
                if (dataSetListContext.getDataSetListId().equals(dataSetListId)
                    && dataSetListContext.getLoadedColumns().containsAll(loadedColumns)
                    && dataSetListContext.getLoadedAttributes().containsAll(attributeTypesToLoad)) {
                        return dataSetListContext;
                }
            }
        }
        return null;
    }

    /**
     * Returns unevaluated DSL structure.
     * pathRestrictions - Used for DSL lazy loading performance. Shows which groups should be loaded,
     * and the last node will be fully loaded.
     * */
    @Transactional(readOnly = true)
    public DataSetListContext getDataSetListContext(UUID dataSetListId,
                                                    List<Integer> dataSetColumns,
                                                    List<AttributeTypeName> attributeTypesToLoad,
                                                    List<UUID> pathRestrictions, Pageable pageable) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        if (dataSetList != null) {
            DataSetListContext dataSetListContext = getCachedDataSetListContext(
                    dataSetListId, dataSetColumns, attributeTypesToLoad
            );
            if (dataSetListContext != null) {
                return dataSetListContext;
            }
            DataSetListContext newContext = new DataSetListContext(
                    dataSetList,
                    dataSetColumns,
                    attributeTypesToLoad,
                    pathRestrictions,
                    pageable
            );
            if (pathRestrictions == null) {
                addNewContext(newContext);
            }
            return newContext;
        }
        log.error("Data Set List not found " + dataSetListId);
        throw new DataSetListNotFoundException();
    }

    /**
     * Returns unevaluated DSL structure.
     * pathRestrictions - Used for DSL lazy loading performance. Shows which groups should be loaded,
     * and the last node will be fully loaded.
     * */
    @Transactional(readOnly = true)
    public DataSetListContext getDataSetListContext(UUID dataSetListId,
                                                    List<Integer> dataSetColumns,
                                                    List<AttributeTypeName> attributeTypesToLoad,
                                                    List<UUID> pathRestrictions) {
        DataSetList dataSetList = modelsProvider.getDataSetListById(dataSetListId);
        if (dataSetList != null) {
            DataSetListContext dataSetListContext = getCachedDataSetListContext(
                    dataSetListId, dataSetColumns, attributeTypesToLoad
            );
            if (dataSetListContext != null) {
                return dataSetListContext;
            }
            DataSetListContext newContext = new DataSetListContext(
                    dataSetList,
                    dataSetColumns,
                    attributeTypesToLoad,
                    pathRestrictions
            );
            if (pathRestrictions == null) {
                addNewContext(newContext);
            }
            return newContext;
        }
        log.error("Data Set List not found " + dataSetListId);
        throw new DataSetListNotFoundException();
    }

    private void addNewContext(DataSetListContext newContext) {
        if (threadLocalValue.get() == null) {
            threadLocalValue.set(new LinkedList<>());
        }
        threadLocalValue.get().add(newContext);
    }

    public void dropLocalThreadCache() {
        threadLocalValue.remove();
    }
}

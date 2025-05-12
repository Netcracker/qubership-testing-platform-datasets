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

package org.qubership.atp.dataset.service.jpa.delegates;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.FilterEntity;
import org.qubership.atp.dataset.db.jpa.entities.TestPlanEntity;
import org.qubership.atp.dataset.db.jpa.entities.VisibilityAreaEntity;

public class VisibilityArea extends AbstractObjectWrapper<VisibilityAreaEntity> {
    public VisibilityArea(VisibilityAreaEntity entity) {
        super(entity);
    }

    public UUID getId() {
        return entity.getId();
    }

    public String getName() {
        return entity.getName();
    }

    /**
     * Set and save name.
     */
    public void setName(String name) {
        entity.setName(name);
        save(entity);
    }

    /**
     * Returns all DSLs.
     */
    public List<DataSetList> getDataSetLists() {
        List<DataSetList> result = new LinkedList<>();
        entity.getDataSetLists().forEach(
                dataSetListEntity -> result.add(
                        modelsProvider.getDataSetList(dataSetListEntity)
                )
        );
        return result;
    }

    /**
     * Returns all DSLs ids.
     */
    public List<UUID> getDataSetListIds() {
        String nativeQuery = "select cast(id as varchar) from datasetlist where visibility_area_id = :va_id";
        List<UUID> result = new LinkedList<>();
        entityManager
                .createNativeQuery(nativeQuery)
                .setParameter("va_id", getId())
                .getResultList().forEach(o -> result.add(UUID.fromString((String) o)));
        return result;
    }

    /**
     * Returns DSL by name if exists.
     */
    public DataSetList getDataSetListByName(String name) {
        String nativeQuery = "select * from datasetlist where visibility_area_id = :va_id and name = :dsl_name";
        List<DataSetListEntity> resultList = entityManager.createNativeQuery(nativeQuery, DataSetListEntity.class)
                .setParameter("va_id", getId())
                .setParameter("dsl_name", name)
                .getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return modelsProvider.getDataSetList(resultList.iterator().next());
    }

    /**
     * Returns child DSL by id if exists.
     */
    public DataSetList getDataSetListById(UUID id) {
        String nativeQuery = "select * from datasetlist where visibility_area_id = :va_id and id = :dsl_id";
        List<DataSetListEntity> resultList = entityManager.createNativeQuery(nativeQuery, DataSetListEntity.class)
                .setParameter("va_id", getId())
                .setParameter("dsl_id", id)
                .getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return modelsProvider.getDataSetList(resultList.iterator().next());
    }

    /**
     * Create with name.
     */
    public DataSetList createDataSetList(String name) {
        Timestamp dslCreationTime = Timestamp.from(Instant.now());
        DataSetListEntity dataSetListEntity = new DataSetListEntity();
        dataSetListEntity.setName(name);
        dataSetListEntity.setVisibilityArea(entity);
        dataSetListEntity.setCreatedWhen(dslCreationTime);
        dataSetListEntity.setModifiedWhen(dslCreationTime);
        save(dataSetListEntity);
        return modelsProvider.getDataSetList(dataSetListEntity);
    }

    /**
     * Insert with name and ID.
     */
    public DataSetList insertDataSetList(UUID id, String name) {
        DataSetListEntity dataSetListEntity = new DataSetListEntity();
        dataSetListEntity.setName(name);
        dataSetListEntity.setVisibilityArea(entity);
        insert(dataSetListEntity, id);
        return modelsProvider.getDataSetList(dataSetListEntity);
    }

    public List<TestPlanEntity> testPlans() {
        return entity.getTestPlans();
    }

    public List<FilterEntity> getFilters() {
        return entity.getFilters();
    }

    @Override
    public void beforeRemove() {
        List<DataSetList> dataSetLists = getDataSetLists();
        for (DataSetList dataSetList : dataSetLists) {
            dataSetList.remove();
        }
    }

    /**
     * Get list of all DSL names from this va.
     * */
    public List<String> getDataSetListsNames() {
        List<String> resultList = entityManager.createNativeQuery(
                "select name from datasetlist where visibility_area_id = :va_id"
        ).setParameter("va_id", getId()).getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return Collections.emptyList();
        }
        return resultList;
    }
}

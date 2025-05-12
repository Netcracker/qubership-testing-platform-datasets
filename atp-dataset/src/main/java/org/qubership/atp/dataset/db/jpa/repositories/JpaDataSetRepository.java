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

package org.qubership.atp.dataset.db.jpa.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaDataSetRepository extends JpaRepository<DataSetEntity, UUID>, JpaDataSetRepositoryCustom,
        PagingAndSortingRepository<DataSetEntity, UUID> {

    List<DataSetEntity> findByDataSetList(DataSetListEntity dsl);

    Optional<DataSetEntity> findById(UUID id);

    @Query("select ds from DataSetEntity ds where ds.name LIKE %:name%")
    Page<DataSetEntity> findAllByNameContains(@Param("name") String name, Pageable pageable);

    @Query("select ds from DataSetEntity ds where ds.name LIKE %:name% and ds.dataSetList.id in :dsl_id")
    Optional<Page<DataSetEntity>> findAllByNameContainsAndDslIn(@Param("name") String name,
                                                                @Param("dsl_id") List<UUID> dsl, Pageable pageable);

    List<DataSetEntity> findByNameAndDataSetListId(String name, UUID dataSetList);

    List<DataSetEntity> findByDataSetListId(UUID dataSetListId);

    List<DataSetEntity> findByDataSetListIdIn(Collection<UUID> dataSetListIds);

    List<DataSetEntity> findBySourceIdAndDataSetListId(UUID sourceId, UUID datasetId);

    List<DataSetEntity> findByDataSetListIdAndLocked(UUID dataSetListId, Boolean isLocked);

    @Query(value = "select ds.name from DataSetEntity ds where ds.dataSetList.id = ?1")
    List<String> getDsNames(UUID datasetListId);

    @Query(value = "select d.dataSetList.id  from DataSetEntity d where d.id = ?1")
    Optional<UUID> getDslId(UUID datasetId);

    @Query(value = "select cast(datasetlist_id as varchar) from dataset where id = :ds_id", nativeQuery = true)
    UUID getDataSetsListIdByDataSetId(@Param("ds_id")UUID dsId);

    @Query(value = "select d.locked  from DataSetEntity d where d.id = ?1")
    boolean isLocked(UUID datasetId);
}

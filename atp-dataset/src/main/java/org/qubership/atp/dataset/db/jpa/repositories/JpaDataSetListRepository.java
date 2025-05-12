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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.VisibilityAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaDataSetListRepository extends JpaRepository<DataSetListEntity, UUID>,
        JpaDataSetListRepositoryCustom {

    List<DataSetListEntity> findByVisibilityArea(VisibilityAreaEntity va);

    List<DataSetListEntity> findByVisibilityAreaId(UUID visibilityArea);

    List<DataSetListEntity> findAllByVisibilityAreaId(UUID visibilityAreaId);

    Optional<DataSetListEntity> findById(UUID id);

    List<DataSetListEntity> getByNameAndVisibilityAreaId(String name, UUID visibilityArea);

    List<DataSetListEntity> getBySourceIdAndVisibilityAreaId(UUID sourceId, UUID visibilityArea);

    @Query(value = "select cast(id as varchar) from dataset where datasetlist_id = :dsl_id order by ordering",
            nativeQuery = true)
    LinkedList<UUID> getDataSetsIdsByDataSetListId(@Param("dsl_id") UUID dslId);

    @Query(value = "select distinct cast(datasetlist_id as varchar) "
            + "from \"attribute\" "
            + "where type_datasetlist_id in (:dsl_ids)",
            nativeQuery = true)
    LinkedList<UUID> getAffectedDataSetListIdsByDataSetListId(@Param("dsl_ids") List<UUID> dslIds);

    @Query(value = "select cast(id as varchar) "
            + "from dataset "
            + "where datasetlist_id in (:dsl_ids)",
            nativeQuery = true)
    Set<UUID> getAffectedDataSetIdsByDataSetListId(@Param("dsl_ids") Set<UUID> dslIds);

    @Query(value = "select distinct name from datasetlist dsl where (select count(*) from datasetlist  dsl2 where "
            + "dsl2.name = dsl.name  and dsl2.visibility_area_id = dsl.visibility_area_id) > 1 and dsl"
            + ".visibility_area_id  = ?1", nativeQuery = true)
    List<String> getNotUniqueDslNames(UUID visibilityArea);

    @Query(value = "select cast(id as varchar) "
            + "from datasetlist "
            + "where saga_session_id = :saga_session_id and visibility_area_id = :visibility_area_id",
            nativeQuery = true)
    Set<UUID> findAllIdsBySagaSessionIdAndVisibilityAreaId(@Param("saga_session_id") UUID sagaSessionId,
                                                           @Param("visibility_area_id") UUID visibilityAreaId);

}

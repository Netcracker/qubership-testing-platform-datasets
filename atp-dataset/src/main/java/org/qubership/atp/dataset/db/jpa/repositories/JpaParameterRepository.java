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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaParameterRepository extends JpaRepository<ParameterEntity, UUID> {
    Set<ParameterEntity> getByAttributeId(UUID attributeId);

    ParameterEntity getOneByAttributeId(UUID attributeId);

    Page<ParameterEntity> getByDataSetReferenceId(UUID dataSetId, Pageable pageable);

    @Query(value = "SELECT p FROM ParameterEntity p WHERE p.sourceId = ?1 AND p.dataSet.id = ?2")
    List<ParameterEntity> getBySourceIdAndDataSetId(UUID sourceId, UUID dataSetId);

    ParameterEntity getByAttributeIdAndDataSet_Id(UUID attributeId, UUID dataSetId);

    @Query(value = "SELECT p.id FROM ParameterEntity p WHERE  p.dataSetReferenceId = ?1")
    List<UUID> getParametersIdByDataSetReferenceId(UUID dsId);

    @Query(value = "select p.* from \"parameter\" p, \"attribute\" "
            + "a where p.dataset_id =?1 and p.attribute_id = a.id ORDER by a"
            + ".\"name\" ", nativeQuery = true)
    List<ParameterEntity> getByDataSetIdSorted(UUID dataSetId);

    @Query(value = "select p.* from parameter p, attribute a, attribute_key ak \n"
            + "where p.dataset_id =?1 \n"
            + "and p.attribute_id = ak.id \n"
            + "and a.id = ak.attribute_id \n"
            + "ORDER by a.name", nativeQuery = true)
    List<ParameterEntity> getOverlapByDataSetIdSorted(UUID dataSetId);

    @Query(value = "select distinct p.dataSet.id from ParameterEntity p where p.dataSetReferenceId = ?1")
    Set<UUID> getUniqueDataSetIdsByDataSetReferenceId(UUID dataSetId);

}

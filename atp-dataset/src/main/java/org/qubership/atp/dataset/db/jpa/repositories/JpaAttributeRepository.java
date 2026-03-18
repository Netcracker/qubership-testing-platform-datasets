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
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAttributeRepository extends JpaAbstractAttributeRepository<AttributeEntity> {

    List<AttributeEntity> getByNameAndDataSetListId(String name, UUID dataSetListId);

    List<AttributeEntity> getByDataSetListId(UUID dataSetListId);

    List<AttributeEntity> getBySourceIdAndDataSetListId(UUID sourceId, UUID dataSetListId);
    
    List<AttributeEntity> getByDataSetListIdIn(Collection<UUID> dataSetListIds);

    Page<AttributeEntity> getByTypeDataSetListId(UUID dataSetListId, Pageable pageable);

    @NativeQuery("select count(a.id)  from \"attribute\" a where datasetlist_id = (select d.datasetlist_id  from "
            + "dataset d where d.id = ?1)")
    int countAttributesByDataset(UUID datasetId);

    @NativeQuery("select * from \"attribute\" a "
            + "where a.datasetlist_id = ("
            + "select datasetlist_id from \"dataset\" d "
            + "where d.id = ?1) "
            + "and a.id not in ("
            + "select attribute_id from \"parameter\" p "
            + "where p.dataset_id = ?1)")
    List<AttributeEntity> getNotUsedByDatasetId(UUID datasetId);

    @NativeQuery("select * from \"attribute\" a where a.datasetlist_id = (select d.datasetlist_id from "
            + "dataset d where d.id = ?1)")
    List<AttributeEntity> getByDatasetId(UUID datasetId);

    @NativeQuery("""
            select\s
               (CASE WHEN leftAttr.type_datasetlist_id  != rightAttr.type_datasetlist_id\s
               and leftAttr.attribute_type_id = 4\s
               and leftAttr.attribute_type_id = rightAttr.attribute_type_id
                THEN true ELSE false END) AS is_equal
               from\s
               (select * from "attribute" a2 where id = ?1) as leftAttr,
                (select * from "attribute" a2 where id = ?2) as rightAttr""")
    boolean isDifferentDslAttributes(UUID leftAttrId, UUID rightAttrId);

    @NativeQuery(value = "SELECT * from \"attribute\" a WHERE a.datasetlist_id = ?1"
            + " AND a.attribute_type_id IN ?2 ORDER BY a.ordering",
            countQuery = "SELECT COUNT(*) FROM \"attribute\" a WHERE a.datasetlist_id = ?1 "
                    + "AND a.attribute_type_id IN ?2")
    Page<AttributeEntity> findByEntityAndTypeIds(UUID dslId, List<Long> typeIds, Pageable pageable);
}

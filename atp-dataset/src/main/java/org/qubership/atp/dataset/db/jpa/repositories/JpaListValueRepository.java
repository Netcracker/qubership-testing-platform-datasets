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
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaListValueRepository extends JpaRepository<ListValueEntity, UUID> {

    List<ListValueEntity> getByAttributeId(UUID id);

    ListValueEntity getById(UUID id);

    List<ListValueEntity> getByAttributeIdAndSourceId(UUID id, UUID sourceId);

    ListValueEntity getByAttributeIdAndText(UUID attributeId, String value);
}

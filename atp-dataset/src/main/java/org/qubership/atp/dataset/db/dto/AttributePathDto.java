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

package org.qubership.atp.dataset.db.dto;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.google.common.base.MoreObjects;

public class AttributePathDto {

    private final UUID id;
    private final UUID dataSetListId;
    private final UUID dataSetId;
    private final UUID targetAttributeId;
    private final List<UUID> attributePathIds;

    /**
     * DataTransferObject for attribute path fields.
     */
    public AttributePathDto(@Nonnull UUID id,
                            @Nonnull UUID dataSetListId,
                            @Nonnull UUID dataSetId,
                            @Nonnull UUID targetAttributeId,
                            @Nonnull List<UUID> attributePathIds) {
        this.id = id;
        this.dataSetListId = dataSetListId;
        this.dataSetId = dataSetId;
        this.targetAttributeId = targetAttributeId;
        this.attributePathIds = attributePathIds;
    }

    @Nonnull
    public UUID getTargetAttributeId() {
        return targetAttributeId;
    }

    @Nonnull
    public UUID getId() {
        return id;
    }

    @Nonnull
    public UUID getDataSetListId() {
        return dataSetListId;
    }

    @Nonnull
    public UUID getDataSetId() {
        return dataSetId;
    }

    @Nonnull
    public List<UUID> getAttributePathIds() {
        return attributePathIds;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("dataSetListId", dataSetListId)
                .add("dataSetId", dataSetId)
                .add("targetAttributeId", targetAttributeId)
                .add("attributePathIds", attributePathIds)
                .toString();
    }
}

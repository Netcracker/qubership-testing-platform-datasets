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

import java.util.UUID;

import javax.annotation.Nonnull;

import com.google.common.base.MoreObjects;

public class ParameterDto {

    private final UUID id;
    private final UUID dataSetId;
    private final UUID attributeId;
    private final ParameterDataDto parameterData;
    private boolean isFile;

    /**
     * DataTransferObject for parameter fields.
     */
    public ParameterDto(UUID id, UUID dataSetId, UUID attributeId, ParameterDataDto parameterData) {
        this.id = id;
        this.dataSetId = dataSetId;
        this.attributeId = attributeId;
        this.parameterData = parameterData;
        this.isFile = parameterData.isFile();
    }

    public UUID getId() {
        return id;
    }

    @Nonnull
    public UUID getDataSetId() {
        return dataSetId;
    }

    @Nonnull
    public UUID getAttributeId() {
        return attributeId;
    }

    @Nonnull
    public ParameterDataDto getParameterData() {
        return parameterData;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setIsFile(boolean isFile) {
        this.isFile = isFile;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("dataSetId", dataSetId)
                .add("attributeId", attributeId)
                .add("isFile", isFile)
                .add("parameterData", parameterData)
                .toString();
    }
}

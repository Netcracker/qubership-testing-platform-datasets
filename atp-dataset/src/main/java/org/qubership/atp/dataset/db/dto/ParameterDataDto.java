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

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

public class ParameterDataDto {

    private final String stringValue;
    private final UUID dataSetReference;
    private final UUID listValue;
    private boolean isFile = false;

    /**
     * DataTransferObject for parameter values. It's just wrapper for parameter.
     */
    public ParameterDataDto(String stringValue, UUID dataSetReference, UUID listValue) {
        this(stringValue, dataSetReference, listValue, false);
    }

    /**
     * DataTransferObject for parameter values. It's just wrapper for parameter.
     */
    public ParameterDataDto(String stringValue, UUID dataSetReference, UUID listValue, boolean isFile) {
        this.stringValue = stringValue;
        this.dataSetReference = dataSetReference;
        this.listValue = listValue;
        this.isFile = isFile;
    }

    @Nullable
    public String getStringValue() {
        return stringValue;
    }

    @Nullable
    public UUID getDataSetReferenceId() {
        return dataSetReference;
    }

    @Nullable
    public UUID getListValueId() {
        return listValue;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setIsFile(boolean file) {
        isFile = file;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("stringValue", stringValue)
                .add("dataSetReference", dataSetReference)
                .add("listValue", listValue)
                .toString();
    }
}

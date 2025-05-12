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

package org.qubership.atp.dataset.model.impl;

import java.sql.Timestamp;
import java.util.UUID;

import org.qubership.atp.dataset.model.CreatedModified;

public abstract class AbstractCreatedModified extends AbstractLabelProvider implements CreatedModified {

    protected Timestamp createdWhen;
    protected UUID createdBy;
    protected Timestamp modifiedWhen;
    protected UUID modifiedBy;

    @Override
    public Timestamp getCreatedWhen() {
        return createdWhen;
    }

    @Override
    public void setCreatedWhen(Timestamp createdWhen) {
        this.createdWhen = createdWhen;
    }

    @Override
    public UUID getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Timestamp getModifiedWhen() {
        return modifiedWhen;
    }

    @Override
    public void setModifiedWhen(Timestamp modifiedWhen) {
        this.modifiedWhen = modifiedWhen;
    }

    @Override
    public UUID getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(UUID modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}

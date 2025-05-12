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

import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.utils.Utils;

import com.google.common.base.MoreObjects;

public abstract class AbstractIdentified implements Identified {
    protected UUID id;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public boolean equals(Object obj) {
        return Utils.isEqual(this, obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getName() + '@' + Integer.toHexString(hashCode()))
                .add("id", getId())
                .toString();
    }
}

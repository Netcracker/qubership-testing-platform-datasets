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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.MixInId;

import com.google.common.base.MoreObjects;

public class MixInIdImpl implements MixInId {

    private UUID id;
    private String metadata;

    public MixInIdImpl(@Nonnull UUID id) {
        this(id, null);
    }

    public MixInIdImpl(@Nonnull UUID id, @Nullable String metadata) {
        this.id = id;
        this.metadata = metadata;
    }

    @Nonnull
    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(@Nonnull UUID id) {
        this.id = id;
    }

    @Nullable
    @Override
    public String getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(@Nullable String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("metadata", metadata)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MixInIdImpl)) {
            return false;
        }
        MixInIdImpl mixInId = (MixInIdImpl) o;
        return Objects.equals(id, mixInId.id)
                && Objects.equals(getMetadata(), mixInId.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getMetadata());
    }
}

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

package org.qubership.atp.dataset.model;

import static org.qubership.atp.dataset.service.rest.MixInIdSerializer.CHARSET;

import java.util.Base64;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.impl.MixInIdImpl;
import org.qubership.atp.dataset.service.rest.MixInIdSerializer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = MixInIdSerializer.class)
public interface MixInId {

    /**
     * Deserialize MixInId from string.
     */
    @JsonCreator
    static MixInId fromString(@Nonnull String value) {
        if (value.length() < 36) {
            throw new RuntimeException("Can not parse uuid from " + value);
        }
        UUID uuid = UUID.fromString(value.substring(0, 36));
        String metadata = null;
        if (value.length() > 36) {
            metadata = value.substring(36);
            metadata = new String(Base64.getUrlDecoder().decode(metadata), CHARSET);
        }
        return new MixInIdImpl(uuid, metadata);
    }

    /**
     * Serializes MixInId to string.
     */
    @Nonnull
    default String serialize() {
        String metadata = "";
        if (getMetadata() != null) {
            metadata = Base64.getUrlEncoder().encodeToString(getMetadata().getBytes(CHARSET));
        }
        return getUuid() + metadata;
    }

    @Nonnull
    UUID getUuid();

    void setUuid(@Nonnull UUID id);

    @Nullable
    String getMetadata();

    void setMetadata(@Nullable String metadata);
}

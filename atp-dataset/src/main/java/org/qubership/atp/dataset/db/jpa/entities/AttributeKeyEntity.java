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

package org.qubership.atp.dataset.db.jpa.entities;

import java.io.Serial;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "attribute_key")
public class AttributeKeyEntity extends AbstractAttributeEntity {
    @Serial
    private static final long serialVersionUID = 9009718864988359127L;

    @Column(name = "key")
    private String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    public DataSetEntity dataSet;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id")
    public AttributeEntity attribute;

    @Override
    public boolean isAttributeKey() {
        return true;
    }

    @Override
    public UUID getOriginAttributeId() {
        return attribute.getId();
    }

    @Override
    public String getName() {
        return attribute.getName();
    }

    /**
     * Path as list of UUIDs.
     * */
    @Transient
    public List<UUID> getPath() {
        List<UUID> result = new LinkedList<>();
        String[] splitResult = key.split("_");
        for (String splitPart : splitResult) {
            result.add(UUID.fromString(splitPart));
        }
        return result;
    }
}

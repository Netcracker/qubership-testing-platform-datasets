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

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public abstract class AbstractUuidBasedEntity implements Serializable {
    @Id
    @Getter
    @Setter
    @Type(type = "pg-uuid")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid DEFAULT uuid_generate_v4()")
    @GeneratedValue(
            strategy = GenerationType.TABLE,
            generator = "pg-uuid"
    )
    @GenericGenerator(
            name = "pg-uuid",
            strategy = "uuid2",
            parameters = @org.hibernate.annotations.Parameter(
                    name = "uuid_gen_strategy_class",
                    value = "org.qubership.atp.dataset.db.jpa.PostgreSqlUuidGenerationStrategy"
            )
    )
    private UUID id;
    /*
    To resolve deletion conflicts. DS removal and attribute removal both
    lead to parameter deletion. If both events happen in single transaction
    there can be conflict, when we trying to delete already deleted parameter.
     */
    @Getter
    @Setter
    @Transient
    private boolean isRemoved = false;

    @Override
    public boolean equals(Object other) {
        if (other instanceof AttributeKeyEntity) {
            AttributeKeyEntity otherEntity = (AttributeKeyEntity) other;
            return otherEntity.getId().equals(getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }
}

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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "attribute")
public class AttributeEntity extends AbstractAttributeEntity {
    private static final long serialVersionUID = -7352787552599934981L;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering")
    private Integer ordering;

    @Column(name = "attribute_type_id")
    private Long attributeTypeId;

    @Column(name = "type_datasetlist_id")
    private UUID typeDataSetListId;

    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListValueEntity> listValues = new LinkedList<>();

    @OneToMany(mappedBy = "attribute", fetch = FetchType.LAZY)
    private Set<ParameterEntity> parameters = new HashSet<>();

    @Override
    public boolean isAttributeKey() {
        return false;
    }

    @Override
    public UUID getOriginAttributeId() {
        return super.getId();
    }
}

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
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "label")
public class LabelEntity extends AbstractUuidBasedEntity {
    @Column(name = "name")
    private String name;

    @ManyToMany(mappedBy = "labels", fetch = FetchType.LAZY)
    private Set<DataSetListEntity> dataSetLists = new HashSet<>();

    @ManyToMany(mappedBy = "labels", fetch = FetchType.LAZY)
    private Set<DataSetEntity> dataSets = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "filter_ds_labels",
            joinColumns = @JoinColumn(name = "ds_label_id"),
            inverseJoinColumns = @JoinColumn(name = "filter_id")
    )
    private Set<FilterEntity> dataSetLabelFilters = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "filter_dsl_labels",
            joinColumns = @JoinColumn(name = "dsl_label_id"),
            inverseJoinColumns = @JoinColumn(name = "filter_id")
    )
    private Set<FilterEntity> dataSetListLabelFilters = new HashSet<>();
}

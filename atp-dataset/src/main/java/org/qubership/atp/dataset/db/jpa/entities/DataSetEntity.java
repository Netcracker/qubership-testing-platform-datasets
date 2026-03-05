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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "dataset")
public class DataSetEntity extends AbstractUuidBasedEntity {

    @Serial
    private static final long serialVersionUID = 137890563796237611L;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering", nullable = false)
    private Long ordering;

    @Column(name = "source_id")
    private UUID sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasetlist_id")
    private DataSetListEntity dataSetList;

    @OneToMany(mappedBy = "dataSet", fetch = FetchType.LAZY)
    private List<ParameterEntity> parameters = new LinkedList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "dataset_label",
            joinColumns = @JoinColumn(name = "dataset_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private List<LabelEntity> labels = new LinkedList<>();

    @Column(name = "locked", nullable = false, columnDefinition = "boolean DEFAULT false")
    private boolean locked;
}

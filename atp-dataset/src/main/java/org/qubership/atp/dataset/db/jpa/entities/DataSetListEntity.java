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
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "datasetlist")
public class DataSetListEntity extends AbstractUuidBasedEntity {

    @Serial
    private static final long serialVersionUID = 1558849568530137446L;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visibility_area_id")
    private VisibilityAreaEntity visibilityArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_plan_id")
    private TestPlanEntity testPlan;

    @Column(name = "source_id")
    private UUID sourceId;

    @OneToMany(mappedBy = "dataSetList", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<AttributeKeyEntity> attributeKeys = new HashSet<>();

    @OrderBy("ordering ASC")
    @OneToMany(mappedBy = "dataSetList", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<AttributeEntity> attributes = new LinkedList<>();

    @OrderBy("ordering ASC")
    @OneToMany(mappedBy = "dataSetList", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<DataSetEntity> dataSets = new LinkedList<>();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_when")
    private Timestamp createdWhen;

    @Column(name = "modified_by")
    private UUID modifiedBy;

    @Column(name = "modified_when")
    private Timestamp modifiedWhen;

    @ManyToMany
    @JoinTable(
            name = "datasetlist_label",
            joinColumns = @JoinColumn(name = "datasetlist_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private List<LabelEntity> labels = new LinkedList<>();

    @Column(name = "saga_session_id")
    private UUID sagaSessionId;
}

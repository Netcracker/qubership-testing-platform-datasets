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

package org.qubership.atp.dataset.versioning.service.changes;

import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.annotation.Entity;
import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;
import org.qubership.atp.dataset.versioning.model.domain.AttributeSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.DataSetListSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.DataSetSnapshot;

import lombok.Getter;

@Getter
@Entity
@TypeName("DataSetList")
public class DataSetListJComparable {
    @Id
    private UUID id;
    private String name;
    private UUID visibilityAreaId;
    private final List<DataSetSnapshot> dataSets;
    private final List<AttributeSnapshot> attributes;

    /**
     * Constructor.
     */
    public DataSetListJComparable(DataSetListSnapshot javersModel) {
        id = javersModel.getId();
        name = javersModel.getName();
        visibilityAreaId = javersModel.getVisibilityAreaId();
        dataSets = javersModel.getDataSets();
        attributes = javersModel.getAttributes();
    }
}

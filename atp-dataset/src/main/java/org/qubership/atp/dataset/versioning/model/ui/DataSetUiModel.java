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

package org.qubership.atp.dataset.versioning.model.ui;

import java.util.UUID;

import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.versioning.model.domain.DataSetSnapshot;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@Value
public class DataSetUiModel {

    private UUID id;
    private String name;

    /**
     * Constructor.
     */
    public DataSetUiModel(DataSetSnapshot snapshot, ModelsProvider modelsProvider) {
        id = snapshot.getId();
        name = snapshot.getName();
    }
}

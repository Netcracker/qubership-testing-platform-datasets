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

package org.qubership.atp.dataset.service.jpa.model;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.VisibilityArea;

import lombok.Data;

@Data
public class VisibilityAreaFlatModel {
    private UUID id;
    private String name;
    private List<UUID> dataSetLists = new LinkedList<>();

    /**
     * Generates from JPA model.
     * */
    public VisibilityAreaFlatModel(VisibilityArea visibilityArea) {
        this.id = visibilityArea.getId();
        this.name = visibilityArea.getName();
        dataSetLists = visibilityArea.getDataSetListIds();
    }

    public VisibilityAreaFlatModel() {
    }
}

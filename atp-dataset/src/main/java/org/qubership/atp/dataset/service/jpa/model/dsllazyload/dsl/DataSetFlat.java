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

package org.qubership.atp.dataset.service.jpa.model.dsllazyload.dsl;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Label;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Used for top level DSL DataSet representation.
 * */
@Getter
@Setter
public class DataSetFlat {
    private UUID id;
    private String name;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LabelFlat> labels = new LinkedList<>();

    /**
     * Default constructor.
     * */
    public DataSetFlat(DataSet dataSet) {
        id = dataSet.getId();
        name = dataSet.getName();
        for (Label label : dataSet.getLabels()) {
            labels.add(new LabelFlat(label.getId(), label.getName()));
        }
    }
}

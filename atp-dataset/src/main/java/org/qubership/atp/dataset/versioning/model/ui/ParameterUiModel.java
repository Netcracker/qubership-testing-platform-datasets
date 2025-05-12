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
import org.qubership.atp.dataset.versioning.model.domain.ParameterSnapshot;

import com.fasterxml.jackson.annotation.JsonInclude;
import joptsimple.internal.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@Value
public class ParameterUiModel {
    private UUID dataSet;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object value;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object valueRef;
    private boolean isOverlap;

    /**
     * Constructor from converted UI-model.
     */
    public ParameterUiModel(
            ParameterSnapshot snapShot,
            ModelsProvider modelsProvider,
            boolean isOverlap
    ) {
        this.dataSet = snapShot.getDataSetId();
        if (!Strings.isNullOrEmpty(snapShot.getText())) {
            value = snapShot.getValuePretty(modelsProvider);
        } else if (snapShot.getListValueId() != null) {
            value = snapShot.getListValueName();
        } else if (snapShot.getFileData() != null) {
            value = snapShot.getFileData().getFileName();
        }
        if (snapShot.getDataSetReference() != null) {
            valueRef = snapShot.getDataSetReference();
        }
        this.isOverlap = isOverlap;
    }
}

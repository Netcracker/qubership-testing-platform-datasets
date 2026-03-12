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
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "parameter")
public class ParameterEntity extends AbstractUuidBasedEntity {
    @Serial
    private static final long serialVersionUID = 5436974377291909573L;

    @Column(name = "string")
    private String stringValue;

    @Column(name = "file")
    private UUID fileValueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private DataSetEntity dataSet;

    @Column(name = "source_id")
    private UUID sourceId;

    @Transient
    public UUID getDataSetId() {
        return dataSet.getId();
    }

    @Column(name = "ds")
    private UUID dataSetReferenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id")
    private AbstractAttributeEntity attribute;

    @ManyToOne (fetch = FetchType.EAGER)
    @JoinColumn(name = "list")
    public ListValueEntity listValue;

    /**
     * Returns value as string from string, file, list or dsl fields, depending on attribute type.
     * */
    public String getParameterValueByType() {
        if (stringValue != null) {
            return stringValue;
        } else if (listValue != null) {
            return listValue.getText();
        } else if (dataSetReferenceId != null) {
            return dataSetReferenceId.toString();
        } else if (fileValueId != null) {
            return getId().toString();
        }
        return null;
    }
}

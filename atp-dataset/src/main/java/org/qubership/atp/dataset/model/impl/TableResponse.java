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

package org.qubership.atp.dataset.model.impl;

import static java.util.Objects.nonNull;

import java.util.UUID;

import org.qubership.atp.dataset.db.generated.QAttribute;
import org.qubership.atp.dataset.db.generated.QDataset;
import org.qubership.atp.dataset.db.generated.QDatasetlist;
import org.qubership.atp.dataset.db.generated.QParameter;
import org.qubership.atp.dataset.db.jpa.entities.AbstractAttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.ListValueEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.model.ParameterOverlap;

import com.querydsl.core.Tuple;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableResponse {

    private static final QParameter PARAM = new QParameter("PARAM");
    protected static final QDatasetlist DSL = new QDatasetlist("DSL");
    protected static final QDataset DS = new QDataset("DS");
    protected static final QAttribute ATTR = new QAttribute("ATTR");

    private UUID id;
    private UUID dslId;
    private String dslName;
    private UUID dsId;
    private String dsName;
    private UUID attributeId;
    private String attributeName;
    private UUID listValueId;
    private UUID ds;

    /**
     * Transform parameter overlap to update parameter response dto.
     */
    public static TableResponse fromParameterOverlap(ParameterOverlap overlap) {
        return new TableResponse(
                null,
                overlap.getDataSet().getDataSetList().getId(),
                overlap.getDataSet().getDataSetList().getName(),
                overlap.getDataSet().getId(),
                overlap.getDataSet().getName(),
                overlap.getAttribute().getId(),
                overlap.getAttribute().getName(),
                null,
                null
        );
    }

    /**
     * Transform row to update parameter response dto.
     */
    public static TableResponse fromParameterTuple(Tuple row) {
        return new TableResponse(
                row.get(PARAM.id),
                row.get(DSL.id),
                row.get(DSL.name),
                row.get(DS.id),
                row.get(DS.name),
                row.get(ATTR.id),
                row.get(ATTR.name),
                row.get(PARAM.list),
                row.get(PARAM.ds)
        );
    }

    /**
     * Map attribute to table response.
     *
     * @param entity attribute
     * @return table response
     */
    public static TableResponse fromAttributeEntity(AttributeEntity entity) {
        DataSetListEntity dataSetList = entity.getDataSetList();
        boolean isDslPresent = nonNull(dataSetList);

        return new TableResponse(
                null,
                isDslPresent ? dataSetList.getId() : null,
                isDslPresent ? dataSetList.getName() : null,
                null,
                null,
                entity.getId(),
                entity.getName(),
                null,
                null
        );
    }

    /**
     * Map parameter to table response.
     *
     * @param entity parameter
     * @return table response
     */
    public static TableResponse fromParameterEntity(ParameterEntity entity) {
        DataSetEntity dataSet = entity.getDataSet();
        boolean isDsPresent = nonNull(dataSet);

        DataSetListEntity dataSetList = isDsPresent ? dataSet.getDataSetList() : null;
        boolean isDslPresent = isDsPresent && nonNull(dataSetList);

        AbstractAttributeEntity attribute = entity.getAttribute();
        boolean isAttrPresent = nonNull(attribute);

        ListValueEntity listValue = entity.getListValue();
        boolean isListValuePresent = nonNull(listValue);

        return new TableResponse(
                entity.getId(),
                isDslPresent ? dataSetList.getId() : null,
                isDslPresent ? dataSetList.getName() : null,
                isDsPresent ? dataSet.getId() : null,
                isDsPresent ? dataSet.getName() : null,
                isAttrPresent ? attribute.getId() : null,
                isAttrPresent ? attribute.getName() : null,
                isListValuePresent ? listValue.getId() : null,
                isDsPresent ? entity.getDataSetReferenceId() : null
        );
    }
}

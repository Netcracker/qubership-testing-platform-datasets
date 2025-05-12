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

package org.qubership.atp.dataset.service.jpa.model.dscontext;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.Label;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.tree.OverlapNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataSetContext implements Serializable {
    private static final long serialVersionUID = -7846470332343480118L;
    private UUID id;
    private String name;
    private List<UUID> parentPath;
    private List<ParameterContext> parameters = new LinkedList<>();
    private Map<UUID, String> labels = new LinkedHashMap<>();
    @Getter
    private int columnNumber;

    /**
     * New Data Set context. Creates full hierarchy. By Data Set.
     * */
    public DataSetContext(
            int dataSetColumnNumber,
            DataSet dataSet,
            List<Attribute> parentAttributes,
            OverlapNode overlaps,
            List<UUID> parentPath,
            List<UUID> pathRestrictions
    ) {
        this.parentPath = parentPath;
        this.columnNumber = dataSetColumnNumber;
        id = dataSet.getId();
        name = dataSet.getName();
        if (pathRestrictions != null && !pathRestrictions.isEmpty()) {
            return;
        }
        for (Label label : dataSet.getLabels()) {
            labels.put(label.getId(), label.getName());
        }
        for (Attribute parentTextAttribute : parentAttributes) {
            boolean valueIsEmpty = true;
            List<Parameter> parametersModels = dataSet.getParameters();
            for (Parameter parameterModel : parametersModels) {
                boolean overlapped = false;
                if (overlaps != null) {
                    AttributeKey overlap = overlaps.getOverlap(
                            parentPath,
                            dataSetColumnNumber,
                            parentTextAttribute.getId()
                    );
                    if (overlap != null) {
                        parameterModel = overlap.getParameter();
                        overlapped = overlap.getDataSetList().getId().equals(overlaps.getDataSetListId());
                    }
                }
                if (parameterModel.getAttributeId().equals(parentTextAttribute.getId())) {
                    parameters.add(new ParameterContext(parentTextAttribute, parameterModel, overlapped));
                    valueIsEmpty = false;
                    break;
                }
            }
            if (valueIsEmpty) {
                if (overlaps != null) {
                    AttributeKey overlap = overlaps.getOverlap(
                            parentPath,
                            dataSetColumnNumber,
                            parentTextAttribute.getId()
                    );
                    if (overlap != null) {
                        boolean overlapped = overlap.getDataSetList().getId().equals(overlaps.getDataSetListId());
                        parameters.add(new ParameterContext(parentTextAttribute, overlap.getParameter(), overlapped));
                    } else {
                        parameters.add(
                                new ParameterContext(parentTextAttribute)
                        );
                    }
                } else {
                    parameters.add(new ParameterContext(parentTextAttribute));
                }
            }
        }
    }

    /**
     * New Data Set context. Empty.
     * */
    public DataSetContext(
            int dataSetColumnNumber,
            List<Attribute> parentAttributes,
            OverlapNode overlaps,
            List<UUID> parentPath,
            List<UUID> pathRestrictions
    ) {
        this.columnNumber = dataSetColumnNumber;
        this.parentPath = parentPath;
        id = null;
        name = "";
        if (pathRestrictions != null && !pathRestrictions.isEmpty()) {
            return;
        }
        for (Attribute parentTextAttribute : parentAttributes) {
            if (overlaps != null) {
                AttributeKey overlap = overlaps.getOverlap(
                        parentPath,
                        dataSetColumnNumber,
                        parentTextAttribute.getId()
                );
                if (overlap != null) {
                    parameters.add(new ParameterContext(parentTextAttribute, overlap.getParameter(), true));
                } else {
                    parameters.add(new ParameterContext(parentTextAttribute));
                }
            }
        }
    }
}

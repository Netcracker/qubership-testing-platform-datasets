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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.tree.OverlapNode;

/**
 * Used for REF_THIS macro. Get already created DS (actually DSL) context
 * and extracts value by path.
 * */
public class DataSetListParameterExtractor {
    private DataSetList dataSetList;
    private PathStep dataSetPath;
    private List<PathStep> referenceAttributePath;
    private PathStep attributePath;
    private static final AttributeTypeName[] attributesToLoad = {
            AttributeTypeName.TEXT,
            AttributeTypeName.ENCRYPTED,
            AttributeTypeName.LIST
    };

    /**
     * Default constructor.
     * */
    public DataSetListParameterExtractor(DataSetList dataSetList,
                                         PathStep dataSetPath,
                                         List<PathStep> referenceAttributePath,
                                         PathStep attributePath) {
        this.dataSetList = dataSetList;
        this.dataSetPath = dataSetPath;
        this.referenceAttributePath = referenceAttributePath;
        this.attributePath = attributePath;
    }

    /**
     * Extracts and returns found value.
     * */
    public String extract() throws DataSetServiceException {
        DataSet dataSet;
        if (dataSetPath.getId() != null) {
            dataSet = dataSetList.getDataSetById(dataSetPath.getId());
        } else {
            dataSet = dataSetList.getDataSetByName(dataSetPath.getName());
        }
        if (dataSet == null) {
            throw new DataSetServiceException("Wrong data set");
        }
        if (referenceAttributePath.isEmpty()) {
            List<Attribute> attributes = dataSetList.getAttributesByTypes(Arrays.asList(attributesToLoad));
            for (Attribute attribute : attributes) {
                if (attributePath.matches(attribute.getName(), attribute.getId())) {
                    return getStringValue(dataSet, attribute);
                }
            }
        } else {
            PathStep searchingPath = referenceAttributePath.get(0);
            List<Attribute> dataSetListReferences = dataSetList.getDataSetListReferences();
            OverlapNode rootOverlapNode = new OverlapNode(null, dataSetList.getId(), 1);
            List<AttributeKey> overlaps = dataSetList.getAttributeKeysByDataSet(dataSet.getId());
            for (AttributeKey overlap : overlaps) {
                rootOverlapNode.addOverlap(overlap.getPath(), 0, overlap);
            }
            for (Attribute dataSetListReference : dataSetListReferences) {
                if (searchingPath.matches(dataSetListReference.getName(), dataSetListReference.getId())) {
                    List<UUID> currentPath = new LinkedList<>();
                    currentPath.add(dataSetListReference.getId());
                    return getParameterFromGroup(
                            dataSet,
                            dataSetListReference,
                            rootOverlapNode,
                            currentPath,
                            referenceAttributePath.subList(1, referenceAttributePath.size()),
                            attributePath
                    );
                }
            }
        }
        throw new DataSetServiceException("Parameter not found");
    }

    private String getStringValue(DataSet dataSet, Attribute attribute) {
        Parameter parameter = null;
        if (dataSet != null) {
            parameter = dataSet.getParameterByAttributeId(attribute.getId());
        }
        if (parameter == null) {
            return null;
        }
        return parameter.getParameterValueByType();
    }

    private String getParameterFromGroup(DataSet previousDataSet,
                                         Attribute dataSetListReference,
                                         OverlapNode rootOverlapNode,
                                         List<UUID> currentPath,
                                         List<PathStep> referenceAttributePath,
                                         PathStep attributePath) throws DataSetServiceException {
        DataSet thisLevelDataSet = null;
        DataSetList thisLevelDataSetList = dataSetListReference.getTypeDataSetList();
        AttributeKey overlap = rootOverlapNode.getOverlap(
                currentPath,
                0,
                dataSetListReference.getId()
        );
        if (overlap != null) {
            thisLevelDataSet = overlap.getParameter().getDataSetReferenceValue();
        }
        if (thisLevelDataSet == null && previousDataSet != null) {
            List<Parameter> parameters = dataSetListReference.getParameters();
            for (Parameter parameter : parameters) {
                UUID dataSetReferenceId = parameter.getDataSetReferenceId();
                if (dataSetReferenceId.equals(previousDataSet.getId())) {
                    thisLevelDataSet = parameter.getDataSet();
                    break;
                }
            }
        }
        if (referenceAttributePath.isEmpty()) {
            List<Attribute> attributes = thisLevelDataSetList.getAttributesByTypes(Arrays.asList(attributesToLoad));
            for (Attribute attribute : attributes) {
                if (attributePath.matches(attribute.getName(), attribute.getId())) {
                    AttributeKey parameterOverlap = rootOverlapNode.getOverlap(
                            currentPath,
                            0,
                            attribute.getId()
                    );
                    if (parameterOverlap != null) {
                        return parameterOverlap.getParameter().getParameterValueByType();
                    }
                    return getStringValue(thisLevelDataSet, attribute);
                }
            }
        } else {
            PathStep searchingPath = referenceAttributePath.get(0);
            List<Attribute> references = thisLevelDataSetList.getDataSetListReferences();
            for (Attribute reference : references) {
                if (searchingPath.matches(reference.getName(), reference.getId())) {
                    if (thisLevelDataSet != null) {
                        dataSetList.getAttributeKeysByDataSet(thisLevelDataSet.getId()).forEach(
                            attributeKey -> rootOverlapNode.addOverlap(attributeKey.getPath(), 0, attributeKey)
                        );
                    }
                    currentPath.add(reference.getId());
                    return getParameterFromGroup(
                            thisLevelDataSet,
                            reference,
                            rootOverlapNode,
                            currentPath,
                            referenceAttributePath.subList(1, referenceAttributePath.size()),
                            attributePath
                    );
                }
            }
        }
        throw new DataSetServiceException("Parameter not found");
    }
}

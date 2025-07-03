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
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.tree.OverlapNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupContext implements Serializable {
    private static final long serialVersionUID = -7833021868343081645L;
    private long order;
    private String name;
    private UUID id;
    private UUID dataSetListId;
    private String dataSetListName;
    private List<UUID> currentPath;
    private List<DataSetContext> dataSets = new LinkedList<>();
    private List<GroupContext> groups = new LinkedList<>();
    private List<Attribute> attributes;

    @JsonIgnore
    private boolean isLastPage = false;


    /**
     * Group context with hierarchy.
     */
    public GroupContext(List<DataSetContext> previousLevelDataSets,
                        Attribute parentDataSetListReferenceAttribute,
                        OverlapNode overlaps,
                        List<UUID> parentPath,
                        List<AttributeTypeName> attributeTypeNames,
                        CycleChecker cycleChecker,
                        List<UUID> pathRestrictions,
                        UUID dataSetListId, Pageable pageable) {
        currentPath = new LinkedList<>(parentPath);
        currentPath.add(parentDataSetListReferenceAttribute.getId());
        name = parentDataSetListReferenceAttribute.getName();
        order = parentDataSetListReferenceAttribute.getOrdering();
        id = parentDataSetListReferenceAttribute.getId();
        DataSetList referencedDataSetList = parentDataSetListReferenceAttribute.getTypeDataSetList();

        List<Attribute> attributeList;

        if (pageable != null) {
            Page<AttributeEntity> attributesByTypesPageable =
                    referencedDataSetList.getAttributesByTypesPageable(dataSetListId, attributeTypeNames, pageable);
            attributeList = referencedDataSetList.getAttributesOfPage(attributesByTypesPageable, attributeTypeNames);
            this.isLastPage = attributesByTypesPageable.isLast();

        } else {
            attributeList = referencedDataSetList.getAttributesByTypes(attributeTypeNames);
        }
        attributes = attributeList;
        this.dataSetListId = dataSetListId;
        this.dataSetListName = referencedDataSetList.getName();
        List<Parameter> dataSetReferences = parentDataSetListReferenceAttribute.getParameters();
        for (DataSetContext previousLevelDataSet : previousLevelDataSets) {
            if (previousLevelDataSet.getName() == null) {
                dataSets.add(
                        new DataSetContext(
                                previousLevelDataSet.getColumnNumber(),
                                attributeList,
                                overlaps,
                                currentPath,
                                pathRestrictions
                        )
                );
                continue;
            }
            boolean referenceFound = false;
            Parameter foundDataSetReference = null;
            for (Parameter dataSetReference : dataSetReferences) {
                if (dataSetReference.getDataSet().getId().equals(previousLevelDataSet.getId())) {
                    AttributeKey overlap = overlaps.getOverlap(
                            parentPath,
                            previousLevelDataSet.getColumnNumber(),
                            parentDataSetListReferenceAttribute.getId()
                    );
                    if (overlap != null) {
                        foundDataSetReference = overlap.getParameter();
                    } else {
                        foundDataSetReference = dataSetReference;
                    }
                    referenceFound = true;
                    break;
                }
            }
            if (!referenceFound) {
                AttributeKey overlap = overlaps.getOverlap(
                        parentPath,
                        previousLevelDataSet.getColumnNumber(),
                        parentDataSetListReferenceAttribute.getId()
                );
                if (overlap != null) {
                    foundDataSetReference = overlap.getParameter();
                }
            }
            if (foundDataSetReference != null && foundDataSetReference.getDataSetReferenceValue() != null) {
                DataSet referencedDataSet = foundDataSetReference.getDataSetReferenceValue();
                dataSets.add(
                        new DataSetContext(
                                previousLevelDataSet.getColumnNumber(),
                                referencedDataSet,
                                attributeList,
                                overlaps,
                                currentPath,
                                pathRestrictions
                        )
                );
            } else {
                dataSets.add(
                        new DataSetContext(
                                previousLevelDataSet.getColumnNumber(),
                                attributeList,
                                overlaps,
                                currentPath,
                                pathRestrictions
                        )
                );
            }
        }
        List<Attribute> dataSetListReferenceAttribute = referencedDataSetList.getDataSetListReferences();
        for (Attribute dataSetListAttribute : dataSetListReferenceAttribute) {
            DataSetList dataSetList = dataSetListAttribute.getDataSetList();
            for (DataSetContext currentLevelDataSet : dataSets) {
                if (currentLevelDataSet.getId() != null) {
                    List<AttributeKey> attributeKeys = dataSetList.getAttributeKeysByDataSet(
                            currentLevelDataSet.getId()
                    );
                    for (AttributeKey attributeKey : attributeKeys) {
                        List<UUID> overlapPath = new LinkedList<>(currentPath);
                        overlapPath.addAll(attributeKey.getPath());
                        overlaps.addOverlap(overlapPath, currentLevelDataSet.getColumnNumber(), attributeKey);
                    }
                }
            }
            if (pathRestrictions == null) {
                UUID referencedDataSetListId = dataSetListAttribute.getTypeDataSetList().getId();
                cycleChecker.openNode(referencedDataSetListId);
                groups.add(
                        new GroupContext(
                                dataSets,
                                dataSetListAttribute,
                                overlaps,
                                currentPath,
                                attributeTypeNames,
                                cycleChecker,
                                null,
                                referencedDataSetListId,
                                pageable
                        )
                );
                cycleChecker.closeNode(referencedDataSetListId);
            } else {
                if (!pathRestrictions.isEmpty() && pathRestrictions.get(0).equals(dataSetListAttribute.getId())) {
                    UUID referencedDataSetListId = dataSetListAttribute.getTypeDataSetList().getId();
                    cycleChecker.openNode(referencedDataSetListId);
                    groups.add(
                            new GroupContext(
                                    dataSets,
                                    dataSetListAttribute,
                                    overlaps,
                                    currentPath,
                                    attributeTypeNames,
                                    cycleChecker,
                                    pathRestrictions.subList(1, pathRestrictions.size()),
                                    referencedDataSetListId,
                                    pageable
                            )
                    );
                    cycleChecker.closeNode(referencedDataSetListId);
                    break;
                }
            }
        }
    }
}

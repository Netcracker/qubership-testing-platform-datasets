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

import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.AttributeKey;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.tree.OverlapNode;
import org.springframework.data.domain.Pageable;

import lombok.Getter;
import lombok.Setter;

/**
 * Root class for DS (DSL) context. It's entities contains maximum context info and used in multiple cases.
 * */
@Getter
@Setter
public class DataSetListContext implements Serializable {
    private static final long serialVersionUID = 8928150301939110223L;
    private UUID visibilityAreaId;
    private UUID dataSetListId;
    private String dataSetListName;
    private List<DataSetContext> dataSets = new LinkedList<>();
    private List<GroupContext> groups = new LinkedList<>();
    private List<Integer> loadedColumns = new LinkedList<>();
    private List<AttributeTypeName> loadedAttributes = new LinkedList<>();
    private OverlapNode rootOverlapNode;
    @Setter
    private List<UUID> pathRestrictions = null;

    public DataSetListContext(UUID dataSetListId) {
        this.dataSetListId = dataSetListId;
    }

    /**
     * New DSL context with hierarchy.
     * */
    public DataSetListContext(DataSetList dataSetList,
                              List<Integer> dataSetColumns,
                              List<AttributeTypeName> attributeTypeNames,
                              List<UUID> pathRestrictions) {
        loadedAttributes.addAll(attributeTypeNames);
        dataSetListId = dataSetList.getId();
        dataSetListName = dataSetList.getName();
        visibilityAreaId = dataSetList.getVisibilityArea().getId();
        loadedColumns.addAll(dataSetColumns);
        this.pathRestrictions = pathRestrictions;
        makeContext(dataSetList, dataSetColumns, attributeTypeNames, null);
    }

    /**
     * New DSL context with hierarchy.
     */
    public DataSetListContext(DataSetList dataSetList,
                              List<Integer> dataSetColumns,
                              List<AttributeTypeName> attributeTypeNames,
                              List<UUID> pathRestrictions,
                              Pageable pageable) {
        loadedAttributes.addAll(attributeTypeNames);
        dataSetListId = dataSetList.getId();
        dataSetListName = dataSetList.getName();
        visibilityAreaId = dataSetList.getVisibilityArea().getId();
        loadedColumns.addAll(dataSetColumns);
        this.pathRestrictions = pathRestrictions;
        makeContext(dataSetList, dataSetColumns, attributeTypeNames, pageable);
    }

    private void makeContext(
            DataSetList dataSetList, List<Integer> dataSetColumns, List<AttributeTypeName> attributeTypeNames,
            Pageable pageable) {
        CycleChecker cycleChecker = new CycleChecker();
        cycleChecker.openNode(dataSetList.getId());
        List<Attribute> contextAttributes = dataSetList.getAttributesByTypes(attributeTypeNames);
        rootOverlapNode = new OverlapNode(null, dataSetList.getId(), dataSetList.getDataSetsCount());
        for (Integer dataSetColumn : dataSetColumns) {
            DataSet rootDataSet = dataSetList.getDataSetByColumn(dataSetColumn);
            DataSetContext dataSetContext = new DataSetContext(
                    dataSetColumn, rootDataSet, contextAttributes, null, null, pathRestrictions
            );
            dataSets.add(dataSetContext);
            List<AttributeKey> attributeKeysByDataSet = dataSetList.getAttributeKeysByDataSet(rootDataSet.getId());
            for (AttributeKey overlap : attributeKeysByDataSet) {
                rootOverlapNode.addOverlap(overlap.getPath(), dataSetColumn, overlap);
            }
        }
        List<Attribute> dataSetListAttributes = dataSetList.getDataSetListReferences();
        for (Attribute dataSetListAttribute : dataSetListAttributes) {
            UUID referencedDataSetListId = dataSetListAttribute.getTypeDataSetList().getId();
            if (pathRestrictions == null) {
                cycleChecker.openNode(referencedDataSetListId);
                groups.add(
                        new GroupContext(
                                dataSets,
                                dataSetListAttribute,
                                rootOverlapNode,
                                new LinkedList<>(),
                                attributeTypeNames,
                                cycleChecker,
                                pathRestrictions,
                                referencedDataSetListId,
                                pageable
                        )
                );
                cycleChecker.closeNode(referencedDataSetListId);
            } else {
                if (pathRestrictions.get(0).equals(dataSetListAttribute.getId())) {
                    groups.add(
                            new GroupContext(
                                    dataSets,
                                    dataSetListAttribute,
                                    rootOverlapNode,
                                    new LinkedList<>(),
                                    attributeTypeNames,
                                    cycleChecker,
                                    pathRestrictions.subList(1, pathRestrictions.size()),
                                    referencedDataSetListId,
                                    pageable
                            )
                    );
                    break;
                }
            }
        }
        cycleChecker.closeNode(dataSetList.getId());
    }
}

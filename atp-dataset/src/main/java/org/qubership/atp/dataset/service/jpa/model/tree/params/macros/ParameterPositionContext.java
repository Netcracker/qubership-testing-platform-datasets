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

package org.qubership.atp.dataset.service.jpa.model.tree.params.macros;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * Class used for determination of parameter position. It can be used in referenced DSLs,
 * so we using dataSetColumn, instead of ID or anything else. Column is a root DSL thing, and it's absolute,
 * when DS ID is relative. Path - means list of referenced Attributes Ids of parameter position in DSL hierarchy.
 * */
@Getter
@Setter
public class ParameterPositionContext {
    private List<UUID> path;
    private int dataSetColumn;
    private Long order;
    private UUID dataSetListId;
    private UUID dataSetInColumnId;

    /**
     * Default constructor.
     * */
    public ParameterPositionContext(
            List<UUID> path, int dataSetColumn, UUID dataSetInColumnId, Long order, UUID dataSetListId
    ) {
        this.path = path;
        this.dataSetColumn = dataSetColumn;
        this.order = order;
        this.dataSetListId = dataSetListId;
        this.dataSetInColumnId = dataSetInColumnId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParameterPositionContext) {
            ParameterPositionContext other = (ParameterPositionContext)obj;
            boolean pathEquals = path.equals(other.getPath());
            boolean columnEquals = dataSetColumn == other.getDataSetColumn();
            boolean orderEquals = order.equals(other.getOrder());
            return pathEquals && columnEquals && orderEquals;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Object obj : path) {
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        hashCode = 31 * hashCode + dataSetColumn;
        hashCode = 31 * hashCode + (dataSetInColumnId != null ? dataSetInColumnId.hashCode() : 0);
        hashCode = 31 * hashCode + (order != null ? order.hashCode() : 0);
        return hashCode;
    }
}

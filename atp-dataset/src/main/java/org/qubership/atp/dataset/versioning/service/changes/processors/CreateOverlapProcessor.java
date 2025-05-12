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

package org.qubership.atp.dataset.versioning.service.changes.processors;

import java.util.List;

import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.NewObject;
import org.qubership.atp.dataset.constants.Constants;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.rest.dto.versioning.ChangeSummary;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.qubership.atp.dataset.versioning.service.changes.ParameterComparable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(4)
public class CreateOverlapProcessor extends AbstractChangeProcessor {
    
    @Autowired
    ModelsProvider modelsProvider;

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<ParameterComparable> newOverlappedParameters = findOverlappedParametersByChangeType(
                diff, NewObject.class);
        return !newOverlappedParameters.isEmpty();
    }

    /**
     * historyItem may contain name of Dataset if there is no Dataset in DSL.
     */
    @Override
    public HistoryItemDto createHistoryItem(
            Diff diff,
            DataSetListComparable oldEntity,
            DataSetListComparable actualEntity
    ) {
        boolean isDsNameIsEmpty = actualEntity.getDataSets().isEmpty();
        ParameterComparable overlappedParameter =
                    findOverlappedParametersByChangeType(diff, NewObject.class).get(0);
        String dataSetName = isDsNameIsEmpty ? Constants.NOT_FOUND_NAME_DS
                : getDataSetById(actualEntity, overlappedParameter.getDataSet()).getName();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setDataSet(dataSetName);
        if (!isDsNameIsEmpty) {
            String value = overlappedParameter.getValue().toString();
            historyItem.setChangeSummary(ChangeSummary.OVERRIDE_CREATE.toString());
            historyItem.setAttribute(overlappedParameter.getAttributeName());
            historyItem.setNewValue(getValuePretty(value, overlappedParameter.getType(), modelsProvider));
            log.trace("Diff processed by NewOverlappedParameterChangeProcessor");
        }
        return historyItem;
    }
}

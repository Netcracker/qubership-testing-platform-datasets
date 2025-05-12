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
import org.javers.core.diff.changetype.ValueChange;
import org.qubership.atp.dataset.service.rest.dto.versioning.ChangeSummary;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.service.changes.DataSetComparable;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(12)
public class RenamedDataSetProcessor extends AbstractChangeProcessor {

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<ValueChange> dataSetNameChanges = filterChangesByTypeAndAffectedTypeAndProperty(
                diff, ValueChange.class, "DataSet", "name");
        return !dataSetNameChanges.isEmpty();
    }

    @Override
    public HistoryItemDto createHistoryItem(Diff diff,
                                            DataSetListComparable oldEntity,
                                            DataSetListComparable actualEntity) {
        ValueChange dataSetNameChange = filterChangesByTypeAndAffectedTypeAndProperty(
                        diff, ValueChange.class, "DataSet", "name").get(0);
        DataSetComparable dataset = (DataSetComparable) dataSetNameChange.getAffectedObject().get();
        String dataSetName = dataset.getName();
        String oldValue = getDataSetById(oldEntity, dataset.getId()).getName();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.CHANGED.toString());
        historyItem.setDataSet(dataSetName);
        historyItem.setOldValue(oldValue);
        historyItem.setNewValue(dataSetName);
        log.trace("Diff processed by RenamedDataSetChangeProcessor");
        return historyItem;
    }
}

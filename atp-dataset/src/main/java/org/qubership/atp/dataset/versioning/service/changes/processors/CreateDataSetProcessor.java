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
import org.qubership.atp.dataset.service.rest.dto.versioning.ChangeSummary;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.service.changes.DataSetComparable;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
public class CreateDataSetProcessor extends AbstractChangeProcessor {

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<NewObject> newDataSets = filterChangesByTypeAndAffectedType(diff, NewObject.class, "DataSet");
        return !newDataSets.isEmpty();
    }

    @Override
    public HistoryItemDto createHistoryItem(
            Diff diff,
            DataSetListComparable oldEntity,
            DataSetListComparable actualEntity
    ) {
        NewObject newDataSet = filterChangesByTypeAndAffectedType(diff, NewObject.class, "DataSet")
                .get(0);
        String dataSetName = ((DataSetComparable) newDataSet.getAffectedObject().get())
                .getName();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.ADDED.toString());
        historyItem.setDataSet(dataSetName);
        log.trace("Diff processed by NewDataSetChangeProcessor");
        return historyItem;
    }
}

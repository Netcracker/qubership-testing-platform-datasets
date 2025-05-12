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
@Order(2)
public class UpdateParameterProcessor extends AbstractChangeProcessor {
    @Autowired
    ModelsProvider modelsProvider;

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<ValueChange> valueChanges = filterChangesByTypeAndProperty(diff, ValueChange.class, "value");
        return !valueChanges.isEmpty();
    }

    @Override
    public HistoryItemDto createHistoryItem(
            Diff diff,
            DataSetListComparable oldEntity,
            DataSetListComparable actualEntity
    ) {
        ValueChange parameterValueChange = filterChangesByTypeAndProperty(diff, ValueChange.class, "value").get(0);
        ParameterComparable parameter =
                (ParameterComparable) parameterValueChange.getAffectedObject().get();
        String attributeName = getAttributeDeepNameByParameter(actualEntity.getAttributes(), parameter);
        String dataSetName = getDataSetById(actualEntity, parameter.getDataSet())
                .getName();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.CHANGED.toString());
        historyItem.setAttribute(attributeName);
        historyItem.setDataSet(dataSetName);
        if (parameterValueChange.getLeft() != null) {
            historyItem.setOldValue(
                    getValuePretty(parameterValueChange.getLeft().toString(), parameter.getType(), modelsProvider)
            );
        }
        if (parameterValueChange.getRight() != null) {
            historyItem.setNewValue(
                    getValuePretty(parameterValueChange.getRight().toString(), parameter.getType(), modelsProvider)
            );
        }
        log.trace("Diff processed by UpdateParameterProcessor");
        return historyItem;
    }
}

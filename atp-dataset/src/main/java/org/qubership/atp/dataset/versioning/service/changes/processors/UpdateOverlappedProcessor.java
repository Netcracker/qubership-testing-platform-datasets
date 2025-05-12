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
import org.qubership.atp.dataset.versioning.model.domain.ParameterSnapshot;
import org.qubership.atp.dataset.versioning.service.changes.AttributeKeyComparable;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(10)
public class UpdateOverlappedProcessor extends AbstractChangeProcessor {
    @Autowired
    ModelsProvider modelsProvider;

    @Override
    protected boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<ValueChange> attributeKeysChanges = filterChangesByTypeAndAffectedType(
                diff, ValueChange.class, "AttributeKey"
        );
        return !attributeKeysChanges.isEmpty();
    }

    @Override
    protected HistoryItemDto createHistoryItem(
            Diff diff,
            DataSetListComparable oldEntity,
            DataSetListComparable actualEntity
    ) {
        ValueChange attributeKeyChange = filterChangesByTypeAndAffectedType(
                diff, ValueChange.class, "AttributeKey"
        ).get(0);
        AttributeKeyComparable attributeKey =
                (AttributeKeyComparable) attributeKeyChange.getAffectedObject().get();
        ParameterSnapshot parameter = attributeKey.getParameter();
        String dataSetName = getDataSetById(actualEntity, parameter.getDataSetId())
                .getName();
        String value = parameter.getText();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.OVERRIDE_CHANGED.toString());
        historyItem.setAttribute(attributeKey.getAttributeName());
        historyItem.setDataSet(dataSetName);
        historyItem.setNewValue(value);
        ParameterSnapshot oldParameter = (ParameterSnapshot) attributeKeyChange.getLeft();
        if (oldParameter != null) {
            historyItem.setOldValue(getValuePretty(oldParameter.getText(), oldParameter.getType(), modelsProvider));
        }
        ParameterSnapshot newParameter = (ParameterSnapshot) attributeKeyChange.getRight();
        if (newParameter != null) {
            historyItem.setNewValue(getValuePretty(newParameter.getText(), oldParameter.getType(), modelsProvider));
        }
        log.trace("Diff processed by ChangeOverlappedParameterProcessor");
        return historyItem;
    }
}

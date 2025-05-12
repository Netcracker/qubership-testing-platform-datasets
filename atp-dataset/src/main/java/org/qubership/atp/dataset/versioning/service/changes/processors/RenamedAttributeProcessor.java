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
import org.qubership.atp.dataset.versioning.service.changes.AttributeComparable;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(8)
public class RenamedAttributeProcessor extends AbstractChangeProcessor {

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<ValueChange> attributeNameChanges = filterChangesByTypeAndAffectedTypeAndProperty(
                diff, ValueChange.class, "Attribute", "name"
        );
        return !attributeNameChanges.isEmpty();
    }

    @Override
    public HistoryItemDto createHistoryItem(Diff diff,
                                                     DataSetListComparable oldEntity,
                                                     DataSetListComparable actualEntity) {
        ValueChange attributeNameChange = filterChangesByTypeAndAffectedTypeAndProperty(
                diff, ValueChange.class, "Attribute", "name"
        ).get(0);
        AttributeComparable attribute = (AttributeComparable) attributeNameChange.getAffectedObject().get();
        String attributeName = attribute.getName();
        String oldValue = getAttributeById(oldEntity, attribute.getId())
                .getName();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.CHANGED.toString());
        historyItem.setAttribute(attributeName);
        historyItem.setOldValue(oldValue);
        historyItem.setNewValue(attributeName);
        log.trace("Diff processed by RenamedAttributeChangeProcessor");
        return historyItem;
    }
}

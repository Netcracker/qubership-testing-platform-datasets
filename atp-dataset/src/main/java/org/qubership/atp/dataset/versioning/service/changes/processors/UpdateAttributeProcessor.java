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
import java.util.UUID;

import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.container.SetChange;
import org.javers.core.metamodel.object.InstanceId;
import org.qubership.atp.dataset.service.rest.dto.versioning.ChangeSummary;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(0)
public class UpdateAttributeProcessor extends AbstractChangeProcessor {

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<SetChange> setChangesDataSetListAttributes = filterChangesByTypeAndAffectedTypeAndProperty(
                diff, SetChange.class, "DataSetList", "attributes");
        if (!setChangesDataSetListAttributes.isEmpty()) {
            List<?> addedAttributesIds = setChangesDataSetListAttributes.get(0)
                    .getAddedValues();
            return !addedAttributesIds.isEmpty();
        }
        return false;
    }

    @Override
    public HistoryItemDto createHistoryItem(
            Diff diff,
            DataSetListComparable oldEntity,
            DataSetListComparable actualEntity
    ) {
        SetChange changesList = filterChangesByTypeAndAffectedTypeAndProperty(
                diff, SetChange.class, "DataSetList", "attributes"
        ).get(0);
        InstanceId instanceId = (InstanceId) changesList.getAddedValues().get(0);
        UUID newAttributeId = (UUID) instanceId.getCdoId();
        String attributeName = getAttributeById(actualEntity, newAttributeId).getName();
        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.ADDED.toString());
        historyItem.setAttribute(attributeName);
        log.trace("Diff processed by NewAttributeChangeProcessor");
        return historyItem;
    }
}

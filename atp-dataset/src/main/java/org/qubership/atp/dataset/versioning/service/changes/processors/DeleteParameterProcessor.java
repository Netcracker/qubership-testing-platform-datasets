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
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ValueChange;
import org.qubership.atp.dataset.service.rest.dto.versioning.ChangeSummary;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.qubership.atp.dataset.versioning.service.changes.ParameterComparable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(5)
public class DeleteParameterProcessor extends AbstractChangeProcessor {

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<ObjectRemoved> removedParameters = filterChangesByTypeAndAffectedType(
                diff, ObjectRemoved.class, "Parameter");
        List<ParameterComparable> newOverlappedParameters = findOverlappedParametersByChangeType(
                diff, NewObject.class);
        List<ObjectRemoved> removedDataSets = filterChangesByTypeAndAffectedType(
                diff, ObjectRemoved.class, "DataSet");
        List<ObjectRemoved> removedAttributes = filterChangesByTypeAndAffectedType(
                diff, ObjectRemoved.class, "Attribute");
        List<ValueChange> valueChanges = filterChangesByTypeAndProperty(
                diff, ValueChange.class, "value");
        return !removedParameters.isEmpty()
                && newOverlappedParameters.isEmpty()
                && removedDataSets.isEmpty()
                && removedAttributes.isEmpty()
                && valueChanges.isEmpty();
    }

    @Override
    public HistoryItemDto createHistoryItem(
            Diff diff,
            DataSetListComparable oldEntity,
            DataSetListComparable actualEntity
    ) {
        ObjectRemoved removedParameter = filterChangesByTypeAndAffectedType(
                diff, ObjectRemoved.class, "Parameter")
                .get(0);
        ParameterComparable parameter = (ParameterComparable) removedParameter.getAffectedObject().get();
        String attributeName = getAttributeDeepNameByParameter(oldEntity.getAttributes(), parameter);
        String dataSetName = getDataSetById(actualEntity, parameter.getDataSet())
                .getName();
        String value = parameter.getValue().toString();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.DELETED.toString());
        historyItem.setAttribute(attributeName);
        historyItem.setDataSet(dataSetName);
        historyItem.setOldValue(value);
        log.trace("Diff processed by DeleteParameterChangeProcessor");
        return historyItem;
    }
}

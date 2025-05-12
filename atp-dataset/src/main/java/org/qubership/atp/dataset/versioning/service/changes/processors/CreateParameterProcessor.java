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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.container.SetChange;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.rest.dto.versioning.ChangeSummary;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.service.changes.AttributeComparable;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.qubership.atp.dataset.versioning.service.changes.ParameterComparable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(3)
public class CreateParameterProcessor extends AbstractChangeProcessor {
    @Autowired
    ModelsProvider modelsProvider;

    @Override
    public boolean isApplicable(Diff diff, DataSetListComparable actualEntity) {
        List<NewObject> newParameters = filterChangesByTypeAndAffectedType(
                diff, NewObject.class, "Parameter");
        List<SetChange> setChangesParameters = filterChangesByTypeAndProperty(
                diff, SetChange.class, "parameters");
        if (!newParameters.isEmpty() && !setChangesParameters.isEmpty()) {
            Optional<AttributeComparable> optionalAttribute = getTopLevelAffectedAttribute(
                    actualEntity, setChangesParameters);
            if (optionalAttribute.isPresent()) {
                AttributeComparable affectedAttribute = optionalAttribute.get();
                List<UUID> topLevelAffectedParametersIds = getTopLevelAffectedParametersIds(
                        affectedAttribute, newParameters);
                return !topLevelAffectedParametersIds.isEmpty();
            }
        }
        return false;
    }

    @Override
    public HistoryItemDto createHistoryItem(Diff diff,
                                                     DataSetListComparable oldEntity,
                                                     DataSetListComparable actualEntity) {
        List<NewObject> newParameters = filterChangesByTypeAndAffectedType(
                diff, NewObject.class, "Parameter");
        List<SetChange> setChangesParameters = filterChangesByTypeAndProperty(
                diff, SetChange.class, "parameters");
        AttributeComparable affectedAttribute = getTopLevelAffectedAttribute(actualEntity, setChangesParameters)
                .get();
        UUID parameterId = getTopLevelAffectedParametersIds(affectedAttribute, newParameters)
                .get(0);
        String attributeName = affectedAttribute.getName();
        ParameterComparable parameter = getParameterById(actualEntity, parameterId);
        String dataSetName = getDataSetById(actualEntity, parameter.getDataSet())
                .getName();

        HistoryItemDto historyItem = new HistoryItemDto();
        historyItem.setChangeSummary(ChangeSummary.CHANGED.toString());
        historyItem.setAttribute(attributeName);
        historyItem.setDataSet(dataSetName);
        if (parameter.getValue() != null) {
            historyItem.setNewValue(
                    getValuePretty(parameter.getValue().toString(), parameter.getType(), modelsProvider)
            );
        }
        log.trace("Diff processed by NewParameterChangeProcessor");
        return historyItem;
    }

    private Optional<AttributeComparable> getTopLevelAffectedAttribute(
            DataSetListComparable dsl,
            List<SetChange> setChangesParameters
    ) {
        List<UUID> affectedAttributesIds = getAffectedIds(setChangesParameters);
        List<UUID> dataSetListAttributesIds = getDataSetListAttributesIds(dsl);
        List<UUID> topLevelAffectedAttributesIds = ListUtils.intersection(
                affectedAttributesIds, dataSetListAttributesIds);
        return !topLevelAffectedAttributesIds.isEmpty()
                ? Optional.of(getAttributeById(dsl, topLevelAffectedAttributesIds.get(0)))
                : Optional.empty();
    }

    private List<UUID> getTopLevelAffectedParametersIds(AttributeComparable affectedAttribute,
                                                        List<NewObject> newParameters) {
        List<UUID> attributeParametersIds = getAttributeParametersIds(affectedAttribute);
        List<UUID> newParametersIds = getAffectedIds(newParameters);
        return ListUtils.intersection(attributeParametersIds, newParametersIds);
    }

    private List<UUID> getAffectedIds(List<? extends Change> changes) {
        return changes
                .stream()
                .map(setChange -> (UUID) setChange.getAffectedLocalId())
                .collect(Collectors.toList());
    }

    private List<UUID> getDataSetListAttributesIds(DataSetListComparable dsl) {
        return dsl
                .getAttributes()
                .stream()
                .map(AttributeComparable::getId)
                .collect(Collectors.toList());
    }

    private List<UUID> getAttributeParametersIds(AttributeComparable attribute) {
        return attribute
                .getParameters()
                .stream()
                .map(ParameterComparable::getId)
                .collect(Collectors.toList());
    }
}

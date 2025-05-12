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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.PropertyChange;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.rest.dto.versioning.HistoryItemDto;
import org.qubership.atp.dataset.versioning.model.domain.ListValueSnapshot;
import org.qubership.atp.dataset.versioning.model.domain.ParameterSnapshot;
import org.qubership.atp.dataset.versioning.service.changes.AttributeComparable;
import org.qubership.atp.dataset.versioning.service.changes.AttributeKeyComparable;
import org.qubership.atp.dataset.versioning.service.changes.DataSetComparable;
import org.qubership.atp.dataset.versioning.service.changes.DataSetListComparable;
import org.qubership.atp.dataset.versioning.service.changes.ParameterComparable;

import lombok.Setter;

@Setter
public abstract class AbstractChangeProcessor implements ChangeProcessor {

    protected ChangeProcessor nextProcessor;

    static DataSetComparable getDataSetById(DataSetListComparable dataSetList, UUID dataSetId) {
        return dataSetList
                .getDataSets()
                .stream()
                .filter(dataSet -> dataSet.getId().equals(dataSetId))
                .findFirst()
                .get();
    }

    static AttributeComparable getAttributeById(DataSetListComparable dataSetList, UUID attributeId) {
        for (AttributeComparable attribute : dataSetList.getAttributes()) {
            if (attribute.getId().equals(attributeId)) {
                return attribute;
            }
        }
        return null;
    }

    static ParameterComparable getParameterById(DataSetListComparable dataSetList, UUID parameterId) {
        return dataSetList
                .getAttributes()
                .stream()
                .flatMap(attribute -> attribute.getParameters().stream())
                .filter(parameter -> parameter.getId().equals(parameterId))
                .findFirst()
                .get();
    }

    static <T extends Change> List<T> filterChangesByTypeAndAffectedType(
            Diff diff,
            Class<T> changeType,
            String affectedType
    ) {
        List<T> list = new ArrayList<>();
        for (T change : diff.getChangesByType(changeType)) {
            if (affectedType.equals(change.getAffectedGlobalId().getTypeName())) {
                list.add(change);
            }
        }
        return list;
    }

    static <T extends PropertyChange> List<T> filterChangesByTypeAndProperty(
            Diff diff,
            Class<T> changeType,
            String propertyName
    ) {
        List<T> result = new ArrayList<>();
        for (T propertyChange : diff.getChangesByType(changeType)) {
            if (propertyName.equals(propertyChange.getPropertyName())) {
                result.add(propertyChange);
            }
        }
        return result;
    }

    static List<ParameterComparable> findOverlappedParametersByChangeType(
            Diff diff,
            Class<? extends Change> changeType
    ) {
        List<ParameterComparable> list = new ArrayList<>();
        for (Change change : filterChangesByTypeAndAffectedType(diff, changeType, "AttributeKey")) {
            AttributeKeyComparable attributeKey = (AttributeKeyComparable) change.getAffectedObject().get();
            ParameterComparable parameter = new ParameterComparable(
                    attributeKey.getAttributePathNames(), attributeKey.getParameter()
            );
            list.add(parameter);
        }
        return list;
    }

    static String getAttributeDeepNameByParameter(
            Set<AttributeComparable> attributes,
            ParameterComparable parameter
    ) {
        for (AttributeComparable attribute : attributes) {
            if (attribute.getParameters()
                    .stream()
                    .anyMatch(param -> param.equals(parameter))) {
                return attribute.getName();
            }
        }
        return null;
    }

    static String convertListValuesToString(AttributeComparable attribute) {
        return attribute
                .getListValues()
                .stream()
                .map(ListValueSnapshot::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    static <T extends PropertyChange> List<T> filterChangesByTypeAndAffectedTypeAndProperty(
            Diff diff,
            Class<T> changeType,
            String affectedType,
            String propertyName
    ) {
        List<T> list = new ArrayList<>();
        for (T propertyChange : diff.getChangesByType(changeType)) {
            if (affectedType.equals(propertyChange.getAffectedGlobalId().getTypeName())
                    && propertyName.equals(propertyChange.getPropertyName())) {
                list.add(propertyChange);
            }
        }
        return list;
    }

    /**
     * Fills fields of {@link HistoryItemDto} in case the incoming data is applicable for it.
     * Return {@link HistoryItemDto} in case data was processed by this {@link ChangeProcessor}
     * In case data cannot be processed by this processor call the next one or return {@code null}
     * if there is no next processor.
     */
    public HistoryItemDto proceed(Diff diff,
                                           DataSetListComparable oldEntity,
                                           DataSetListComparable actualEntity) {
        if (isApplicable(diff, actualEntity)) {
            return createHistoryItem(diff, oldEntity, actualEntity);
        } else if (nextProcessor != null) {
            return nextProcessor.proceed(diff, oldEntity, actualEntity);
        } else {
            return null;
        }
    }

    protected abstract boolean isApplicable(Diff diff, DataSetListComparable actualEntity);

    protected abstract HistoryItemDto createHistoryItem(
            Diff diff,
            DataSetListComparable oldEntity,
            DataSetListComparable actualEntity
    );

    /**
     * Mask for encrypted, special text view for change type.
     * */
    public String getValuePretty(
            String value,
            AttributeTypeName typeName,
            ModelsProvider modelsProvider
    ) {
        if (typeName == null) {
            return value;
        }
        switch (typeName) {
            case CHANGE:
                return ParameterSnapshot.getMultipleValuePretty(value, modelsProvider).toString();
            case ENCRYPTED:
                return  "**********";
            default:
                return value;
        }
    }
}

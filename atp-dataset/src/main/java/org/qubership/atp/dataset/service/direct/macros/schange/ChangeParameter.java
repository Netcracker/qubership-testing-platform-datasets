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

package org.qubership.atp.dataset.service.direct.macros.schange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.utils.Change;
import org.qubership.atp.dataset.model.utils.ChangeType;
import org.qubership.atp.dataset.model.utils.Utils;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

class ChangeParameter {
    private static final int UUID_LENGTH = 36;
    protected final Attribute targetAttribute;
    protected final Parameter targetParameter;
    protected final Change change;

    private ChangeParameter(@Nonnull Attribute targetAttribute,
                            @Nonnull Parameter targetParameter,
                            @Nonnull Change change) {
        this.targetAttribute = targetAttribute;
        this.targetParameter = targetParameter;
        this.change = change;
    }

    public static ChangeParameter create(@Nonnull Attribute targetAttribute,
                                         @Nonnull Parameter targetParameter,
                                         @Nonnull Change change) {
        switch (change.type) {
            case MULTIPLY:
                return new Multiplication(targetAttribute, targetParameter, change);
            default:
                throw new UnsupportedOperationException(String.format(
                        "Unknown type of structure change %s in %s", change, targetParameter));
        }
    }

    public static Stream<List<MultiplicationEntry>> getCombinations(Stream<ChangeParameter> changes) {
        return Utils.combinations(getMultiplications(changes).collect(Collectors.toList()),
                Collectors.toList());
    }

    public static List<MultiplicationEntry> getCombination(Stream<ChangeParameter> changes, String metadata) {
        Iterator<String> toChose = Splitter.fixedLength(UUID_LENGTH).splitToList(metadata).iterator();
        Iterator<List<MultiplicationEntry>> toChoseFrom = ChangeParameter.getMultiplications(changes).iterator();
        List<MultiplicationEntry> result = new ArrayList<>(Math.round(metadata.length() / UUID_LENGTH));
        while (toChoseFrom.hasNext()) {
            List<MultiplicationEntry> toSelectFrom = toChoseFrom.next();
            if (!toChose.hasNext()) {
                throw new RuntimeException("Metadata is not fully provided. "
                        + "Resolved path: " + result + "; unresolved part - one of: " + toSelectFrom);
            }
            UUID target;
            try {
                target = UUID.fromString(toChose.next());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Metadata does not contain an id. "
                        + "Resolved path: " + result + "; unresolved part - one of: " + toSelectFrom, e);
            }
            MultiplicationEntry entry = toSelectFrom.stream()
                    .filter(item -> target.equals(item.getValue()))
                    .findAny()
                    .orElseThrow(() ->
                            new RuntimeException("Metadata contain an unknown id. "
                                    + "Resolved path: " + result + "; unresolved part - one of: " + toSelectFrom
                                    + "; looking for: " + target));
            result.add(entry);
        }
        if (toChose.hasNext()) {
            List<String> unresolvedPart = Lists.newArrayList(toChose);
            throw new RuntimeException("Metadata contain extra unresolved part. "
                    + "Resolved path: " + result + "; unresolved part: " + unresolvedPart);
        }
        return result;
    }

    public static Stream<List<MultiplicationEntry>> getMultiplications(Stream<ChangeParameter> changes) {
        return changes.filter(change -> ChangeType.MULTIPLY == change.change.type)
                .map(ChangeParameter::asMultiplication)
                .map(Multiplication::getEntries);
    }

    @Nonnull
    public static Stream<ChangeParameter> getChanges(@Nonnull Collection<Attribute> structureChanges, DataSet source) {
        if (structureChanges.isEmpty()) {
            return Stream.empty();
        }
        List<ChangeParameter> result = null;
        for (Attribute changeAttr : structureChanges) {
            Optional<Parameter> parameter = changeAttr.getParameters().stream()
                    .filter(param -> source.getId().equals(param.getDataSet().getId()))
                    .findAny();
            if (!parameter.isPresent()) {
                continue;
            }
            String text = parameter.get().getText();
            if (Strings.isNullOrEmpty(text)) {
                continue;
            }
            Change change = Change.fromText(text);
            if (result == null) {
                result = new ArrayList<>(5);
            }
            result.add(create(changeAttr, parameter.get(), change));
        }
        return result == null ? Stream.empty() : result.stream();
    }

    public Multiplication asMultiplication() {
        Preconditions.checkArgument(ChangeType.MULTIPLY == change.type,
                "%s is not a multiplication", this);
        return (Multiplication) this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("targetAttribute", targetAttribute)
                .add("targetParameter", targetParameter)
                .add("change", change)
                .toString();
    }

    public static class Multiplication extends ChangeParameter {
        protected final UUID dataSetListId;
        protected final List<UUID> dataSetIds;

        private Multiplication(@Nonnull Attribute targetAttribute,
                               @Nonnull Parameter targetParameter,
                               @Nonnull Change change) {
            super(targetAttribute, targetParameter, change);
            dataSetListId = targetAttribute.getDataSetListReference().getId();
            dataSetIds = change.arguments;
        }

        private List<MultiplicationEntry> getEntries() {
            return dataSetIds.stream()
                    .map(id -> new MultiplicationEntry(this, id))
                    .collect(Collectors.toList());
        }

    }
}

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

package org.qubership.atp.dataset.model.utils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.ParameterOverlap;

import com.google.common.collect.AbstractIterator;

/**
 * Applies overlaps to a parameter, returns the overlap chain starting from top one (visible). All
 * parameters except the last one will be {@link Parameter#isOverlap()}. The last one as an original
 * overlapped parameter.
 */
public class OverlapIterator extends AbstractIterator<OverlapItem> {

    private final DataSet sourceDs;
    private final Collection<UUID> attrPath;
    private final UUID targetAttrId;
    private final List<Attribute> resolvedAttrPath;
    private final Iterator<DataSet> layersToCheck;
    private DataSet lastDs = null;
    private Supplier<OverlapItem> nextStrategy;

    OverlapIterator(@Nonnull DataSet sourceDs,
                    @Nullable Collection<UUID> attrPath,
                    @Nonnull UUID targetAttrId,
                    @Nonnull Iterator<DataSet> layersToCheck,
                    @Nonnull List<Attribute> resolvedAttrPath) {
        this.attrPath = attrPath;
        this.sourceDs = sourceDs;
        this.targetAttrId = targetAttrId;
        this.layersToCheck = layersToCheck;
        this.resolvedAttrPath = resolvedAttrPath;
        if (attrPath == null || attrPath.isEmpty()) {
            nextStrategy = this::checkTargetAttr;
        } else {
            nextStrategy = new CheckAttrPath(new ArrayDeque<>(attrPath));
        }
    }

    /**
     * See {@link OverlapIterator}.
     *
     * @param attrPathIds may be empty.
     */
    public static OverlapIterator create(@Nonnull DataSet dataSet,
                                         @Nonnull UUID targetAttrId,
                                         @Nullable Collection<UUID> attrPathIds) {
        return OverlapLayers.overlapIterator(dataSet, targetAttrId, attrPathIds);
    }

    public static OverlapIterator from(@Nonnull DataSet ds, @Nonnull Attribute targetAttribute,
                                       @Nonnull List<Attribute> attributePath) {
        return OverlapIterator.create(ds, targetAttribute.getId(),
                attributePath.stream().map(Identified::getId).collect(Collectors.toList()));
    }

    /**
     * See {@link OverlapIterator}.
     */
    @Nonnull
    public static OverlapIterator from(@Nonnull AttributePath attributePath) {
        return from(attributePath.getDataSet(), attributePath.getTargetAttribute(), attributePath.getPath());
    }

    @SuppressWarnings("PMD")
    private static Optional<ParameterOverlap> getCachedOverlap(DataSet ds,
                                                               UUID targetAttrId,
                                                               Iterable<UUID> attrIdPath) {
        return ds.getParameters().stream().filter(Parameter::isOverlap).map(Parameter::asOverlap)
                .filter(overlap -> {
                    AttributePath attrPath = overlap.getAttributePath();
                    if (!targetAttrId.equals(attrPath.getTargetAttribute().getId())) {
                        return false;
                    }
                    Iterator<UUID> attrIdPathIter = attrIdPath.iterator();
                    return attrPath.getPath().stream()
                            .allMatch(attr -> {
                                while (attrIdPathIter.hasNext()) {
                                    return attrIdPathIter.next().equals(attr.getId());
                                }
                                return false;
                            });
                }).findAny();
    }

    private static Optional<Parameter> getCachedParameter(DataSet currentDs, UUID targetAttrId) {
        return currentDs.getParameters().stream()
                .filter(parameter -> parameter.getAttribute().getId().equals(targetAttrId))
                .findAny();
    }

    private static Optional<Attribute> getCachedAttribute(DataSetList dsl, UUID targetAttrId) {
        return dsl.getAttributes().stream()
                .filter(attribute -> targetAttrId.equals(attribute.getId()))
                .findAny();
    }

    @Override
    protected OverlapItem computeNext() {
        OverlapItem result = null;
        while (nextStrategy != null && result == null) {
            result = nextStrategy.get();
        }
        if (result == null) {
            return endOfData();
        }
        return result;
    }

    @Nonnull
    private OverlapItem canNotBeReached(UUID attrId) {
        assert !layersToCheck.hasNext();
        assert lastDs != null;
        nextStrategy = null;
        return new OverlapItem.Unreachable(sourceDs, attrPath, targetAttrId, lastDs, attrId);
    }

    @Nonnull
    private OverlapItem checkTargetAttr() {
        if (!layersToCheck.hasNext()) {
            return canNotBeReached(targetAttrId);
        }
        nextStrategy = null;
        lastDs = layersToCheck.next();
        Optional<Parameter> param = getCachedParameter(lastDs, targetAttrId);
        if (param.isPresent()) {
            return new OverlapItem.DefaultInitialized(sourceDs, resolvedAttrPath, lastDs, param.get());
        }
        Optional<Attribute> attr = getCachedAttribute(lastDs.getDataSetList(), targetAttrId);
        if (attr.isPresent()) {
            return new OverlapItem.DefaultUninitialized(sourceDs, resolvedAttrPath, attr.get(), lastDs);
        }
        return canNotBeReached(targetAttrId);
    }

    private class CheckAttrPath implements Supplier<OverlapItem> {

        private final Deque<UUID> attrPathToCheck;

        private CheckAttrPath(@Nonnull Deque<UUID> attrPathToCheck) {
            this.attrPathToCheck = attrPathToCheck;
        }

        @Override
        public OverlapItem get() {
            assert attrPath != null;
            assert !attrPathToCheck.isEmpty();
            if (!layersToCheck.hasNext()) {
                return canNotBeReached(attrPathToCheck.getFirst());
            } else {
                lastDs = layersToCheck.next();
                Optional<ParameterOverlap> cachedOverlap = getCachedOverlap(lastDs, targetAttrId, attrPathToCheck);
                OverlapItem.Overlap result = cachedOverlap
                        .map(overlap -> new OverlapItem.Overlap(sourceDs, resolvedAttrPath, lastDs, overlap))
                        .orElse(null);
                attrPathToCheck.removeFirst();
                if (attrPathToCheck.isEmpty()) {
                    nextStrategy = OverlapIterator.this::checkTargetAttr;
                }
                return result;
            }
        }
    }
}

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Parameter;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

/**
 * Lazy initialized {@link Iterable} of Data set path with reference attributes between each other.
 * Path is resolved using provided attribute path and starting data set (context). Caches
 * initialized data sets path parts for performance reason. It is recommended to reuse this object
 * between multiple {@link OverlapIterator}'s.
 */
public class OverlapLayers {

    private final Deque<UUID> currentAttrPath;
    private final Deque<DataSet> resolvedLayers = new ArrayDeque<>();
    private final List<Attribute> resolvedAttrPath;

    private OverlapLayers(@Nonnull DataSet ds) {
        this(ds, new ArrayDeque<>());
    }

    /**
     * See {@link OverlapLayers}.
     */
    private OverlapLayers(@Nonnull DataSet ds, @Nonnull Deque<UUID> currentAttrPath) {
        resolvedLayers.add(ds);
        this.currentAttrPath = currentAttrPath;
        this.resolvedAttrPath = currentAttrPath.isEmpty() ? Collections.emptyList()
                : new ArrayList<>(currentAttrPath.size());
    }

    /**
     * See {@link OverlapIterator}.
     */
    public static OverlapIterator overlapIterator(@Nonnull DataSet ds, @Nonnull UUID targetAttrId,
                                                  @Nullable Collection<UUID> attrPath) {
        if (attrPath == null || attrPath.isEmpty()) {
            return new OverlapIterator(ds, null, targetAttrId, Iterators.singletonIterator(ds),
                    Collections.emptyList());
        } else {
            OverlapLayers overlapLayers = new OverlapLayers(ds, new ArrayDeque<>(attrPath));
            return new OverlapIterator(ds,
                    new ArrayDeque<>(attrPath), targetAttrId, overlapLayers.iterator(),
                    overlapLayers.resolvedAttrPath
            );
        }
    }

    @Nonnull
    private Iterator<DataSet> iterator() {
        if (resolvedLayers.size() > currentAttrPath.size()) {
            //all layers are resolved;
            return resolvedLayers.iterator();
        }
        return Iterators.concat(resolvedLayers.iterator(), new LazyResolveIterator());
    }

    private class LazyResolveIterator extends AbstractIterator<DataSet> {

        @Override
        protected DataSet computeNext() {
            if (currentAttrPath.size() < resolvedLayers.size()) {
                return endOfData();//nothing to resolve
            }
            ArrayDeque<UUID> attrPath = currentAttrPath.stream()
                    .limit(resolvedLayers.size())
                    .collect(Collectors.toCollection(ArrayDeque::new));
            UUID targetAttrId = attrPath.removeLast();
            Iterator<DataSet> layersToCheck = resolvedLayers.iterator();
            Iterator<OverlapItem> ref = new OverlapIterator(resolvedLayers.getFirst(),
                    attrPath, targetAttrId, layersToCheck, resolvedAttrPath);
            if (!ref.hasNext()) {
                return endOfData();
            }
            OverlapItem refSup = ref.next();
            if (!refSup.isReachable()) {
                return endOfData();
            }
            OverlapItem.Reachable reachable = refSup.asReachable();
            Optional<Parameter> parameterOpt = reachable.getParameter();
            if (!parameterOpt.isPresent() || reachable.getAttribute().getType() != AttributeType.DSL) {
                return endOfData();
            }
            DataSet dataSetReference = parameterOpt.get().getDataSetReference();
            if (dataSetReference == null) {
                return endOfData();
            }
            resolvedAttrPath.add(reachable.getAttribute());
            resolvedLayers.add(dataSetReference);
            return dataSetReference;
        }
    }
}

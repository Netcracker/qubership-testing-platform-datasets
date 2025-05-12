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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.model.utils.tree.Leaf;
import org.qubership.atp.dataset.model.utils.tree.TraverseAndLeafsHandler;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class DsParamHandler implements TraverseAndLeafsHandler<DataSet, OverlapItem.Reachable, Leaf> {

    private final DsEvaluator evaluator;
    private final ParamDsHandler paramDs = new ParamDsHandler();
    private final LinkedList<DataSet> dsPath = new LinkedList<>();
    private final LinkedList<OverlapItem.Reachable> paramPath = new LinkedList<>();

    public DsParamHandler(DsEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Nonnull
    @Override
    public TraverseAndLeafsHandler<?, DataSet, Leaf> backToPreviousParent() {
        paramPath.removeLast();
        return paramDs;
    }

    @Nonnull
    @Override
    public TraverseAndLeafsHandler<OverlapItem.Reachable, ?, Leaf> forwardToNewParent(@Nonnull DataSet parent) {
        dsPath.add(parent);
        return paramDs;
    }

    @Nullable
    @Override
    public Iterator<? extends OverlapItem.Reachable> getChildren(@Nonnull DataSet item) {
        DataSet rootDs;
        List<UUID> attrPath;
        if (dsPath.isEmpty()) {
            //parameters belongs to the root dataSet and can not be overlapped
            rootDs = item;
            attrPath = Collections.emptyList();
        } else {
            rootDs = dsPath.getFirst();
            attrPath = paramPath.stream().map(p -> p.getAttribute().getId()).collect(Collectors.toList());
        }
        return item.getDataSetList().getAttributes().stream()
                .map(attr -> {
                    OverlapIterator overlapIterator = OverlapIterator.create(rootDs, attr.getId(), attrPath);
                    return overlapIterator.next().asReachable();
                }).iterator();
    }

    @Nullable
    @Override
    public Leaf constructLeaf(@Nonnull DataSet leaf) {
        return null;
    }

    private class ParamDsHandler implements TraverseAndLeafsHandler<OverlapItem.Reachable, DataSet, Leaf> {

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<?, OverlapItem.Reachable, Leaf> backToPreviousParent() {
            DsParamHandler.this.dsPath.removeLast();
            return DsParamHandler.this;
        }

        @Nonnull
        @Override
        public TraverseAndLeafsHandler<DataSet, ?, Leaf> forwardToNewParent(@Nonnull OverlapItem.Reachable parent) {
            DsParamHandler.this.paramPath.add(parent);
            return DsParamHandler.this;
        }

        @Nullable
        @Override
        public Iterator<? extends DataSet> getChildren(@Nonnull OverlapItem.Reachable item) {
            return item.getParameter()
                    .map(Parameter::getDataSetReference)
                    .map(Iterators::singletonIterator)
                    .orElse(null);
        }

        @Nullable
        @Override
        public Leaf constructLeaf(@Nonnull OverlapItem.Reachable leaf) {
            List<String> path = Lists.newArrayListWithExpectedSize(DsParamHandler.this.paramPath.size() + 1);
            DsParamHandler.this.paramPath.stream().map(p -> p.getAttribute().getName()).forEach(path::add);
            path.add(leaf.getAttribute().getName());
            Optional<String> value;
            if (leaf.getAttribute().getType() == AttributeType.FILE) {
                //skip evaluation
                value = leaf.getParameter().map(Parameter::getFileData).map(FileData::getUrl);
            } else {
                value = evaluator.apply(leaf);
            }
            return new Leaf(path, value.orElse(""));
        }
    }
}

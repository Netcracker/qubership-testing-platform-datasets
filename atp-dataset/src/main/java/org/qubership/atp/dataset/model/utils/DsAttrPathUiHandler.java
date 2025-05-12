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
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManParameter;

import com.google.common.collect.Iterators;

public class DsAttrPathUiHandler extends AbstractDsUiHandler {

    private final DsEvaluator evaluator;
    private final List<UUID> attrPath;
    private final Deque<Attribute> groupsPath = new ArrayDeque<>();
    private boolean isLeaf = false;
    private UiManAttribute result = null;
    private Deque<UiManAttribute> convertedGroupsPath = new ArrayDeque<>();

    public DsAttrPathUiHandler(DsEvaluator evaluator, List<UUID> attrPath) {
        this.evaluator = evaluator;
        this.attrPath = attrPath;
    }

    public UiManAttribute getAttribute() {
        return result;
    }

    private UiManAttribute convertAttribute(Attribute targetAttr) {
        UiManAttribute result = new UiManAttribute();
        result.setSource(targetAttr);
        List<UUID> attrPath = groupsPath.stream().map(Identified::getId).collect(Collectors.toList());
        OverlapIterator overlapIterator = OverlapIterator.create(curDs, targetAttr.getId(), attrPath);
        OverlapItem context = overlapIterator.next();
        Optional<Parameter> parameterOpt = context.getParameter();
        if (parameterOpt.isPresent()) {
            OverlapItem.Reachable reachable = context.asReachable();
            Parameter parameter = parameterOpt.get();
            Optional<String> valueOpt = evaluator.apply(reachable);
            Optional<?> valueRefOpt = reachable.getValueRef();
            boolean overlap = reachable.isOverlap();
            if (valueOpt.isPresent() || valueRefOpt.isPresent() || overlap) {
                result.getParameters().add(new UiManParameter(parameter, reachable.getSourceDs(),
                        overlap,
                        valueOpt.orElse(null),
                        valueRefOpt.orElse(null)));
            }
        }
        return result;
    }

    @Override
    protected void goBackFromAttribute() {
        super.goBackFromAttribute();
        isLeaf = false;
        //leave group attr
        if (groupsPath.size() >= attrPath.size()) {
            /*conversion starts at the last element of attrPath*/
            UiManAttribute convertedAttr = convertedGroupsPath.removeLast();
            if (convertedGroupsPath.isEmpty()) {
                result = convertedAttr;
            } else {
                convertedGroupsPath.getLast().getAttributes().add(convertedAttr);
            }
        }
        groupsPath.removeLast();
    }

    @Override
    protected void attributeStarts(Attribute attr) {
        super.attributeStarts(attr);
        isLeaf = true;
    }

    @Nullable
    @Override
    protected Iterator<? extends Attribute> getChildren(@Nonnull DataSetList item) {
        if (dslDepth < attrPath.size()) {
            UUID attrId = attrPath.get(dslDepth);
            return item.getAttributes().stream()
                    .filter(attr -> attrId.equals(attr.getId()))
                    .findAny()
                    .map(Iterators::singletonIterator)
                    .orElse(null);
        } else {
            return item.getAttributes().iterator();
        }
    }

    @Override
    protected void goForwardUnderAttribute() {
        super.goForwardUnderAttribute();
        isLeaf = false;
        //visit group attr
        if (groupsPath.size() >= attrPath.size() - 1) {
            /*conversion starts at the last element of attrPath*/
            UiManAttribute convertedAttr = convertAttribute(curAttr);
            convertedGroupsPath.add(convertedAttr);
        }
        groupsPath.add(curAttr);
    }

    @Override
    protected void attributeEnds() {
        if (isLeaf) {
            //consumer leaf attr
            UiManAttribute leafAttr = convertAttribute(curAttr);
            if (!convertedGroupsPath.isEmpty()) {
                convertedGroupsPath.getLast().getAttributes().add(leafAttr);
            } else {
                result = leafAttr;
            }
        }
    }
}

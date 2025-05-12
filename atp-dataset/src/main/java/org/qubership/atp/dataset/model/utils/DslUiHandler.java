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
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSet;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManParameter;

public class DslUiHandler extends AbstractDslUiHandler {

    private static final String OLD_MACROS_MARKER = "#REF";
    protected final Deque<UiManAttribute> groupsPath = new ArrayDeque<>();
    protected final DsEvaluator evaluator;
    protected UiManDataSetList result;
    protected UiManAttribute currentAttr;
    protected DataSetParameterProvider dataSetParameterProvider;
    protected MacroContext macroContext;
    protected boolean isEvaluate;

    public DslUiHandler(DsEvaluator evaluator, boolean expandAll) {
        super(expandAll);
        this.evaluator = evaluator;
    }

    /**
     * Constructor for new flow which evaluate macros from ATP macros.
     */
    public DslUiHandler(DsEvaluator evaluator,
                        MacroContext macroContext,
                        DataSetParameterProvider dataSetParameterProvider,
                        boolean isEvaluate,
                        boolean expandAll) {
        super(expandAll);
        this.evaluator = evaluator;
        this.macroContext = macroContext;
        this.dataSetParameterProvider = dataSetParameterProvider;
        this.isEvaluate = isEvaluate;
    }

    public UiManDataSetList getResult() {
        return result;
    }

    @Override
    protected void attributeStarts(Attribute attr) {
        currentAttr = new UiManAttribute();
        currentAttr.setSource(attr);
        List<UUID> attrPath = groupsPath.stream().map(UiManAttribute::getId).collect(Collectors.toList());
        for (int datasetNumber = 0; datasetNumber < result.getDataSets().size(); datasetNumber++) {
            UiManDataSet uiDataSet = result.getDataSets().get(datasetNumber);
            OverlapIterator overlapIterator = OverlapIterator.create(uiDataSet.getSource(), attr.getId(), attrPath);
            OverlapItem context = overlapIterator.next();
            Optional<Parameter> parameter = context.getParameter();
            if (!parameter.isPresent()) {
                continue;
            }
            Parameter param = parameter.get();
            OverlapItem.Reachable reachable = context.asReachable();
            String stingValue = reachable.getValue().orElse("null");
            Optional<?> valueOpt;
            if (isEvaluate && !stingValue.startsWith(OLD_MACROS_MARKER)) {
                AbstractParameter resolvedParameter = dataSetParameterProvider.getDataSetParameterResolved(
                        result.getId(),
                        parameter.get().getId(),
                        AttributeTypeName.getTypeByName(parameter.get().getAttribute().getType().getName()),
                        isEvaluate,
                        macroContext,
                        new ParameterPositionContext(
                                Collections.emptyList(),
                                datasetNumber,
                                uiDataSet.getId(),
                                new Random().nextLong(),
                                result.getId()
                        )
                );
                valueOpt = Optional.ofNullable(resolvedParameter.getValue());
            } else {
                valueOpt = evaluator.apply(reachable);
            }
            Optional<?> valueRefOpt = reachable.getValueRef();
            boolean isOverlap = reachable.isOverlap();
            if (valueOpt.isPresent() || valueRefOpt.isPresent() || isOverlap) {
                currentAttr.getParameters().add(new UiManParameter(param,
                        reachable.getSourceDs(),
                        reachable.isOverlap(),
                        valueOpt.orElse(null), valueRefOpt.orElse(null)));
            }
        }
    }

    @Override
    protected void goForwardUnderAttribute() {
        groupsPath.add(currentAttr);
        currentAttr = null;
    }

    @Override
    protected void goBackFromAttribute() {
        currentAttr = groupsPath.removeLast();
    }

    @Override
    protected void attributeEnds() {
        if (groupsPath.isEmpty()) {
            result.getAttributes().add(currentAttr);
        } else {
            groupsPath.getLast().getAttributes().add(currentAttr);
        }
    }

    @Override
    public void notifyItemStarts(DataSetList dsl) {
        if (result == null) {
            result = new UiManDataSetList();
            result.setSource(dsl);
            for (DataSet dataSet : dsl.getDataSets()) {
                UiManDataSet uiDs = new UiManDataSet();
                uiDs.setSource(dataSet);
                result.getDataSets().add(uiDs);
            }
        }
    }

    @Override
    public void notifyProcessingChildren() {
    }

    @Override
    public void notifyChildrenProcessed() {
    }

    @Override
    public void notifyItemEnds() {
    }
}

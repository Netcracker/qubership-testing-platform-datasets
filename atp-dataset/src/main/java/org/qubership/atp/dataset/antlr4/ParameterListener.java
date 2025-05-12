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

package org.qubership.atp.dataset.antlr4;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.qubership.atp.dataset.antlr.DataSetParameterBaseListener;
import org.qubership.atp.dataset.antlr.DataSetParameterParser;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.TextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.AbstractMacro;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import lombok.Getter;

@Deprecated
public class ParameterListener extends DataSetParameterBaseListener {
    private MacroContext macroContext;
    private Stack<AbstractTextParameter> parametersStack = new Stack<>();
    @Getter
    private List<AbstractTextParameter> parsedParameters = new LinkedList<>();
    private ParameterPositionContext parameterPositionContext;
    @Getter
    private boolean setEvaluate;

    /**
     * Constructor. Used for single parameter, so it need macro context,
     * parameter position and evaluation flag.
     * */
    public ParameterListener(
            MacroContext macroContext, ParameterPositionContext parameterPositionContext, boolean setEvaluate
    ) {
        this.macroContext = macroContext;
        this.parameterPositionContext = parameterPositionContext;
        this.setEvaluate = setEvaluate;
    }

    @Override
    public void enterMacro(DataSetParameterParser.MacroContext ctx) {
        String macroName = ctx.getChild(0).getText();
        AbstractMacro macro = AbstractMacro.getMacroByName(
            macroName.substring(0, macroName.indexOf("(")),
            macroContext,
            setEvaluate,
            parameterPositionContext
        );
        parametersStack.push(macro);
    }

    @Override
    public void exitMacro(DataSetParameterParser.MacroContext ctx) {
        AbstractTextParameter pop = parametersStack.pop();
        if (parametersStack.empty()) {
            parsedParameters.add(pop);
        } else {
            AbstractTextParameter parent = parametersStack.peek();
            parent.addTextParameter(pop);
        }
    }

    @Override
    public void enterText(DataSetParameterParser.TextContext ctx) {
        parametersStack.push(
                new TextParameter(
                        ctx.getText(),
                        parameterPositionContext
                )
        );
    }

    @Override
    public void exitText(DataSetParameterParser.TextContext ctx) {
        AbstractTextParameter pop = parametersStack.pop();
        if (parametersStack.empty()) {
            parsedParameters.add(pop);
        } else {
            AbstractTextParameter parent = parametersStack.peek();
            parent.setParameters(ctx.getParent().getText());
            parent.addTextParameter(pop);
        }
    }
}

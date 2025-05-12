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

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.TextParameter;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.AbstractMacro;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;
import org.qubership.atp.macros.core.parser.antlr4.MacrosBaseListener;
import org.qubership.atp.macros.core.parser.antlr4.MacrosParser;

import lombok.Getter;
import lombok.Setter;

public class MacroListener extends MacrosBaseListener {

    private static final String ENDS_WITH_ESC_REGEX = ".*\\\\[&$\"'\\\\<>]";

    private MacroContext macroContext;
    private Stack<AbstractTextParameter> parametersStack = new Stack<>();
    @Getter
    private List<AbstractTextParameter> parsedParameters = new LinkedList<>();
    private ParameterPositionContext parameterPositionContext;
    @Setter
    private boolean setEvaluate;
    private boolean isNewParam;

    /**
     * Default constructor.
     */
    public MacroListener(
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext,
            boolean setEvaluate
    ) {
        this.macroContext = macroContext;
        this.parameterPositionContext = parameterPositionContext;
        this.setEvaluate = setEvaluate;
    }

    @Override
    public void enterMacros(MacrosParser.MacrosContext ctx) {
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
    public void exitMacros(MacrosParser.MacrosContext ctx) {
        AbstractTextParameter topTextParameter = parametersStack.pop();
        if (parametersStack.empty()) {
            parsedParameters.add(topTextParameter);
        } else {
            AbstractTextParameter parent = parametersStack.peek();
            addParameter(parent, topTextParameter, ctx);
        }
    }

    @Override
    public void enterText(MacrosParser.TextContext ctx) {
        parametersStack.push(
                new TextParameter(
                        ctx.getText(),
                        parameterPositionContext
                )
        );
    }

    @Override
    public void exitText(MacrosParser.TextContext ctx) {
        AbstractTextParameter topTextParameter = parametersStack.pop();
        if (parametersStack.empty()) {
            parsedParameters.add(topTextParameter);
        } else {
            AbstractTextParameter parent = parametersStack.peek();
            if (isNewParam) {
                addParameter(parent, topTextParameter, ctx);
                isNewParam = false;
            } else {
                List<AbstractTextParameter> childTextParameters = parent.getChildTextParameters();
                int lastIndex = childTextParameters.size() - 1;
                String value = childTextParameters.get(lastIndex).getValue();
                String escaped = ctx.getText();
                if (ctx.ESC().size() != INTEGER_ZERO || value.matches(ENDS_WITH_ESC_REGEX)) {
                    escaped = ctx.getText().substring(1);
                }
                childTextParameters.set(
                        lastIndex, new TextParameter(value + escaped, parameterPositionContext));
            }
        }
    }

    @Override
    public void enterMacroParam(MacrosParser.MacroParamContext ctx) {
        isNewParam = true;
    }

    @Override
    public void exitQuote(MacrosParser.QuoteContext ctx) {
        parsedParameters.add(new TextParameter(
                ctx.getText(),
                parameterPositionContext
        ));
    }

    @Override
    public void exitSlash(MacrosParser.SlashContext ctx) {
        parsedParameters.add(new TextParameter(
                ctx.getText(),
                parameterPositionContext
        ));
    }

    private void addParameter(AbstractTextParameter parent, AbstractTextParameter parameter, ParserRuleContext ctx) {
        if (!parent.isSkipExternalChildrenParse()) {
            parent.addTextParameter(parameter);
        }
        parent.setParameters(ctx.getParent().getParent().getParent().getText());
    }
}

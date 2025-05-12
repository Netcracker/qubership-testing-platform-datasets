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

package org.qubership.atp.dataset.service.jpa.model.tree.params.macros;

import java.util.List;

import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.macros.core.model.Macros;
import org.qubership.atp.macros.core.processor.SimpleContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AtpMacro extends AbstractMacro {

    private static final String UNKNOWN = "Unknown ";
    private final Macros macros;

    public AtpMacro(String realMacroName,
                    MacroContext macroContext,
                    ParameterPositionContext parameterPositionContext,
                    Macros macros) {
        super(realMacroName, macroContext, parameterPositionContext);
        this.macros = macros;
    }

    @Override
    String getEvaluatedValue(List<String> arguments) {
        log.debug("AtpMacro#getEvaluatedValue(childrenValue: {})", arguments);
        MacroContext macroContext = getMacroContext();
        SimpleContext simpleContext = new SimpleContext();
        simpleContext.setContextParameters(macroContext.getAtpContext());
        String evaluatedValue = macroContext.getMacrosCalculator().calculate(macros, arguments, simpleContext);

        String macrosNameWithMarkerAndArguments = this.getUnevaluatedValue(String.join(",", arguments));
        if (evaluatedValue.equals(UNKNOWN + macros.getName())) {
            evaluatedValue = macrosNameWithMarkerAndArguments;
            this.setEvaluate(false);
        }
        log.debug("Evaluated value: {}. Macro: {}", evaluatedValue, macrosNameWithMarkerAndArguments);
        return evaluatedValue;
    }
}

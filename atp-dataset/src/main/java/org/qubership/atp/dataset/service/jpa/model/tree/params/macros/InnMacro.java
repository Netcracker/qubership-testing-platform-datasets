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

@Deprecated
public class InnMacro extends AbstractMacro {
    public static final String MACRO_NAME = "INN";
    public static final int[] FACTORS = new int[] {2, 4, 10, 3, 5, 9, 4, 6, 8};

    public InnMacro(
            String realMacroName,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        super(realMacroName, macroContext, parameterPositionContext);
    }

    @Override
    public String getEvaluatedValue(List<String> arguments) {
        StringBuilder result = new StringBuilder();
        int last = 0;
        for (int i = 0; i < 9; i++) {
            int floor = (int) Math.floor(Math.random() * (10 - 1) + 1);
            result.append(floor);
            last += FACTORS[i] * floor;
        }
        last = last % 11 % 10;
        result.append(last);
        return result.toString();
    }
}

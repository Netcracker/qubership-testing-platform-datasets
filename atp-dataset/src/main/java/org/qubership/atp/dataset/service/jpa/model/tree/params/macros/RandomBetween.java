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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;

@Deprecated
public class RandomBetween extends AbstractMacro {
    public static final String MACRO_NAME = "RANDOMBETWEEN";

    public RandomBetween(
            String realMacroName,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        super(realMacroName, macroContext, parameterPositionContext);
    }

    @Override
    public String getEvaluatedValue(List<String> arguments) {
        if (CollectionUtils.isEmpty(arguments)) {
            return "No arguments";
        }
        List<String> stringArguments = new LinkedList<>();
        for (String split : arguments) {
            if (StringUtils.isNotEmpty(split)) {
                stringArguments.add(split);
            }
        }
        if (stringArguments.size() != 2) {
            return "Invalid arguments count";
        }
        List<Long> integerArguments = new LinkedList<>();
        for (String stringArgument : stringArguments) {
            String trimmedValue = stringArgument.trim();
            try {
                integerArguments.add(Long.parseLong(trimmedValue));
            } catch (NumberFormatException e) {
                return "Invalid argument " + trimmedValue;
            }
        }
        long from = integerArguments.get(0);
        long to = integerArguments.get(1);
        if (from == to) {
            return String.valueOf(from);
        }
        if (from > to) {
            Collections.swap(integerArguments, 0, 1);
        }
        return String.valueOf(ThreadLocalRandom.current().nextLong(integerArguments.get(0), integerArguments.get(1)));
    }
}

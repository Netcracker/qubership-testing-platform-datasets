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

package org.qubership.atp.dataset.macros.impl;

import java.util.List;
import java.util.stream.Stream;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.DumbArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArg;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;

@Component
public class SumMacros extends Macros<Integer> {

    public SumMacros() {
        super("SUM");
    }

    @Override
    public Integer evaluate(Stream<? extends MacroArg> input, EvaluationContext context) throws Exception {
        String text = input.findAny()
                .orElseThrow(() -> new IllegalArgumentException("One argument expected"))
                .getText();
        final List<String> strings = Splitter.on(',').trimResults().splitToList(text);
        if (strings.size() != 2) {
            throw new IllegalArgumentException(
                    "Invalid input data. Data must matches to (numeric,numeric). Data: " + input
            );
        }
        return Integer.parseInt(strings.get(0)) + Integer.parseInt(strings.get(1));
    }

    @Override
    public ArgsParser createArgsParser() {
        return new DumbArgsParser();
    }

    @Override
    public boolean doCache() {
        return true;
    }

    @Override
    public String toString() {
        return "sum";
    }
}

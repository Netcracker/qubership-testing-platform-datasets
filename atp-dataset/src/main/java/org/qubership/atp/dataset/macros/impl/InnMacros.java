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

import java.util.stream.Stream;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.DumbArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArg;
import org.springframework.stereotype.Component;

@Component
public class InnMacros extends Macros<String> {

    public static final int[] FACTORS = new int[] {2, 4, 10, 3, 5, 9, 4, 6, 8};

    public InnMacros() {
        super("INN");
    }

    @Override
    public String evaluate(Stream<? extends MacroArg> input, EvaluationContext context) throws Exception {
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

    @Override
    public ArgsParser createArgsParser() {
        return new DumbArgsParser();
    }

    @Override
    public boolean doCache() {
        return true;
    }
}

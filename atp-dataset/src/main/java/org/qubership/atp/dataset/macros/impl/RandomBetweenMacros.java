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

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

@Component
public class RandomBetweenMacros extends Macros<Long> {

    public RandomBetweenMacros() {
        super("RANDOMBETWEEN");
    }

    @Override
    public Long evaluate(Stream<? extends MacroArg> input, EvaluationContext context)
            throws Exception {
        String text = input.findAny()
                .orElseThrow(() -> new IllegalArgumentException("One argument expected"))
                .getText();
        List<String> list = Splitter.on(',').trimResults().splitToList(text);
        validateArgument(list);
        long min = getLong(list.get(0));
        long max = getLong(list.get(1));

        return min + (long) (Math.random() * ((max - min) + 1));
    }

    @Override
    public ArgsParser createArgsParser() {
        return new DumbArgsParser();
    }

    @Override
    public boolean doCache() {
        return true;
    }

    private void validateArgument(List<String> list) {
        Preconditions.checkArgument(
                list.size() == 2,
                "Unable to evaluate RANDOMBETWEEN macros, due to invalid count of arguments received. Args: "
                        + list
        );
    }

    private long getLong(String input) {
        Preconditions.checkArgument(
                input != null && input.matches("\\d+"),
                "Invalid argument of RANDOMBETWEEN macros. Expected numeric, but received: " + input
        );
        return Long.parseLong(input);
    }
}

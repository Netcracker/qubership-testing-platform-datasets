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

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.DotSeparatedArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArg;
import org.qubership.atp.dataset.macros.args.MacroArgFactory;
import org.qubership.atp.dataset.macros.args.MacroArgsFactory;
import org.qubership.atp.dataset.macros.args.SignatureArg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * atp-dataset-aggregator
 */
@Component
public class ReferenceThisDsMacros extends Macros<String> {

    public static final String MACROS_DEFINITION = "REF_THIS";

    @Autowired
    private ReferenceToDslMacros referenceDslMacros;
    @Autowired
    private MacroArgsFactory args;

    public ReferenceThisDsMacros() {
        super(MACROS_DEFINITION);
    }

    @Override
    public String evaluate(Stream<? extends MacroArg> input, EvaluationContext context) throws Exception {
        input = Stream.concat(Stream.of(args.dsl(), args.ds()), input);
        return referenceDslMacros.evaluate(input, context);
    }

    @Override
    public ArgsParser createArgsParser() {
        return new DotSeparatedArgsParser(args) {

            @Override
            protected SignatureArg createArg(int index, @Nonnull MacroArgFactory args) throws Exception {
                return args.attr();
            }
        };
    }

    @Override
    public boolean doCache() {
        return referenceDslMacros.doCache();
    }
}

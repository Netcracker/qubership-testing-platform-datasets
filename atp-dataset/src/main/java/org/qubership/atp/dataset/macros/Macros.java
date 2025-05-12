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

package org.qubership.atp.dataset.macros;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArg;
import org.qubership.atp.dataset.macros.args.TextArg;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.model.Parameter;

public abstract class Macros<T> {

    private final String definition;

    public Macros(@Nonnull String definition) {
        this.definition = definition;
    }

    public T evaluate(String input, EvaluationContext context) throws Exception {
        return evaluate(Stream.of(TextArg.of(new Position(0, input.length()), input)), context);
    }

    /**
     * Evaluates {@link Parameter#getText()} expressions.
     *
     * @param input   {@link Parameter#getText()}  expression
     * @param context to evaluate in.
     * @return result of evaluation.
     * @throws EvalException if something go wrong
     */
    public abstract T evaluate(Stream<? extends MacroArg> input, EvaluationContext context) throws Exception;

    public abstract ArgsParser createArgsParser();

    public String getDefinition() {
        return definition;
    }

    public abstract boolean doCache();
}

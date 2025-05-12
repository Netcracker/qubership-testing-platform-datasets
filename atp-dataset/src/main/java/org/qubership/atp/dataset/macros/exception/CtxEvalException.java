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

package org.qubership.atp.dataset.macros.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.processor.MacroContextBase;

import com.google.common.collect.Lists;

public class CtxEvalException extends EvalException {

    private final String source;
    private final Object ctx;

    /**
     * Wraps a macro context with cause exception.
     */
    public CtxEvalException(@Nonnull String message, @Nonnull MacroContextBase ctx, @Nullable Throwable cause) {
        super(message, cause);
        this.ctx = ctx;
        this.source = ctx.toString();
    }

    public CtxEvalException(@Nonnull String message, @Nonnull EvaluationContext ctx) {
        this(message, ctx, null);
    }

    /**
     * Wraps a macro context with cause exception.
     */
    public CtxEvalException(@Nonnull String message, @Nonnull EvaluationContext ctx, @Nullable Throwable cause) {
        super(message, cause);
        this.ctx = ctx;
        List<EvaluationContext> fromParent = new ArrayList<>();
        EvaluationContext current = ctx;
        while (current != null) {
            fromParent.add(current);
            current = current.getParent();
        }
        this.source = Lists.reverse(fromParent).stream().map(Object::toString)
                .collect(Collectors.joining("<into>"));
    }

    public CtxEvalException(@Nonnull String message, @Nonnull MacroContextBase ctx) {
        this(message, ctx, null);
    }

    @Nonnull
    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "Problem here: " + ctx + "\n" + super.toString();
    }
}

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

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.macros.args.RefArg;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.processor.MacroContext;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.utils.OverlapItem;

public interface EvaluationContext {

    @Nullable
    EvaluationContext getParent();

    @Nonnull
    DataSetList getDsl();

    @Nonnull
    DataSet getDs();

    /**
     * When you are searching for params in shared context by using refs, defined in this context,
     * you should relativize attribute path like if param was defined in shared context.
     */
    @Nonnull
    List<RefArg<Attribute>> relativizePathFromSharedToThis(@Nonnull EvaluationContext shared,
                                                           @Nonnull List<RefArg<Attribute>> attrArgs);

    /**
     * Returns root context of a context chain.
     */
    @Nonnull
    default EvaluationContext getRoot() {
        EvaluationContext result = this;
        while (getParent() != null) {
            result = getParent();
        }
        return result;
    }

    /**
     * Executes {@link MacroContext.ContextedTask} in a context chain. Starting context is the
     * provided one (current). Going upper through callers contexts if the task failed with an
     * exception. Returns first valid task result.
     *
     * @param of typically is {@link #getStrictContexts()}.
     * @throws CtxEvalException if args are not resolved or task failed with an exception in all
     *                          possible contexts.
     */
    @Nonnull
    <O> O executeInAnyContext(@Nonnull Iterator<? extends EvaluationContext> of,
                              @Nonnull MacroContext.ContextedTask<O> toExecute) throws CtxEvalException;

    void resolveArguments(@Nonnull List<? extends RefArg<?>> args) throws Exception;

    /**
     * Finds context to evaluate parameter in. Evaluates parameter using found context.
     *
     * @param target is {@code source.getParameter().get()}. Ensure that it exists first.
     * @param value  is {@code source.getParameter().get().getValue().get()}. Ensure that it exists
     *               first.
     * @throws IllegalArgumentException if {@code value} is empty.
     */
    @Nonnull
    String evaluate(@Nonnull OverlapItem.Reachable source,
                    @Nonnull Parameter target,
                    @Nonnull String value) throws CtxEvalException;

    /**
     * Evaluates parameter using this context. Use {@link #evaluate(OverlapItem.Reachable,
     * Parameter, String)} when possible!
     *
     * @param parameter to evaluate.
     * @param value     is {@code parameter.getValue().get()}. Ensure that it is exists first.
     * @throws IllegalArgumentException if {@code value} is empty.
     */
    @Nonnull
    String evaluate(@Nonnull Parameter parameter, @Nonnull String value) throws CtxEvalException;

    /**
     * Evaluates text using this context. Use {@link #evaluate(Parameter, String)} when possible!
     *
     * @param inputText text to evaluate.
     * @throws IllegalArgumentException if {@code value} is empty.
     */
    @Nonnull
    String evaluate(@Nonnull String inputText) throws CtxEvalException;

    /**
     * May not be empty. Returned contexts are relative and {@link #relativizePathFromSharedToThis(EvaluationContext,
     * List)} should be used for each of them.
     *
     * @return callers plus this.
     */
    Iterator<? extends EvaluationContext> getStrictContexts();

    /**
     * May be empty. Returned contexts are not relative and {@link #relativizePathFromSharedToThis(EvaluationContext,
     * List)} should not be used for each of them. Used when no one string context fits to your
     * needs.
     *
     * @return additional contexts.
     */
    Iterator<? extends EvaluationContext> getNonStrictContexts();
}

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

package org.qubership.atp.dataset.macros.processor;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.parser.Token;
import org.qubership.atp.dataset.macros.parser.TokenType;
import org.qubership.atp.dataset.macros.parser.TokensIterator;

public abstract class AbstractMacroProcessor<T extends MacroContextBase> extends MacroProcessorBase<T> {

    protected final StringBuilder tail;
    protected final MacroRegistry registry;
    protected final TokensIterator tokens;
    protected String lastEvaluationResult;

    protected AbstractMacroProcessor(StringBuilder tail, MacroRegistry registry) {
        this.tail = tail;
        this.registry = registry;
        this.tokens = new TokensIterator(registry, tail);
    }

    public String getValue() throws CtxEvalException {
        assertEvaluationEnded();
        return getText(state);
    }

    /**
     * Evaluates a part of an expression.
     *
     * @return true if has next
     * @throws CtxEvalException if got evaluation error
     */
    public boolean evaluateNext() throws CtxEvalException {
        if (!tokens.hasNext()) {
            return reduceTreeLevel() && evaluateNext();
        }
        Token next = tokens.next();
        read(next);
        return true;
    }

    @Override
    protected boolean reevaluateRootLevel(@Nonnull T root) throws CtxEvalException {
        String evaluatedRoot = getText(root);
        if (evaluatedRoot.isEmpty() || evaluatedRoot.equals(lastEvaluationResult)) {
            return false;
        }
        tokens.reset();
        this.tail.append(evaluatedRoot);
        root.uninitialize();
        dropText(root);
        lastEvaluationResult = evaluatedRoot;
        return true;
    }

    @Nonnull
    protected abstract String getArguments(@Nonnull T context);

    @Nonnull
    protected abstract String getText(@Nonnull T context);

    protected abstract void dropArguments(@Nonnull T context);

    protected abstract void dropText(@Nonnull T context);

    @Override
    protected void reduce(@Nonnull T parent, @Nonnull T child) throws CtxEvalException {
        this.tail.append(getArguments(parent)).append(TokenType.FORMULA_END.fromValue.apply(getText(child)));
        this.tokens.notifyReduce();
        dropArguments(parent);
    }

    /**
     * Evaluates full expression.
     *
     * @return evaluated value.
     */
    public String evaluateAll() throws CtxEvalException {
        try {
            boolean hasNext;
            do {
                hasNext = evaluateNext();
            } while (hasNext);
            return getValue();
        } catch (RuntimeException e) {
            throw new CtxEvalException(this.state.toString() + "<error here>" + tail, state, e);
        }
    }
}

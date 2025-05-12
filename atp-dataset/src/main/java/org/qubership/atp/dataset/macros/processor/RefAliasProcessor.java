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
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.macros.parser.Token;
import org.qubership.atp.dataset.macros.parser.TokensIterator;

public abstract class RefAliasProcessor extends MacroProcessorBase<RefAliasContext> {

    protected final MacroRegistry registry;
    protected final TokensIterator tokens;

    /**
     * Operates with ref alias macro context.
     */
    public RefAliasProcessor(@Nonnull MacroRegistry registry,
                             @Nonnull String inputText) {
        this.registry = registry;
        this.tokens = new TokensIterator(registry, new StringBuilder(inputText));
        state = new RefAliasContext(this, registry);
    }

    /**
     * Returns a result value when processing ends.
     */
    public String getValue() throws EvalException {
        assertEvaluationEnded();
        return state.text.toString();
    }

    /**
     * Evaluates next part of a macro, returns true if has next.
     */
    public boolean evaluateNext() throws EvalException {
        if (!tokens.hasNext()) {
            return reduceTreeLevel();
        }
        Token next = tokens.next();
        read(next);
        return true;
    }

    /**
     * Evaluates the macro. Returns evaluated value.
     */
    public String evaluateAll() throws EvalException {
        boolean hasNext;
        do {
            hasNext = evaluateNext();
        } while (hasNext);
        return getValue();
    }

    @Override
    protected boolean reevaluateRootLevel(@Nonnull RefAliasContext root) throws CtxEvalException {
        return false;
    }

    @Override
    protected void reduce(@Nonnull RefAliasContext parent, @Nonnull RefAliasContext child) throws CtxEvalException {
        parent.evaluate();
        parent.offset = child.offset;
        parent.replacementDiff += child.replacementDiff;
        parent.uninitialize();
    }

    @Override
    protected RefAliasContext createChildContext(@Nonnull RefAliasContext parent, @Nonnull String macro) {
        return new RefAliasContext(parent, macro);
    }

    protected abstract void notifyRefsFound(RefAliasContext context, ArgsParser.Result args)
            throws CtxEvalException;
}

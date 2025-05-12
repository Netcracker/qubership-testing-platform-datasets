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
import javax.annotation.Nullable;

import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.parser.Token;

public abstract class MacroProcessorBase<T extends MacroContextBase> {

    protected static final Start START = new Start();
    protected static final End END = new End();
    protected T state;

    /**
     * Invoked on the end of root macro processing. Should drop text into tail and parse again.
     * Returns true if text has changed after extra processing step.
     */
    protected abstract boolean reevaluateRootLevel(@Nonnull T root) throws CtxEvalException;

    /**
     * Child will be thrown away. Child is evaluated - content in it's text. Parent may has an
     * arguments, which should be reevaluated too. Sample: this.tail.append(parent.args).append(child.text).append(")");
     * parent.args.clear();
     */
    protected abstract void reduce(@Nonnull T parent,
                                   @Nonnull T child) throws CtxEvalException;

    protected void read(Token token) throws CtxEvalException {
        state = (T) state.strategy.read(token, state);
    }

    /**
     * Invoked on the end of siblings processing. End of macros tree level processing. Should union
     * edge with parent, push the union result into tokens, return new edge. Or return false if no
     * parent to union with and this is a root level.
     */
    public boolean reduceTreeLevel() throws CtxEvalException {
        MacroContextBase state = this.state.strategy.reduceTreeLevel(this.state);
        if (state == null) {
            return false;
        }
        this.state = (T) state;
        return true;
    }

    /**
     * Evaluation result will be in state text.
     */
    public void assertEvaluationEnded() throws CtxEvalException {
        state.strategy.assertEvaluationEnded(state);
    }

    protected abstract T createChildContext(@Nonnull T parent,
                                            @Nonnull String macro);

    @Override
    public String toString() {
        return state.toString();
    }

    protected interface Strategy {

        @Nonnull
        MacroContextBase read(Token token, MacroContextBase state) throws CtxEvalException;

        void assertEvaluationEnded(MacroContextBase state) throws CtxEvalException;

        @Nullable
        MacroContextBase reduceTreeLevel(MacroContextBase state) throws CtxEvalException;
    }

    private static class Start implements Strategy {

        @Nonnull
        @Override
        public MacroContextBase read(Token token, MacroContextBase state) throws CtxEvalException {
            switch (token.getKey()) {
                case FORMULA_START:
                    return state.getProcessor().createChildContext(state, token.getValue());
                case FORMULA_END:
                    state.pushArguments(token.getValue());
                    state.evaluate();
                    state.uninitialize();
                    return state;
                default:
                    state.pushArguments(token.getValue());
                    return state;
            }
        }

        @Override
        public void assertEvaluationEnded(MacroContextBase state) throws CtxEvalException {
            throw new CtxEvalException("Macro is not closed properly", state);
        }

        @Nullable
        @Override
        public MacroContextBase reduceTreeLevel(MacroContextBase state) throws CtxEvalException {
            throw new CtxEvalException("Macro is not ended properly", state);
        }
    }

    private static class End implements Strategy {

        @Nonnull
        @Override
        public MacroContextBase read(Token token, MacroContextBase state) throws CtxEvalException {
            switch (token.getKey()) {
                case FORMULA_START:
                    state.initialize(token.getValue());
                    return state;
                case FORMULA_END:
                    if (state.parent == null) {
                        throw new CtxEvalException("Unexpected macro ending: " + token, state);
                    }
                    state.pushText(token.getValue());
                    MacroContextBase result = reduceTreeLevel(state);
                    if (result == null) {
                        throw new CtxEvalException("Unexpected macro ending: " + token, state);
                    }
                    return result;
                default:
                    state.pushText(token.getValue());
                    return state;
            }
        }

        @Override
        public void assertEvaluationEnded(MacroContextBase state) throws CtxEvalException {
            if (state.parent != null) {
                throw new CtxEvalException("Macro is not closed properly", state);
            }
        }

        @Nullable
        @Override
        public MacroContextBase reduceTreeLevel(MacroContextBase state) throws CtxEvalException {
            if (state.parent == null) {
                if (state.getProcessor().reevaluateRootLevel(state)) {
                    return state;
                }
                return null;
            }
            state.getProcessor().reduce(state.parent, state);
            return state.parent;
        }
    }
}

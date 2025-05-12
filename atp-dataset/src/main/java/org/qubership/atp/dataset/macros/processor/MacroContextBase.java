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

import org.qubership.atp.dataset.macros.exception.CtxEvalException;

public abstract class MacroContextBase {

    protected final MacroContextBase parent;
    protected MacroProcessorBase.Strategy strategy;

    /**
     * Creates root.
     */
    public MacroContextBase() {
        this.parent = null;
        this.strategy = MacroProcessorBase.END;
    }

    /**
     * Creates child.
     */
    public MacroContextBase(@Nonnull MacroContextBase parent,
                            @Nonnull String macro) {
        this.parent = parent;
        this.strategy = MacroProcessorBase.START;
    }

    /**
     * Initializes with macro. Should clear args in case it was uninitialized previously.
     */
    public void initialize(@Nonnull String macro) {
        this.strategy = MacroProcessorBase.START;
    }

    /**
     * Invoked only on initialized context.
     */
    protected abstract void pushArguments(@Nonnull String args);

    /**
     * Args are filled up now. Result of evaluation should be pushed into state text.
     */
    protected abstract void evaluate() throws CtxEvalException;

    /**
     * Invoked after {@link #evaluate()}. Reused for next sibling which will complete the text.
     */
    public void uninitialize() {
        this.strategy = MacroProcessorBase.END;
    }

    /**
     * Invoked only on uninitialized context.
     */
    protected abstract void pushText(@Nonnull String text);

    @Nonnull
    public abstract MacroProcessorBase getProcessor();
}

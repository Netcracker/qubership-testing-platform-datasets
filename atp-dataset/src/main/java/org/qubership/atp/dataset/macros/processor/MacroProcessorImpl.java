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

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.cache.MacroCacheKey;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;

public class MacroProcessorImpl extends AbstractMacroProcessor<MacroContext> {
    private final MacroCacheKey cache;

    /**
     * Operates with macro context.
     */
    public MacroProcessorImpl(@Nonnull EvaluationContext current,
                              @Nonnull MacroRegistry registry,
                              @Nonnull AliasWrapperService wrapperService,
                              @Nonnull String inputText,
                              @Nonnull MacroCacheKey cache) {
        super(new StringBuilder(inputText), registry);
        this.cache = cache;
        state = new MacroContext(wrapperService, current, this, registry, current);
    }

    @Override
    protected MacroContext createChildContext(@Nonnull MacroContext parent, @Nonnull String macro) {
        return new MacroContext(parent, macro);
    }

    @Nonnull
    @Override
    protected String getArguments(@Nonnull MacroContext macroContext) {
        return macroContext.args.toString();
    }

    @Nonnull
    @Override
    protected String getText(@Nonnull MacroContext macroContext) {
        return macroContext.text.toString();
    }

    @Override
    protected void dropArguments(@Nonnull MacroContext macroContext) {
        macroContext.beforeReevaluation();
    }

    @Override
    protected void dropText(@Nonnull MacroContext macroContext) {
        macroContext.text.setLength(0);
    }

    @Override
    protected void reduce(@Nonnull MacroContext parent, @Nonnull MacroContext child) throws CtxEvalException {
        super.reduce(parent, child);
        if (!child.doCache) {
            parent.doCache = false;
        }
    }

    @Override
    public String getValue() throws CtxEvalException {
        String result = super.getValue();
        if (state.doCache) {
            cache.cacheValue(result);
        }
        return result;
    }
}

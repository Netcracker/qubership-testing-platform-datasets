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
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.parser.TokenType;

public final class RefAliasContext extends MacroContextBase {

    protected final StringBuilder text = new StringBuilder();
    private final RefAliasProcessor processor;
    private final MacroRegistry registry;
    public int offset;
    public int replacementDiff;
    protected Macros macros;
    protected ArgsParser args;
    private boolean hasChildren;

    /**
     * Macro context used for ref alias processing.
     */
    public RefAliasContext(@Nonnull RefAliasProcessor processor,
                           @Nonnull MacroRegistry registry) {
        super();
        this.processor = processor;
        this.registry = registry;
        this.offset = 0;
        this.replacementDiff = 0;
    }

    /**
     * Child macro context used for ref alias processing.
     */
    public RefAliasContext(@Nonnull RefAliasContext parent, @Nonnull String macro) {
        super(parent, macro);
        this.processor = parent.processor;
        this.registry = parent.registry;
        this.offset = parent.offset;
        this.replacementDiff = parent.replacementDiff;
        initialize(macro);
        parent.hasChildren = true;
    }

    @Override
    public void initialize(@Nonnull String macro) {
        super.initialize(macro);
        macros = registry.getMacros(macro);
        args = macros.createArgsParser();
        hasChildren = false;
        this.offset += macro.length() + TokenType.FORMULA_START.extraSymbolsCount;
    }

    @Override
    public void uninitialize() {
        this.offset += TokenType.FORMULA_END.extraSymbolsCount;
        super.uninitialize();
    }

    @Override
    protected void pushArguments(@Nonnull String args) {
        this.args.append(args);
        this.offset += args.length();
    }

    @Override
    protected void evaluate() throws CtxEvalException {
        ArgsParser.Result parsedArgs;
        if (hasChildren) {
            parsedArgs = args.tryParse();
        } else {
            parsedArgs = args.parseToTheEnd();
        }
        getProcessor().notifyRefsFound(this, parsedArgs);
    }

    @Override
    protected void pushText(@Nonnull String text) {
        this.text.append(text);
        this.offset += text.length();
    }

    @Nonnull
    @Override
    public RefAliasProcessor getProcessor() {
        return processor;
    }
}

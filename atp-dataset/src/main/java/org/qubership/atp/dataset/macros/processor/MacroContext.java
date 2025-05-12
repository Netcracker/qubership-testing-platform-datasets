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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.Macros;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.RefArg;
import org.qubership.atp.dataset.macros.args.SignatureArg;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.parser.TokenType;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.UnwrapStatus;

public final class MacroContext extends MacroContextBase {

    protected final AliasWrapperService wrapperService;
    private final EvaluationContext parent;
    private final MacroProcessorImpl processor;
    private final MacroRegistry registry;
    private final EvaluationContext evaluationContext;
    protected boolean doCache = true;
    protected Macros macros;
    protected ArgsParser args;
    protected StringBuilder text = new StringBuilder();
    private int wrappedArgs;

    /**
     * For root {@code parent == null} or refs {@code parent != null}.
     */
    public MacroContext(@Nonnull AliasWrapperService wrapperService,
                        @Nullable EvaluationContext parent,
                        @Nonnull MacroProcessorImpl processor,
                        @Nonnull MacroRegistry registry, EvaluationContext evaluationContext) {
        super();
        this.wrapperService = wrapperService;
        this.parent = parent;
        this.processor = processor;
        this.registry = registry;
        this.evaluationContext = evaluationContext;
        this.wrappedArgs = -1;//means all are wrapped
    }

    /**
     * For nested.
     */
    public MacroContext(@Nonnull MacroContext parent, @Nonnull String macro) {
        super(parent, macro);
        this.wrapperService = parent.wrapperService;
        this.parent = parent.parent;
        this.processor = parent.processor;
        this.registry = parent.registry;
        this.evaluationContext = parent.evaluationContext;
        this.wrappedArgs = -1;
        this.doCache = parent.doCache;
        initialize(macro);
        parent.wrappedArgs = 0;
    }

    @Override
    public void initialize(@Nonnull String macro) {
        super.initialize(macro);
        macros = registry.getMacros(macro);
        args = macros.createArgsParser();
    }

    @Override
    protected void pushArguments(@Nonnull String args) {
        this.args.append(args);
    }

    @Override
    protected void pushText(@Nonnull String text) {
        this.text.append(text);
    }

    @Nonnull
    @Override
    public MacroProcessorImpl getProcessor() {
        return processor;
    }

    @Override
    protected void evaluate() throws CtxEvalException {
        assert this.macros != null;
        assert this.args != null;
        ArgsParser.Result result = this.args.parseToTheEnd();
        Optional<Exception> error = result.getError();
        if (error.isPresent()) {
            throw new CtxEvalException("Can not parse args: " + this.args, evaluationContext, error.get());
        }
        Optional<String> unparsed = result.getUnparsed();
        if (unparsed.isPresent()) {
            throw new CtxEvalException("Macro arguments not fully parsed: " + unparsed.get(), this);
        }
        try {
            unwrapAliases(result);
        } catch (Exception e) {
            throw new CtxEvalException("Can not unwrap aliases: " + result, evaluationContext, e);
        }
        Object evaluate;
        try {
            evaluate = this.macros.evaluate(result.getParsed().stream(), evaluationContext);
        } catch (CtxEvalException e) {
            throw e;
        } catch (Exception e) {
            throw new CtxEvalException("Can not evaluate macro ["
                    + this.macros + "] with params ["
                    + result.getParsed() + "]", evaluationContext, e);
        }
        this.text.append(Optional.ofNullable(evaluate).map(Objects::toString).orElse(""));
    }

    private void unwrapAliases(ArgsParser.Result result) {
        List<SignatureArg> parsed = result.getParsed();
        int wrappedArgs = this.wrappedArgs == -1 ? parsed.size() : this.wrappedArgs;
        Iterator<SignatureArg> iterator = parsed.iterator();
        for (int i = 0; i < wrappedArgs; i++) {
            Optional<? extends RefArg.Signature<?>> signature = iterator.next().asRef();
            if (!signature.isPresent()) {
                continue;//nothing to init
            }
            RefArg.Signature<?> arg = signature.get();
            UnwrapStatus unwrapStatus = wrapperService.unWrapAlias(arg);
            switch (unwrapStatus) {
                case UNUSED:
                    iterator.remove();
                    break;
                default:
                    break;
            }
        }
    }

    protected void beforeReevaluation() {
        wrappedArgs = args.tryParse().getParsed().size();
        args.clear();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (strategy == MacroProcessorBase.START) {
            //text == prefix
            result.append(text);
        }
        if (macros != null) {
            result.append(TokenType.FORMULA_START.fromValue.apply(macros.getDefinition()));
        }
        if (args != null) {
            if (strategy == MacroProcessorBase.END) {
                //already evaluated
                result.append(TokenType.FORMULA_END.fromValue.apply(args.toString()));
            } else {
                //not evaluated yet
                result.append(args.toString());
            }
        }
        if (strategy == MacroProcessorBase.END) {
            //text == part is currently evaluating
            result.append(text);
        }
        return result.toString();
    }

    public interface ContextedTask<O> {

        O apply(@Nonnull EvaluationContext ctx) throws Exception;
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.macros.args.RefArg;
import org.qubership.atp.dataset.macros.cache.Cache;
import org.qubership.atp.dataset.macros.cache.CachedParameter;
import org.qubership.atp.dataset.macros.cache.MacroCacheKey;
import org.qubership.atp.dataset.macros.cache.NoCache;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.impl.reference.ReferenceAliasType;
import org.qubership.atp.dataset.macros.processor.MacroContext;
import org.qubership.atp.dataset.macros.processor.MacroProcessorImpl;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;

public class EvalContextImpl implements EvaluationContext {

    public static final Comparator<EvalContextImpl> SAME_NON_STRICT = Comparator.comparing(ctx -> ctx.getDs().getId());
    private static final String ALL_CONTEXTS_TRIED = "All contexts are tried.";
    protected final MacroRegistry registry;
    protected final AliasWrapperService wrapperService;
    protected final Cache cache;
    private final EvalContextImpl root;
    private final EvalContextImpl parent;
    private final DataSetList dsl;
    private final DataSet ds;
    private final List<Attribute> pathFromParent;
    private final boolean parentIsStrict;
    private final Attribute targetAttrFromParent;
    private Function<String, String> debugIdentifier = AppendDebugIdentifier.INSTANCE;

    /**
     * Contains state of evaluation plus all used resources.
     */
    public EvalContextImpl(@Nullable EvalContextImpl parent,
                           @Nonnull MacroRegistry registry,
                           @Nonnull AliasWrapperService wrapperService,
                           @Nonnull Cache cache,
                           @Nonnull DataSetList dsl,
                           @Nonnull DataSet ds,
                           boolean parentIsStrict,
                           @Nonnull List<Attribute> pathFromParent,
                           @Nullable Attribute targetAttrFromParent) {
        this.parent = parent;
        this.registry = registry;
        this.wrapperService = wrapperService;
        this.cache = cache;
        this.dsl = dsl;
        this.ds = ds;
        this.pathFromParent = pathFromParent;
        this.parentIsStrict = parentIsStrict;
        this.targetAttrFromParent = targetAttrFromParent;
        if (parent == null) {
            this.root = this;
        } else {
            this.root = parent.getRoot();
        }
    }

    private static <O> O executeInContext(@Nonnull EvaluationContext context,
                                          @Nonnull MacroContext.ContextedTask<O> toExecute) throws Exception {
        try {
            return toExecute.apply(context);
        } catch (Exception e) {
            throw new Exception("Operation failed; context used: " + context, e);
        }
    }

    @Nonnull
    private static <T extends Identified & Named> T resolveUsingCtx(
            @Nonnull List<? extends RefArg<?>> previousArgs,
            @Nonnull RefArg<T> arg,
            @Nonnull EvaluationContext context)
            throws Exception {
        try {
            Optional<RefArg.Signature<T>> sigOpt = arg.asSignature();
            if (!sigOpt.isPresent()) {
                RefArg.Context<T> ctx = arg.asContextRef().orElseThrow(() ->
                        new IllegalArgumentException("Expected to be context ref: " + arg));
                return ctx.resolve(context);
            }
            RefArg.Signature<T> sig = sigOpt.get();
            return sig.resolve(previousArgs, context.getDsl().getVisibilityArea(), context.getDsl());
        } catch (Exception e) {
            throw new Exception("Can not resolve argument [" + arg + "]", e);
        }
    }

    @Nullable
    @Override
    public EvalContextImpl getParent() {
        return parent;
    }

    @Nonnull
    @Override
    public DataSetList getDsl() {
        return dsl;
    }

    @Nonnull
    @Override
    public DataSet getDs() {
        return ds;
    }

    @Nonnull
    @Override
    public EvalContextImpl getRoot() {
        return root;
    }

    @Nonnull
    @Override
    public String evaluate(@Nonnull OverlapItem.Reachable source,
                           @Nonnull Parameter target,
                           @Nonnull String value) throws CtxEvalException {
        UUID sourceId = source.getSourceDs().getId();
        MacroCacheKey cacheKey = cache.newKey(new CachedParameter(target, sourceId, source.getUuidPath()));
        String cachedValue = cacheKey.lookupValue();
        if (cachedValue == null) {
            cachedValue = createChild(source, target).evaluate(value, cacheKey);
        }
        return cachedValue;
    }

    @Nonnull
    @Override
    public String evaluate(@Nonnull String inputText) throws CtxEvalException {
        return evaluate(inputText, NoCache.KEY);
    }

    @Nonnull
    protected String evaluate(@Nonnull String inputText, @Nonnull MacroCacheKey cacheKey) throws CtxEvalException {
        Preconditions.checkArgument(!inputText.isEmpty(), "Input should not be empty");
        MacroProcessorImpl macroProcessor = new MacroProcessorImpl(this, registry, wrapperService,
                inputText, cacheKey);
        debugIdentifier = (str) -> macroProcessor + " " + str;
        return macroProcessor.evaluateAll();
    }

    @Nonnull
    @Override
    public String evaluate(@Nonnull Parameter parameter, @Nonnull String value) throws CtxEvalException {
        MacroCacheKey cacheKey = cache.newKey(parameter);
        String cachedValue = cacheKey.lookupValue();
        if (cachedValue != null) {
            return cachedValue;
        }
        return evaluate(value, cacheKey);
    }

    @Nonnull
    @Override
    public List<RefArg<Attribute>> relativizePathFromSharedToThis(@Nonnull EvaluationContext shared,
                                                                  @Nonnull List<RefArg<Attribute>> attrArgs) {
        List<Attribute> result = new LinkedList<>();
        EvalContextImpl current = this;
        while (true) {
            assert current != null;//assert something found
            result.addAll(0, current.pathFromParent);
            if (shared.equals(current)) {
                break;
            }
            current = current.getParent();
        }
        if (result.isEmpty()) {
            return attrArgs;
        }
        return Stream.concat(result.stream().map(attr -> RefArg.of(ReferenceAliasType.ATTR, attr)), attrArgs.stream())
                .collect(Collectors.toList());
    }

    @Nonnull
    protected EvalContextImpl createChild(@Nonnull OverlapItem.Reachable source,
                                          @Nonnull Parameter target) {
        DataSet sourceDs = source.getSourceDs();

        //check for back reference
        if (getDs().equals(sourceDs)) {
            //circular reference is not supported
            //#REF_THIS or #REF_DSL/#REF back reference
            List<Attribute> pathFromSourceDs;
            if (target.isOverlap()) {
                List<Attribute> tail = target.asOverlap().getAttributePath().getPath();
                pathFromSourceDs = new ArrayList<>(source.getFoundByAttrPath().size() + tail.size());
                pathFromSourceDs.addAll(source.getFoundByAttrPath());
                pathFromSourceDs.addAll(tail);
            } else {
                pathFromSourceDs = source.getFoundByAttrPath();
            }
            if (pathFromParent.equals(pathFromSourceDs)) {
                return this;
            } else {
                //not back reference
                return createChild(sourceDs.getDataSetList(),
                        sourceDs, true,
                        pathFromSourceDs, target.getAttribute());
            }
        } else {
            //#REF,#REF_DSL
            DataSet targetDs = source.getTargetDs();
            return createChild(targetDs.getDataSetList(),
                    targetDs, false,
                    Collections.emptyList(), target.getAttribute());
        }
    }

    @Nonnull
    protected EvalContextImpl createChild(@Nonnull DataSetList dsl,
                                          @Nonnull DataSet ds,
                                          boolean parentIsStrict,
                                          @Nonnull List<Attribute> pathFromParent,
                                          @Nonnull Attribute targetAttrFromParent) {
        return new EvalContextImpl(this, registry, wrapperService, cache, dsl, ds,
                parentIsStrict, pathFromParent, targetAttrFromParent);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public <O> O executeInAnyContext(@Nonnull Iterator<? extends EvaluationContext> of,
                                     @Nonnull MacroContext.ContextedTask<O> toExecute) throws CtxEvalException {
        List<Exception> errors = null;
        while (of.hasNext()) {
            EvaluationContext ctx = of.next();
            try {
                O result = executeInContext(ctx, toExecute);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                if (errors == null) {
                    errors = new LinkedList<>();
                }
                errors.add(e);
            }
        }
        if (errors != null) {
            if (errors.size() == 1) {
                throw new CtxEvalException(ALL_CONTEXTS_TRIED, this, errors.iterator().next());
            } else {
                CtxEvalException toThrow = new CtxEvalException(ALL_CONTEXTS_TRIED, this);
                errors.forEach(toThrow::addSuppressed);
                throw toThrow;
            }
        }
        throw new NullPointerException("No value provided");
    }

    @Override
    public void resolveArguments(@Nonnull List<? extends RefArg<?>> args) throws Exception {
        for (int i = 0; i < args.size(); i++) {
            RefArg<?> toResolve = args.get(i);
            List<? extends RefArg<?>> previousArgs = args.subList(0, i);
            resolveUsingCtx(previousArgs, toResolve, this);
        }
    }

    @Override
    public String toString() {
        StringBuilder thisStr = new StringBuilder("#context: DSL [")
                .append(ds.getDataSetList().getName())
                .append("]; DS [")
                .append(ds.getName())
                .append("]");
        if (!pathFromParent.isEmpty()) {
            thisStr.append("; Relative path [")
                    .append(pathFromParent.stream()
                            .map(Named::getName)
                            .collect(Collectors.joining(".")))
                    .append("]; Target attr [")
                    .append(targetAttrFromParent.getName())
                    .append("]");
        }
        return debugIdentifier.apply(thisStr.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EvalContextImpl that = (EvalContextImpl) o;
        return Objects.equals(getDs(), that.getDs())
                && Objects.equals(pathFromParent, that.pathFromParent)
                && parentIsStrict == that.parentIsStrict;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDs(), pathFromParent, parentIsStrict);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Iterator<EvalContextImpl> getStrictContexts() {
        return new AbstractIterator<EvalContextImpl>() {

            private EvalContextImpl next = EvalContextImpl.this;

            @Override
            protected EvalContextImpl computeNext() {
                if (next == null) {
                    return endOfData();
                }
                EvalContextImpl result = next;
                next = result.parentIsStrict ? result.getParent() : null;
                return result;
            }
        };
    }

    @Nonnull
    @Override
    public Iterator<EvalContextImpl> getNonStrictContexts() {
        return new AbstractIterator<EvalContextImpl>() {
            private final TreeSet<EvalContextImpl> checked = new TreeSet<>(SAME_NON_STRICT);
            private EvalContextImpl next = EvalContextImpl.this;
            private boolean isStrict = true;

            @Override
            protected EvalContextImpl computeNext() {
                //seek to valid
                while (next != null && (isStrict && next.pathFromParent.isEmpty() || checked.contains(next))) {
                    //skip
                    isStrict = next.parentIsStrict;
                    next = next.getParent();
                }
                if (next == null) {
                    return endOfData();
                }
                //got valid
                EvalContextImpl result = next;
                checked.add(result);
                next = result.getParent();
                return result;
            }
        };
    }

    private static class AppendDebugIdentifier implements Function<String, String> {

        private static AppendDebugIdentifier INSTANCE = new AppendDebugIdentifier();

        @Override
        public String apply(String s) {
            return s;
        }
    }
}

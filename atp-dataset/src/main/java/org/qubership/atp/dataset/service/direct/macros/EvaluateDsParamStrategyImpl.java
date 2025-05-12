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

package org.qubership.atp.dataset.service.direct.macros;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.macros.EvalContextImpl;
import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.cache.Cache;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.utils.OverlapItem;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Evaluates text value of the parameter.
 */
public class EvaluateDsParamStrategyImpl implements EvaluateDsParamStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluateDsParamStrategyImpl.class);
    protected final MacroRegistry registry;
    protected final AliasWrapperService wrapperService;
    protected final Cache cache;
    private final boolean acceptFails;

    /**
     * Evaluates text value of the parameter.
     */
    public EvaluateDsParamStrategyImpl(@Nonnull MacroRegistry registry,
                                       @Nonnull AliasWrapperService wrapperService,
                                       @Nonnull Cache cache,
                                       boolean acceptFails) {
        this.registry = registry;
        this.wrapperService = wrapperService;
        this.cache = cache;
        this.acceptFails = acceptFails;
    }

    @Nonnull
    @Override
    public Optional<String> apply(@Nonnull OverlapItem.Reachable target) {
        Optional<Parameter> parameterOpt = target.getParameter();
        if (!parameterOpt.isPresent()) {
            return Optional.empty();
        }
        Parameter parameter = parameterOpt.get();
        Optional<String> valueOpt = target.getValue();
        if (!valueOpt.isPresent()) {
            return Optional.empty();
        }
        String value = valueOpt.get();
        if (value.isEmpty()) {
            return Optional.of(value);
        }
        EvalContextImpl context = createContext(
                target.getSourceDs().getDataSetList(), target.getSourceDs(),
                target.getFoundByAttrPath(),
                target.getAttribute());
        try {
            return Optional.of(context.evaluate(target, parameter, value));
        } catch (CtxEvalException e) {
            StringBuilder messageBuilder = new StringBuilder("Error during evaluating [")
                    .append(e.getSource()).append("]");
            if (!acceptFails || Strings.isNullOrEmpty(e.getMessage())) {
                String message = messageBuilder.toString();
                LOG.error(message, e);
                throw new IllegalArgumentException(message, e);
            }
            return Optional.of(Utils.appendFriendlyMessage(messageBuilder, e).toString());
        }
    }

    @Nonnull
    protected EvalContextImpl createContext(@Nonnull DataSetList dsl,
                                            @Nonnull DataSet ds,
                                            @Nonnull List<Attribute> pathFromParent,
                                            @Nullable Attribute targetAttrFromParent) {
        return new EvalContextImpl(null, registry, wrapperService, cache, dsl, ds,
                true, pathFromParent, targetAttrFromParent);
    }

    @Nonnull
    @Override
    public String evaluateText(@Nonnull DataSet ds, @Nonnull String text) {
        if (text.isEmpty()) {
            return text;
        }
        try {
            return createContext(ds.getDataSetList(), ds, Collections.emptyList(), null)
                    .evaluate(text);
        } catch (CtxEvalException e) {
            StringBuilder messageBuilder = new StringBuilder("Error during evaluating [")
                    .append(e.getSource()).append("]");
            if (!acceptFails || Strings.isNullOrEmpty(e.getMessage())) {
                String message = messageBuilder.toString();
                LOG.error(message, e);
                throw new IllegalArgumentException(message, e);
            }
            return Utils.appendFriendlyMessage(messageBuilder, e).toString();
        }
    }
}

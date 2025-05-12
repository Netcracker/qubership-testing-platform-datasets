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

package org.qubership.atp.dataset.service.direct.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.macros.MacroRegistry;
import org.qubership.atp.dataset.macros.args.ArgsParser;
import org.qubership.atp.dataset.macros.args.MacroArg;
import org.qubership.atp.dataset.macros.args.RefArg;
import org.qubership.atp.dataset.macros.args.SignatureArg;
import org.qubership.atp.dataset.macros.exception.CtxEvalException;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.macros.impl.reference.ReferenceAliasType;
import org.qubership.atp.dataset.macros.processor.RefAliasContext;
import org.qubership.atp.dataset.macros.processor.RefAliasProcessor;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.AliasWrapperService;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.IdentifiedService;
import org.qubership.atp.dataset.service.direct.UnwrapStatus;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Service
@Slf4j
public class AliasWrapperServiceImpl implements AliasWrapperService {

    private final UnWrap unwrap = new UnWrap();
    private final MacroRegistry macroRegistry;
    private final Provider<DataSetListService> dslServiceProvider;
    private final Provider<DataSetService> dsServiceProvider;
    private final Provider<AttributeService> attrServiceProvider;

    private static void processRefs(ArgsParser.Result args,
                                    StringBuilder toReplaceIn,
                                    RefAliasContext context,
                                    Replacer replacer) throws CtxEvalException {
        String source = args.toString();
        int argsStartPosition = context.offset - args.toString().length();
        boolean replaced;
        try {
            replaced = replacer.replace(args);
        } catch (Exception e) {
            throw new CtxEvalException("Can not process arguments: " + args, context, e);
        }
        if (replaced) {
            String replacement = args.toString();
            toReplaceIn.replace(argsStartPosition + context.replacementDiff,
                    context.offset + context.replacementDiff, replacement);
            context.replacementDiff += replacement.length() - source.length();
        }
    }

    private String process(@Nonnull String text,
                           @Nonnull Replacer replacer)
            throws EvalException {
        StringBuilder result = new StringBuilder(text);
        new RefAliasProcessor(macroRegistry, text) {
            @Override
            protected void notifyRefsFound(RefAliasContext context, ArgsParser.Result args)
                    throws CtxEvalException {
                processRefs(args, result, context, replacer);
            }
        }.evaluateAll();
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String wrapToAlias(String text, VisibilityArea visibilityArea, DataSetList dsl) {
        if (Strings.isNullOrEmpty(text)) {
            return text;
        }
        try {
            return process(text, new Wrap(visibilityArea, dsl));
        } catch (Exception e) {
            log.warn("Failed wrapping to alias: " + text, e);
        }
        return text;
    }

    @Override
    public <T extends Identified & Named> UnwrapStatus unWrapAlias(RefArg.Signature<T> ref) {
        String source = ref.getText();
        UUID uuid;
        try {
            uuid = UUID.fromString(source);
        } catch (IllegalArgumentException e) {
            return UnwrapStatus.SKIPPED;
        }
        T value;
        try {
            value = getAlias(ref.getType(), uuid);
        } catch (IllegalArgumentException e) {
            //deprecated
            return UnwrapStatus.UNUSED;
        }
        if (value == null) {
            return UnwrapStatus.SKIPPED;
        }
        ref.resolve(value);
        ref.setText(value.getName());
        return UnwrapStatus.UNWRAPPED;
    }

    /**
     * Method converts alias chain to user friendly value: ALIAS_UUID.ALIAS_UUID.ALIAS_UUID to
     * DSL_NAME.DS.ATTR.
     */
    @Override
    public String unWrapAlias(String text) {
        if (Strings.isNullOrEmpty(text)) {
            return text;
        }
        try {
            return process(text, unwrap);
        } catch (Exception e) {
            log.error("Failed unwrapping from alias: " + text, e);
        }
        return text;
    }

    @Nonnull
    private UUID createAlias(@Nonnull ReferenceAliasType type, @Nonnull UUID objectId) {
        return objectId;
    }

    @Nullable
    private <T extends Identified & Named> T getAlias(@Nonnull ReferenceAliasType<T> type, @Nonnull UUID objectId) {
        T result = getAliasObject(type, objectId);
        if (result != null) {
            return result;
        }
        return null;
    }

    @Nullable
    private <T extends Identified & Named> T getAliasObject(@Nonnull ReferenceAliasType<T> type,
                                                            @Nonnull UUID objectId) {
        IdentifiedService<T> service = type.chooseService(dslServiceProvider.get(),
                dsServiceProvider.get(), attrServiceProvider.get());
        return service.get(objectId);
    }

    private interface Replacer {

        boolean replace(ArgsParser.Result args) throws Exception;
    }

    private class Wrap implements Replacer {

        private final VisibilityArea va;
        private final DataSetList dsl;

        private Wrap(VisibilityArea va, DataSetList dsl) {
            this.va = va;
            this.dsl = dsl;
        }

        private void resolve(@Nonnull List<? extends MacroArg> previousArgs,
                             RefArg.Signature<?> source) throws Exception {
            Identified instance = source.resolve(previousArgs, va, dsl);
            UUID result = createAlias(source.getType(), instance.getId());
            source.setText(result.toString());
        }

        @Override
        public boolean replace(ArgsParser.Result args) throws Exception {
            boolean changed = false;
            List<SignatureArg> toResolve = args.getParsed();
            for (int i = 0; i < toResolve.size(); i++) {
                Optional<? extends RefArg.Signature<?>> signature = toResolve.get(i).asRef();
                if (!signature.isPresent()) {
                    continue;
                }
                resolve(toResolve.subList(0, i), signature.get());
                changed = true;
            }
            return changed;
        }
    }

    private class UnWrap implements Replacer {

        @Override
        public boolean replace(ArgsParser.Result args) throws Exception {
            boolean changed = false;
            Iterator<SignatureArg> argsIter = args.getParsed().iterator();
            while (argsIter.hasNext()) {
                SignatureArg macroArg = argsIter.next();
                Optional<? extends RefArg.Signature<?>> signature = macroArg.asRef();
                if (!signature.isPresent()) {
                    continue;
                }
                UnwrapStatus unwrapStatus = AliasWrapperServiceImpl.this.unWrapAlias(signature.get());
                switch (unwrapStatus) {
                    case UNWRAPPED:
                        changed = true;
                        break;
                    case UNUSED:
                        argsIter.remove();
                        break;
                    default:
                        break;
                }
            }
            return changed;
        }
    }
}

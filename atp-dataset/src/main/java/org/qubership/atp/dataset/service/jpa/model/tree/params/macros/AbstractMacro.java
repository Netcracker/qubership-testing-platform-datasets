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

package org.qubership.atp.dataset.service.jpa.model.tree.params.macros;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.dataset.antlr4.TextParameterParser;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractTextParameter;
import org.qubership.atp.macros.core.model.Macros;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMacro extends AbstractTextParameter {
    public static final String DATASET_MACRO_MARKER = "#";
    private static final Logger LOG = LoggerFactory.getLogger(TextParameterParser.class);
    @Getter
    private final MacroContext macroContext;
    @Getter
    private final ParameterPositionContext parameterPositionContext;
    @Setter
    @Getter
    private boolean evaluate = false;
    private final String macroNameWithMarker;

    /**
     * Default constructor.
     * */
    public AbstractMacro(
            String macroNameWithMarker,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        this.macroContext = macroContext;
        this.parameterPositionContext = parameterPositionContext;
        this.macroNameWithMarker = macroNameWithMarker;
    }

    /**
     * Returns macro implementation by it's name.
     * */
    public static AbstractMacro getMacroByName(
            String macroNameWithMarker,
            MacroContext macroContext,
            boolean evaluate,
            ParameterPositionContext parameterPositionContext
    ) {
        AbstractMacro macro;
        String nameWithoutMacroMarker = nonNull(macroNameWithMarker) ? macroNameWithMarker.substring(1) : null;
        String macroMarker = nonNull(macroNameWithMarker) ? macroNameWithMarker.substring(0,1) : null;
        if (ContextMacro.MACRO_NAME.equalsIgnoreCase(nameWithoutMacroMarker)) {
            macro = new ContextMacro(macroNameWithMarker, macroContext, parameterPositionContext);
        } else if (RefMacro.MACRO_NAME.equalsIgnoreCase(nameWithoutMacroMarker)) {
            macro = new RefMacro(macroNameWithMarker, macroContext, parameterPositionContext);
        } else if (DateMacro.MACRO_NAME.equalsIgnoreCase(nameWithoutMacroMarker)
                && DATASET_MACRO_MARKER.equalsIgnoreCase(macroMarker)) {
            macro = new DateMacro(macroNameWithMarker, macroContext, parameterPositionContext);
        } else if (RefDslMacro.MACRO_NAME.equalsIgnoreCase(nameWithoutMacroMarker)) {
            macro = new RefDslMacro(macroNameWithMarker, macroContext, parameterPositionContext);
        } else if (RefThisMacro.MACRO_NAME.equalsIgnoreCase(nameWithoutMacroMarker)) {
            macro = new RefThisMacro(macroNameWithMarker, macroContext, parameterPositionContext);
        } else {
            macro = getAtpOrUnknownMacros(
                macroNameWithMarker, nameWithoutMacroMarker, macroContext, parameterPositionContext);
        }
        macro.setOrder(parameterPositionContext.getOrder());
        macro.setEvaluate(evaluate);
        return macro;
    }

    abstract String getEvaluatedValue(List<String> arguments);

    @Override
    public String getValue() {
        if (!isEvaluate()) {
            return getUnevaluatedValue(parameters);
        }
        try {
            String localValue = getEvaluatedValue(getArguments());
            TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
            if (StringUtils.isEmpty(localValue)) {
                return localValue;
            }
            List<AbstractTextParameter> parsedResult = parser.parse(localValue, evaluate);
            StringBuilder result = new StringBuilder();
            for (AbstractTextParameter abstractTextParameter : parsedResult) {
                result.append(abstractTextParameter.getValue());
            }
            return result.toString();
        } catch (Exception e) {
            LOG.debug("Error macro evaluation", e);
            return "[Error macro evaluation]";
        }
    }

    public String getUnevaluatedValue(String childrenValue) {
        return macroNameWithMarker + "(" + defaultIfEmpty(childrenValue, EMPTY) + ")";
    }

    @Override
    public boolean isNullValue() {
        return false;
    }

    private static AbstractMacro getAtpOrUnknownMacros(String name,
                                                       String nameWithoutMacroMarker,
                                                       MacroContext macroContext,
                                                       ParameterPositionContext parameterPositionContext) {
        Macros macros = getMacros(macroContext.getMacros(), nameWithoutMacroMarker);
        return nonNull(macros)
                ? new AtpMacro(name, macroContext, parameterPositionContext, macros)
                : new UnknownMacro(name, macroContext, parameterPositionContext);
    }

    @Nullable
    private static Macros getMacros(@Nonnull List<Macros> macroses, @Nonnull String key) {
        return macroses.stream()
                .filter(macros -> key.equals(macros.getName()))
                .findFirst()
                .orElse(null);
    }
}

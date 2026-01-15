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

package org.qubership.atp.dataset.service.jpa.model.tree.params;

import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.dataset.antlr4.TextParameterParser;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;
import lombok.Setter;

public class TextParameter extends AbstractTextParameter {
    private String value = null;
    //If parameter has non null fields below, that means it's a root text parameter, and it'll try to use cache.
    @Setter
    @JsonIgnore
    protected Map<ParameterPositionContext, String> cachedValues = null;
    @JsonIgnore
    private ParameterPositionContext positionContext;

    /**
     * Default constructor.
     * */
    public TextParameter(
            String value,
            boolean evaluate,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        TextParameterParser parser = new TextParameterParser(macroContext, parameterPositionContext);
        if (StringUtils.isEmpty(value)) {
            childTextParameters.add(new TextParameter("", parameterPositionContext));
        } else {
            List<AbstractTextParameter> parseResult = parser.parse(value, evaluate);
            childTextParameters.addAll(parseResult);
        }
        setOrder(parameterPositionContext.getOrder());
        this.positionContext = parameterPositionContext;
        //Parser will not add children parameters, and this parameter will parse children internally.
        this.skipExternalChildrenParse = true;
    }

    public TextParameter(String value, ParameterPositionContext parameterPositionContext) {
        setOrder(parameterPositionContext.getOrder());
        this.value = value;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getValue() {
        if (cachedValues != null
                && positionContext != null
                && cachedValues.containsKey(positionContext)
        ) {
            //If value is in cache, that means it was evaluated by demand by reference macro.
            return cachedValues.get(positionContext);
        }
        String result;
        if (!childTextParameters.isEmpty()) {
            result = join(EMPTY, getArguments());
        } else {
            result = value;
        }
        if (cachedValues != null && positionContext != null) {
            cachedValues.put(positionContext, result);
        }
        return result;
    }

    @Override
    public boolean isNullValue() {
        List<AbstractTextParameter> childTextParameters = getChildTextParameters();
        if (childTextParameters.isEmpty()) {
            return Strings.isNullOrEmpty(value);
        }
        for (AbstractTextParameter childTextParameter : childTextParameters) {
            if (!childTextParameter.isNullValue()) {
                return false;
            }
        }
        return true;
    }
}

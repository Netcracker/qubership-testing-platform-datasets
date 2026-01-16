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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

public abstract class AbstractTextParameter extends AbstractParameter {
    @JsonIgnore
    @Getter
    protected List<AbstractTextParameter> childTextParameters = new LinkedList<>();
    @JsonIgnore
    @Setter
    protected String parameters;

    //Is set as TRUE, that means it's root text parameter. Actually it's value as true used only in one case
    // - when macro listener found macroParams. atp-macro parser have some behaviour in current version (1.0.3):
    // it doesn't recognize ',' as 'text' inside macroParams, so, if we don't want to loose separation in params,
    // we have to parse it manually in text parameter constructor. This flag used to prevent adding incorrectly
    // externally parsed children, because we already have them.
    @JsonIgnore
    @Getter
    protected boolean skipExternalChildrenParse = false;

    @Override
    public AttributeTypeName getType() {
        return AttributeTypeName.TEXT;
    }

    public void addTextParameter(AbstractTextParameter textParameter) {
        childTextParameters.add(textParameter);
    }

    @JsonIgnore
    public List<String> getArguments() {
        return childTextParameters.stream()
            .map(AbstractParameter::getValue)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }
}

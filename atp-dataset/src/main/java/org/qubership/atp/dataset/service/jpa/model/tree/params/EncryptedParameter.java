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

import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.tree.params.macros.ParameterPositionContext;

public class EncryptedParameter extends TextParameter {
    public EncryptedParameter(
            String value,
            boolean evaluate,
            MacroContext macroContext,
            ParameterPositionContext parameterPositionContext
    ) {
        super(value, evaluate, macroContext, parameterPositionContext);
    }

    public EncryptedParameter(String value, ParameterPositionContext parameterPositionContext) {
        super(value, parameterPositionContext);
    }

    @Override
    public AttributeTypeName getType() {
        return AttributeTypeName.ENCRYPTED;
    }
}

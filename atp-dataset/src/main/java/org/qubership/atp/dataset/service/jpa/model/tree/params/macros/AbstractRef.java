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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;

import lombok.Getter;

public abstract class AbstractRef extends AbstractMacro {

    @Getter
    protected Set<UUID> dataSets = new HashSet<>();
    @Getter
    protected Set<UUID> dataSetLists = new HashSet<>();

    public AbstractRef(String realMacroName, MacroContext macroContext,
                       ParameterPositionContext parameterPositionContext) {
        super(realMacroName, macroContext, parameterPositionContext);
    }


}
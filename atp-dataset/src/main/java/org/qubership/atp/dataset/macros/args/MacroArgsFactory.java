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

package org.qubership.atp.dataset.macros.args;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.Position;
import org.qubership.atp.dataset.macros.impl.reference.ReferenceAliasType;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.springframework.stereotype.Component;

@Component
public class MacroArgsFactory {

    public RefArg.Context<DataSetList> dsl() {
        return RefArg.of(ReferenceAliasType.DSL, CtxValueSup.dsl());
    }

    public RefArg.Signature<DataSetList> dsl(@Nonnull Position position, @Nonnull String text,
                                             @Nonnull SigValueSup<? extends DataSetList> getDsl) {
        return RefArg.of(ReferenceAliasType.DSL, new ArgSignatureImpl(position, text), getDsl);
    }

    public RefArg.Context<DataSet> ds() {
        return RefArg.of(ReferenceAliasType.DS, CtxValueSup.ds());
    }

    public RefArg.Signature<DataSet> ds(@Nonnull Position position, @Nonnull String text,
                                        @Nonnull SigValueSup<? extends DataSet> getDs) {
        return RefArg.of(ReferenceAliasType.DS, new ArgSignatureImpl(position, text), getDs);
    }

    public RefArg.Signature<Attribute> attr(@Nonnull Position position, @Nonnull String text,
                                            @Nonnull SigValueSup<? extends Attribute> getAttr) {
        return RefArg.of(ReferenceAliasType.ATTR, new ArgSignatureImpl(position, text), getAttr);
    }

    public TextArg.NoSignature text(@Nonnull String text) {
        return TextArg.of(text);
    }

    public TextArg.WithSignature text(@Nonnull Position position, @Nonnull String text) {
        return TextArg.of(position, text);
    }
}

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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.Position;
import org.qubership.atp.dataset.macros.exception.EvalException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.VisibilityArea;

import com.google.common.collect.Lists;

public class MacroArgFactoryImpl implements MacroArgFactory {

    private final MacroArgsFactory delegate;
    private Position position;
    private String text;

    public MacroArgFactoryImpl(MacroArgsFactory delegate) {
        this.delegate = delegate;
    }

    private static Optional<Attribute> getAttribute(@Nonnull DataSetList dsl, @Nonnull String text) {
        return dsl.getAttributes().stream()
                .filter(ds -> text.equals(ds.getName()))
                .findAny();
    }

    /**
     * Creates macro arg by using text and it's position in the macro.
     */
    public MacroArgFactoryImpl withText(Position position, String text) {
        this.position = position;
        this.text = text;
        return this;
    }

    @Override
    public TextArg.WithSignature text() {
        return delegate.text(position, text);
    }

    @Override
    public RefArg.Signature<DataSetList> dsl() {
        return delegate.dsl(position, text, DslByName.INSTANCE);
    }

    @Override
    public RefArg.Signature<DataSet> ds() {
        return delegate.ds(position, text, DsByName.INSTANCE);
    }

    @Override
    public RefArg.Signature<Attribute> attr() {
        return delegate.attr(position, text, AttrByName.INSTANCE);
    }

    private static class DslByName implements SigValueSup<DataSetList> {

        private static final DslByName INSTANCE = new DslByName();

        @Nonnull
        @Override
        public DataSetList get(@Nonnull List<? extends MacroArg> previousArgs,
                               @Nonnull String text,
                               @Nonnull VisibilityArea va,
                               @Nonnull DataSetList contextDsl) throws Exception {
            Optional<DataSetList> byName = va.getDataSetLists().stream()
                    .filter(dsl -> text.equals(dsl.getName()))
                    .findAny();
            if (byName.isPresent()) {
                return byName.get();
            }
            throw new NoSuchElementException("No data set list found by name [" + text
                    + "] in " + va);
        }

        @Override
        public String toString() {
            return "DSL by name in context va";
        }
    }

    private static class DsByName implements SigValueSup<DataSet> {

        private static final DsByName INSTANCE = new DsByName();

        @Nonnull
        @Override
        public DataSet get(@Nonnull List<? extends MacroArg> previousArgs,
                           @Nonnull String text,
                           @Nonnull VisibilityArea contextVa,
                           @Nonnull DataSetList contextDsl) throws Exception {
            DataSetList dsl = contextDsl;
            for (MacroArg arg : Lists.reverse(previousArgs)) {
                Optional<DataSetList> dslOpt = arg.asRef().flatMap(RefArg::asDsl).map(RefArg::get);
                if (dslOpt.isPresent()) {
                    dsl = dslOpt.get();
                    break;
                }
            }
            Optional<DataSet> byName = dsl.getDataSets().stream()
                    .filter(ds -> text.equals(ds.getName()))
                    .findAny();
            if (byName.isPresent()) {
                return byName.get();
            }
            throw new NoSuchElementException("No data set found by name [" + text
                    + "] in " + dsl);
        }

        @Override
        public String toString() {
            return "DS by name";
        }
    }

    private static class AttrByName implements SigValueSup<Attribute> {

        private static final AttrByName INSTANCE = new AttrByName();

        @Nonnull
        @Override
        public Attribute get(@Nonnull List<? extends MacroArg> previousArgs,
                             @Nonnull String text,
                             @Nonnull VisibilityArea contextVa,
                             @Nonnull DataSetList contextDsl) throws Exception {
            DataSetList dsl = contextDsl;
            for (MacroArg arg : Lists.reverse(previousArgs)) {
                Optional<Attribute> attrOpt = arg.asRef().flatMap(RefArg::asAttr).map(RefArg::get);
                if (attrOpt.isPresent()) {
                    Attribute attr = attrOpt.get();
                    dsl = attr.getDataSetListReference();
                    if (dsl == null) {
                        throw new EvalException("No attribute found by name [" + text
                                + "] because dsl ref is not set in parent attribute" + attr);
                    }
                    break;
                }
                Optional<DataSetList> dslOpt = arg.asRef().flatMap(RefArg::asDsl).map(RefArg::get);
                if (dslOpt.isPresent()) {
                    dsl = dslOpt.get();
                    break;
                }
            }
            Optional<Attribute> result = getAttribute(dsl, text);
            if (result.isPresent()) {
                return result.get();
            }
            throw new NoSuchElementException("No attribute found by name [" + text
                    + "] in " + dsl);
        }

        @Override
        public String toString() {
            return "ATTR by name";
        }
    }
}

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

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.impl.reference.ReferenceAliasType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Named;

public abstract class CtxValueSup<T> {

    private CtxValueSup() {
    }

    @SuppressWarnings("unchecked")
    public static <O extends Identified & Named> CtxValueSup<O> ofInstance(@Nonnull ReferenceAliasType<O> type,
                                                                           @Nonnull O instance) {
        return new OfInstance<>(instance);
    }

    public static CtxValueSup<DataSetList> dsl() {
        return Dsl.INSTANCE;
    }

    public static CtxValueSup<DataSet> ds() {
        return Ds.INSTANCE;
    }

    @Nonnull
    public abstract T get(@Nonnull EvaluationContext context);

    private static class Dsl extends CtxValueSup<DataSetList> {
        protected static Dsl INSTANCE = new Dsl();

        @Nonnull
        @Override
        public DataSetList get(@Nonnull EvaluationContext context) {
            return context.getDsl();
        }

        @Override
        public String toString() {
            return "context DSL";
        }
    }

    private static class Ds extends CtxValueSup<DataSet> {
        protected static Ds INSTANCE = new Ds();

        @Nonnull
        @Override
        public DataSet get(@Nonnull EvaluationContext context) {
            return context.getDs();
        }

        @Override
        public String toString() {
            return "context DS";
        }
    }

    private static class OfInstance<T> extends CtxValueSup<T> {
        private final T instance;

        private OfInstance(@Nonnull T instance) {
            this.instance = instance;
        }

        @Nonnull
        @Override
        public T get(@Nonnull EvaluationContext context) {
            return instance;
        }

        @Override
        public String toString() {
            return "instance";
        }
    }

}

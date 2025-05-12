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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.macros.EvaluationContext;
import org.qubership.atp.dataset.macros.impl.reference.ReferenceAliasType;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.VisibilityArea;

import com.google.common.base.Preconditions;

public abstract class RefArg<T extends Named & Identified> implements MacroArg {

    protected final ReferenceAliasType<T> refType;
    protected String text;
    protected T instance = null;

    protected RefArg(@Nonnull ReferenceAliasType<T> refType, @Nonnull String text) {
        this.refType = refType;
        this.text = text;
    }

    /**
     * Used to create arg from macro parameter.
     */
    public static <T extends Named & Identified> RefArg.Signature<T> of(@Nonnull ReferenceAliasType<T> refType,
                                                                        @Nonnull ArgSignature signature,
                                                                        @Nonnull SigValueSup<? extends T> valueSup) {
        return new Signature<>(refType, signature, valueSup);
    }

    /**
     * Used to create arg in runtime.
     */
    public static <T extends Named & Identified> RefArg.Context<T> of(@Nonnull ReferenceAliasType<T> refType,
                                                                      @Nonnull CtxValueSup<? extends T> valueSup) {
        return new Context<>(refType, valueSup);
    }

    /**
     * Used to create arg in runtime.
     */
    public static <T extends Named & Identified> RefArg.Context<T> of(@Nonnull ReferenceAliasType<T> refType,
                                                                      @Nonnull T instance) {
        Context<T> result = new Context<>(refType, CtxValueSup.ofInstance(refType, instance));
        result.resolve(instance);
        return result;
    }

    @Nonnull
    public ReferenceAliasType<T> getType() {
        return refType;
    }

    /**
     * Returns arg of a dsl if it is a dsl arg. Or empty. Safe way to make a cast.
     */
    public Optional<? extends RefArg<DataSetList>> asDsl() {
        if (ReferenceAliasType.DSL == refType) {
            return Optional.of((RefArg<DataSetList>) this);
        }
        return Optional.empty();
    }

    /**
     * Returns arg of a ds if it is a ds arg. Or empty. Safe way to make a cast.
     */
    public Optional<? extends RefArg<DataSet>> asDs() {
        if (ReferenceAliasType.DS == refType) {
            return Optional.of((RefArg<DataSet>) this);
        }
        return Optional.empty();
    }

    /**
     * Returns arg of a attr if it is an attr arg. Or empty. Safe way to make a cast.
     */
    public Optional<? extends RefArg<Attribute>> asAttr() {
        if (ReferenceAliasType.ATTR == refType) {
            return Optional.of((RefArg<Attribute>) this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Signature<T>> asSignature() {
        return Optional.empty();
    }

    public Optional<Context<T>> asContextRef() {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public String getText() {
        return text;
    }

    public void setText(@Nonnull String text) {
        this.text = text;
    }

    public void resolve(@Nullable T value) {
        this.instance = value;
    }

    public boolean isResolved() {
        return instance != null;
    }

    /**
     * Asserts that it was previously resolved.
     *
     * @throws IllegalStateException if not.
     */
    @Nonnull
    public T get() {
        Preconditions.checkState(isResolved(),
                "Not resolved: " + this);
        return instance;
    }

    @Override
    public String toString() {
        return "<" + refType + " with value " + (instance == null ? text : instance.toString()) + ">";
    }

    /**
     * It is a reference, represented as some name/id in macros arguments signature.
     */
    public static class Signature<T extends Identified & Named> extends RefArg<T> implements Supplier<T>, SignatureArg {

        private final ArgSignature signature;
        private final SigValueSup<? extends T> valueSup;


        private Signature(@Nonnull ReferenceAliasType<T> refType, @Nonnull ArgSignature signature,
                          @Nonnull SigValueSup<? extends T> valueSup) {
            super(refType, signature.getSource());
            this.signature = signature;
            this.valueSup = valueSup;
        }

        @Override
        public Optional<Signature<T>> asSignature() {
            return Optional.of(this);
        }

        /**
         * Resolves and memorizes an actual value of an arg.
         */
        @Nonnull
        public T resolve(@Nonnull List<? extends MacroArg> previousArgs,
                         @Nonnull VisibilityArea contextVa,
                         @Nonnull DataSetList contextDsl)
                throws Exception {
            T instance = valueSup.get(previousArgs, text, contextVa, contextDsl);
            resolve(instance);
            return instance;
        }


        @Override
        public String toString() {
            return "<" + valueSup + " with value " + (instance == null ? text : instance.toString()) + ">";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            Signature<?> refArg = (Signature<?>) o;
            return Objects.equals(refType, refArg.refType)
                    && Objects.equals(signature, refArg.signature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(refType, signature);
        }

        @Override
        public Optional<Signature<T>> asRef() {
            return Optional.of(this);
        }

        @Nonnull
        @Override
        public ArgSignature getSignature() {
            return signature;
        }

        @Override
        public Optional<Signature<DataSetList>> asDsl() {
            if (ReferenceAliasType.DSL == refType) {
                return Optional.of((Signature<DataSetList>) this);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Signature<DataSet>> asDs() {
            if (ReferenceAliasType.DS == refType) {
                return Optional.of((Signature<DataSet>) this);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Signature<Attribute>> asAttr() {
            if (ReferenceAliasType.ATTR == refType) {
                return Optional.of((Signature<Attribute>) this);
            }
            return Optional.empty();
        }
    }

    /**
     * An adapter for reference which may be not specified in macro arguments signature and should
     * be taken from context.
     */
    public static class Context<T extends Identified & Named> extends RefArg<T> {

        private final CtxValueSup<? extends T> valueSup;

        private Context(@Nonnull ReferenceAliasType<T> refType,
                        @Nonnull CtxValueSup<? extends T> valueSup) {
            super(refType, valueSup.toString());
            this.valueSup = valueSup;
        }

        /**
         * Resolves and memorizes an actual value of an arg.
         */
        @Nonnull
        public T resolve(@Nonnull EvaluationContext context) {
            T result = this.valueSup.get(context);
            resolve(result);
            return result;
        }

        @Override
        public Optional<Context<T>> asContextRef() {
            return Optional.of(this);
        }

        @Override
        public Optional<Context<DataSetList>> asDsl() {
            if (ReferenceAliasType.DSL == refType) {
                return Optional.of((Context<DataSetList>) this);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Context<DataSet>> asDs() {
            if (ReferenceAliasType.DS == refType) {
                return Optional.of((Context<DataSet>) this);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Context<Attribute>> asAttr() {
            if (ReferenceAliasType.ATTR == refType) {
                return Optional.of((Context<Attribute>) this);
            }
            return Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            Context<?> refArg = (Context<?>) o;
            return Objects.equals(refType, refArg.refType)
                    && Objects.equals(valueSup, refArg.valueSup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(refType, valueSup);
        }

        @Override
        public Optional<? extends RefArg<?>> asRef() {
            return Optional.of(this);
        }

        @Override
        public String toString() {
            return "<" + valueSup + " with value " + (instance == null ? text : instance.toString()) + ">";
        }
    }
}

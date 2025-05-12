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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.Position;

public abstract class TextArg implements MacroArg {

    public static TextArg.NoSignature of(@Nonnull String text) {
        return new NoSignature(text);
    }

    protected final String value;

    private TextArg(@Nonnull String value) {
        this.value = value;
    }

    public static TextArg.WithSignature of(@Nonnull Position position, @Nonnull String text) {
        return new WithSignature(position, text);
    }

    @Override
    public String getText() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static class NoSignature extends TextArg {

        private NoSignature(@Nonnull String value) {
            super(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NoSignature textArg = (NoSignature) o;
            return Objects.equals(value, textArg.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public Optional<? extends RefArg<?>> asRef() {
            return Optional.empty();
        }

        @Override
        public Optional<? extends SignatureArg> asSignature() {
            return Optional.empty();
        }
    }

    public static class WithSignature extends TextArg implements ArgSignature, SignatureArg {

        protected final Position position;

        private WithSignature(@Nonnull Position position, @Nonnull String value) {
            super(value);
            this.position = position;
        }

        @Nonnull
        @Override
        public Position getPosition() {
            return position;
        }

        @Nonnull
        @Override
        public String getSource() {
            return value;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            WithSignature textArg = (WithSignature) o;
            return Objects.equals(value, textArg.value)
                    && Objects.equals(position, textArg.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, position);
        }

        @Override
        public Optional<RefArg.Signature<?>> asRef() {
            return Optional.empty();
        }

        @Nonnull
        @Override
        public ArgSignature getSignature() {
            return this;
        }

        @Override
        public Optional<? extends SignatureArg> asSignature() {
            return Optional.of(this);
        }
    }
}

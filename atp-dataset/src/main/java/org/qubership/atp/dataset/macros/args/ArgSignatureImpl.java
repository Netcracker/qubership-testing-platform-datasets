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

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.macros.Position;

import com.google.common.base.MoreObjects;

public class ArgSignatureImpl implements ArgSignature {

    private final Position position;
    private final String source;

    public ArgSignatureImpl(@Nonnull Position position, @Nonnull String source) {
        this.position = position;
        this.source = source;
    }

    @Nonnull
    @Override
    public Position getPosition() {
        return position;
    }

    @Nonnull
    @Override
    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArgSignatureImpl)) {
            return false;
        }
        ArgSignatureImpl that = (ArgSignatureImpl) o;
        return Objects.equals(getPosition(), that.getPosition())
                && Objects.equals(getSource(), that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPosition(), getSource());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("position", position)
                .add("source", source)
                .toString();
    }
}

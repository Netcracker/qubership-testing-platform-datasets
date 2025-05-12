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

package org.qubership.atp.dataset.migration.model;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;

import com.google.common.base.MoreObjects;

public abstract class ParameterData<T extends ParamContainer> {

    /**
     * No point to expose this field for the basic ParamContainer.
     */
    @Nonnull
    public abstract T getContainer();

    /**
     * Dsl in which parameter is defined.
     */
    @Nonnull
    public abstract DataSetList getDsl();

    /**
     * Ds in which parameter is defined.
     */
    @Nonnull
    public abstract DataSet getDs();

    @Nonnull
    public abstract String getAttrName();

    /**
     * Means that {@link #getContainer()} is actually a {@link OverlapParamContainer}.
     */
    public abstract boolean isOverlap();

    /**
     * Can be used safely if {@link #isOverlap()} returns true.
     *
     * @throws IllegalStateException otherwise.
     */
    public abstract ToOverlap toOverlap();

    /**
     * Can be used safely if {@link #isOverlap()} returns false.
     *
     * @throws IllegalStateException otherwise.
     */
    public abstract ToCreate toCreate();

    public String getLocation() {
        return String.format("%s.%s.%s", getDsl().getName(), getDs().getName(), getAttrName());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("container", getContainer())
                .add("dsl", getDsl())
                .add("ds", getDs())
                .add("attributeName", getAttrName())
                .add("isOverlap", isOverlap())
                .toString();
    }
}

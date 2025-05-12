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

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;

import com.google.common.base.MoreObjects;

public abstract class ToOverlap extends ParameterData<OverlapParamContainer> {

    @Nonnull
    @Override
    public abstract OverlapParamContainer getContainer();

    @Nonnull
    @Override
    public DataSetList getDsl() {
        return getContainer().getDsl();
    }

    @Nonnull
    @Override
    public DataSet getDs() {
        return getContainer().getDs();
    }

    @Nonnull
    @Override
    public String getAttrName() {
        return getAttributeToOverlap().getName();
    }

    public Attribute getAttributeToOverlap() {
        return getParameterToOverlap().getAttribute();
    }

    /**
     * Original parameter from parent excel in {@link ParamContainer}.
     */
    public abstract Parameter getParameterToOverlap();

    @Override
    public boolean isOverlap() {
        return true;
    }

    @Override
    public ToOverlap toOverlap() {
        return this;
    }

    @Override
    public ToCreate toCreate() {
        throw new IllegalStateException("It is should be overlapped, not created.");
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("container", getContainer())
                .add("dsl", getDsl())
                .add("ds", getDs())
                .add("attributeName", getAttrName())
                .add("attributeToOverlap", getAttributeToOverlap())
                .add("parameterToOverlap", getParameterToOverlap())
                .add("isOverlap", isOverlap())
                .toString();
    }
}

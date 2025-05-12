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

public abstract class ToCreate extends ParameterData<ParamContainer> {

    @Nonnull
    @Override
    public DataSetList getDsl() {
        return getContainer().getGroupDsl();
    }

    @Nonnull
    @Override
    public DataSet getDs() {
        return getContainer().getGroupDs();
    }

    @Override
    public boolean isOverlap() {
        return false;
    }

    @Override
    public ToCreate toCreate() {
        return this;
    }

    @Override
    public ToOverlap toOverlap() {
        throw new IllegalStateException("It is should be created, not overlapped.");
    }
}

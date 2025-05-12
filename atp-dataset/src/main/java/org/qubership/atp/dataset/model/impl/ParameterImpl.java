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

package org.qubership.atp.dataset.model.impl;

import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;

public class ParameterImpl extends ParameterBaseImpl implements Parameter {

    protected Attribute attribute;

    public ParameterImpl() {
    }

    /**
     * Just a bean.
     */
    public ParameterImpl(UUID id, Attribute attribute, DataSet dataSet, String text, ListValue listValue,
                         DataSet dataSetReference) {
        super(id, dataSet, text, listValue, dataSetReference);
        this.attribute = attribute;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.of(getDataSet(), getAttribute(), getDataSetReference());
    }

    @Nonnull
    @Override
    public Attribute getAttribute() {
        return attribute;
    }

    @Override
    public void setAttribute(@Nonnull Attribute attribute) {
        this.attribute = attribute;
    }
}

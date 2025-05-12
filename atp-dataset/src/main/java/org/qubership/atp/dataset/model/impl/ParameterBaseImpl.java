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

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;

public abstract class ParameterBaseImpl extends AbstractIdentified implements Parameter {
    protected DataSet dataSet;
    protected String stringValue;
    protected DataSet dataSetReference;
    protected ListValue listValue;
    protected FileData fileData;

    /**
     * Base parameter which has value. List/Text/Ref.
     */
    public ParameterBaseImpl() {
    }

    /**
     * Base parameter which has value. List/Text/Ref.
     */
    public ParameterBaseImpl(UUID id, DataSet dataSet, String text, ListValue listValue,
                             DataSet dataSetReference) {
        this.id = id;
        this.dataSet = dataSet;
        this.stringValue = text;
        this.listValue = listValue;
        this.dataSetReference = dataSetReference;
    }

    @Override
    public ListValue getListValue() {
        return listValue;
    }

    @Override
    public void setListValue(ListValue listValue) {
        this.listValue = listValue;
    }

    @Nonnull
    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(@Nonnull DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public String getText() {
        return this.stringValue;
    }

    @Override
    public void setText(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public DataSet getDataSetReference() {
        return dataSetReference;
    }

    @Override
    public void setDataSetReference(DataSet ds) {
        this.dataSetReference = ds;
    }

    @Override
    public FileData getFileData() {
        return this.fileData;
    }

    @Override
    public void setFileData(FileData fileData) {
        this.fileData = fileData;
    }
}

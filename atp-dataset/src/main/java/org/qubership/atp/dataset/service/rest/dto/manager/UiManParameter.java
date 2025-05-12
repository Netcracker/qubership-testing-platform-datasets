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

package org.qubership.atp.dataset.service.rest.dto.manager;

import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Parameter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"dataSet", "value", "valueRef", "overlap"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UiManParameter {

    private DataSet sourceDs;
    private UUID dataSet;
    private Parameter source;
    private Object value;
    private Object valueRef;
    private boolean isOverlap;

    public UiManParameter() {
    }

    /**
     * Decorates a source parameter for ui serialization.
     */
    public UiManParameter(Parameter source,
                          DataSet sourceDs,
                          boolean isOverlap,
                          @Nullable Object value,
                          @Nullable Object valueRef) {
        setSource(source);
        setSourceDs(sourceDs);
        setOverlap(isOverlap);
        setValue(value);
        setValueRef(valueRef);
    }

    /**
     * Id of a value for reference based values.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Object getValueRef() {
        return valueRef;
    }

    public void setValueRef(Object valueRef) {
        this.valueRef = valueRef;
    }

    /**
     * It is {@link DataSet} in which context we are getting this param. In case of nested ds ref
     * params the context = the root data set.
     */
    @JsonIgnore
    public DataSet getSourceDs() {
        return sourceDs;
    }

    public void setSourceDs(DataSet sourceDs) {
        this.sourceDs = sourceDs;
        setDataSet(sourceDs.getId());
    }

    public boolean isOverlap() {
        return isOverlap;
    }

    public void setOverlap(boolean overlap) {
        isOverlap = overlap;
    }

    @JsonIgnore
    public Parameter getSource() {
        return source;
    }

    public void setSource(Parameter source) {
        this.source = source;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public UUID getDataSet() {
        return this.dataSet;
    }

    public void setDataSet(UUID dataSet) {
        this.dataSet = dataSet;
    }
}

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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Label;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

@JsonPropertyOrder({"id", "name", "labels", "locked"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UiManDataSet {

    private DataSet source;

    public UiManDataSet() {
    }

    public UiManDataSet(DataSet source) {
        this.source = source;
    }

    @JsonIgnore
    public DataSet getSource() {
        return source;
    }

    public void setSource(DataSet source) {
        this.source = source;
    }

    public UUID getId() {
        return source.getId();
    }

    public String getName() {
        return source.getName();
    }

    public List<Label> getLabels() {
        return source.getLabels();
    }

    public Boolean isLocked() {
        return source.isLocked();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("source", source)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UiManDataSet that = (UiManDataSet) o;
        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }
}

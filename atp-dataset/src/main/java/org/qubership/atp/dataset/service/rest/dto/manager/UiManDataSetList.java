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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.dataset.model.DataSetList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"id", "name", "dataSets", "attributes"})
public class UiManDataSetList {

    private List<UiManDataSet> dataSets;
    private List<UiManAttribute> attributes;
    private DataSetList source;

    public UiManDataSetList() {
        dataSets = new ArrayList<>();
        attributes = new ArrayList<>();
    }

    @JsonIgnore
    public DataSetList getSource() {
        return source;
    }

    public void setSource(DataSetList source) {
        this.source = source;
    }

    public UUID getId() {
        return source.getId();
    }

    public String getName() {
        return source.getName();
    }

    public List<UiManDataSet> getDataSets() {
        return dataSets;
    }

    public void setDataSets(List<UiManDataSet> dataSets) {
        this.dataSets = dataSets;
    }

    public List<UiManAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<UiManAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("source", source)
                .add("dataSets", dataSets)
                .add("attributes", attributes)
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
        UiManDataSetList that = (UiManDataSetList) o;
        return Objects.equals(dataSets, that.dataSets)
                && Objects.equals(attributes, that.attributes)
                && source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSets, attributes, source);
    }
}

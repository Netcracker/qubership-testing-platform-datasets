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

package org.qubership.atp.dataset.ui.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.qubership.atp.dataset.model.AttributeType;

public class AttributePojo {

    private UUID id;
    private String name;
    private UUID dataSetList;
    private AttributeType type;
    private UUID dataSetListReference;
    private List<ListValuePojo> listValues;
    private List<UUID> parameters;

    public AttributePojo() {
    }

    public AttributePojo(UUID id, String name, UUID dataSetList, AttributeType type, UUID dataSetListReference,
                         List<ListValuePojo> listValues, List<UUID> parameters) {
        this.id = id;
        this.name = name;
        this.dataSetList = dataSetList;
        this.type = type;
        this.dataSetListReference = dataSetListReference;
        this.listValues = listValues;
        this.parameters = parameters;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getDataSetList() {
        return dataSetList;
    }

    public void setDataSetList(UUID dataSetList) {
        this.dataSetList = dataSetList;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public UUID getDataSetListReference() {
        return dataSetListReference;
    }

    public void setDataSetListReference(UUID dataSetListReference) {
        this.dataSetListReference = dataSetListReference;
    }

    public List<ListValuePojo> getListValues() {
        return listValues;
    }

    public void setListValues(List<ListValuePojo> listValues) {
        this.listValues = listValues;
    }

    public List<UUID> getParameters() {
        return parameters;
    }

    public void setParameters(List<UUID> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributePojo that = (AttributePojo) o;
        return id.equals(that.id) &&
                name.equals(that.name) &&
                dataSetList.equals(that.dataSetList) &&
                type == that.type &&
                Objects.equals(listValues, that.listValues) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dataSetList, type, listValues, parameters);
    }

    @Override
    public String toString() {
        return "AttributePojo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dataSetList=" + dataSetList +
                ", type=" + type +
                ", dataSetListReference=" + dataSetListReference +
                ", listValues=" + listValues +
                ", parameters=" + parameters +
                '}';
    }
}

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

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.impl.AbstractNamed;

public class ListValuePojo extends AbstractNamed {

    private UUID attributePojoId;

    public static ListValuePojo convert(ListValue listValue) {
        ListValuePojo listValuePojo = new ListValuePojo();
        listValuePojo.setId(listValue.getId());
        listValuePojo.setName(listValue.getName());
        listValuePojo.setAttribute(listValue.getAttribute().getId());
        return listValuePojo;
    }

    public UUID getAttribute() {
        return attributePojoId;
    }

    public void setAttribute(UUID attributePojoId) {
        this.attributePojoId = attributePojoId;
    }

    @Nonnull
    @Override
    public Stream<Identified> getReferences() {
        return Stream.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ListValuePojo that = (ListValuePojo) o;
        return attributePojoId.equals(that.attributePojoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attributePojoId);
    }

    @Override
    public String toString() {
        return "ListValuePojo{" +
                "attributePojoId=" + attributePojoId +
                ", name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}

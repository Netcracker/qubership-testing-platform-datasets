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

package org.qubership.atp.dataset.service.jpa.model.tree.ds.itf;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetGroup;
import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetTree;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ItfContextSerializer extends StdSerializer<DataSetTree> {

    public ItfContextSerializer() {
        this(null);
    }

    protected ItfContextSerializer(Class<DataSetTree> t) {
        super(t);
    }

    @Override
    public void serialize(
            DataSetTree dataSetTree,
            JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider
    ) throws IOException {
        jsonGenerator.writeStartObject();
        Map<Long, AttributeSerializer> attributes = new TreeMap<>();

        Map<String, AbstractParameter> parameters = dataSetTree.getParameters();
        for (String parameterName : parameters.keySet()) {
            AbstractParameter parameter = parameters.get(parameterName);
            AttributeSerializer serializer = AttributeSerializer.getSerializer(parameterName, parameter, jsonGenerator);
            attributes.put(parameter.getOrder(), serializer);
        }

        Map<String, DataSetGroup> groups = dataSetTree.getGroups();
        for (String groupName : groups.keySet()) {
            DataSetGroup group = groups.get(groupName);
            AttributeSerializer serializer = AttributeSerializer.getSerializer(groupName, group, jsonGenerator);
            attributes.put(group.getOrder(), serializer);
        }

        for (AttributeSerializer serializer : attributes.values()) {
            serializer.serialize();
        }

        jsonGenerator.writeEndObject();
        jsonGenerator.close();
    }
}

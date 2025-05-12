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

import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetGroup;
import org.qubership.atp.dataset.service.jpa.model.tree.params.AbstractParameter;

import com.fasterxml.jackson.core.JsonGenerator;

public abstract class AttributeSerializer {
    public abstract void serialize() throws IOException;

    public static AttributeSerializer getSerializer(
            String name, DataSetGroup group, JsonGenerator jsonGenerator
    ) {
        return new GroupSerializer(name, group, jsonGenerator);
    }

    public static AttributeSerializer getSerializer(
            String name, AbstractParameter parameter, JsonGenerator jsonGenerator
    ) {
        return new ParameterSerializer(name, parameter, jsonGenerator);
    }
}

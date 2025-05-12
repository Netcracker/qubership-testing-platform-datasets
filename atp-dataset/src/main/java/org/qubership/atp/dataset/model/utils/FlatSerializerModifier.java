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

package org.qubership.atp.dataset.model.utils;

import java.io.IOException;
import java.util.Set;
import java.util.function.Supplier;

import org.qubership.atp.dataset.model.Identified;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.impl.WritableObjectId;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;


public class FlatSerializerModifier extends BeanSerializerModifier {

    @Override
    public JsonSerializer<?> modifySerializer(
            SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        if (Identified.class.isAssignableFrom(beanDesc.getBeanClass())) {
            return new FlatSerializer((BeanSerializerBase) serializer);
        }
        return serializer;
    }

    private static class FlatSerializer extends BeanSerializerBase {

        BeanSerializerBase defaultSerializer;

        public FlatSerializer(BeanSerializerBase src) {
            super(src);
            defaultSerializer = src;
        }

        protected FlatSerializer(BeanSerializerBase src, Set<String> toIgnore) {
            super(src, toIgnore);
            defaultSerializer = src;
        }

        protected FlatSerializer(BeanSerializerBase src, Set<String> toIgnore, Set<String> toInclude) {
            super(src, toIgnore, toInclude);
            defaultSerializer = src;
        }

        protected FlatSerializer(BeanSerializerBase src,
                                 ObjectIdWriter oiw) {
            super(src, oiw);
            defaultSerializer = src;
        }

        protected FlatSerializer(BeanSerializerBase src,
                                 ObjectIdWriter oiw, Object filterId) {
            super(src, oiw, filterId);
            defaultSerializer = src;
        }

        protected FlatSerializer(BeanSerializerBase src,
                                 BeanPropertyWriter[] properties,
                                 BeanPropertyWriter[] filteredProperties) {
            super(src, properties, filteredProperties);
            defaultSerializer = src;
        }

        @Override
        public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
            return new FlatSerializer(defaultSerializer, objectIdWriter);
        }

        @Override
        protected BeanSerializerBase withIgnorals(Set<String> toIgnore) {
            return new FlatSerializer(defaultSerializer, toIgnore);
        }

        @Override
        protected BeanSerializerBase withByNameInclusion(Set<String> toIgnore, Set<String> toInclude) {
            return new FlatSerializer(this, toIgnore, toInclude);
        }

        /**
         * Copy-paste from {@link BeanSerializer#asArraySerializer()}.
         */
        @Override
        protected BeanSerializerBase asArraySerializer() {
            if (_objectIdWriter == null && _anyGetterWriter == null && _propertyFilterId == null) {
                return new BeanAsArraySerializer(this);
            }
            return this;
        }

        @Override
        public BeanSerializerBase withFilterId(Object filterId) {
            return new FlatSerializer(this, _objectIdWriter, filterId);
        }

        @Override
        protected BeanSerializerBase withProperties(BeanPropertyWriter[] properties,
                                                    BeanPropertyWriter[] filteredProperties) {
            return new FlatSerializer(this, properties, filteredProperties);
        }

        @Override
        public void serialize(Object bean, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (_objectIdWriter != null) {
                gen.setCurrentValue(bean); // [databind#631]
                flatSerializeWithObjectId(bean, gen, provider, true, () -> asId(gen));
                return;
            }
            gen.writeStartObject(bean);
            if (_propertyFilterId != null) {
                serializeFieldsFiltered(bean, gen, provider);
            } else {
                serializeFields(bean, gen, provider);
            }
            gen.writeEndObject();
        }

        protected void flatSerializeWithObjectId(Object bean, JsonGenerator gen, SerializerProvider provider,
                                                 boolean startEndObject, Supplier<Boolean> asId) throws IOException {
            final ObjectIdWriter w = _objectIdWriter;
            WritableObjectId objectId = provider.findObjectId(bean, w.generator);
            // If possible, write as id already
            if (objectId.writeAsId(gen, provider, w)) {
                return;
            }
            // If not, need to inject the id:
            Object id = objectId.generateId(bean);
            if (w.alwaysAsId || asId.get()) {
                w.serializer.serialize(id, gen, provider);
                return;
            }
            if (startEndObject) {
                gen.writeStartObject(bean);
            }
            objectId.writeAsField(gen, provider, w);
            if (_propertyFilterId != null) {
                serializeFieldsFiltered(bean, gen, provider);
            } else {
                serializeFields(bean, gen, provider);
            }
            if (startEndObject) {
                gen.writeEndObject();
            }
        }

        private boolean asId(JsonGenerator gen) {
            JsonStreamContext current = gen.getOutputContext();
            while (!current.inRoot()) {
                current = current.getParent();
                Object objToCheck = current.getCurrentValue();
                if (objToCheck == null) {
                    continue;
                }
                if (Identified.class.isAssignableFrom(objToCheck.getClass())) {
                    return true;
                }
            }
            return false;
        }
    }

}


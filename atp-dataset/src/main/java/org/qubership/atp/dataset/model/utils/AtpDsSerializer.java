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
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.model.utils.tree.RefsVisitor;
import org.qubership.atp.dataset.service.direct.macros.DsEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;

public class AtpDsSerializer extends StdSerializer<DataSet> {
    private final DsEvaluator evaluator;

    public AtpDsSerializer(@Nonnull DsEvaluator evaluator) {
        super(DataSet.class);
        this.evaluator = evaluator;
    }

    private static ObjectMapper mapper(@Nonnull DsEvaluator evaluator) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(DataSet.class, new AtpDsSerializer(evaluator));
        mapper.registerModule(module);
        return mapper;
    }

    public static String writeValueAsString(@Nonnull DataSet ds,
                                            @Nonnull DsEvaluator evaluator) throws JsonProcessingException {
        return mapper(evaluator).writeValueAsString(ds);
    }

    public static void writeValue(@Nonnull OutputStream os,
                                  @Nonnull DataSet ds,
                                  @Nonnull DsEvaluator evaluator) throws IOException {
        mapper(evaluator).writeValue(os, ds);
    }

    @Override
    public void serialize(DataSet value, JsonGenerator gen, SerializerProvider provider) throws
            IOException {
        Handler h = new Handler(evaluator, new SneakyWriter(gen));
        RefsVisitor<DataSet, Attribute> visitor = new RefsVisitor<>(Iterators
                .singletonIterator(value), h, null);
        while (visitor.hasNext()) {
            visitor.next();
        }
    }

    private static class SneakyWriter {
        private final JsonGenerator gen;

        private SneakyWriter(JsonGenerator gen) {
            this.gen = gen;
        }

        private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
            throw (E) e;
        }

        public void write(CheckedConsumer<JsonGenerator, IOException> consumer) {
            try {
                consumer.accept(gen);
            } catch (IOException e) {
                sneakyThrow(e);
            }
        }
    }


    private static class Handler extends AbstractDsUiHandler {
        private final Deque<Attribute> groupsPath = new ArrayDeque<>();
        private final SneakyWriter sneaky;
        private final DsEvaluator evaluator;

        public Handler(DsEvaluator evaluator, SneakyWriter sneaky) {
            this.evaluator = evaluator;
            this.sneaky = sneaky;
        }

        @Nullable
        @Override
        public Iterator<? extends Attribute> getChildren(@Nonnull DataSetList item) {
            return item.getAttributes().stream().filter(attr -> AttributeType.DSL == attr.getType()).iterator();
        }

        @Override
        protected void dslStarts(DataSetList dsl) {
            super.dslStarts(dsl);
            if (dslDepth == 0) {
                sneaky.write(JsonGenerator::writeStartObject);
            }
            List<Attribute> collect = curDsl.getAttributes().stream().filter(attr -> AttributeType.DSL != attr
                    .getType()).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                sneaky.write(gen -> {
                    gen.writeFieldName("parameters");
                    gen.writeStartObject();
                    for (Attribute attribute : collect) {
                        writeAttr(attribute, true);
                    }
                    gen.writeEndObject();
                });
            }
        }

        @Override
        protected void goForwardUnderDsl() {
            super.goForwardUnderDsl();
            sneaky.write(gen -> {
                gen.writeFieldName("groups");
                gen.writeStartObject();
            });
        }

        @Override
        protected void goBackFromDsl() {
            sneaky.write(JsonGenerator::writeEndObject);
            super.goBackFromDsl();
        }

        @Override
        protected void dslEnds() {
            if (dslDepth == 0) {
                sneaky.write(JsonGenerator::writeEndObject);
            }
            curDsl = null;
        }

        private void writeAttr(Attribute attr, boolean doClose) {
            List<UUID> attrPath = groupsPath.stream().map(Attribute::getId).collect(Collectors.toList());
            OverlapIterator overlapIterator = OverlapIterator.create(curDs, attr.getId(), attrPath);
            OverlapItem context = overlapIterator.next();
            Optional<Parameter> parameterOpt = context.getParameter();
            Optional<?> valueOpt;
            Optional<?> valueRefOpt;
            if (parameterOpt.isPresent()) {
                Parameter param = parameterOpt.get();
                OverlapItem.Reachable reachable = context.asReachable();
                if (AttributeType.FILE == attr.getType()) {
                    Optional<AtpFileData> fileData = Optional
                            .ofNullable(param.getFileData())
                            .map(AtpFileData::create);
                    valueOpt = fileData.map(AtpFileData::getUniqueFileName);
                    valueRefOpt = fileData;
                } else {
                    valueOpt = evaluator.apply(reachable);
                    valueRefOpt = Optional.empty();//ATP deserializes it into a POJO
                    //, so types of valueRef should match for all parameter types.
                    //valueRef of parameters other that FILE is not needed, so do skip.
                }
            } else {
                valueOpt = Optional.empty();
                valueRefOpt = Optional.empty();
            }
            sneaky.write(gen -> {
                gen.writeFieldName(attr.getName());
                gen.writeStartObject();
                gen.writeFieldName("type");
                gen.writeObject(attr.getType());
                if (valueOpt.isPresent()) {
                    gen.writeFieldName("value");
                    gen.writeObject(valueOpt.get());
                }
                if (valueRefOpt.isPresent()) {
                    gen.writeFieldName("valueRef");
                    gen.writeObject(valueRefOpt.get());
                }
                if (doClose) {
                    gen.writeEndObject();
                }
            });
        }

        @Override
        protected void attributeStarts(Attribute attr) {
            curAttr = attr;
        }

        @Override
        protected void goForwardUnderAttribute() {
            writeAttr(curAttr, false);
            groupsPath.add(curAttr);
            curAttr = null;
        }

        @Override
        protected void goBackFromAttribute() {
            curAttr = groupsPath.removeLast();
            sneaky.write(JsonGenerator::writeEndObject);
        }


    }

    private static class AtpFileData {
        private final String uniqueFileName;
        private final String contentType;
        private final String url;

        private AtpFileData(String uniqueFileName, String contentType, String url) {
            this.uniqueFileName = uniqueFileName;
            this.contentType = contentType;
            this.url = url;
        }

        public static AtpFileData create(@Nonnull FileData data) {
            String uniqueFileName = data.getParameterUuid()
                    + "."
                    + Files.getFileExtension(data.getFileName());
            return new AtpFileData(uniqueFileName, data.getContentType(), data.getUrl());
        }

        @JsonIgnore
        public String getUniqueFileName() {
            return uniqueFileName;
        }

        public String getContentType() {
            return contentType;
        }

        public String getUrl() {
            return url;
        }
    }
}

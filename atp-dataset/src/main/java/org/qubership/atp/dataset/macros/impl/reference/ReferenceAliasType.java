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

package org.qubership.atp.dataset.macros.impl.reference;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.service.direct.IdentifiedService;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableSet;

public abstract class ReferenceAliasType<T extends Identified & Named> {

    public static final ReferenceAliasType<DataSetList> DSL;
    public static final ReferenceAliasType<DataSet> DS;
    public static final ReferenceAliasType<Attribute> ATTR;
    private static final Set<ReferenceAliasType> VALUES;
    private static final String EXPECTED_VALUES;

    static {
        ReferenceAliasType<DataSetList> dsl = new ReferenceAliasType<DataSetList>((short) 1, "DSL") {
            @Override
            public Optional<DataSetList> asDsl(@Nonnull DataSetList instance) {
                return Optional.of(instance);
            }

            @Nonnull
            @Override
            public IdentifiedService<DataSetList> chooseService(@Nonnull IdentifiedService<DataSetList> dslService,
                                                                @Nonnull IdentifiedService<DataSet> dsService,
                                                                @Nonnull IdentifiedService<Attribute> attrService) {
                return dslService;
            }
        };
        ReferenceAliasType<DataSet> ds = new ReferenceAliasType<DataSet>((short) 2, "DS") {
            @Override
            public Optional<DataSet> asDs(@Nonnull DataSet instance) {
                return Optional.of(instance);
            }

            @Nonnull
            @Override
            public IdentifiedService<DataSet> chooseService(@Nonnull IdentifiedService<DataSetList> dslService,
                                                            @Nonnull IdentifiedService<DataSet> dsService,
                                                            @Nonnull IdentifiedService<Attribute> attrService) {
                return dsService;
            }
        };
        ReferenceAliasType<Attribute> attr = new ReferenceAliasType<Attribute>((short) 3, "ATTR") {
            @Override
            public Optional<Attribute> asAttr(@Nonnull Attribute instance) {
                return Optional.of(instance);
            }

            @Nonnull
            @Override
            public IdentifiedService<Attribute> chooseService(@Nonnull IdentifiedService<DataSetList> dslService,
                                                              @Nonnull IdentifiedService<DataSet> dsService,
                                                              @Nonnull IdentifiedService<Attribute> attrService) {
                return attrService;
            }
        };
        DSL = dsl;
        DS = ds;
        ATTR = attr;
        VALUES = ImmutableSet.of(dsl, ds, attr);
        EXPECTED_VALUES = buildNames(VALUES);
    }

    private final short id;
    private final String name;

    public ReferenceAliasType(short id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * returns {@link ReferenceAliasType} by Id.
     *
     * @param id short value
     * @return ReferenceAliasType bt {@code id}
     */
    public static ReferenceAliasType from(short id) {
        for (ReferenceAliasType type : VALUES) {
            if (type.getId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Undefined reference type with id: " + id + ". Expected: " + EXPECTED_VALUES
        );
    }

    /**
     * returns {@link ReferenceAliasType} by name.
     *
     * @param name value
     * @return ReferenceAliasType.
     */
    @JsonCreator
    public static ReferenceAliasType from(@Nonnull String name) {
        for (ReferenceAliasType type : VALUES) {
            if (name.equals(type.name())) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Undefined reference type with name: " + name + ". Expected: " + EXPECTED_VALUES
        );
    }

    @Nonnull
    private static String buildNames(@Nonnull Iterable<ReferenceAliasType> values) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (ReferenceAliasType type : values) {
            builder.append(type.name()).append('(').append(type.getId()).append(')').append(',');
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(']');
        return builder.toString();
    }

    public static Set<ReferenceAliasType> values() {
        return VALUES;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceAliasType)) {
            return false;
        }
        ReferenceAliasType<?> that = (ReferenceAliasType<?>) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public Optional<DataSetList> asDsl(@Nonnull T instance) {
        return Optional.empty();
    }

    public Optional<DataSet> asDs(@Nonnull T instance) {
        return Optional.empty();
    }

    public Optional<Attribute> asAttr(@Nonnull T instance) {
        return Optional.empty();
    }

    @Nonnull
    public abstract IdentifiedService<T> chooseService(@Nonnull IdentifiedService<DataSetList> dslService,
                                                       @Nonnull IdentifiedService<DataSet> dsService,
                                                       @Nonnull IdentifiedService<Attribute> attrService);

    public short getId() {
        return id;
    }

    @JsonValue
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

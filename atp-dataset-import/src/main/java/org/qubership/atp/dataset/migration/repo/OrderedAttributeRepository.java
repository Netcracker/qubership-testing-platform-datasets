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

package org.qubership.atp.dataset.migration.repo;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.AttributeRepository;
import org.qubership.atp.dataset.db.CacheRepository;
import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.ListValueRepository;
import org.qubership.atp.dataset.db.ParameterRepository;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;

import com.querydsl.sql.SQLQueryFactory;

public class OrderedAttributeRepository extends AttributeRepository {

    public OrderedAttributeRepository(
            @Nonnull SQLQueryFactory queryFactory, @Nonnull Provider<DataSetListRepository> dslRepo,
            @Nonnull Provider<ParameterRepository> paramRepo, @Nonnull Provider<ListValueRepository> lvRepo,
            @Nonnull CacheRepository cacheRepo) {
        super(queryFactory, dslRepo, paramRepo, lvRepo, cacheRepo);
    }

    /**
     * allows to create attribute without specified ordering (attribute is always created as last
     * attribute).
     */
    @Nonnull
    public Attribute create(@Nonnull UUID dslId, @Nonnull String name, @Nonnull AttributeType type, @Nullable UUID
            dslRefId, List<String> listValues) {
        final Integer first = queryFactory.select(ATTR.ordering.max()).from(ATTR).fetchFirst();
        int order = (first == null ? 0 : first) + 1;
        return super.create(dslId, order, name, type, dslRefId, listValues);
    }
}

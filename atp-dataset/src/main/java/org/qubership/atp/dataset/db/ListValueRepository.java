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

package org.qubership.atp.dataset.db;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.impl.ListValueImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLInsertClause;

@Repository
public class ListValueRepository extends AbstractRepository {

    private final SQLQueryFactory queryFactory;
    private final ListValuesProjection projection;
    private final Provider<AttributeRepository> attrRepo;
    private final Provider<ParameterRepository> paramRepo;
    private final CacheRepository cacheRepo;

    /**
     * AttributeRepository.
     */
    @Autowired
    public ListValueRepository(@Nonnull SQLQueryFactory queryFactory,
                               @Nonnull Provider<AttributeRepository> attrRepo,
                               @Nonnull Provider<ParameterRepository> paramRepo,
                               @Nonnull CacheRepository cacheRepo) {
        super();
        this.queryFactory = queryFactory;
        this.attrRepo = attrRepo;
        this.paramRepo = paramRepo;
        this.cacheRepo = cacheRepo;
        this.projection = new ListValuesProjection(this);
    }

    /**
     * Creates a list value objects for specified string values.
     */
    @Nonnull
    public List<ListValue> create(@Nonnull UUID attributeId, @Nonnull List<String> values) {
        SQLInsertClause insert = queryFactory.insert(LV);
        for (String value : values) {
            insert.set(LV.attributeId, attributeId).set(LV.text, value);
            insert.addBatch();
        }
        insert.setBatchToBulk(false);
        List<UUID> ids = Preconditions.checkNotNull(insert.executeWithKeys(LV.id), "nothing created");
        Iterator<UUID> id = ids.iterator();
        return values.stream()
                .map(value -> projection.create(id.next(), attributeId, value))
                .collect(Collectors.toList());
    }

    /**
     * Creates a list value object for specified string values.
     */
    @Nonnull
    public ListValue create(@Nonnull UUID attributeId, @Nonnull String text) {
        SQLInsertClause insert = queryFactory.insert(LV);
        insert.set(LV.attributeId, attributeId).set(LV.text, text);
        UUID id = Preconditions.checkNotNull(insert.executeWithKey(LV.id));
        return projection.create(id, attributeId, text);
    }

    public boolean update(@Nonnull UUID id, @Nonnull String text) {
        return update(LV.id.eq(id), text) > 0;
    }

    protected long update(@Nonnull Predicate predicate, @Nonnull String text) {
        return queryFactory.update(LV).where(predicate).set(LV.text, text).execute();
    }

    @Nullable
    public ListValue getById(@Nonnull UUID id) {
        return cacheRepo.tryComputeIfAbsent(ListValue.class, id, uuid -> select(LV.id.eq(uuid)).fetchOne());
    }

    public boolean existsById(@Nonnull UUID id) {
        return cacheRepo.getIfPresent(ListValue.class, id) != null
                || select(LV.id.eq(id)).fetchCount() > 0;
    }

    @Nonnull
    List<ListValue> getByAttributeId(@Nonnull UUID attrId) {
        return select(LV.attributeId.eq(attrId)).orderBy(LV.text.asc()).fetch();
    }

    @Nonnull
    public List<ListValue> getAll() {
        return queryFactory.select(projection).from(LV).orderBy(LV.text.asc()).fetch();
    }

    @Nonnull
    protected SQLQuery<ListValue> select(@Nonnull Predicate predicate) {
        return queryFactory.select(projection).from(LV).where(predicate);
    }

    void onAttrDeleteCascade(@Nonnull UUID attributeId) {
        delete(LV.attributeId.eq(attributeId));
    }

    public boolean delete(@Nonnull UUID id) {
        paramRepo.get().onListValueDeleteCascade(id);
        return delete(LV.id.eq(id)) > 0;
    }

    protected long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(LV).where(predicate).execute();
    }

    /**
     * Bulk and cascade delete of list values.
     */
    public boolean bulkDelete(@Nonnull List<UUID> ids) {
        paramRepo.get().onListValuesDeleteCascade(ids);
        return delete(LV.id.in(ids)) > 0;
    }

    private class ListValuesProjection extends MappingProjection<ListValue> {

        private final ListValueRepository repo;

        ListValuesProjection(ListValueRepository repo) {
            super(ListValue.class, LV.all());
            this.repo = repo;
        }

        @Override
        protected ListValue map(Tuple row) {
            UUID id = row.get(LV.id);
            assert id != null;
            UUID attributeId = row.get(LV.attributeId);
            assert attributeId != null;
            String contents = row.get(LV.text);
            assert contents != null;
            return create(id, attributeId, contents);
        }

        protected ListValue create(@Nonnull UUID id, @Nonnull UUID attributeId, @Nonnull String contents) {
            Attribute attr = Proxies.withId(Attribute.class, attributeId, uuid -> repo.attrRepo.get().getById(uuid));
            return new ListValueImpl(id, attr, contents);
        }
    }
}

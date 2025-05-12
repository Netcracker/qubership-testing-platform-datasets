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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.generated.QAttribute;
import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.AttributeImpl;
import org.qubership.atp.dataset.model.impl.ListValueImpl;
import org.qubership.atp.dataset.service.ws.entities.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mysema.commons.lang.CloseableIterator;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLInsertClause;

@Repository
public class AttributeRepository extends AbstractRepository {

    private static final QAttribute ATTR_ALIAS;
    private static final Expression<?>[] COLUMNS;

    static {
        QAttribute attributeAlias = new QAttribute("attr");
        ImmutableList<Expression<?>> expressions =
                ImmutableList.<Expression<?>>builder()
                        .addAll(Arrays.asList(attributeAlias.all()))
                        .add(PARAM.id)
                        .add(LV.id)
                        .add(LV.text)
                        .build();
        COLUMNS = expressions.toArray(new Expression[0]);
        ATTR_ALIAS = attributeAlias;
    }

    protected final SQLQueryFactory queryFactory;
    private final Provider<DataSetListRepository> dslRepo;
    private final Provider<ParameterRepository> paramRepo;
    private final Provider<ListValueRepository> lvRepo;
    private final CacheRepository cacheRepo;
    private final AttributeProjection attrProjection;

    /**
     * AttributeRepository.
     */
    @Autowired
    public AttributeRepository(@Nonnull SQLQueryFactory queryFactory,
                               @Nonnull Provider<DataSetListRepository> dslRepo,
                               @Nonnull Provider<ParameterRepository> paramRepo,
                               @Nonnull Provider<ListValueRepository> lvRepo,
                               @Nonnull CacheRepository cacheRepo) {
        super();
        this.queryFactory = queryFactory;
        this.dslRepo = dslRepo;
        this.paramRepo = paramRepo;
        this.lvRepo = lvRepo;
        this.cacheRepo = cacheRepo;
        this.attrProjection = new AttributeProjection(this);
    }

    /**
     * Creates attribute.
     *
     * @param dslRefId if type == {@link AttributeType#DSL}
     */
    @Nonnull
    public Attribute create(@Nonnull UUID dslId, @Nonnull Integer order, @Nonnull String name,
                            @Nonnull AttributeType type, @Nullable UUID dslRefId, @Nullable List<String> listValues) {
        SQLInsertClause insert = queryFactory.insert(ATTR).set(ATTR.ordering, order);
        if (dslRefId != null) {
            Preconditions.checkArgument(type == AttributeType.DSL || type == AttributeType.CHANGE,
                    "It is not possible to set DSL reference [%s] to attribute with type [%s]",
                    dslRefId, type);
            insert.set(ATTR.typeDatasetlistId, dslRefId);
        }
        insert.set(ATTR.name, name).set(ATTR.datasetlistId, dslId).set(ATTR.attributeTypeId, type.getId());
        UUID id = Preconditions.checkNotNull(insert.executeWithKey(ATTR.id), "nothing created");
        List<ListValue> listValueObjs = null;
        if (listValues != null && !listValues.isEmpty()) {
            listValueObjs = lvRepo.get().create(id, listValues);
        }
        return attrProjection.create(id, name, dslId, type, dslRefId, null, listValueObjs);
    }

    public boolean updateTypeDslId(@Nonnull UUID dslId, UUID id) {
        return queryFactory.update(ATTR).where(
                ATTR.typeDatasetlistId.eq(dslId)).set(ATTR.typeDatasetlistId, id).execute() > 0;
    }

    public boolean update(@Nonnull UUID id, @Nonnull String name) {
        return update(ATTR.id.eq(id), name) > 0;
    }

    protected long update(@Nonnull Predicate predicate, @Nonnull String name) {
        return queryFactory.update(ATTR).where(predicate).set(ATTR.name, name).execute();
    }

    public boolean updateDslRef(@Nonnull UUID id, @Nonnull UUID dataSetListId) {
        return queryFactory.update(ATTR).where(ATTR.id.eq(id).and(ATTR.attributeTypeId.eq(AttributeType.DSL.getId())))
                .set(ATTR.typeDatasetlistId, dataSetListId).execute() > 0;
    }

    @Nullable
    public Attribute getById(@Nonnull UUID id) {
        return cacheRepo.tryComputeIfAbsent(Attribute.class, id, uuid -> select(ATTR.id.eq(uuid)).fetchOne());
    }

    public boolean existsById(@Nonnull UUID id) {
        return cacheRepo.getIfPresent(Attribute.class, id) != null
                || select(ATTR.id.eq(id)).fetchCount() > 0;
    }

    @Nonnull
    public List<Attribute> getByParentId(@Nonnull UUID dslId) {
        return select(ATTR.datasetlistId.eq(dslId)).orderBy(ATTR.ordering.asc()).fetch();
    }

    @Nonnull
    public List<Attribute> getByParentId(@Nonnull UUID dslId, @Nonnull AttributeType type) {
        return select(ATTR.datasetlistId.eq(dslId).and(ATTR.attributeTypeId.eq(type.getId())))
                .orderBy(ATTR.ordering.asc()).fetch();
    }

    /**
     * Sorted collection of attributes that matches DataSetList id.
     *
     * @param dslId - DataSetList id.
     * @return sorted collection of Attribute.
     */
    @Nonnull
    public Collection<Attribute> getEagerByParentId(@Nonnull UUID dslId) {
        SQLQuery<Tuple> sqlQuery = getQuerySelectFromAttributeByDslIdJoinParameterAndJoinListValue(dslId);
        Map<UUID, Attribute> attributes = getAttributes(dslId, sqlQuery);
        return attributes.values();
    }

    private SQLQuery<Tuple> getQuerySelectFromAttributeByDslIdJoinParameterAndJoinListValue(@Nonnull UUID dslId) {
        return queryFactory.select(COLUMNS)
                .from(ExpressionUtils.as(SQLExpressions.select(ATTR.all())
                        .from(ATTR)
                        .where(ATTR.datasetlistId.eq(dslId)), "attr"))
                .leftJoin(PARAM).on(PARAM.attributeId.eq(ATTR_ALIAS.id))
                .leftJoin(LV).on(LV.attributeId.eq(ATTR_ALIAS.id))
                .orderBy(ATTR_ALIAS.ordering.asc(), LV.text.asc());
    }

    @SuppressWarnings("ConstantConditions")
    private Map<UUID, Attribute> getAttributes(@Nonnull UUID dslId, SQLQuery<Tuple> attr) {
        Map<UUID, Attribute> attributes = new LinkedHashMap<>();
        Map<UUID, ListValue> listValues = new HashMap<>();
        Map<UUID, Parameter> parameters = new HashMap<>();
        try (CloseableIterator<Tuple> iter = attr.iterate()) {
            while (iter.hasNext()) {
                Tuple row = iter.next();
                UUID id = row.get(ATTR_ALIAS.id);
                String name = row.get(ATTR_ALIAS.name);
                Short attrTypeId = row.get(ATTR_ALIAS.attributeTypeId);
                AttributeType type = AttributeType.from(attrTypeId);
                UUID typeDslId = row.get(ATTR_ALIAS.typeDatasetlistId);
                UUID lvId = row.get(LV.id);
                String lvText = row.get(LV.text);
                UUID paramId = row.get(PARAM.id);
                Attribute attribute = attributes.computeIfAbsent(id, key -> {
                    DataSetList dslRef = null;
                    if (typeDslId != null) {
                        dslRef = Proxies.withId(DataSetList.class, typeDslId,
                                uuid -> dslRepo.get().getById(uuid));
                    }
                    DataSetList dsl = Proxies.withId(DataSetList.class, dslId, uuid -> dslRepo.get().getById(uuid));
                    return new AttributeImpl(id, name, dsl, type, dslRef, new ArrayList<>(), new ArrayList<>());
                });
                fillListValues(lvId, listValues, lvText, attribute);
                fillParameters(paramId, parameters, attribute);
            }
        }
        return attributes;
    }

    private void fillListValues(UUID lvId, Map<UUID, ListValue> listValues, String lvText, Attribute attribute) {
        if (lvId != null) {
            listValues.computeIfAbsent(lvId, key -> {
                assert lvText != null;
                ListValueImpl result = new ListValueImpl(lvId, attribute, lvText);
                attribute.getListValues().add(result);
                return result;
            });
        }
    }

    private void fillParameters(UUID paramId, Map<UUID, Parameter> parameters, Attribute attribute) {
        if (paramId != null) {
            parameters.computeIfAbsent(paramId, key -> {
                Parameter result = Proxies.withId(Parameter.class, paramId,
                        uuid -> paramRepo.get().getById(uuid));
                attribute.getParameters().add(result);
                return result;
            });
        }
    }

    @Nonnull
    public List<Attribute> getAll() {
        return queryFactory.select(attrProjection).from(ATTR).fetch();
    }

    @Nonnull
    protected SQLQuery<Attribute> select(@Nonnull Predicate predicate) {
        return queryFactory.select(attrProjection).from(ATTR).where(predicate);
    }

    void onDslDeleteCascade(UUID dslId) {
        getByParentId(dslId).forEach(attr -> delete(attr.getId()));
        queryFactory.select(ATTR.id).from(ATTR).where(ATTR.typeDatasetlistId.eq(dslId)).fetch().forEach(this::delete);
    }

    /**
     * Cascade delete attributes. Also removes list value, and parameters which bound to this
     * attribute.
     *
     * @param id - attribute id.
     * @return true attribute removed and false if not deleted.
     */
    public boolean delete(@Nonnull UUID id) {
        paramRepo.get().onAttrDeleteCascade(id);
        lvRepo.get().onAttrDeleteCascade(id);
        return delete(ATTR.id.eq(id)) > 0;
    }

    private long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(ATTR).where(predicate).execute();
    }

    /**
     * Update ordering of attributes.
     *
     * @param attributesOrdering info about attributes for updating ({ID, ordering})
     */
    public void updateOrdering(List<Pair<UUID, Integer>> attributesOrdering) {
        for (Pair attrToUpdate : attributesOrdering) {
            queryFactory.update(ATTR)
                    .set(ATTR.ordering, (Integer) attrToUpdate.getSecond())
                    .where(ATTR.id.eq(UUID.fromString(attrToUpdate.getFirst().toString())))
                    .execute();
        }
    }

    private static class AttributeProjection extends MappingProjection<Attribute> {

        private final AttributeRepository repo;

        AttributeProjection(@Nonnull AttributeRepository repo) {
            super(Attribute.class, ATTR.id, ATTR.name, ATTR.datasetlistId,
                    ATTR.attributeTypeId, ATTR.typeDatasetlistId);
            this.repo = repo;
        }

        @Override
        protected AttributeImpl map(Tuple row) {
            UUID id = row.get(ATTR.id);
            assert id != null;
            String name = row.get(ATTR.name);
            assert name != null;
            UUID dslId = row.get(ATTR.datasetlistId);
            assert dslId != null;
            Short attrTypeId = row.get(ATTR.attributeTypeId);
            assert attrTypeId != null;
            AttributeType type = AttributeType.from(attrTypeId);
            UUID typeDslId = row.get(ATTR.typeDatasetlistId);
            return create(id, name, dslId, type, typeDslId, null, null);
        }

        public AttributeImpl create(@Nonnull UUID id, @Nonnull String name, @Nonnull UUID dslId,
                                    @Nonnull AttributeType type, @Nullable UUID typeDslId,
                                    @Nullable List<Parameter> parameters,
                                    @Nullable List<ListValue> listValues) {
            DataSetList dslRef = null;
            if (typeDslId != null) {
                dslRef = Proxies.withId(DataSetList.class, typeDslId, uuid -> repo.dslRepo.get().getById(uuid));
            }
            if (parameters == null) {
                parameters = Proxies.list(() -> repo.paramRepo.get().getByAttributeId(id));
            }
            if (listValues == null) {
                listValues = Proxies.list(() -> repo.lvRepo.get().getByAttributeId(id));
            }
            DataSetList dsl = Proxies.withId(DataSetList.class, dslId, uuid -> repo.dslRepo.get().getById(uuid));
            return new AttributeImpl(id, name, dsl, type, dslRef, listValues, parameters);
        }
    }
}

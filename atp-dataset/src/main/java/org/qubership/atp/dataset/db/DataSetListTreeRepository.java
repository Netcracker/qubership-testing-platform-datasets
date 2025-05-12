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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.dto.AttributePathDto;
import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.db.utils.StrongIdentifiedCache;
import org.qubership.atp.dataset.exception.attribute.AttributeTypeException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributePath;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.AttributeImpl;
import org.qubership.atp.dataset.model.impl.AttributePathImpl;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.impl.ParameterImpl;
import org.qubership.atp.dataset.model.impl.ParameterOverlapImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mysema.commons.lang.CloseableIterator;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.Union;

@Repository
public class DataSetListTreeRepository extends AbstractRepository {
    private final SQLQueryFactory queryFactory;
    private final Provider<DataSetListRepository> dslRepo;
    private final Provider<DataSetRepository> dsRepo;
    private final Provider<AttributeRepository> attrRepo;
    private final Provider<ParameterRepository> paramRepo;
    private final Provider<GridFsRepository> gridFsRepoProvider;
    private final Provider<LabelRepository> labelRepo;
    private final Provider<VisibilityAreaRepository> vaRepo;
    private final Provider<TestPlanRepository> testPlanRepo;
    private final Provider<ListValueRepository> lvRepo;
    private final CacheRepository cacheRepo;
    private final Expression<String> typeSafeNullString;
    private final Expression<UUID> typeSafeNullUuid;
    private final QDslDs dslDs;
    private final QDslDs.QDslAttr dslAttr;
    private final QDslDs.QDslAttrPath dslAttrPath;

    /**
     * data set list tree repository.
     *
     * @param queryFactory is used for creating sql queries.
     */
    @Autowired
    public DataSetListTreeRepository(@Nonnull SQLQueryFactory queryFactory,
                                     @Nonnull Provider<VisibilityAreaRepository> vaRepo,
                                     @Nonnull Provider<TestPlanRepository> testPlanRepo,
                                     @Nonnull Provider<DataSetListRepository> dslRepo,
                                     @Nonnull Provider<DataSetRepository> dsRepo,
                                     @Nonnull Provider<AttributeRepository> attrRepo,
                                     @Nonnull Provider<ParameterRepository> paramRepo,
                                     @Nonnull Provider<GridFsRepository> gridFsRepoProvider,
                                     @Nonnull Provider<LabelRepository> labelRepo,
                                     @Nonnull Provider<ListValueRepository> lvRepo,
                                     @Nonnull CacheRepository cacheRepo,
                                     @Nonnull Configuration configuration) {
        super();
        this.queryFactory = queryFactory;
        this.vaRepo = vaRepo;
        this.testPlanRepo = testPlanRepo;
        this.dsRepo = dsRepo;
        this.attrRepo = attrRepo;
        this.labelRepo = labelRepo;
        this.cacheRepo = cacheRepo;
        this.typeSafeNullString = Expressions.template(String.class,
                "null::" + configuration.getTypeNameForCast(String.class));
        this.dslRepo = dslRepo;
        this.typeSafeNullUuid = Expressions.template(UUID.class,
                "null::" + "uuid");
        this.lvRepo = lvRepo;
        this.paramRepo = paramRepo;
        this.gridFsRepoProvider = gridFsRepoProvider;
        dslDs = new QDslDs("dsl_ds");
        dslAttr = dslDs.createAttrs("dsl_attr");
        dslAttrPath = dslDs.createAttrPath("dsl_ak");
    }


    /**
     * Loads eager data set list with all it's references. Used to cache all of the data for
     * performance reasons. Requires cache to be enabled.
     *
     * @param id               data set list id.
     * @param dataSetsFilter   data set ids you want to load. Will load all if collection is empty.
     * @param attributesFilter attributes you don't want to load. Will load all if collection is empty.
     */
    @Nullable
    public DataSetList getEagerById(@Nonnull UUID id,
                                    @Nullable Collection<UUID> dataSetsFilter,
                                    @Nullable Collection<UUID> attributesFilter,
                                    Integer startIndex,
                                    Integer endIndex,
                                    boolean isSortEnabled) {
        DataSetList result = cacheRepo.getIfPresent(DataSetList.class, id);
        if (result != null) {
            return result;
        }
        IdentifiedCache cache;
        if (dataSetsFilter == null || attributesFilter == null) {
            cache = cacheRepo.getCache();
        } else {
            cache = new StrongIdentifiedCache(); //don't use dsl cache with filtered ds's
            // because of probable data inconsistency
        }
        return getDslTree(id, dataSetsFilter, attributesFilter, new ToLoadLater(this, cache),
                startIndex, endIndex, isSortEnabled);
    }

    protected DataSetList getDslTree(@Nonnull UUID id,
                                     @Nonnull ToLoadLater loadLater) {
        return getDslTree(id, null, null, loadLater, null, null, false);
    }

    protected DataSetList getDslTree(@Nonnull UUID id,
                                     @Nullable Collection<UUID> dataSetsFilter,
                                     @Nonnull ToLoadLater loadLater) {
        return getDslTree(id, dataSetsFilter, null, loadLater, null, null, false);
    }

    @Nullable
    protected DataSetList getDslTree(@Nonnull UUID id,
                                     @Nullable Collection<UUID> dataSetsFilter,
                                     @Nullable Collection<UUID> attributesFilter,
                                     @Nonnull ToLoadLater loadLater,
                                     Integer startIndex,
                                     Integer endIndex,
                                     boolean isSortEnabled) {
        Union<Tuple> tupleUnion = createUnion(id, dataSetsFilter, attributesFilter, startIndex, endIndex);
        DataSetList dsl = null;
        try (CloseableIterator<Tuple> iter = tupleUnion.iterate()) {
            while (iter.hasNext()) {
                Tuple next = iter.next();
                UUID vaId = next.get(dslAttrPath.vaId);
                UUID dslId = next.get(dslAttrPath.dslId);
                UUID testPlanId = next.get(dslAttrPath.testPlanId);
                String dslName = next.get(dslAttrPath.dslName);
                assert vaId != null;
                assert dslId != null;
                UUID createdBy = next.get(dslAttrPath.createdBy);
                Timestamp createdWhen = next.get(dslAttrPath.createdWhen);
                UUID modifiedBy = next.get(dslAttrPath.modifiedBy);
                Timestamp modifiedWhen = next.get(dslAttrPath.modifiedWhen);
                dsl = createDsl(vaId, dslId, dslName, loadLater, testPlanId,
                        createdBy, createdWhen, modifiedBy, modifiedWhen);
                UUID dsId = next.get(dslAttrPath.dsId);
                String dsName = next.get(dslAttrPath.dsName);
                Boolean dsLocked = next.get(dslAttrPath.dsLocked);
                DataSet dataSet = null;
                if (dsId != null) {
                    dataSet = createDs(dsl, dsId, dsName, dsLocked, loadLater);
                }
                UUID attrId = next.get(dslAttrPath.attrId);
                String attrName = next.get(dslAttrPath.attrName);
                Short attrTypeId = next.get(dslAttrPath.attrType);
                UUID attrDslRef = next.get(dslAttrPath.attrDslRef);
                String attrKey = next.get(dslAttrPath.attrKey);
                Attribute attribute = null;
                AttributePath attrPath = null;
                if (attrId != null) {
                    assert attrTypeId != null;
                    AttributeType attrType = AttributeType.from(attrTypeId);
                    List<ListValue> listValues = Proxies.list(() -> lvRepo.get().getByAttributeId(attrId));
                    if (attrKey == null) {
                        attribute = createAttr(dsl, attrId, attrName, attrType, attrDslRef, listValues, loadLater);
                    } else {
                        assert dataSet != null;
                        UUID targetAttrDslId = next.get(dslAttrPath.targetAttrDslId);
                        assert targetAttrDslId != null;
                        UUID targetAttrId = next.get(dslAttrPath.targetAttrId);
                        assert targetAttrId != null;
                        attrPath = createAttrPath(attrId, dslId, dataSet, targetAttrId, attrKey,
                                targetAttrDslId, loadLater);
                    }
                    UUID paramId = next.get(dslAttrPath.paramId);
                    String string = next.get(dslAttrPath.string);
                    UUID list = next.get(dslAttrPath.list);
                    UUID ds = next.get(dslAttrPath.ds);
                    if (paramId != null) {
                        assert dataSet != null;
                        createParameter(attrPath, attribute, attrType,
                                paramId, dataSet, string, list, attrDslRef, ds, loadLater);
                    }
                }
            }

        }

        if (isSortEnabled) {
            assert dsl != null;
            Comparator<Attribute> comparator = Comparator.comparing(
                    attribute -> !AttributeType.DSL.equals(attribute.getType()));
            comparator = comparator.thenComparing(Named::getName);
            dsl.setAttributes(
                    dsl.getAttributes().stream()
                            .sorted(comparator)
                            .collect(Collectors.toList())
            );
        }
        return dsl;
    }

    private Union<Tuple> createUnion(UUID id, Collection<UUID> dataSetsFilter, Collection<UUID> attributesFilter,
                                     Integer startIndex, Integer endIndex) {
        return startIndex != null && endIndex != null && endIndex > startIndex
                ? queryFactory.query()
                        .with(dslDs, dslDs.getQuery(id, dataSetsFilter)
                                .offset(startIndex)
                                .limit(endIndex - startIndex + 1))
                        .with(dslAttr, dslAttr.getQuery(attributesFilter))
                        .with(dslAttrPath, dslAttrPath.getQuery())
                        .union(ImmutableList.of(queryFactory.select(dslAttrPath.all()).from(dslAttrPath),
                                queryFactory.select(dslAttr.all()).from(dslAttr)))
                        .orderBy(dslAttrPath.dsOrder.asc(), dslAttrPath.attrOrder.asc()) :
                queryFactory.query()
                        .with(dslDs, dslDs.getQuery(id, dataSetsFilter))
                        .with(dslAttr, dslAttr.getQuery(attributesFilter))
                        .with(dslAttrPath, dslAttrPath.getQuery())
                        .union(ImmutableList.of(queryFactory.select(dslAttrPath.all()).from(dslAttrPath),
                                queryFactory.select(dslAttr.all()).from(dslAttr)))
                        .orderBy(dslAttrPath.dsOrder.asc(), dslAttrPath.attrOrder.asc());
    }

    @Nonnull
    private DataSetList createDsl(@Nonnull UUID vaId, @Nonnull UUID dslId, String dslName,
                                  @Nonnull IdentifiedCache cache, @Nullable UUID testPlanId,
                                  UUID createdBy, Timestamp createdWhen, UUID modifiedBy, Timestamp modifiedWhen) {
        return cache.computeIfAbsent(DataSetList.class, dslId, dataSetListId -> {
            VisibilityArea va = Proxies.withId(VisibilityArea.class, vaId, uuid -> vaRepo.get().getById(uuid));
            List<Label> labels = Proxies.list(() -> labelRepo.get().getLabelsOfDsl(dataSetListId));
            TestPlan testPlan = null;
            if (testPlanId != null) {
                testPlan = Proxies.withId(TestPlan.class, testPlanId, uuid -> testPlanRepo.get().getById(uuid));
            }
            return new DataSetListImpl(dataSetListId, va, dslName, new ArrayList<>(), new ArrayList<>(), labels,
                    testPlan, createdBy, createdWhen, modifiedBy, modifiedWhen);
        });
    }

    private DataSet createDs(@Nonnull DataSetList dsl, @Nonnull UUID dsId, String dsName, Boolean dsLocked,
                             @Nonnull IdentifiedCache cache) {
        return cache.computeIfAbsent(DataSet.class, dsId, dataSetId -> {
            List<Label> labels = Proxies.list(() -> labelRepo.get().getLabelsOfDs(dataSetId));
            DataSetImpl result = new DataSetImpl(dataSetId, dsName, dsl, new ArrayList<>(), labels, dsLocked);
            dsl.getDataSets().add(result);
            return result;
        });
    }

    private Attribute createAttr(@Nonnull DataSetList dataSetList,
                                 @Nonnull UUID attrId, String attrName, @Nonnull AttributeType attrType,
                                 @Nullable UUID dslRef,
                                 @Nonnull List<ListValue> listValues,
                                 @Nonnull ToLoadLater toLoadLater) {
        return toLoadLater.computeIfAbsent(Attribute.class, attrId, attributeId -> {
            DataSetList dslReference = null;
            if (AttributeType.DSL == attrType && dslRef != null) {
                dslReference = toLoadLater.provideDslRef(dslRef);
            }
            AttributeImpl result = new AttributeImpl(attributeId, attrName, dataSetList, attrType,
                    dslReference, listValues, new ArrayList<>());
            dataSetList.getAttributes().add(result);
            return result;
        });
    }

    @Nonnull
    private Parameter createParameter(@Nullable AttributePath attrPath, @Nullable Attribute attribute,
                                      @Nonnull AttributeType type,
                                      @Nonnull UUID paramId,
                                      @Nonnull DataSet dataSet,
                                      @Nullable String string,
                                      @Nullable UUID list,
                                      @Nullable UUID dslRef,
                                      @Nullable UUID ds,
                                      @Nonnull ToLoadLater loadLater) {
        return loadLater.computeIfAbsent(Parameter.class, paramId, uuid -> {
            Parameter parameter;
            if (attrPath == null) {
                assert attribute != null;
                parameter = new ParameterImpl();
                parameter.setAttribute(attribute);
                attribute.getParameters().add(parameter);
            } else {
                ParameterOverlapImpl overlap = new ParameterOverlapImpl();
                overlap.setAttributePath(attrPath);
                parameter = overlap;
            }
            parameter.setId(paramId);
            parameter.setDataSet(dataSet);
            switch (type) {
                case CHANGE:
                case ENCRYPTED:
                case TEXT:
                    parameter.setText(string);
                    break;
                case FILE:
                    gridFsRepoProvider.get().getFileInfo(paramId).ifPresent(parameter::setFileData);
                    break;
                case LIST:
                    parameter.setListValue(list == null ? null : lvRepo.get().getById(list));
                    break;
                case DSL:
                    if (dslRef != null && ds != null) {
                        parameter.setDataSetReference(loadLater.provideDsRef(dslRef, ds));
                    }
                    break;
                default:
                    throw new AttributeTypeException(attribute.getName(), attribute.getType().toString());
            }
            dataSet.getParameters().add(parameter);
            return parameter;
        });
    }

    private AttributePath createAttrPath(@Nonnull UUID attrId, @Nonnull UUID dslId,
                                         @Nonnull DataSet dataSet, @Nonnull UUID targetAttrId,
                                         @Nonnull String attrKey, @Nonnull UUID targetAttrDslId,
                                         @Nonnull ToLoadLater loadLater) {
        AttributePathDto attrPathDto = AttributePathRepository.AttributePathProjection
                .createDto(attrId, dslId, dataSet.getId(), targetAttrId, attrKey);
        Attribute targetAttribute = loadLater.provideAttr(targetAttrDslId, targetAttrId);
        List<Attribute> attrList = attrPathDto.getAttributePathIds().stream()
                .map(uuid -> Proxies.withId(Attribute.class, uuid, attributeId -> attrRepo.get()
                        .getById(attributeId)))
                .collect(Collectors.toList());
        return new AttributePathImpl(attrPathDto.getId(), dataSet, targetAttribute, attrList);
    }

    /**
     * Does not take any items from backing cache. Puts newly created items into backing cache.
     */
    protected static class ToLoadLater extends StrongIdentifiedCache {
        private final DataSetListTreeRepository repo;
        private final IdentifiedCache cache;
        private final Multimap<UUID, UUID> loadLater = MultimapBuilder.hashKeys().hashSetValues().build();

        protected ToLoadLater(DataSetListTreeRepository repo, IdentifiedCache cache) {
            this.repo = repo;
            this.cache = cache;
        }

        @Nonnull
        public DataSet provideDsRef(@Nonnull UUID dslId, @Nonnull UUID dsId) {
            loadLater.put(dslId, dsId);
            return Proxies.withId(DataSet.class, dsId, uuid -> {
                DataSet result = cache.getIfPresent(DataSet.class, uuid);
                if (result != null) {
                    return result;
                }
                Collection<UUID> uuids = loadLater.get(dslId);
                repo.getDslTree(dslId, uuids, this);
                return cache.getIfPresent(DataSet.class, uuid);
            });
        }

        @Nonnull
        public DataSetList provideDslRef(@Nonnull UUID dslId) {
            return Proxies.withId(DataSetList.class, dslId, uuid -> {
                DataSetList result = cache.getIfPresent(DataSetList.class, uuid);
                if (result != null) {
                    return result;
                }
                Collection<UUID> uuids = loadLater.get(dslId);
                return repo.getDslTree(dslId, uuids, this);
            });
        }

        @Nonnull
        public Attribute provideAttr(@Nonnull UUID dslId, @Nonnull UUID attrId) {
            return Proxies.withId(Attribute.class, attrId, uuid -> {
                Attribute result = cache.getIfPresent(Attribute.class, attrId);
                if (result != null) {
                    return result;
                }
                Collection<UUID> uuids = loadLater.get(dslId);
                repo.getDslTree(dslId, uuids, this);
                return cache.getIfPresent(Attribute.class, uuid);
            });
        }

        @Nonnull
        @Override
        public <T extends Identified> T computeIfAbsent(@Nonnull Class<T> type, @Nonnull UUID id,
                                                        @Nonnull Function<UUID, ? extends T> sourceFunc) {
            T result = tryComputeIfAbsent(type, id, sourceFunc);
            Preconditions.checkNotNull(result,
                    "[%s] with id [%s] is not provided by [%s]", type, id, sourceFunc);
            return result;
        }

        @Override
        public <T extends Identified> void put(@Nonnull Class<T> type, @Nonnull T identified) {
            super.put(type, identified);
            cache.put(type, identified);
        }
    }

    abstract class TemporaryPathBase<T> extends SimplePath<T> {
        protected final Expression<List<?>> bindings;
        protected final Path<?>[] columns;
        private final ImmutableList.Builder<Expression<?>> bindingsBuilder = ImmutableList.builder();
        private final List<Path<?>> columnsBuilder = new LinkedList<>();

        /**
         * Contains helper methods for building aliases like 'datasetlist.id as dsl_id'
         */
        TemporaryPathBase(Class<? extends T> type, String variable) {
            super(type, null, variable);
            init();
            bindings = Projections.list(bindingsBuilder.build());
            columns = columnsBuilder.toArray(new Path<?>[columnsBuilder.size()]);
        }

        protected abstract void init();

        <V> SimplePath<V> delegateSimple(String property, @Nonnull SimplePath<V> delegate) {
            SimplePath<V> result = Expressions.path(delegate.getType(), this, property);
            bindingsBuilder.add(delegate.as(property));
            columnsBuilder.add(result);
            return result;
        }

        <V> SimplePath<V> delegateSimple(String property, @Nonnull Class<V> type, @Nullable SimplePath<V>
                delegate) {
            SimplePath<V> result = Expressions.path(type, this, property);
            SimpleExpression<V> as;
            if (delegate == null) {
                Preconditions.checkArgument(UUID.class.isAssignableFrom(type), "Null values casts not supported for "
                        + "type " + type);
                as = (SimpleExpression<V>) Expressions.as(typeSafeNullUuid, property);
            } else {
                as = delegate.as(property);
            }
            bindingsBuilder.add(as);
            columnsBuilder.add(result);
            return result;
        }

        <V> SimplePath<V> delegateSimple(SimplePath<V> delegate) {
            SimplePath<V> result = Expressions.path(delegate.getType(), this, delegate.getMetadata().getName());
            bindingsBuilder.add(delegate);
            columnsBuilder.add(result);
            return result;
        }

        StringPath delegateString(String property, @Nullable StringPath delegate) {
            StringPath result = Expressions.stringPath(this, property);
            Expression<String> as;
            if (delegate == null) {
                as = Expressions.as(typeSafeNullString, property);
            } else {
                as = delegate.as(property);
            }
            bindingsBuilder.add(as);
            columnsBuilder.add(result);
            return result;
        }

        StringPath delegateString(StringPath delegate) {
            StringPath result = Expressions.stringPath(this, delegate.getMetadata().getName());
            bindingsBuilder.add(delegate);
            columnsBuilder.add(result);
            return result;
        }

        <V extends Number
                & Comparable<?>> NumberPath<V> delegateNumber(String property,
                                                              NumberPath<V> delegate) {
            NumberPath<V> result = Expressions.numberPath(delegate.getType(), this, property);
            bindingsBuilder.add(delegate.as(property));
            columnsBuilder.add(result);
            return result;
        }

        <V extends Number
                & Comparable<?>> NumberPath<V> delegateNumber(NumberPath<V> delegate) {
            NumberPath<V> result = Expressions.numberPath(delegate.getType(), this,
                    delegate.getMetadata().getName());
            bindingsBuilder.add(delegate);
            columnsBuilder.add(result);
            return result;
        }

        BooleanPath delegateBoolean(String property, BooleanPath delegate) {
            BooleanPath result = Expressions.booleanPath(this, property);
            bindingsBuilder.add(delegate.as(property));
            columnsBuilder.add(result);
            return result;
        }

        BooleanPath delegateBoolean(BooleanPath delegate) {
            BooleanPath result = Expressions.booleanPath(this, delegate.getMetadata().getName());
            bindingsBuilder.add(delegate);
            columnsBuilder.add(result);
            return result;
        }

        <V extends Comparable> DateTimePath<V> delegateDateTime(String property, DateTimePath<V> delegate) {
            DateTimePath<V> result = Expressions.dateTimePath(delegate.getType(), this, property);
            bindingsBuilder.add(delegate.as(property));
            columnsBuilder.add(result);
            return result;
        }

        <V extends Comparable> DateTimePath<V> delegateDateTime(DateTimePath<V> delegate) {
            DateTimePath<V> result =
                    Expressions.dateTimePath(delegate.getType(), this, delegate.getMetadata().getName());
            bindingsBuilder.add(delegate);
            columnsBuilder.add(result);
            return result;
        }

        public Expression<List<?>> bindings() {
            return bindings;
        }

        public Path<?>[] all() {
            return columns;
        }

    }

    class QDslDs extends TemporaryPathBase<QDslDs> {

        SimplePath<UUID> dslId;
        SimplePath<UUID> vaId;
        StringPath dslName;
        SimplePath<UUID> testPlanId;
        SimplePath<UUID> createdBy;
        DateTimePath<Timestamp> createdWhen;
        SimplePath<UUID> modifiedBy;
        DateTimePath<Timestamp> modifiedWhen;
        SimplePath<UUID> dsId;
        StringPath dsName;
        NumberPath<Long> dsOrder;
        BooleanPath dsLocked;

        QDslDs(String variable) {
            super(QDslDs.class, variable);
        }

        @Override
        protected void init() {
            dslId = delegateSimple("dsl_id", DSL.id);
            vaId = delegateSimple("va_id", DSL.visibilityAreaId);
            dslName = delegateString("dsl_name", DSL.name);
            testPlanId = delegateSimple("test_plan_id", DSL.testPlanId);
            createdBy = delegateSimple("created_by", DSL.createdBy);
            createdWhen = delegateDateTime("created_when", DSL.createdWhen);
            modifiedBy = delegateSimple("modified_by", DSL.modifiedBy);
            modifiedWhen = delegateDateTime("modified_when", DSL.modifiedWhen);
            dsId = delegateSimple("ds_id", DS.id);
            dsName = delegateString("ds_name", DS.name);
            dsOrder = delegateNumber("ds_order", DS.ordering);
            dsLocked = delegateBoolean("ds_locked",DS.locked);
        }

        QDslAttr createAttrs(String alias) {
            return new QDslAttr(alias);
        }

        QDslAttrPath createAttrPath(String alias) {
            return new QDslAttrPath(alias);
        }

        SQLQuery<List<?>> getQuery(@Nonnull UUID id, @Nullable Collection<UUID> dataSetsFilter) {
            BooleanExpression predicate = DS.datasetlistId.eq(DSL.id);
            if (dataSetsFilter != null && !dataSetsFilter.isEmpty()) {
                predicate = predicate.and(DS.id.in(dataSetsFilter));
            }

            return queryFactory.select(bindings())
                    .from(DSL)
                    .leftJoin(DS).on(predicate)
                    .where(DSL.id.eq(id))
                    .orderBy(DS.ordering.asc());
        }

        private abstract class AbstractQDslAttr<T extends AbstractQDslAttr> extends TemporaryPathBase<T> {
            SimplePath<UUID> dslId;
            SimplePath<UUID> vaId;
            StringPath dslName;
            SimplePath<UUID> testPlanId;
            SimplePath<UUID> createdBy;
            DateTimePath<Timestamp> createdWhen;
            SimplePath<UUID> modifiedBy;
            DateTimePath<Timestamp> modifiedWhen;
            SimplePath<UUID> dsId;
            BooleanPath dsLocked;
            StringPath dsName;
            NumberPath<Long> dsOrder;
            SimplePath<UUID> attrId;
            StringPath attrName;
            NumberPath<Short> attrType;
            SimplePath<UUID> attrDslRef;
            NumberPath<Integer> attrOrder;
            StringPath attrKey;
            SimplePath<UUID> targetAttrId;
            SimplePath<UUID> targetAttrDslId;
            SimplePath<UUID> paramId;
            StringPath string;
            SimplePath<UUID> list;
            SimplePath<UUID> ds;

            /**
             * Used to declare the same set of fields for different 'with' clauses. To be able to do
             * union later.
             */
            private AbstractQDslAttr(Class<? extends T> type, String variable) {
                super(type, variable);
            }

            @Override
            protected void init() {
                dslId = delegateSimple(QDslDs.this.dslId);
                vaId = delegateSimple(QDslDs.this.vaId);
                dslName = delegateString(QDslDs.this.dslName);
                testPlanId = delegateSimple(QDslDs.this.testPlanId);
                createdBy = delegateSimple(QDslDs.this.createdBy);
                createdWhen = delegateDateTime(QDslDs.this.createdWhen);
                modifiedBy = delegateSimple(QDslDs.this.modifiedBy);
                modifiedWhen = delegateDateTime(QDslDs.this.modifiedWhen);
                dsId = delegateSimple(QDslDs.this.dsId);
                dsLocked = delegateBoolean(QDslDs.this.dsLocked);
                dsName = delegateString(QDslDs.this.dsName);
                dsOrder = delegateNumber(QDslDs.this.dsOrder);
                attrId = delegateSimple("attr_id", UUID.class, getAttrId());
                attrName = delegateString("attr_name", ATTR.name);
                attrType = delegateNumber("attr_type", ATTR.attributeTypeId);
                attrDslRef = delegateSimple("attr_dsl_ref", ATTR.typeDatasetlistId);
                attrOrder = delegateNumber("attr_order", ATTR.ordering);
                attrKey = delegateString("attr_key", getAttrKey());
                targetAttrDslId = delegateSimple("target_attr_dsl_id", UUID.class, getTargetAttrDslId());
                targetAttrId = delegateSimple("target_attr_id", UUID.class, getTargetAttrId());
                paramId = delegateSimple("param_id", PARAM.id);
                string = delegateString("string", PARAM.string);
                list = delegateSimple("list", PARAM.list);
                ds = delegateSimple("ds", PARAM.ds);
            }

            @Nullable
            protected abstract SimplePath<UUID> getAttrId();

            @Nullable
            protected abstract StringPath getAttrKey();

            @Nullable
            protected abstract SimplePath<UUID> getTargetAttrId();

            @Nullable
            protected abstract SimplePath<UUID> getTargetAttrDslId();
        }

        class QDslAttr extends AbstractQDslAttr<QDslAttr> {

            private QDslAttr(String variable) {
                super(QDslAttr.class, variable);
            }


            @Nullable
            @Override
            protected SimplePath<UUID> getAttrId() {
                return ATTR.id;
            }

            @Nullable
            @Override
            protected StringPath getAttrKey() {
                return null;
            }

            @Nullable
            @Override
            protected SimplePath<UUID> getTargetAttrId() {
                return null;
            }

            @Nullable
            @Override
            protected SimplePath<UUID> getTargetAttrDslId() {
                return null;
            }

            SQLQuery<List<?>> getQuery(@Nullable Collection<UUID> attributesFilter) {

                SQLQuery<List<?>> query = queryFactory.select(bindings())
                        .from(QDslDs.this);
                if (!CollectionUtils.isEmpty(attributesFilter)) {
                    query.leftJoin(ATTR).on(ATTR.datasetlistId.eq(QDslDs.this.dslId)
                            .and(ATTR.id.in(attributesFilter)));
                } else {
                    query.leftJoin(ATTR).on(ATTR.datasetlistId.eq(QDslDs.this.dslId));
                }
                query.leftJoin(PARAM).on(PARAM.attributeId.eq(ATTR.id).and(PARAM.datasetId.eq(QDslDs.this.dsId)));
                return query;

            }
        }

        class QDslAttrPath extends AbstractQDslAttr<QDslAttrPath> {

            private QDslAttrPath(String variable) {
                super(QDslAttrPath.class, variable);
            }

            @Nullable
            @Override
            protected SimplePath<UUID> getAttrId() {
                return AK.id;
            }

            @Nullable
            @Override
            protected StringPath getAttrKey() {
                return AK.key;
            }

            @Nullable
            @Override
            protected SimplePath<UUID> getTargetAttrId() {
                return AK.attributeId;
            }

            @Nullable
            @Override
            protected SimplePath<UUID> getTargetAttrDslId() {
                return ATTR.datasetlistId;
            }

            SQLQuery<List<?>> getQuery() {
                return queryFactory.select(bindings())
                        .from(QDslDs.this)
                        .leftJoin(AK).on(AK.datasetlistId.eq(QDslDs.this.dslId).and(AK.datasetId.eq(QDslDs.this.dsId)))
                        .leftJoin(PARAM).on(PARAM.attributeId.eq(AK.id))
                        .leftJoin(ATTR).on(ATTR.id.eq(AK.attributeId));
            }
        }
    }
}

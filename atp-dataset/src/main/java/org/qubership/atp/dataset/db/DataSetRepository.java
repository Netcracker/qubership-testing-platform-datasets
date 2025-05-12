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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;

@Repository
public class DataSetRepository extends AbstractRepository {

    private final Provider<DataSetListRepository> dslRepo;
    private final Provider<ParameterRepository> paramRepo;
    private final Provider<LabelRepository> labelRepo;
    private final CacheRepository cacheRepo;
    private final DataSetProjection projection;
    private final SQLQueryFactory queryFactory;
    private final AttributePathRepository attrPathRepo;

    /**
     * data set repository.
     *
     * @param queryFactory is used for creating sql queries.
     */
    @Autowired
    public DataSetRepository(@Nonnull SQLQueryFactory queryFactory,
                             @Nonnull Provider<DataSetListRepository> dslRepo,
                             @Nonnull Provider<ParameterRepository> paramRepo,
                             @Nonnull Provider<LabelRepository> labelRepo,
                             @Nonnull AttributePathRepository attrPathRepo,
                             @Nonnull CacheRepository cacheRepo) {
        super();
        this.queryFactory = queryFactory;
        this.dslRepo = dslRepo;
        this.paramRepo = paramRepo;
        this.labelRepo = labelRepo;
        this.cacheRepo = cacheRepo;
        this.attrPathRepo = attrPathRepo;
        this.projection = new DataSetProjection(this);
    }

    /**
     * Creates data set.
     */
    @Nonnull
    public DataSet create(@Nonnull UUID dslId, @Nonnull String name) {
        UUID id = queryFactory.insert(DS)
                .set(DS.name, name)
                .set(DS.datasetlistId, dslId)
                .set(DS.ordering, getNextvalOfSequenceDataset())
                .executeWithKey(DS.id);
        Preconditions.checkNotNull(id, "nothing created");
        return projection.create(id, dslId, name, false);
    }

    /**
     * Restores data set.
     */
    public boolean restore(@Nonnull UUID dslId, @Nonnull UUID dsId,
                           @Nonnull String name, @Nullable UUID previousDataSet) {
        Long ordering = 0L;
        if (previousDataSet != null) {
            ordering = queryFactory.select(DS.ordering).from(DS).where(DS.id.eq(previousDataSet)).fetchFirst() + 1;
        }
        return queryFactory.insert(DS)
                .set(DS.id, dsId)
                .set(DS.name, name)
                .set(DS.datasetlistId, dslId)
                .set(DS.ordering, ordering).execute() > 0;
    }

    @Nullable
    public DataSet getById(@Nonnull UUID id) {
        return cacheRepo.tryComputeIfAbsent(DataSet.class, id, uuid -> select(DS.id.eq(uuid)).fetchOne());
    }

    public boolean existsById(UUID id) {
        return cacheRepo.getIfPresent(DataSet.class, id) != null
                || select(DS.id.eq(id)).fetchCount() > 0;
    }

    @Nonnull
    public List<DataSet> getByParentId(@Nonnull UUID dslId) {
        return select(DS.datasetlistId.eq(dslId)).orderBy(DS.ordering.asc()).fetch();
    }

    @Nonnull
    public List<String> getOccupiedNamesByParentId(@Nonnull UUID dslId) {
        return queryFactory.select(DS.name).from(DS).where(DS.datasetlistId.eq(dslId)).fetch();
    }

    @Nonnull
    public List<DataSet> getByParentIdAndLabel(@Nonnull UUID dslId, @Nonnull String labelName) {
        return select(DS.datasetlistId.eq(dslId).and(labelRepo.get().dsByLabelName(labelName))).fetch();
    }

    @Nonnull
    public List<DataSet> getAll() {
        return queryFactory.select(projection).from(DS).fetch();
    }

    @Nonnull
    public List<DataSet> getAll(List<UUID> ids) {
        return queryFactory.select(projection).from(DS).where(DS.id.in(ids)).fetch();
    }

    @Nonnull
    public SimpleExpression<Long> getNextvalOfSequenceDataset() {
        return SQLExpressions.nextval("SEQUENCE_DATASET");
    }


    @Nonnull
    protected SQLQuery<DataSet> select(@Nonnull Predicate predicate) {
        return queryFactory.select(projection).from(DS).where(predicate);
    }

    public boolean rename(@Nonnull UUID id, @Nonnull String name) {
        return queryFactory.update(DS).where(DS.id.eq(id)).set(DS.name, name).execute() > 0;
    }

    /**
     * DataSets ids lock.
     */
    public boolean lock(@Nonnull List<UUID> ids, boolean isLock) {
        return ids.stream().allMatch(id ->
            queryFactory.update(DS).where(DS.id.eq(id)).set(DS.locked, isLock).execute() > 0
        );
    }

    void onDslDeleteCascade(UUID dslId) {
        getByParentId(dslId).forEach(ds -> delete(ds.getId()));
    }

    /**
     * Cascade delete of parameters, labels.
     */
    public boolean delete(@Nonnull UUID id) {
        paramRepo.get().onDsDeleteCascade(id);
        labelRepo.get().onDsDeleteCascade(id);
        attrPathRepo.onDsDeleteCascade(id);
        return delete(DS.id.eq(id)) > 0;
    }

    private long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(DS).where(predicate).execute();
    }

    /**
     * Finds dataSets with provided dataSet id.
     */
    @Nonnull
    public List<DataSet> getAffectedDataSetsByChangesDataSetReference(@Nonnull UUID dataSetId) {
        return select(DS.id.in(queryFactory
                .select(PARAM.datasetId)
                .from(PARAM)
                .where(PARAM.ds.eq(dataSetId)))).fetch();
    }

    /**
     * Finds all info with provided by affected dataSet id.
     */
    @Nonnull
    @SuppressWarnings("PMD")
    public List<?> getAffectedInfoByChangesDataSetReference(@Nonnull UUID dataSetId) {
        List<Tuple> result = queryFactory
                .select(DSL.id, DSL.name, DS.id, DS.name, PARAM.id, PARAM.ds, ATTR.id, ATTR.name)
                .from(PARAM)
                .where(PARAM.ds.eq(dataSetId))
                .leftJoin(DS).on(PARAM.datasetId.eq(DS.id))
                .leftJoin(DSL).on(DS.datasetlistId.eq(DSL.id))
                .leftJoin(ATTR).on(ATTR.id.eq(PARAM.attributeId))
                .fetch();

        return result.stream().map(TableResponse::fromParameterTuple).collect(Collectors.toList());
    }

    @Nonnull
    public Label mark(@Nonnull UUID dsId, @Nonnull String labelName) {
        return labelRepo.get().markDs(dsId, labelName);
    }

    public boolean unmark(@Nonnull UUID dsId, @Nonnull UUID labelId) {
        return labelRepo.get().unmarkDs(dsId, labelId);
    }

    @Nonnull
    public List<Label> getLabels(@Nonnull UUID dsId) {
        return labelRepo.get().getLabelsOfDs(dsId);
    }

    private static class DataSetProjection extends MappingProjection<DataSet> {

        private final DataSetRepository repo;

        private DataSetProjection(@Nonnull DataSetRepository repo) {
            super(DataSet.class, DS.all());
            this.repo = repo;
        }

        @Override
        protected DataSetImpl map(Tuple row) {
            UUID id = row.get(DS.id);
            assert id != null;
            UUID dslId = row.get(DS.datasetlistId);
            assert dslId != null;
            Boolean locked = row.get(DS.locked);
            return create(id, dslId, row.get(DS.name), locked);
        }

        private DataSetImpl create(UUID id, UUID dslId, String name, Boolean locked) {
            DataSetList dsl = Proxies.withId(DataSetList.class, dslId, uuid -> repo.dslRepo.get().getById(uuid));
            List<Parameter> parameters = Proxies.list(() -> repo.paramRepo.get().getByDataSetId(id));
            List<Label> labels = Proxies.list(() -> repo.labelRepo.get().getLabelsOfDs(id));
            return new DataSetImpl(id, name, dsl, parameters, labels, locked);
        }
    }
}

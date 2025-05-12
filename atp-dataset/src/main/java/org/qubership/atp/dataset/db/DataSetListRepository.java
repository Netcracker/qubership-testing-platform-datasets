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

import static java.util.Objects.nonNull;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.impl.TableResponse;
import org.qubership.atp.dataset.service.rest.dto.manager.AffectedDataSetList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;

@Repository
public class DataSetListRepository extends AbstractRepository {

    private final DataSetListProjection dslProjection;
    private final SQLQueryFactory queryFactory;
    private final Provider<DataSetRepository> dsRepo;
    private final Provider<AttributeRepository> attrRepo;
    private final Provider<LabelRepository> labelRepo;
    private final Provider<VisibilityAreaRepository> vaRepo;
    private final Provider<TestPlanRepository> testPlanRepo;
    private final CacheRepository cacheRepo;

    /**
     * data set list repository.
     *
     * @param queryFactory is used for creating sql queries.
     */
    @Autowired
    public DataSetListRepository(@Nonnull SQLQueryFactory queryFactory,
                                 @Nonnull Provider<VisibilityAreaRepository> vaRepo,
                                 @Nonnull Provider<DataSetRepository> dsRepo,
                                 @Nonnull Provider<AttributeRepository> attrRepo,
                                 @Nonnull Provider<LabelRepository> labelRepo,
                                 @Nonnull CacheRepository cacheRepo,
                                 @Nonnull Provider<TestPlanRepository> testPlanRepo) {
        super();
        this.queryFactory = queryFactory;
        this.vaRepo = vaRepo;
        this.dsRepo = dsRepo;
        this.attrRepo = attrRepo;
        this.labelRepo = labelRepo;
        this.cacheRepo = cacheRepo;
        this.testPlanRepo = testPlanRepo;
        this.dslProjection = new DataSetListProjection(this);
    }

    /**
     * Creates data set list.
     */
    @Nonnull
    public DataSetList create(@Nonnull UUID visibilityAreaId, @Nonnull String name, @Nullable UUID testPlanId,
                              @Nonnull UUID createdBy, @Nonnull Timestamp createdWhen) {
        UUID dataSetListId = Preconditions.checkNotNull(
                queryFactory.insert(DSL).set(DSL.visibilityAreaId, visibilityAreaId)
                        .set(DSL.name, name).set(DSL.testPlanId, testPlanId)
                        .set(DSL.createdBy, createdBy).set(DSL.createdWhen, createdWhen)
                        .set(DSL.modifiedBy, createdBy).set(DSL.modifiedWhen, createdWhen)
                        .executeWithKey(DSL.id), "nothing created");
        Preconditions.checkNotNull(dataSetListId, "nothing created");
        return dslProjection.create(dataSetListId, name, visibilityAreaId, testPlanId,
                createdBy, createdWhen, createdBy, createdWhen);
    }

    @Nullable
    public DataSetList getById(@Nonnull UUID id) {
        return cacheRepo.tryComputeIfAbsent(DataSetList.class, id, uuid -> select(DSL.id.eq(uuid)).fetchOne());
    }

    @Nonnull
    public List<DataSetList> getAll() {
        return queryFactory.select(dslProjection).from(DSL).fetch();
    }

    @Nonnull
    public List<DataSetList> getAll(@Nonnull UUID visibilityArea) {
        return queryFactory.select(dslProjection).from(DSL).where(DSL.visibilityAreaId.eq(visibilityArea)).fetch();
    }

    @Nonnull
    public List<DataSetList> getAll(@Nonnull List<UUID> datasetListIds) {
        return queryFactory.select(dslProjection).from(DSL).where(DSL.id.in(datasetListIds)).fetch();
    }

    @Nonnull
    protected SQLQuery<DataSetList> select(@Nonnull Predicate predicate) {
        return queryFactory.select(dslProjection).from(DSL).where(predicate);
    }

    /**
     * Rename dsl.
     */
    public boolean rename(@Nonnull UUID id, @Nonnull String name, UUID modifiedBy, Timestamp modifiedWhen) {
        long update = queryFactory.update(DSL)
                .where(DSL.id.eq(id))
                .set(DSL.name, name)
                .set(DSL.modifiedBy, modifiedBy)
                .set(DSL.modifiedWhen, modifiedWhen)
                .execute();
        return entitiesWasUpdated(update);
    }

    /**
     * Update or add test plan id in dsl.
     */
    public boolean setTestPlan(@Nonnull UUID id, @Nullable UUID testPlanId, UUID modifiedBy, Timestamp modifiedWhen) {
        long update = queryFactory.update(DSL)
                .where(DSL.id.eq(id))
                .set(DSL.testPlanId, testPlanId)
                .set(DSL.modifiedBy, modifiedBy)
                .set(DSL.modifiedWhen, modifiedWhen)
                .execute();
        return entitiesWasUpdated(update);
    }

    private boolean entitiesWasUpdated(long amount) {
        return amount > 0;
    }

    void onVaDeleteCascade(UUID vaId) {
        getAll(vaId).forEach(dsl -> delete(dsl.getId()));
    }

    /**
     * Cascade delete of dataset. Method will remove all referenced parameters, attributes,
     * parameters, labels.
     */
    public boolean delete(@Nonnull UUID id) {
        attrRepo.get().onDslDeleteCascade(id);
        dsRepo.get().onDslDeleteCascade(id);
        labelRepo.get().onDslDeleteCascade(id);
        return delete(DSL.id.eq(id)) > 0;
    }

    private long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(DSL).where(predicate).execute();
    }

    /**
     * Returns {@link DataSetList} by name under {@link VisibilityArea}.
     *
     * @param visibilityArea current VA
     * @param name           dslName
     * @return dsl
     */
    public DataSetList getByNameUnderVisibilityArea(UUID visibilityArea, String name) {
        return queryFactory.select(dslProjection)
                .from(DSL)
                .where(DSL.visibilityAreaId.eq(visibilityArea).and(DSL.name.eq(name)))
                .fetchOne();
    }

    /**
     * Adds new dataSetListLabel.
     */
    @Nonnull
    public Label mark(@Nonnull UUID dslId, @Nonnull String labelName, UUID modifiedBy, Timestamp modifiedWhen) {
        Label label = labelRepo.get().markDsl(dslId, labelName);
        updateModifiedFields(dslId, modifiedBy, modifiedWhen);
        return label;
    }

    /**
     * Deletes dataSetListLabel.
     */
    public boolean unmark(@Nonnull UUID dslId, @Nonnull UUID labelId, UUID modifiedBy, Timestamp modifiedWhen) {
        boolean unmark = labelRepo.get().unmarkDsl(dslId, labelId);
        if (unmark) {
            updateModifiedFields(dslId, modifiedBy, modifiedWhen);
        }
        return unmark;
    }

    @Nonnull
    public List<Label> getLabels(@Nonnull UUID dslId) {
        return labelRepo.get().getLabelsOfDsl(dslId);
    }

    @Nonnull
    public List<DataSetList> getAllByLabel(@Nonnull UUID visibilityArea, @Nonnull String labelName) {
        return select(DSL.visibilityAreaId.eq(visibilityArea).and(labelRepo.get().dslByLabelName(labelName))).fetch();
    }

    public List<DataSetList> getUnderTestPlan(UUID testPlanId) {
        return select(DSL.testPlanId.eq(testPlanId)).fetch();
    }

    /**
     * Deletes test plan for DSL.
     */
    public void onTestPlanDeleteCascade(UUID testPlanId, UUID modifiedBy, Timestamp modifiedWhen) {
        queryFactory.update(DSL)
                .where(DSL.testPlanId.eq(testPlanId))
                .set(DSL.testPlanId, (UUID) null)
                .set(DSL.modifiedBy, modifiedBy)
                .set(DSL.modifiedWhen, modifiedWhen)
                .execute();
    }

    /**
     * Updates modifiedBy and modifiedWhen fields.
     */
    public void updateModifiedFields(UUID dataSetListId, UUID modifiedBy, Timestamp modifiedWhen) {
        queryFactory.update(DSL)
                .where(DSL.id.eq(dataSetListId))
                .set(DSL.modifiedBy, modifiedBy)
                .set(DSL.modifiedWhen, modifiedWhen)
                .execute();
    }

    /**
     * Get attributes affected by dsl.
     */
    public List<TableResponse> getAffectedAttributes(UUID dataSetListId, Integer page, Integer size) {
        SQLQuery<Tuple> query = queryFactory
                .select(DSL.id, DSL.name, ATTR.id, ATTR.name)
                .from(ATTR)
                .where(ATTR.typeDatasetlistId.eq(dataSetListId))
                .leftJoin(DSL).on(ATTR.datasetlistId.eq(DSL.id));

        if (nonNull(page) && nonNull(size)) {
            query.offset(page * size)
                    .limit(size);
        }

        return query.fetch()
                .stream()
                .map(TableResponse::fromParameterTuple)
                .collect(Collectors.toList());
    }

    /**
     * Check if dsl exists.
     *
     * @param dataSetListId DSL id
     * @return 'true' if dsl exists, otherwise 'false'
     */
    public boolean existsById(@Nonnull UUID dataSetListId) {
        DataSetList dataSetList = getById(dataSetListId);
        return nonNull(dataSetList);
    }

    /**
     * Get affected DSL by dataSetListId.
     *
     * @param dataSetListId DSL id
     * @param limit number of rows that are returned
     * @param offset number of rows that are skipped
     * @return list with DSL id and DSL name
     */
    public List<AffectedDataSetList> getAffectedDataSetLists(UUID dataSetListId, Integer limit, Integer offset) {
        List<Tuple> result = queryFactory
                .select(DSL.id, DSL.name)
                .from(ATTR)
                .where(ATTR.typeDatasetlistId.eq(dataSetListId))
                .innerJoin(DSL)
                .on(ATTR.datasetlistId.eq(DSL.id))
                .groupBy(DSL.id, DSL.name)
                .orderBy(DSL.name.asc())
                .limit(limit)
                .offset(offset)
                .fetch();
        return result.stream()
                .map(AffectedDataSetList::fromTuple)
                .collect(Collectors.toList());
    }

    /**
     * Get modifiedWhen by dataSetListId.
     *
     * @param dataSetListId dataSetListId
     * @return {@link Timestamp} of modified
     */
    public Timestamp getModifiedWhen(UUID dataSetListId) {
        return queryFactory
                .select(DSL.modifiedWhen)
                .from(DSL)
                .where(DSL.id.eq(dataSetListId))
                .fetchOne();
    }

    private static class DataSetListProjection extends MappingProjection<DataSetList> {

        private final DataSetListRepository repo;

        private DataSetListProjection(DataSetListRepository repo) {
            super(DataSetList.class, DSL.all());
            this.repo = repo;
        }

        @Override
        protected DataSetListImpl map(Tuple row) {
            UUID id = row.get(DSL.id);
            Preconditions.checkNotNull(id);
            UUID vaId = row.get(DSL.visibilityAreaId);
            Preconditions.checkNotNull(vaId);
            UUID testPlanId = row.get(DSL.testPlanId);
            String name = row.get(DSL.name);
            Preconditions.checkNotNull(name);
            Timestamp createdWhen = row.get(DSL.createdWhen);
            UUID createdBy = row.get(DSL.createdBy);
            Timestamp modifiedWhen = row.get(DSL.modifiedWhen);
            UUID modifiedBy = row.get(DSL.modifiedBy);
            return create(id, name, vaId, testPlanId, createdBy, createdWhen, modifiedBy, modifiedWhen);
        }

        private DataSetListImpl create(@Nonnull UUID dataSetListId,
                                       @Nonnull String name,
                                       @Nonnull UUID visibilityAreaId,
                                       @Nullable UUID testPlanId,
                                       UUID createdBy,
                                       Timestamp createdWhen,
                                       UUID modifiedBy,
                                       Timestamp modifiedWhen) {
            VisibilityArea va = Proxies.withId(VisibilityArea.class, visibilityAreaId,
                    uuid -> repo.vaRepo.get().getById(uuid));
            List<DataSet> dataSets = Proxies.list(() -> repo.dsRepo.get().getByParentId(dataSetListId));
            final AttributeRepository attrRepo = repo.attrRepo.get();
            List<Attribute> attrs = Proxies.list(() -> attrRepo.getByParentId(dataSetListId));
            List<Label> labels = Proxies.list(() -> repo.labelRepo.get().getLabelsOfDsl(dataSetListId));

            TestPlan testPlan = null;

            if (testPlanId != null) {
                testPlan = Proxies.withId(TestPlan.class, testPlanId, uuid -> repo.testPlanRepo.get().getById(uuid));
            }

            return new DataSetListImpl(dataSetListId, va, name, dataSets, attrs, labels, testPlan,
                    createdBy, createdWhen, modifiedBy, modifiedWhen) {

                @Nonnull
                @Override
                public Collection<Attribute> getAttributes(@Nonnull AttributeType type) {
                    return attrRepo.getByParentId(id, type);
                }
            };
        }
    }
}

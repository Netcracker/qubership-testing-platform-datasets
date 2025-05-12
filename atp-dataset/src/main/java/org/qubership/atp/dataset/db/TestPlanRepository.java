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
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.TestPlanImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.mysema.commons.lang.Pair;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;

@Repository
public class TestPlanRepository extends AbstractRepository {

    private static final Logger LOG = LoggerFactory.getLogger(TestPlanRepository.class);

    private final TestPlanProjection testPlanProjection;
    private final SQLQueryFactory queryFactory;
    private final Provider<VisibilityAreaRepository> visibilityAreaRepo;
    private final Provider<DataSetListRepository> dslRepo;
    private final CacheRepository cacheRepo;

    /**
     * visibility area repository.
     */
    @Autowired
    public TestPlanRepository(@Nonnull SQLQueryFactory queryFactory,
                              @Nonnull CacheRepository cacheRepo,
                              @Nonnull Provider<VisibilityAreaRepository> visibilityAreaRepo,
                              @Nonnull Provider<DataSetListRepository> dslRepo) {
        super();
        this.visibilityAreaRepo = visibilityAreaRepo;
        this.queryFactory = queryFactory;
        this.cacheRepo = cacheRepo;
        this.dslRepo = dslRepo;
        this.testPlanProjection = new TestPlanProjection(this);
    }

    /**
     * Creates new test plan.
     *
     * @param name             name of test plan.
     * @param visibilityAreaId va where to create test plan.
     */
    @Nonnull
    public Pair<TestPlan, Boolean> create(@Nonnull String name, @Nonnull UUID visibilityAreaId) {
        UUID testPlanId = null;
        boolean created = false;
        try {
            testPlanId = Preconditions.checkNotNull(
                    queryFactory.insert(TEST_PLAN).set(TEST_PLAN.visibilityAreaId, visibilityAreaId)
                            .set(TEST_PLAN.name, name).executeWithKey(TEST_PLAN.id), "nothing created");
            created = true;
        } catch (RuntimeException e) {
            try {
                TestPlan testPlan = getByNameUnderVisibilityArea(visibilityAreaId, name);
                if (testPlan.getId() != null) {
                    LOG.warn("Test plan with name " + name + " already exists in VA with id: " + visibilityAreaId
                            + ". Test Plan Id: " + testPlanId);
                    testPlanId = testPlan.getId();
                }
                Preconditions.checkNotNull(testPlanId, "nothing created");
            } catch (RuntimeException nested) {
                e.addSuppressed(nested);
                throw e;
            }
        }
        return Pair.of(testPlanProjection.create(testPlanId, name, visibilityAreaId), created);
    }

    /**
     * Get test plan by id.
     */
    @Nullable
    public TestPlan getById(@Nonnull UUID id) {
        return cacheRepo.tryComputeIfAbsent(TestPlan.class, id, uuid -> select(TEST_PLAN.id.eq(uuid)).fetchOne());
    }

    public boolean existsById(UUID id) {
        return cacheRepo.getIfPresent(TestPlan.class, id) != null
                || select(TEST_PLAN.id.eq(id)).fetchCount() > 0;
    }

    /**
     * Get all test plans under va.
     *
     * @param visibilityArea va
     * @return list of test plans
     */
    @Nonnull
    public List<TestPlan> getAll(@Nonnull UUID visibilityArea) {
        return queryFactory.select(testPlanProjection)
                .from(TEST_PLAN)
                .where(TEST_PLAN.visibilityAreaId.eq(visibilityArea))
                .fetch();
    }

    /**
     * Get all test plans.
     */
    @Nonnull
    public List<TestPlan> getAll() {
        return queryFactory.select(testPlanProjection).from(TEST_PLAN).fetch();
    }

    @Nonnull
    protected SQLQuery<TestPlan> select(@Nonnull Predicate predicate) {
        return queryFactory.select(testPlanProjection).from(TEST_PLAN).where(predicate);
    }

    /**
     * Rename test plan.
     *
     * @param id   id of test plan.
     * @param name new name.
     * @return true if operation is successful.
     */
    public boolean rename(@Nonnull UUID id, @Nonnull String name) {
        return queryFactory.update(TEST_PLAN).where(TEST_PLAN.id.eq(id)).set(TEST_PLAN.name, name).execute() > 0;
    }

    void onVaDeleteCascade(UUID vaId) {
        getAll(vaId).forEach(testPlan -> delete(testPlan.getId(), null, null));
    }

    /**
     * Cascade delete of testplans.
     */
    public boolean delete(@Nonnull UUID id, UUID modifiedBy, Timestamp modifiedWhen) {
        dslRepo.get().onTestPlanDeleteCascade(id, modifiedBy, modifiedWhen);
        return delete(TEST_PLAN.id.eq(id)) > 0;
    }

    /**
     * Delete {@link TestPlan} by name under {@link VisibilityArea}.
     *
     * @param vaId  VisibilityArea id
     * @param name   test plan name
     * @return {@code true} if test plan was deleted successfully and {@code false} if nothing deleted.
     */
    public boolean delete(@Nonnull UUID vaId, @Nonnull String name, UUID modifiedBy, Timestamp modifiedWhen) {
        TestPlan testPlan = this.getByNameUnderVisibilityArea(vaId, name);
        return testPlan != null && this.delete(testPlan.getId(), modifiedBy, modifiedWhen);
    }

    private long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(TEST_PLAN).where(predicate).execute();
    }

    /**
     * Returns {@link TestPlan} by name under {@link VisibilityArea}.
     *
     * @param visibilityArea current VA
     * @param name           test plan name
     * @return test plan
     */
    public TestPlan getByNameUnderVisibilityArea(UUID visibilityArea, String name) {
        return queryFactory.select(testPlanProjection)
                .from(TEST_PLAN)
                .where(TEST_PLAN.visibilityAreaId.eq(visibilityArea).and(TEST_PLAN.name.eq(name)))
                .fetchOne();
    }

    private static class TestPlanProjection extends MappingProjection<TestPlan> {

        private final TestPlanRepository repo;

        TestPlanProjection(TestPlanRepository repo) {
            super(TestPlan.class, TEST_PLAN.all());
            this.repo = repo;
        }

        @Override
        protected TestPlanImpl map(Tuple row) {
            UUID id = row.get(TEST_PLAN.id);
            assert id != null;
            UUID vaId = row.get(TEST_PLAN.visibilityAreaId);
            assert vaId != null;
            String name = row.get(TEST_PLAN.name);
            assert name != null;
            return create(id, name, vaId);
        }

        public TestPlanImpl create(@Nonnull UUID testPlanId, @Nonnull String name, @Nonnull UUID visibilityAreaId) {
            VisibilityArea va = Proxies.withId(VisibilityArea.class,
                    visibilityAreaId, uuid -> repo.visibilityAreaRepo.get().getById(uuid));
            return new TestPlanImpl(testPlanId, name, va);
        }
    }
}

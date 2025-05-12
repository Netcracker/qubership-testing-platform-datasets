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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;

import org.qubership.atp.dataset.db.utils.Proxies;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.impl.VisibilityAreaImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.MappingProjection;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;

@Repository
public class VisibilityAreaRepository extends AbstractRepository {

    private final Provider<DataSetListRepository> dslRepo;
    private final VisibilityAreaProjection visibilityAreaProjection;
    private final SQLQueryFactory queryFactory;
    private final CacheRepository cacheRepo;
    private final Provider<FilterRepository> filtersRepo;
    private final Provider<TestPlanRepository> testPlanRepo;

    /**
     * visibility area repository.
     *
     * @param queryFactory is used for creating sql queries.
     * @param filtersRepo  repository for deleting filters;
     */
    @Autowired
    public VisibilityAreaRepository(@Nonnull SQLQueryFactory queryFactory,
                                    @Nonnull Provider<DataSetListRepository> dslRepo,
                                    @Nonnull CacheRepository cacheRepo,
                                    @Nonnull Provider<FilterRepository> filtersRepo,
                                    @Nonnull Provider<TestPlanRepository> testPlanRepo) {
        super();
        this.queryFactory = queryFactory;
        this.dslRepo = dslRepo;
        this.cacheRepo = cacheRepo;
        this.testPlanRepo = testPlanRepo;
        this.visibilityAreaProjection = new VisibilityAreaProjection(this);
        this.filtersRepo = filtersRepo;
    }

    /**
     * Creates visibility area with name.
     */
    @Nonnull
    public VisibilityArea create(@Nonnull String name) {
        UUID vaId = Preconditions.checkNotNull(
                queryFactory.insert(VA).set(VA.name, name).executeWithKey(VA.id), "nothing created");
        return visibilityAreaProjection.create(vaId, name);
    }

    /**
     * Creates visibility area with name and id.
     */
    @Nonnull
    public VisibilityArea create(@Nonnull UUID id, @Nonnull String name) {
        VisibilityArea visibilityArea = create(name);

        queryFactory.update(VA).where(VA.id.eq(visibilityArea.getId())).set(VA.id, id).execute();
        visibilityArea.setId(id);

        return visibilityArea;
    }

    @Nullable
    public VisibilityArea getById(@Nonnull UUID id) {
        return cacheRepo.tryComputeIfAbsent(VisibilityArea.class, id, uuid -> select(VA.id.eq(uuid)).fetchOne());
    }

    public boolean existsById(UUID id) {
        return cacheRepo.getIfPresent(VisibilityArea.class, id) != null
                || select(VA.id.eq(id)).fetchCount() > 0;
    }

    @Nonnull
    protected SQLQuery<VisibilityArea> select(@Nonnull Predicate predicate) {
        return queryFactory.select(visibilityAreaProjection).from(VA).where(predicate);
    }

    @Nonnull
    public List<VisibilityArea> getAll() {
        return queryFactory.select(visibilityAreaProjection).from(VA).fetch();
    }

    @Nonnull
    public List<VisibilityArea> getAllSorted() {
        return queryFactory.select(visibilityAreaProjection).from(VA).orderBy(VA.name.asc()).fetch();
    }

    public boolean rename(@Nonnull UUID id, @Nonnull String name) {
        return queryFactory.update(VA).where(VA.id.eq(id)).set(VA.name, name).execute() > 1;
    }

    /**
     * Delete Visibility area and all entities under it.
     *
     * @param id - target visibility area ID.
     * @return true if deleted successfully and false if nothing deleted.
     */

    public boolean delete(@Nonnull UUID id) {
        dslRepo.get().onVaDeleteCascade(id);
        testPlanRepo.get().onVaDeleteCascade(id);
        filtersRepo.get().onVaDeleteCascade(id);
        return delete(VA.id.eq(id)) > 0;
    }

    private long delete(@Nonnull Predicate predicate) {
        return queryFactory.delete(VA).where(predicate).execute();
    }

    private static class VisibilityAreaProjection extends MappingProjection<VisibilityArea> {

        private final VisibilityAreaRepository repo;

        VisibilityAreaProjection(VisibilityAreaRepository repo) {
            super(VisibilityArea.class, VA.all());
            this.repo = repo;
        }

        @Override
        protected VisibilityAreaImpl map(Tuple row) {
            UUID id = row.get(VA.id);
            assert id != null;
            String name = row.get(VA.name);
            assert name != null;
            return create(id, name);
        }

        private VisibilityAreaImpl create(@Nonnull UUID id, @Nonnull String name) {
            List<DataSetList> dataSetLists = Proxies.list(() -> repo.dslRepo.get().getAll(id));
            return new VisibilityAreaImpl(id, name, dataSetLists);
        }
    }
}

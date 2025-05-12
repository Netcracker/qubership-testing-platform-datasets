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

package org.qubership.atp.dataset.service.direct.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.dataset.db.DataSetListRepository;
import org.qubership.atp.dataset.db.TestPlanRepository;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.service.direct.TestPlanService;
import org.qubership.atp.dataset.versioning.service.DataSetListSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysema.commons.lang.Pair;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class TestPlanServiceImpl implements TestPlanService {

    private final TestPlanRepository repo;
    private final DataSetListRepository dataSetListRepo;
    private final Provider<UserInfo> userInfoProvider;
    private final DataSetListSnapshotService dataSetListSnapshotService;

    @Nonnull
    @Override
    public Pair<TestPlan, Boolean> create(@Nonnull UUID visibilityArea, @Nonnull String name) {
        return repo.create(name, visibilityArea);
    }

    @Nonnull
    @Override
    public List<TestPlan> getAll(@Nonnull UUID visibilityArea) {
        return repo.getAll(visibilityArea);
    }

    @Nonnull
    @Override
    public List<TestPlan> getAll() {
        return repo.getAll();
    }

    @Override
    public boolean rename(@Nonnull UUID id, @Nonnull String name) {
        TestPlan testPlan = get(id);
        if (testPlan == null) {
            return false;
        }
        return repo.rename(id, name);
    }

    @Override
    public TestPlan getByNameUnderVisibilityArea(UUID visibilityArea, String name) {
        return repo.getByNameUnderVisibilityArea(visibilityArea, name);
    }


    @Override
    public boolean delete(@Nonnull UUID testPlanId) {
        UUID modifiedBy = userInfoProvider.get().getId();
        Timestamp modifiedWhen = Timestamp.from(Instant.now());
        return repo.delete(testPlanId, modifiedBy, modifiedWhen);
    }

    @Transactional
    @Override
    public boolean delete(@Nonnull UUID vaId, @Nonnull String name) {
        UUID modifiedBy = userInfoProvider.get().getId();
        Timestamp modifiedWhen = Timestamp.from(Instant.now());
        TestPlan testPlan = getByNameUnderVisibilityArea(vaId, name);
        if (testPlan != null) {
            dataSetListRepo.getUnderTestPlan(testPlan.getId())
                    .forEach(dataSetList -> {
                        dataSetListSnapshotService.commitEntity(dataSetList.getId());
                    });
        }
        return repo.delete(vaId, name, modifiedBy, modifiedWhen);
    }

    @Override
    public List<DataSetList> getChildren(@Nonnull UUID testPlanId) {
        return dataSetListRepo.getUnderTestPlan(testPlanId);
    }

    @Nullable
    @Override
    public TestPlan get(@Nonnull UUID id) {
        return repo.getById(id);
    }

    @Override
    public boolean existsById(@NotNull UUID id) {
        return repo.existsById(id);
    }
}

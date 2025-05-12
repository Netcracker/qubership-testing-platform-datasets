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

package org.qubership.atp.dataset.service.direct;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.TestPlan;

import com.mysema.commons.lang.Pair;

public interface TestPlanService extends IdentifiedService<TestPlan> {

    @Nonnull
    Pair<TestPlan, Boolean> create(@Nonnull UUID visibilityArea, @Nonnull String name);

    @Nonnull
    List<TestPlan> getAll(@Nonnull UUID visibilityArea);

    boolean rename(@Nonnull UUID id, @Nonnull String name);

    TestPlan getByNameUnderVisibilityArea(UUID visibilityArea, String name);

    /**
     * Deletes Test Plan by id.
     */
    boolean delete(@Nonnull UUID testPlanId);

    /**
     * Deletes Test Plan by name under visibility area.
     */
    boolean delete(@Nonnull UUID vaId, @Nonnull String name);

    /**
     * Get dataset lists under test plan.
     */
    List<DataSetList> getChildren(@Nonnull UUID testPlanId);
}

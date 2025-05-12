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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.utils.TestData;
import org.qubership.atp.dataset.service.AbstractTest;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class TestPlanImplTest extends AbstractTest {

    private UUID visibilityAreaId = UUID.fromString("fcc17c65-6c8c-4ee4-878c-8b7d70664b06");

    @Test
    @Sql(scripts = "classpath:test_data/sql/test_plan_impl_test/TestPlanImplTest.sql")
    public void renameTestPlan_testPlanWasRenamed() {
        TestPlan testPlan = testPlanService.create(visibilityAreaId, "TP").getFirst();
        testPlanService.rename(testPlan.getId(), "renamedTP");
        TestPlan renamedTestPlan = testPlanService.get(testPlan.getId());
        assertEquals("renamedTP", renamedTestPlan.getName());
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/test_plan_impl_test/TestPlanImplTest.sql")
    public void createTestPlanWithDuplicatedName_testPlanWasNotCreated() {
        testPlanService.create(visibilityAreaId, "TP");
        assertFalse(testPlanService.create(visibilityAreaId, "TP").getSecond());
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/test_plan_impl_test/TestPlanImplTest.sql")
    public void deleteTestPlan_testPlanWasRemoved() {
        VisibilityArea visibilityArea = createTestData(TestData::addTestPlan);
        boolean deleted = testPlanService.delete(
                testPlanService.getByNameUnderVisibilityArea(visibilityArea.getId(), "TP").getId());
        assertTrue(deleted);
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/test_plan_impl_test/TestPlanImplTest.sql")
    public void deleteTestPlanByName_testPlanWasRemoved() {
        VisibilityArea visibilityArea = createTestData(TestData::addTestPlan);
        boolean deleted = testPlanService.delete(visibilityArea.getId(), "TP");
        assertTrue(deleted);
    }
}

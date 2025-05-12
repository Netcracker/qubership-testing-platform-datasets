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

package org.qubership.atp.dataset.ui.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.VisibilityArea;

@Isolated
public class CreateDslITCase extends AbstractApiIntegrationCase {

    private VisibilityArea va;
    private TestPlan testPlan;

    @BeforeEach
    public void init() {
        va = factory.va("DslVa");
        testPlan = factory.testPlan(va, "TP");
    }

    @Test
    public void createDsl_withDslNameAndVaId_DslIsCreated() {
        Response put = baseUrl.path("dsl/va")
                .path(va.getId().toString())
                .queryParam("name", "DSL1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(null);
        Assertions.assertSame(Response.Status.CREATED, put.getStatusInfo().toEnum());
        UUID id = put.readEntity(UUID.class);
        DataSetList dsl = dataSetListService.get(id);
        Assertions.assertNotNull(dsl);
        va = dsl.getVisibilityArea();
        assertThat(va.getDataSetLists(), hasSize(1));
    }

    @Test
    public void createDsl_withDslNameAndVaIdAndTestPlan_DslIsCreated() {
        Response put = baseUrl.path("dsl/va")
                .path(va.getId().toString())
                .queryParam("name", "DSL1")
                .queryParam("testPlan", testPlan.getId().toString())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(null);
        Assertions.assertSame(Response.Status.CREATED, put.getStatusInfo().toEnum());
        UUID id = put.readEntity(UUID.class);
        DataSetList dsl = dataSetListService.get(id);
        Assertions.assertNotNull(dsl);
        va = dsl.getVisibilityArea();
        assertThat(va.getDataSetLists(), hasSize(1));
        Assertions.assertNotNull(va.getDataSetLists().get(0).getTestPlan());
    }
}

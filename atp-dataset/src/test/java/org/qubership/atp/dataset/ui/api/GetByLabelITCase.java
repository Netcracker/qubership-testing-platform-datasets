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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.JsonMatcher;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;

@Isolated
public class GetByLabelITCase extends AbstractApiIntegrationCase {

    @Test
    public void getDataSetLists_UsingLabel_GotOne() throws Exception {
        TestData data = createTestDataInstance(TestData::new);
        Invocation.Builder request = baseUrl.path("dsl/va")
                .path(data.va.getId().toString())
                .queryParam("label", data.dslLabel.getName())
                .request(MediaType.APPLICATION_JSON_TYPE);
        String ar = request.get(String.class);
        List<String> actual = getElements(ar);
        assertThat(Response.Status.OK.getStatusCode(), equalTo(request.get().getStatus()));
        assertThat(actual, hasSize(1));
    }

    @Test
    public void getDataSetLists_UsingIllegalLabel_GotNothing() throws Exception {
        TestData data = createTestDataInstance(TestData::new);
        String er = "[]";
        String ar = baseUrl.path("dsl/va")
                .path(data.va.getId().toString())
                .queryParam("label", "Illegal")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
        assertThat(ar, JsonMatcher.isMinified(er));
    }

    @Test
    public void getDataSets_UsingLabel_GotOne() throws Exception {
        TestData data = createTestDataInstance(TestData::new);
        String er = "[{"
                + "  \"id\": \"" + data.dsWithLabel.getId() + "\","
                + "  \"name\": \"" + data.dsWithLabel.getName() + "\","
                + "  \"labels\": [{"
                + "                  \"id\": \"" + data.dsLabel.getId() + "\","
                + "                  \"name\": \"" + data.dsLabel.getName() + "\""
                + "              }]"
                + "}]";
        String ar = baseUrl.path("dsl")
                .path(data.dslNoLabel.getId().toString())
                .path("ds")
                .queryParam("label", data.dsLabel.getName())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
        assertThat(ar, JsonMatcher.isMinified(er));
    }

    @Test
    public void getDataSets_UsingIllegalLabel_GotNothing() throws Exception {
        TestData data = createTestDataInstance(TestData::new);
        String er = "[]";
        String ar = baseUrl.path("dsl")
                .path(data.dslNoLabel.getId().toString())
                .path("ds")
                .queryParam("label", "Illegal")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
        assertThat(ar, JsonMatcher.isMinified(er));
    }

    private static class TestData implements Supplier<VisibilityArea> {
        private final VisibilityArea va;
        private final DataSetList dslNoLabel;
        private final DataSetList dslWithLabel;
        private final Label dslLabel;
        private final DataSet dsNoLabel;
        private final DataSet dsWithLabel;
        private final Label dsLabel;

        public TestData(CreationFacade create) {
            va = create.va("ATPII-3262");
            dslNoLabel = create.dsl(va, "No label");
            dslWithLabel = create.dsl(va, "With label");
            dslLabel = create.label(dslWithLabel, "Some Label");
            dsNoLabel = create.ds(dslNoLabel, "No label");
            dsWithLabel = create.ds(dslNoLabel, "With label");
            dsLabel = create.label(dsWithLabel, "Some Label");
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }
}

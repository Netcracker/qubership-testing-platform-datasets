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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.utils.MultiplyTestData;

@Isolated
public class JsonViewITCase extends AbstractApiIntegrationCase {
    private MultiplyTestData data;

    @BeforeEach
    public void init() {
        data = createTestDataInstance(MultiplyTestData::new);
    }

    @Test
    public void getDataSetLists_UnderVa_OnlyIdNameLabelsAreProvided() throws JSONException {
        Invocation.Builder request =
                baseUrl.path("dsl/va").path(data.va.getId().toString()).request(MediaType.APPLICATION_JSON_TYPE);
        String ar = request.get(String.class);
        List<DataSetListImpl> actual = getResponse(ar);
        assertThat(Response.Status.OK.getStatusCode(), equalTo(request.get().getStatus()));
        assertThat(actual, hasSize(3));
        assertThat(actual, hasItem(hasProperty("id", equalTo(data.accounts.getId()))));
        assertThat(actual, hasItem(hasProperty("id", equalTo(data.subscriptions.getId()))));
        assertThat(actual, hasItem(hasProperty("id", equalTo(data.requested.getId()))));
    }

    @Test
    public void getDataSets_UnderDsl_OnlyIdNameLabelsAreProvided() throws JSONException {
        Invocation.Builder request = baseUrl.path("dsl").path(data.accounts.getId().toString()).path("ds")
                        .request(MediaType.APPLICATION_JSON_TYPE);
        String ar = request.get(String.class);
        List<DataSetListImpl> actual = getResponse(ar);
        assertThat(Response.Status.OK.getStatusCode(), equalTo(request.get().getStatus()));
        assertThat(actual, hasSize(2));
        assertThat(actual, hasItem(hasProperty("id", equalTo(data.resCA.getId()))));
        assertThat(actual, hasItem(hasProperty("id", equalTo(data.b2bCA.getId()))));
    }
}

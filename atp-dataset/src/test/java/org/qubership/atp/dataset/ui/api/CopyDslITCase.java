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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.model.utils.AtpDsTestData;

@Isolated
public class CopyDslITCase extends AbstractApiIntegrationCase {

    @Test
    public void copyDsl_WithData_ContainsCopiesOfDs() {
        AtpDsTestData.ShuffleGroups data = createTestDataInstance(AtpDsTestData.ShuffleGroups::new);
        Response put = baseUrl.path("dsl/va")
                .path(data.va.getId().toString())
                .path("dsl")
                .path(data.dsl.getId().toString())
                .path("copy")
                .queryParam("name", "Dsl Copy")
                .queryParam("type", "true")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(null);
        Assertions.assertSame(Response.Status.OK, put.getStatusInfo().toEnum());
        UUID copyId = put.readEntity(UUID.class);
        DataSetList copy = dataSetListService.get(copyId);
        Assertions.assertNotNull(copy);
        va = copy.getVisibilityArea();//new dsl was added, but old va instance outdated
        assertThat(copy.getDataSets(), hasSize(1));
        Parameter param2 = data.param2;
        FileData fileData = param2.getFileData();
        String url = fileData.getUrl();
        DataSet dataSet = copy.getDataSets().get(0);
        String urlOfCopiedFileVariable = dataSet.getParameters().get(2).getFileData().getUrl();
        Assertions.assertNotEquals(url, urlOfCopiedFileVariable);
        //WA FOR -7564
        copy.getDataSets().stream().flatMap(ds -> ds.getParameters().stream()).forEach(parameterService::delete);
    }

    @Test
    public void copyDsl_WithoutData_DoesntContainDses() {
        AtpDsTestData.ShuffleGroups data = createTestDataInstance(AtpDsTestData.ShuffleGroups::new);
        Response put = baseUrl.path("dsl/va")
                .path(data.va.getId().toString())
                .path("dsl")
                .path(data.dsl.getId().toString())
                .path("copy")
                .queryParam("name", "Dsl Copy")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(null);
        Assertions.assertSame(Response.Status.OK, put.getStatusInfo().toEnum());
        UUID copyId = put.readEntity(UUID.class);
        DataSetList copy = dataSetListService.get(copyId);
        Assertions.assertNotNull(copy);
        Assertions.assertTrue(copy.getDataSets().isEmpty());
    }
}

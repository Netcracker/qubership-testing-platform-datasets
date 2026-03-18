/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.dataset.controllers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.MixInIdImpl;
import org.qubership.atp.dataset.model.utils.DatasetResponse;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.impl.CompareDatasetServiceImpl;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListExportService;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListImportService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListCheckService;
import org.qubership.atp.dataset.service.rest.server.DataSetController;
import org.qubership.atp.dataset.service.rest.server.DataSetListController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import lombok.extern.slf4j.Slf4j;

@Isolated
@Provider("atp-datasets")
@PactUrl(urls = {"file:./src/test/resources/pacts/atp-ram-atp-datasets.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {DataSetController.class,
        DataSetListController.class
})
@SpringJUnitConfig(classes = {DatasetsAndRamContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        DataSetListController.class,
        DataSetController.class
})
@Slf4j
public class DatasetsAndRamContractTest {
    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private DataSetService dsService;
    @MockitoBean
    private ConcurrentModificationService concurrentModificationService;
    @MockitoBean
    private JpaDataSetService dataSetService;

    @MockitoBean
    private DataSetListService dslService;
    @MockitoBean
    private DataSetListCheckService dataSetListCheckService;
    @MockitoBean
    private JpaDataSetListService jpaDataSetListService;
    @MockitoBean
    private AttributeService attributeService;
    @MockitoBean
    private org.qubership.atp.auth.springbootstarter.ssl.Provider<UserInfo> userInfoProvider;
    @MockitoBean
    private DatasetListExportService datasetListExportService;
    @MockitoBean
    private DatasetListImportService importService;
    @MockitoBean
    CompareDatasetServiceImpl compareDs;
    public void beforeAll() {
        List<DatasetResponse> resultList = new ArrayList<>();
        DatasetResponse datasetResponse =
                new DatasetResponse(UUID.randomUUID(), "dsName", UUID.randomUUID(), "dslName");
        resultList.add(datasetResponse);

        DataSetImpl dataSet = new DataSetImpl();
        dataSet.setName("dataSetName");
        dataSet.setMixInId(new MixInIdImpl(UUID.randomUUID()));
        dataSet.setId(UUID.randomUUID());

        when(dsService.get(any(UUID.class)))
                .thenReturn(dataSet);
        when(dslService.getListOfDsIdsAndNameAndDslId(any()))
                .thenReturn(resultList);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        beforeAll();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("all ok")
    public void allPass() {
    }
}

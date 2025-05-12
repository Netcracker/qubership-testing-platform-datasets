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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
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
import lombok.extern.slf4j.Slf4j;

@Isolated
@Provider("atp-datasets")
@PactUrl(urls = {"src/test/resources/pacts/atp-ram-atp-datasets.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {DataSetController.class,
        DataSetListController.class
})
@ContextConfiguration(classes = {DatasetsAndRamContractTest.TestApp.class})
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
    @MockBean
    private DataSetService dsService;
    @MockBean
    private ConcurrentModificationService concurrentModificationService;
    @MockBean
    private JpaDataSetService dataSetService;

    @MockBean
    private DataSetListService dslService;
    @MockBean
    private DataSetListCheckService dataSetListCheckService;
    @MockBean
    private JpaDataSetListService jpaDataSetListService;
    @MockBean
    private AttributeService attributeService;
    @MockBean
    private org.qubership.atp.auth.springbootstarter.ssl.Provider<UserInfo> userInfoProvider;
    @MockBean
    private DatasetListExportService datasetListExportService;
    @MockBean
    private DatasetListImportService importService;
    @MockBean
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
    void before(PactVerificationContext context) throws Exception {
        beforeAll();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("all ok")
    public void allPass() {
    }
}

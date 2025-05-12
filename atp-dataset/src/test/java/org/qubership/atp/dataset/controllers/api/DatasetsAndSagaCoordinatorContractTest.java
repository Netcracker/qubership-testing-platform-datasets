package org.qubership.atp.dataset.controllers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.rest.server.CopyDataSetListsResponse;
import org.qubership.atp.dataset.service.rest.server.SagaController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Isolated
@Provider("atp-datasets")
@PactUrl(urls = {"src/test/resources/pacts/atp-saga-coordinator-atp-datasets.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {SagaController.class
})
@ContextConfiguration(classes = {DatasetsAndSagaCoordinatorContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        SagaController.class
})
public class DatasetsAndSagaCoordinatorContractTest {

    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JpaDataSetListService jpaDataSetListService;
    @MockBean
    private DataSetListService dslService;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        CopyDataSetListsResponse response = new CopyDataSetListsResponse();
        response.setCopyId(UUID.randomUUID());
        response.setOriginalId(UUID.randomUUID());
        Map<UUID, UUID> datasetMap = new HashMap<>();
        datasetMap.put(UUID.fromString("ef0acfd4-a83d-47e1-a4ce-0d36f7c69751"),
                UUID.fromString("e2490de5-5bd3-43d5-b7c4-526e33f71304"));
        response.setDatasets(datasetMap);
        when(jpaDataSetListService.copyDataSetLists(any(ArrayList.class), any(boolean.class), any(UUID.class),
                any(String.class), any(String.class), any(UUID.class))).thenReturn(Collections.singletonList(response));
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("all ok")
    public void allPass() {
    }
}

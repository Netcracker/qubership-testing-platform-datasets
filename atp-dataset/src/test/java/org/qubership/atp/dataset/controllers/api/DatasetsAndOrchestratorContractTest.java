package org.qubership.atp.dataset.controllers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.dataset.service.jpa.impl.MetricsService;
import org.qubership.atp.dataset.service.rest.facade.AttachmentControllerFacade;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.model.utils.DatasetResponse;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListExportService;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListImportService;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListCheckService;
import org.qubership.atp.dataset.service.rest.server.AttachmentController;
import org.qubership.atp.dataset.service.rest.server.DataSetListController;
import lombok.extern.slf4j.Slf4j;

@Isolated
@Provider("atp-datasets")
@PactUrl(urls = {"src/test/resources/pacts/atp-orchestrator-atp-datasets.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {
        DataSetListController.class,
        AttachmentController.class
})
@ContextConfiguration(classes = {DatasetsAndOrchestratorContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        DataSetListController.class,
        AttachmentController.class
})
@Slf4j
public class DatasetsAndOrchestratorContractTest {
    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentControllerFacade attachmentControllerFacade;
    @MockBean
    private MetricsService metricsService;

    @MockBean
    private ConcurrentModificationService concurrentModificationService;

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
    private ParameterService parameterService;
    @MockBean
    private JpaParameterService jpaParameterService;

    @MockBean
    private GridFsService gridFsService;
    @MockBean
    private DataSetService dataSetService;

    public void beforeAll() throws DataSetServiceException {
        List<DatasetResponse> resultList = new ArrayList<>();
        DatasetResponse datasetResponse =
                new DatasetResponse(UUID.randomUUID(), "dsName", UUID.randomUUID(), "dslName");
        resultList.add(datasetResponse);

        UUID id = UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01");
        InputStream inputStream = new ByteArrayInputStream( "blabla".getBytes() );
        Optional<InputStream> optional = Optional.ofNullable(inputStream);

        FileData fileData = new FileData();
        fileData.setContentType("multipart/form-data");
        fileData.setFileName("name");

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename("name", StandardCharsets.UTF_8)
                .build();

        InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

        ResponseEntity<InputStreamResource> re =
                ResponseEntity
                        .ok()
                        .contentType(MediaType.parseMediaType(fileData.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                        .body(inputStreamResource);


        when(dslService.getListOfDsIdsAndNameAndDslId(any()))
                .thenReturn(resultList);

        when(gridFsService.get(id)).thenReturn(optional);
        when(gridFsService.getFileInfo(id)).thenReturn(fileData);

        when(attachmentControllerFacade.getAttachmentByParameterId(any()))
                .thenReturn(re);
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

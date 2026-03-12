package org.qubership.atp.dataset.controllers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.rest.server.AttachmentController;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;

@Isolated
@Provider("atp-datasets")
@PactUrl(urls = {"file:./src/test/resources/pacts/atp-itf-stubs-atp-datasets.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {AttachmentController.class})
@SpringJUnitConfig(classes = {DatasetsAndItfStubsContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        AttachmentController.class})
public class DatasetsAndItfStubsContractTest {

    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AttachmentController attachmentController;

    private void beforeAll() {
        InputStreamResource responseBody = new InputStreamResource(new ByteArrayInputStream("test".getBytes()));

        FileData fileData = new FileData();
        fileData.setContentType("multipart/form-data");
        fileData.setFileName("name");

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename("name", StandardCharsets.UTF_8)
                .build();
        ResponseEntity<InputStreamResource> attachmentResponse =
                ResponseEntity
                        .ok()
                        .contentType(MediaType.parseMediaType(fileData.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                        .body(responseBody);

        when(attachmentController.getAttachmentByParameterId(any()))
                .thenReturn(attachmentResponse);
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

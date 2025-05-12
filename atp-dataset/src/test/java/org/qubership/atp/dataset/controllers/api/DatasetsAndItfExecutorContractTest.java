package org.qubership.atp.dataset.controllers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.VisibilityAreaEntity;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.TestPlan;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.impl.MixInIdImpl;
import org.qubership.atp.dataset.model.impl.TestPlanImpl;
import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.impl.DataSetParameterProvider;
import org.qubership.atp.dataset.service.jpa.impl.macro.MacroContext;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.VisibilityAreaFlatModel;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.DataSetListContext;
import org.qubership.atp.dataset.service.jpa.model.dscontext.ParameterContext;
import org.qubership.atp.dataset.service.jpa.model.tree.OverlapNode;
import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetTree;
import org.qubership.atp.dataset.service.rest.server.AttachmentController;
import org.qubership.atp.dataset.service.rest.server.AttributeController;
import org.qubership.atp.dataset.service.rest.server.DataSetController;
import org.qubership.atp.dataset.service.rest.server.DataSetListController;
import org.qubership.atp.dataset.service.rest.server.VisibilityAreaController;

@Isolated
@Provider("atp-datasets")
@PactUrl(urls = {"src/test/resources/pacts/atp-itf-executor-atp-datasets.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {AttachmentController.class, DataSetController.class,
        DataSetListController.class, AttributeController.class, VisibilityAreaController.class})
@ContextConfiguration(classes = {DatasetsAndItfExecutorContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
        AttachmentController.class, DataSetController.class, DataSetListController.class, AttributeController.class,
        VisibilityAreaController.class
})
public class DatasetsAndItfExecutorContractTest {

    @Configuration
    public static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AttachmentController attachmentController;
    @MockBean
    private DataSetController dataSetController;
    @MockBean
    private DataSetListController dataSetListController;
    @MockBean
    private AttributeController attributeController;
    @MockBean
    private VisibilityAreaController visibilityAreaController;

    public void beforeAll() throws DataSetServiceException, IOException {
        InputStreamResource responseBody1 = new InputStreamResource(new ByteArrayInputStream("test".getBytes()));
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Disposition", "attachment; filename=\"name\"");

        when(attachmentController.getAttachmentByParameterId(any()))
                .thenReturn(new ResponseEntity<>(responseBody1, headers, HttpStatus.OK));

        String responseBody2 = "{\"key\":\"context\"}";
        when(dataSetController.getItfContext(any())).thenReturn(responseBody2);

        when(dataSetController.getAtpContextFull(any(UUID.class), any(), any()))
                .thenReturn(getResponseBody3_4_5_6(ContextType.FULL));

        when(dataSetController.getAtpContextObject(any(UUID.class), any(), any()))
                .thenReturn(getResponseBody3_4_5_6(ContextType.OBJECT));

        when(dataSetController.getAtpContextObjectExtended(any(UUID.class), any(), any()))
                .thenReturn(getResponseBody3_4_5_6(ContextType.OBJECT_EXTENDED));

        when(dataSetController.getAtpContextOptimized(any(UUID.class), any(), any()))
                .thenReturn(getResponseBody3_4_5_6(ContextType.NO_NULL_VALUES));

        when(visibilityAreaController.getVisibilityAreas()).thenReturn(getResponseBody7());

        when(dataSetListController.getDataSetLists(any(UUID.class), any())).thenReturn(getResponseBody8());

        when(dataSetListController.getDataSets(any(UUID.class), any(), any())).thenReturn(getResponseBody9());

        when(attributeController.getAttributesInItfFormat(any(UUID.class))).thenReturn(getResponseBody10());
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


    private DataSetTree getResponseBody3_4_5_6(ContextType contextType) {
        DataSetContext dataSetContext = getDataSetContext();
        DataSetParameterProvider dataSetParameterProvider = new DataSetParameterProvider();
        MacroContext macroContext = new MacroContext();
        DataSetListContext dataSetListContext = new DataSetListContext(UUID.randomUUID());
        macroContext.setDataSetListContext(dataSetListContext);

        DataSetTree dataSetTree = new DataSetTree(dataSetContext, 0, true, macroContext,
                dataSetListContext, dataSetParameterProvider, contextType);

        return dataSetTree;
    }

    private DataSetContext getDataSetContext(){
        OverlapNode overlapNode = new OverlapNode(UUID.randomUUID(), UUID.randomUUID(), 0);
        DataSetContext dataSetContext = new DataSetContext(0, new ArrayList<>(), overlapNode,
                new ArrayList<>(), new ArrayList<>());

        AttributeEntity attributeEntity2 = new AttributeEntity();
        attributeEntity2.setAttributeTypeId(2l);
        attributeEntity2.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"));
        Attribute attribute2 = new Attribute(attributeEntity2);
        attribute2.setAttributeType(AttributeTypeName.TEXT);
        attribute2.setOrdering(1);
        attribute2.setName("1ATTRIBUTE");

        ParameterContext parameterContext2 = new ParameterContext(attribute2);
        dataSetContext.setParameters(Arrays.asList(parameterContext2));

        return dataSetContext;
    }

    private VisibilityAreaFlatModel getVisibilityAreaFlatModel() {
        VisibilityAreaEntity visibilityAreaEntity = new VisibilityAreaEntity();
        visibilityAreaEntity.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"));

        List<DataSetListEntity> dataSetListEntityList = new LinkedList<>();
        DataSetListEntity dataSetListEntity = new DataSetListEntity();
        dataSetListEntity.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa03"));
        dataSetListEntityList.add(dataSetListEntity);

        visibilityAreaEntity.setDataSetLists(dataSetListEntityList);

        VisibilityAreaFlatModel visibilityAreaFlatModel = new VisibilityAreaFlatModel();
        visibilityAreaFlatModel.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"));
        visibilityAreaFlatModel.setName("visibilityArea");

        List<UUID> uuids = new ArrayList<>();
        uuids.add(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa03"));
        visibilityAreaFlatModel.setDataSetLists(uuids);
        return visibilityAreaFlatModel;
    }

    private List<VisibilityAreaFlatModel> getResponseBody7() {
        return Arrays.asList(getVisibilityAreaFlatModel());
    }

    private DataSetList getDataSetList() {
        DataSetList dataSetList = new DataSetListImpl();
        dataSetList.setCreatedBy(UUID.fromString("f0c1d2ba-7c99-4e0b-a39d-0566f2ae9f25"));
        dataSetList.setModifiedBy(UUID.fromString("f0c1d2ba-7c99-4e0b-a39d-0566f2ae9f26"));

        TestPlan testPlanCreatedModifiedViewDto = new TestPlanImpl(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"), null, null);
        dataSetList.setTestPlan(testPlanCreatedModifiedViewDto);

        return dataSetList;
    }

    private List<DataSetList> getResponseBody8() {
        return Arrays.asList(getDataSetList());
    }

    private DataSet getDataSet() {
        DataSet dataSet = new DataSetImpl();
        dataSet.setName("dataSetName");
        dataSet.setMixInId(new MixInIdImpl(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01")));
        dataSet.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01"));
        return dataSet;
    }

    private List<DataSet> getResponseBody9() {
        return Arrays.asList(getDataSet());
    }

    private List<String> getResponseBody10() {
        List<String> list = new ArrayList<>();
        list.add("1ATTRIBUTE");
        list.add("2ATTRIBUTE");
        return list;
    }
}

package org.qubership.atp.dataset.controllers.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONObject;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Label;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsResponse;
import org.qubership.atp.dataset.model.enums.DetailedComparisonStatus;
import org.qubership.atp.dataset.model.impl.ComparedAttribute;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.impl.LabelImpl;
import org.qubership.atp.dataset.model.impl.MixInIdImpl;
import org.qubership.atp.dataset.model.impl.TestPlanImpl;
import org.qubership.atp.dataset.model.impl.VisibilityAreaImpl;
import org.qubership.atp.dataset.model.utils.DatasetResponse;
import org.qubership.atp.dataset.service.direct.AttributeService;
import org.qubership.atp.dataset.service.direct.ConcurrentModificationService;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.DataSetService;
import org.qubership.atp.dataset.service.direct.ParameterService;
import org.qubership.atp.dataset.service.direct.impl.CompareDatasetServiceImpl;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListExportService;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListImportService;
import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.JpaDataSetService;
import org.qubership.atp.dataset.service.jpa.JpaParameterService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListCheckService;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.DataSetListDependencyNode;
import org.qubership.atp.dataset.service.rest.server.CopyDataSetListsResponse;
import org.qubership.atp.dataset.service.rest.server.DataSetController;
import org.qubership.atp.dataset.service.rest.server.DataSetListController;
import org.qubership.atp.dataset.service.rest.server.ParameterController;
import lombok.extern.slf4j.Slf4j;

@Isolated
@Provider("atp-datasets")
@PactUrl(urls = {"src/test/resources/pacts/atp-catalogue-atp-datasets.json"})
@AutoConfigureMockMvc(addFilters = false, webDriverEnabled = false)
@WebMvcTest(controllers = {
        DataSetController.class,
        ParameterController.class,
        DataSetListController.class
})
@ContextConfiguration(classes = {DatasetsAndCatalogContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        DataSetListController.class,
        ParameterController.class,
        DataSetController.class
})
@Slf4j
public class DatasetsAndCatalogContractTest {

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
    private ParameterService parameterService;
    @MockBean
    private JpaParameterService jpaParameterService;
    @MockBean
    CompareDatasetServiceImpl compareDs;

    public void beforeAll() throws DataSetServiceException {
        JSONObject jsonObject = new JSONObject();
        List<JSONObject> resultListJson = new ArrayList<>();
        resultListJson.add(jsonObject);
        List<DatasetResponse> resultList = new ArrayList<>();
        DatasetResponse datasetResponse =
                new DatasetResponse(UUID.randomUUID(), "dsName", UUID.randomUUID(), "dslName");
        resultList.add(datasetResponse);
        DataSetImpl dataSet = new DataSetImpl();
        dataSet.setName("dataSetName");
        dataSet.setMixInId(new MixInIdImpl(UUID.randomUUID()));
        dataSet.setId(UUID.randomUUID());
        DataSetList dataSetList = new DataSetListImpl();
        VisibilityArea visibilityArea = new VisibilityAreaImpl();
        visibilityArea.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"));
        dataSetList.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01"));
        dataSetList.setName("name");
        dataSetList.setVisibilityArea(visibilityArea);
        dataSetList.setCreatedBy(UUID.fromString("f0c1d2ba-7c99-4e0b-a39d-0566f2ae9f25"));
        dataSetList.setModifiedBy(UUID.randomUUID());
        List<Label> labelList = new ArrayList<>();
        labelList.add(new LabelImpl(UUID.randomUUID(), "name"));
        dataSetList.setLabels(labelList);
        dataSetList.setModifiedWhen(new Timestamp(0));
        dataSetList.setTestPlan(new TestPlanImpl(UUID.randomUUID(), "name", visibilityArea));
        List<DataSetList> dataSetListLinkedList = new LinkedList<>();
        dataSetListLinkedList.add(dataSetList);
        DataSet ds = new DataSetImpl();
        ds.setName("dataSetName");
        ds.setMixInId(new MixInIdImpl(UUID.randomUUID()));
        ds.setId(UUID.randomUUID());
        List<DataSet> list = new LinkedList<>();
        list.add(ds);
        when(concurrentModificationService.getHttpStatus(any(), any()))
                .thenReturn(HttpStatus.OK);
        when(dsService.get(any(UUID.class)))
                .thenReturn(dataSet);
        doNothing().when(dsService).delete(any());
        when(dataSetService.getDataSetTreeInAtpFormat(any(UUID.class), anyBoolean(),
                anyString(), any(ContextType.class), anyInt()))
                .thenReturn(resultListJson);
        when(dslService.get(any())).thenReturn(getDataSetList());
        doNothing().when(dslService).delete(any());
        when(userInfoProvider.get()).thenReturn(new UserInfo());
        when(dslService.getListOfDsIdsAndNameAndDslId(any()))
                .thenReturn(resultList);
        when(dslService.getAll(any(), any()))
                .thenReturn(dataSetListLinkedList);
        when(dslService.getChildren(any(UUID.class), anyBoolean(), any()))
                .thenReturn(list);
        when(jpaDataSetListService.copyDataSetLists(any(), anyBoolean(), anyString(), any()))
                .thenReturn(getCopyDataSetListsResponseDto());
        when(jpaDataSetListService.getDependenciesRecursive(any()))
                .thenReturn(getListDataSetListDependencyNode());
        when(jpaParameterService.bulkUpdateValues(any(), any(), any(),
                any(), any(), any())).thenReturn(true);
        when(compareDs.detailedComparison(any())).thenReturn(getDetailedComparisonDsResponse());
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

    private DataSetList getDataSetList() {
        DataSetList dataSetList = new DataSetListImpl();
        VisibilityArea visibilityArea = new VisibilityAreaImpl();
        visibilityArea.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa02"));
        dataSetList.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01"));
        dataSetList.setName("name");
        dataSetList.setVisibilityArea(visibilityArea);
        dataSetList.setCreatedBy(UUID.fromString("f0c1d2ba-7c99-4e0b-a39d-0566f2ae9f25"));
        dataSetList.setCreatedWhen(new Timestamp(1));
        dataSetList.setModifiedBy(UUID.randomUUID());
        List<Label> labelList = new ArrayList<>();
        labelList.add(new LabelImpl(UUID.randomUUID(), "name"));
        dataSetList.setLabels(labelList);
        dataSetList.setTestPlan(new TestPlanImpl(UUID.randomUUID(), "name", visibilityArea));
        return dataSetList;
    }

    public List<DataSet> getListDataSet() {
        List<DataSet> list = new LinkedList<>();
        list.add(getDataSet());
        return list;
    }

    public DataSet getDataSet() {
        DataSetImpl dataSet = new DataSetImpl();
        dataSet.setName("dataSetName");
        dataSet.setMixInId(new MixInIdImpl(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01")));
        dataSet.setId(UUID.fromString("c2737427-05e4-4c17-8032-455539deaa01"));
        return dataSet;
    }

    public List<CopyDataSetListsResponse> getCopyDataSetListsResponseDto() {
        CopyDataSetListsResponse copy = new CopyDataSetListsResponse();
        copy.setCopyId(UUID.fromString("fb551031-4bd0-4a10-aa0a-98f43e208fcf"));
        copy.setOriginalId(UUID.fromString("30c0ba97-7576-40fe-b62a-c85ddce50baf"));
        List<CopyDataSetListsResponse> list = new ArrayList<>();
        list.add(copy);
        return list;
    }

    public List<DataSetListDependencyNode> getListDataSetListDependencyNode() {
        List<DataSetListDependencyNode> list = new ArrayList<>();
        DataSetListDependencyNode dataSetListDependencyNode = new DataSetListDependencyNode();
        dataSetListDependencyNode.setName("asd");
        dataSetListDependencyNode.setId(UUID.randomUUID());
        dataSetListDependencyNode.setDependencies(new LinkedList<>());
        list.add(dataSetListDependencyNode);
        return list;
    }

    public DetailedComparisonDsResponse getDetailedComparisonDsResponse() {
        ComparedAttribute comparedAttribute1 = generateComparedAttribute();
        return new DetailedComparisonDsResponse(
                UUID.randomUUID(),
                "leftDatasetName",
                "leftDslName",
                UUID.randomUUID(),
                "rightDatasetName",
                "rightDslName",
                1,
                Collections.singletonList(comparedAttribute1)
        );
    }

    public ComparedAttribute generateComparedAttribute() {
        ComparedAttribute comparedAttr = new ComparedAttribute();
        comparedAttr.setRightAttributeType(AttributeTypeName.FILE);
        comparedAttr.setStatus(DetailedComparisonStatus.NOT_EQUAL);
        comparedAttr.setAttributeName("string");
        return comparedAttr;
    }
}

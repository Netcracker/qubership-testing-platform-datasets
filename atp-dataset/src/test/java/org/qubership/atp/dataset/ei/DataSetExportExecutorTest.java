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

package org.qubership.atp.dataset.ei;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.qubership.atp.dataset.db.GridFsRepository;
import org.qubership.atp.dataset.db.jpa.entities.AttributeEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetEntity;
import org.qubership.atp.dataset.db.jpa.entities.DataSetListEntity;
import org.qubership.atp.dataset.db.jpa.entities.ParameterEntity;
import org.qubership.atp.dataset.db.jpa.entities.VisibilityAreaEntity;
import org.qubership.atp.dataset.db.jpa.repositories.JpaDataSetListRepository;
import org.qubership.atp.dataset.db.jpa.repositories.JpaDataSetRepository;
import org.qubership.atp.dataset.ei.model.DataSet;
import org.qubership.atp.dataset.model.impl.file.FileData;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.JpaDataSetListService;
import org.qubership.atp.dataset.service.jpa.impl.DataSetListContextService;
import org.qubership.atp.dataset.service.jpa.impl.JpaDataSetServiceImpl;
import org.qubership.atp.dataset.service.jpa.model.MacroContextService;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;

@Isolated
@ExtendWith(SpringExtension.class)
public class DataSetExportExecutorTest {

    @Spy
    private ObjectSaverToDiskService objectSaverToDiskService =
            new ObjectSaverToDiskService(new FileService(), true);
    @Mock
    private JpaDataSetListRepository jpaDataSetListRepository;
    @Mock
    private JpaDataSetRepository jpaDataSetRepository;
    @Mock
    private GridFsRepository gridFsRepository;
    @Mock
    private MacroContextService macroContextService;
    @Mock
    private DataSetListContextService dataSetListContextService;
    @Spy
    private ObjectMapper objectMapper;
    @InjectMocks
    DataSetExportExecutor dataSetExportExecutor;
    @Mock
    private JpaDataSetListService dslService;
    @Mock
    private JpaDataSetServiceImpl dsService;
    @TempDir
    private Path tempDir;

    private String erDatasetFile =
                    "{\n" +
                    "  \"id\" : \"8147cd6a-b742-4f63-be42-59c7c1ab5f2f\",\n" +
                    "  \"name\" : \"ds1\",\n" +
                    "  \"ordering\" : null,\n" +
                    "  \"dataSetList\" : \"70030fdd-816a-4455-b117-b2e718f0396c\",\n" +
                    "  \"sourceId\" : null,\n" +
                    "  \"isLocked\" : false\n" +
                    "}\n";

    private String serviceName = "dataset-export";
    private String dataSetListName = "dsl";
    private static final String PROJECT_BODY_DATA_SET_IN_NTT_EXPORT =
            "dsName=ds1\n" +
                    "\n" +
                    "name=attribute 2; description=; value=./files/dsl/ds1/filename.txt\n"
                    +
                    "name=attribute 1; description=; value=123\n";
    private String dsName;
    private UUID dsId = UUID.fromString("8147cd6a-b742-4f63-be42-59c7c1ab5f2f");
    private UUID dslId = UUID.fromString("70030fdd-816a-4455-b117-b2e718f0396c");
    private UUID paramId;
    private ExportScope atpExportScope;
    private DataSetEntity dataSet = new DataSetEntity();
    private DataSetListEntity dataSetList = new DataSetListEntity();

    @BeforeEach
    public void setUp() {
        VisibilityAreaEntity visibilityArea = new VisibilityAreaEntity();
        visibilityArea.setId(UUID.fromString("0cce99d6-ef89-4163-8518-9e1dbf173a6d"));
        visibilityArea.setName("va");

        dataSetList.setName(dataSetListName);
        dataSetList.setId(dslId);
        dataSetList.setVisibilityArea(visibilityArea);
        dataSetList.setAttributes(new ArrayList<>());

        AttributeEntity attr1 = new AttributeEntity();
        attr1.setId(UUID.fromString("226c77ce-4d9d-44d5-a9c3-4063e440f0fc"));
        attr1.setName("attribute 1");
        attr1.setAttributeTypeId(Long.valueOf("1"));
        attr1.setDataSetList(dataSetList);

        AttributeEntity attr2 = new AttributeEntity();
        attr2.setId(UUID.fromString("d194b108-e208-4f94-b3ef-5cae740dbccf"));
        attr2.setName("attribute 2");
        attr2.setAttributeTypeId(Long.valueOf("2"));
        attr2.setDataSetList(dataSetList);

        dataSetList.getAttributes().add(attr1);
        dataSetList.getAttributes().add(attr2);

        dataSet.setId(dsId);
        dsName = "ds1";
        dataSet.setName(dsName);
        dataSet.setDataSetList(dataSetList);
        dataSet.setParameters(new ArrayList<>());

        ParameterEntity textParam = new ParameterEntity();
        textParam.setId(UUID.fromString("8281ec64-8f3b-4eae-bec7-1b41671464a1"));
        textParam.setAttribute(attr1);
        textParam.setDataSet(dataSet);
        textParam.setStringValue("123");

        ParameterEntity fileParam = new ParameterEntity();
        paramId = UUID.fromString("5dd548e9-e63b-47c5-834e-ae754730e62d");
        fileParam.setId(paramId);
        fileParam.setDataSet(dataSet);
        fileParam.setAttribute(attr2);

        FileData fileData = new FileData();
        fileData.setParameterUuid(paramId);
        fileData.setFileName(paramId.toString());
        fileData.setFileType("txt");

        dataSet.getParameters().add(textParam);
        dataSet.getParameters().add(fileParam);

        InputStream stream = IOUtils.toInputStream("string1\nstring2\nstring3", StandardCharsets.UTF_8);
        Optional<InputStream> testFile = Optional.of(stream);
        Map<UUID, Optional<InputStream>> files = new HashMap<>();
        files.put(fileParam.getId(), testFile);

        atpExportScope = new ExportScope();
        atpExportScope.getEntities().put(Constants.ENTITY_DATASET_STORAGE,
                Sets.newHashSet(dataSetList.getId().toString()));
        atpExportScope.getEntities().put(Constants.ENTITY_DATASETS, Sets.newHashSet(dataSet.getId().toString()));
        Optional<DataSetListEntity> dataSetListOptional = Optional.of(dataSetList);
        Mockito.when(jpaDataSetListRepository.findById(any(UUID.class))).thenReturn(dataSetListOptional);
        Mockito.when(jpaDataSetRepository.findById(any())).thenReturn(Optional.of(dataSet));
        Mockito.when(gridFsRepository.getAll(any())).thenReturn(files);
        Mockito.when(gridFsRepository.getFileInfo(any())).thenReturn(Optional.of(fileData));
        Mockito.when(dsService.getDataSetsListIdByDataSetId(any(UUID.class))).thenReturn(dslId);
        ReflectionTestUtils.setField(dataSetExportExecutor, "implementationName", serviceName);
        ReflectionTestUtils.setField(dataSetExportExecutor, "fileService", new FileService());
    }

    @Test
    public void exportToFolder_GetNeededDatasetAndPlacedItToFolderAsFile_NeededDataSetPlacedToFolderAndCorrectlyOpened()
            throws Exception {
        Set<UUID> datasetIds = new HashSet<>();
        datasetIds.add(dsId);
        Path path = Files.createDirectories(tempDir.resolve("dsExport"));
//        Path path = folder.newFolder("dsExport").toPath();
        UUID visibilityAreaId = UUID.fromString("0cce99d6-ef89-4163-8518-9e1dbf173a6d");
        ExportImportData exportData = new ExportImportData(visibilityAreaId, atpExportScope, ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASETS,
                datasetIds.stream().map(UUID::toString).collect(Collectors.toSet()));
        dataSetExportExecutor.exportToFolder(exportData, path);
        Path arDatasetFilePath =
                path.resolve(DataSet.class.getSimpleName()).resolve(dslId.toString()).resolve(dsId + ".json");
        StringBuilder dataFromFile = new StringBuilder();
        try (Stream<String> lines = Files.lines(arDatasetFilePath, Charset.defaultCharset())) {
            lines.forEach(str -> dataFromFile.append(str).append("\n"));
        }

        assertEquals(erDatasetFile, dataFromFile.toString());
    }

    @Test
    public void getExportImplementationName() {
        assertEquals(serviceName, dataSetExportExecutor.getExportImplementationName());
    }

    @Test
    public void exportToFolder_convertToNttFormat() throws Exception {

        Set<UUID> datasetIds = new HashSet<>();
        UUID dataSetId = UUID.fromString("8147cd6a-b742-4f63-be42-59c7c1ab5f2f");
        datasetIds.add(dataSetId);
        Path path = Files.createDirectories(tempDir.resolve("dsExport"));
//        Path path = folder.newFolder("dsExport").toPath();
        UUID visibilityAreaId = UUID.fromString("0cce99d6-ef89-4163-8518-9e1dbf173a6d");
        ExportImportData exportData = new ExportImportData(visibilityAreaId, atpExportScope, ExportFormat.NTT);
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASETS,
                datasetIds.stream().map(UUID::toString).collect(Collectors.toSet()));
        dataSetExportExecutor.exportToFolder(exportData, path);

        Path projectFile = path.resolve("AT_configuration").resolve(dataSetListName + ".cds");
        assertTrue(Files.exists(projectFile));

        List<String> projectBodyLines = Files.readAllLines(projectFile);
        projectBodyLines.forEach(s -> {
            if (s.startsWith("dsName=")) {
                String _dsName = s.replace("dsName=", "");
                assertEquals(dsName, _dsName);
            } else if (s.startsWith("name=attribute 2;")) {
                String filePath = s.replace("name=attribute 2; description=; value=", "");
                String expectedFilePath = "./files/" + dslId.toString() + "/" + dsId.toString() + "/" + paramId.toString();
                assertEquals(Paths.get(filePath).toString(), Paths.get(expectedFilePath).toString());
            } else if (s.startsWith("name=attribute 1;")) {
                String value = s.replace("name=attribute 1; description=; value=", "");
                String expectedValue = "123";
                assertEquals(value, expectedValue);
            }
        });

        Path defaultProjectFile = path.resolve("AT_configuration").resolve("Default Project.cds");
        assertTrue(Files.exists(defaultProjectFile));

        String defaultProjectBody = StringUtils.join(Files.readAllLines(defaultProjectFile), "\n");
        assertTrue(StringUtils.isEmpty(defaultProjectBody));
    }

    @Test
    public void expandExportScope_exportSpecificDsls_DslForDsIsCollected() throws DataSetServiceException {
        ExportImportData exportData = new ExportImportData(UUID.randomUUID(), atpExportScope, ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASET_STORAGE, new HashSet<>());
        Set<UUID> idsFromDb = new HashSet<>();
        idsFromDb.add(dslId);
        Mockito.when(jpaDataSetListRepository.getDslIdsByVa(exportData.getProjectId())).thenReturn(idsFromDb);
        dataSetExportExecutor.expandExportScope(exportData);
        assertTrue(exportData.getExportScope().getEntities()
                .getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>()).contains(dslId.toString()));
    }

    @Test
    public void expandExportScope_DsReferencedFromParamIsAndCorrespondingDslIsCollected() throws DataSetServiceException {
        ExportImportData exportData = new ExportImportData(null, atpExportScope, ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASET_STORAGE, new HashSet<>());

        UUID referencedDataSetListId = UUID.randomUUID();
        DataSetListEntity referencedDataSetList = new DataSetListEntity();
        referencedDataSetList.setName("dsl2");
        referencedDataSetList.setId(referencedDataSetListId);
        referencedDataSetList.setAttributes(new ArrayList<>());

        UUID referencedDataSetId = UUID.randomUUID();
        DataSetEntity referencedDataSet = new DataSetEntity();
        referencedDataSet.setId(referencedDataSetId);
        referencedDataSet.setName("ds2");
        referencedDataSet.setDataSetList(referencedDataSetList);
        referencedDataSet.setParameters(new ArrayList<>());

        UUID referencedDataSetId2 = UUID.randomUUID();
        DataSetEntity referencedDataSet2 = new DataSetEntity();
        referencedDataSet2.setId(referencedDataSetId2);
        referencedDataSet2.setName("ds3");
        referencedDataSet2.setDataSetList(referencedDataSetList);
        referencedDataSet2.setParameters(new ArrayList<>());

        Set<UUID> idsFromDb = new HashSet<>();
        idsFromDb.add(dslId);
        Mockito.when(jpaDataSetListRepository.getDslIdsByVa(exportData.getProjectId())).thenReturn(idsFromDb);
        Mockito.when(jpaDataSetRepository.findById(referencedDataSetId)).thenReturn(Optional.of(referencedDataSet));
        Mockito.when(jpaDataSetRepository.findById(referencedDataSetId2)).thenReturn(Optional.of(referencedDataSet2));

        ParameterEntity parameterEntity = dataSet.getParameters().get(0);
        parameterEntity.setDataSetReferenceId(referencedDataSetId);

        ParameterEntity ds2Param = new ParameterEntity();
        ds2Param.setDataSet(referencedDataSet);
        ds2Param.setDataSetReferenceId(referencedDataSetId2);
        ds2Param.setAttribute(new AttributeEntity());
        ds2Param.getAttribute().setId(UUID.randomUUID());
        referencedDataSet.setParameters(asList(ds2Param));

        dataSetExportExecutor.expandExportScope(exportData);

        assertTrue(exportData.getExportScope().getEntities().getOrDefault(Constants.ENTITY_DATASETS, new HashSet<>())
                        .contains(referencedDataSetId.toString()),
                "Referenced from parameter DS should be collected");
        assertTrue(exportData.getExportScope().getEntities().getOrDefault(Constants.ENTITY_DATASETS, new HashSet<>())
                        .contains(referencedDataSetId2.toString()),
                "Referenced from parameter DS should be collected");
        assertTrue(exportData.getExportScope().getEntities()
                        .getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>())
                        .contains(referencedDataSetListId.toString()),
                "Referenced from parameter DS and corresponding DLS should be collected");
    }

    @Test
    public void expandExportScope_specificDsls_circlesShouldBeHandledProperly() throws DataSetServiceException {
        ExportImportData exportData = new ExportImportData(null, atpExportScope, ExportFormat.ATP);
        exportData.getExportScope().getEntities().put(Constants.ENTITY_DATASET_STORAGE, new HashSet<>());

        UUID referencedDataSetListId = UUID.randomUUID();
        DataSetListEntity referencedDataSetList = new DataSetListEntity();
        referencedDataSetList.setName("dsl2");
        referencedDataSetList.setId(referencedDataSetListId);
        referencedDataSetList.setAttributes(new ArrayList<>());

        UUID referencedDataSetId = UUID.randomUUID();
        DataSetEntity referencedDataSet = new DataSetEntity();
        referencedDataSet.setId(referencedDataSetId);
        referencedDataSet.setDataSetList(referencedDataSetList);
        referencedDataSet.setName("ds2");
        referencedDataSet.setParameters(new ArrayList<>());

        ParameterEntity parameterEntity = dataSet.getParameters().get(0);
        parameterEntity.setDataSetReferenceId(referencedDataSetId);

        ParameterEntity ds2Param = new ParameterEntity();
        ds2Param.setDataSet(referencedDataSet);
        ds2Param.setDataSetReferenceId(dataSet.getId());
        ds2Param.setAttribute(new AttributeEntity());
        ds2Param.getAttribute().setId(UUID.randomUUID());
        referencedDataSet.setParameters(asList(ds2Param));

        Set<UUID> idsFromDb = new HashSet<>();
        idsFromDb.add(dslId);
        Mockito.when(jpaDataSetListRepository.getDslIdsByVa(exportData.getProjectId())).thenReturn(idsFromDb);
        Mockito.when(jpaDataSetRepository.findById(referencedDataSetId)).thenReturn(Optional.of(referencedDataSet));

        dataSetExportExecutor.expandExportScope(exportData);

        assertTrue(exportData.getExportScope().getEntities().getOrDefault(Constants.ENTITY_DATASETS, new HashSet<>())
                .contains(dsId.toString()));
        assertTrue(exportData.getExportScope().getEntities().getOrDefault(Constants.ENTITY_DATASETS, new HashSet<>())
                .contains(referencedDataSetId.toString()));
    }

    @Test
    public void expandExportScope_exportAllDsls_noDslExpansion() throws DataSetServiceException {
        ExportImportData exportData = new ExportImportData(UUID.randomUUID(), atpExportScope, ExportFormat.ATP);
        Set<UUID> dslIds = new HashSet<>();
        dslIds.add(dslId);
        Mockito.when(jpaDataSetListRepository.getDslIdsByVa(exportData.getProjectId())).thenReturn(dslIds);

        dataSetExportExecutor.expandExportScope(exportData);

        Mockito.verify(jpaDataSetListRepository, Mockito.times(1))
                .getDslIdsByVa(Mockito.eq(exportData.getProjectId()));
        Mockito.verifyNoInteractions(jpaDataSetRepository);
        Mockito.verifyNoInteractions(dataSetListContextService);

    }

    @Test
    public void expandExportScope_DslReferencedFromAttributesAreCollected() throws DataSetServiceException {
        ExportImportData exportData = new ExportImportData(null, atpExportScope, ExportFormat.ATP);

        UUID referencedDataSetListId = UUID.randomUUID();
        DataSetListEntity referencedDataSetList = new DataSetListEntity();
        referencedDataSetList.setName("dsl2");
        referencedDataSetList.setId(referencedDataSetListId);
        referencedDataSetList.setAttributes(new ArrayList<>());

        AttributeEntity attr = new AttributeEntity();
        UUID attrId =  UUID.randomUUID();
        attr.setId(attrId);
        attr.setName("attribute 4");
        attr.setAttributeTypeId(Long.valueOf("4"));
        attr.setDataSetList(dataSetList);
        attr.setTypeDataSetListId(referencedDataSetListId);

        dataSetList.getAttributes().add(attr);

        dataSetExportExecutor.expandExportScope(exportData);

        assertTrue(exportData.getExportScope().getEntities()
                        .getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>())
                        .contains(referencedDataSetListId.toString()),
                "Referenced from attribute DSL should be collected");
    }

    @Test
    public void expandExportScope_circlesAreHandledProperlyForAttributesDslReference() throws DataSetServiceException {
        ExportImportData exportData = new ExportImportData(null, atpExportScope, ExportFormat.ATP);

        UUID referencedDataSetListId = UUID.randomUUID();
        DataSetListEntity referencedDataSetList = new DataSetListEntity();
        referencedDataSetList.setName("dsl2");
        referencedDataSetList.setId(referencedDataSetListId);
        referencedDataSetList.setAttributes(new ArrayList<>());

        AttributeEntity attr = new AttributeEntity();
        UUID attrId =  UUID.randomUUID();
        attr.setId(attrId);
        attr.setName("attribute 4");
        attr.setAttributeTypeId(Long.valueOf("4"));
        attr.setDataSetList(dataSetList);
        attr.setTypeDataSetListId(referencedDataSetListId);

        dataSetList.getAttributes().add(attr);

        AttributeEntity attr2 = new AttributeEntity();
        UUID attrId2 =  UUID.randomUUID();
        attr2.setId(attrId2);
        attr2.setName("attribute 5");
        attr2.setAttributeTypeId(Long.valueOf("4"));
        attr2.setDataSetList(referencedDataSetList);
        attr2.setTypeDataSetListId(dataSetList.getId());

        referencedDataSetList.getAttributes().add(attr);

        dataSetExportExecutor.expandExportScope(exportData);

        assertTrue(exportData.getExportScope().getEntities()
                        .getOrDefault(Constants.ENTITY_DATASET_STORAGE, new HashSet<>())
                        .contains(referencedDataSetListId.toString()),
                "Referenced from attribute DSL should be collected");
    }
}

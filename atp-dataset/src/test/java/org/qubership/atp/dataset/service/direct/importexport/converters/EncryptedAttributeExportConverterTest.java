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

package org.qubership.atp.dataset.service.direct.importexport.converters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.MixInId;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.impl.AttributeImpl;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;
import org.qubership.atp.dataset.model.impl.MixInIdImpl;
import org.qubership.atp.dataset.model.impl.ParameterImpl;
import org.qubership.atp.dataset.model.utils.ExcelRowsReader;
import org.qubership.atp.dataset.service.direct.DataSetListService;
import org.qubership.atp.dataset.service.direct.importexport.service.DataSetListImportExportFactory;
import org.qubership.atp.dataset.service.direct.importexport.service.DatasetListExportService;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSet;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManParameter;

@Isolated
@ExtendWith(SpringExtension.class)
public class EncryptedAttributeExportConverterTest {

    private static final String RESOURCES_PATH = "src/test/resources/exportToFile/";
    private static final String EXCEL_FILE = "DSL_ENCRYPTED_ATTR.xlsx";
    private static final AttributeType attrType = AttributeType.ENCRYPTED;

    private UUID datasetListId;

    private static DatasetListExportService datasetListExportService;

    @BeforeEach
    public void setUp() {
        DataSetListService dslService = mock(DataSetListService.class);
        DataSetListImportExportFactory factory = mock(DataSetListImportExportFactory.class);
        datasetListExportService = new DatasetListExportService(dslService, factory);

        DataSetList dataSetList = new DataSetListImpl();
        datasetListId = UUID.randomUUID();
        dataSetList.setId(datasetListId);
        dataSetList.setName("DSL_ENCRYPTED_ATTR");
        UiManDataSetList uiManDataSetList = new UiManDataSetList();
        uiManDataSetList.setSource(dataSetList);

        List<DataSet> dataSets = new ArrayList<>();
        List<UiManDataSet> uiManDataSets = new ArrayList<>();
        DataSet ds1 = new DataSetImpl();
        UUID ds1Id = UUID.randomUUID();
        MixInId mId1 = new MixInIdImpl(ds1Id);
        ds1.setMixInId(mId1);
        ds1.setId(ds1Id);
        ds1.setName("DS1");
        UiManDataSet uds1 = new UiManDataSet();
        uds1.setSource(ds1);
        DataSet ds2 = new DataSetImpl();
        UUID ds2Id = UUID.randomUUID();
        MixInId mId2 = new MixInIdImpl(ds2Id);
        ds2.setMixInId(mId2);
        ds2.setId(ds2Id);
        ds2.setName("DS2");
        UiManDataSet uds2 = new UiManDataSet();
        uds2.setSource(ds2);

        dataSets.add(ds1);
        dataSets.add(ds2);
        dataSetList.setDataSets(dataSets);
        uiManDataSets.add(uds1);
        uiManDataSets.add(uds2);
        uiManDataSetList.setDataSets(uiManDataSets);

        Attribute attribute = new AttributeImpl();
        attribute.setType(attrType);
        attribute.setName("Encrypted Attribute");
        UiManAttribute uiManAttribute = new UiManAttribute();
        uiManAttribute.setSource(attribute);

        Parameter parameter1 = new ParameterImpl();
        parameter1.setDataSet(ds1);
        parameter1.setText("any password");
        UiManParameter uiManParameter1 = new UiManParameter();
        uiManParameter1.setDataSet(ds1Id);
        uiManParameter1.setSource(parameter1);
        uiManParameter1.setValue(parameter1.getText());

        Parameter parameter2 = new ParameterImpl();
        parameter2.setDataSet(ds2);
        parameter2.setText("any password");
        UiManParameter uiManParameter2 = new UiManParameter();
        uiManParameter2.setDataSet(ds2Id);
        uiManParameter2.setSource(parameter2);
        uiManParameter2.setValue(parameter2.getText());

        attribute.setParameters(Arrays.asList(parameter1, parameter2));
        dataSetList.setAttributes(Collections.singletonList(attribute));
        uiManAttribute.setParameters(Arrays.asList(uiManParameter1, uiManParameter2));
        uiManDataSetList.setAttributes(Collections.singletonList(uiManAttribute));

        when(dslService.getAsTree(datasetListId, false)).thenReturn(uiManDataSetList);
        when(factory.getAttributeExportConverter(any())).thenReturn(new EncryptedAttributeExportConverter());
    }

    @Test
    public void datasetListExportService_mapEncryptedAttributeToRow_returnDocumentWithEqualStructure() {
        File erFile = new File(RESOURCES_PATH + EXCEL_FILE);
        File arFile = datasetListExportService.exportDataSetList(datasetListId);
        List<List<String>> erRows = ExcelRowsReader.read(erFile).collect(Collectors.toList());
        List<List<String>> arRows = ExcelRowsReader.read(arFile).collect(Collectors.toList());
        Assertions.assertEquals(erRows, arRows);
    }
}

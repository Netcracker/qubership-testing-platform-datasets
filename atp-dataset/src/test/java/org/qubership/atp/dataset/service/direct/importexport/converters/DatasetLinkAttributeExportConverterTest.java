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
import org.mockito.Mockito;
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
public class DatasetLinkAttributeExportConverterTest {

    private static final String RESOURCES_PATH = "src/test/resources/exportToFile/";
    private static final String EXCEL_FILE = "DSL_LINK_ATTR.xlsx";
    private static final AttributeType attrType = AttributeType.DSL;

    private UUID datasetListId;

    private static DatasetListExportService datasetListExportService;

    @BeforeEach
    public void setUp() {
        DataSetListService dslService = mock(DataSetListService.class);
        DataSetListImportExportFactory factory = mock(DataSetListImportExportFactory.class);
        datasetListExportService = new DatasetListExportService(dslService, factory);

        //--------------------DSL2
        DataSetList dataSetList2 = new DataSetListImpl();
        UUID datasetListId2 = UUID.randomUUID();
        dataSetList2.setId(datasetListId2);
        dataSetList2.setName("DSL2");
        UiManDataSetList uiManDataSetList2 = new UiManDataSetList();
        uiManDataSetList2.setSource(dataSetList2);

        List<DataSet> dataSets2 = new ArrayList<>();
        List<UiManDataSet> uiManDataSets2 = new ArrayList<>();
        DataSet ds3 = new DataSetImpl();
        UUID ds1Id = UUID.randomUUID();
        UUID dsId3 = UUID.randomUUID();
        MixInId mId3 = new MixInIdImpl(dsId3);
        ds3.setMixInId(mId3);
        ds3.setId(dsId3);
        ds3.setName("DS3");
        UiManDataSet uds3 = new UiManDataSet();
        uds3.setSource(ds3);
        DataSet ds4 = new DataSetImpl();
        UUID ds2Id = UUID.randomUUID();
        UUID dsId4 = UUID.randomUUID();
        MixInId mId4 = new MixInIdImpl(dsId4);
        ds4.setMixInId(mId4);
        ds4.setId(dsId4);
        ds4.setName("DS4");
        UiManDataSet uds4 = new UiManDataSet();
        uds4.setSource(ds4);

        dataSets2.add(ds3);
        dataSets2.add(ds4);
        dataSetList2.setDataSets(dataSets2);
        uiManDataSets2.add(uds3);
        uiManDataSets2.add(uds4);
        uiManDataSetList2.setDataSets(uiManDataSets2);

        Attribute attribute2 = new AttributeImpl();
        attribute2.setType(AttributeType.TEXT);
        attribute2.setName("attr");
        UiManAttribute uiManAttribute2 = new UiManAttribute();
        uiManAttribute2.setSource(attribute2);

        Parameter parameter3 = new ParameterImpl();
        parameter3.setDataSet(ds3);
        parameter3.setText("value1");
        UiManParameter uiManParameter3 = new UiManParameter();
        uiManParameter3.setDataSet(ds1Id);
        uiManParameter3.setSource(parameter3);
        uiManParameter3.setValue(parameter3.getText());

        Parameter parameter4 = new ParameterImpl();
        parameter4.setDataSet(ds4);
        parameter4.setText("value2");
        UiManParameter uiManParameter4 = new UiManParameter();
        uiManParameter4.setDataSet(ds2Id);
        uiManParameter4.setSource(parameter4);
        uiManParameter4.setValue(parameter4.getText());

        attribute2.setParameters(Arrays.asList(parameter3, parameter4));
        dataSetList2.setAttributes(Collections.singletonList(attribute2));
        uiManAttribute2.setParameters(Arrays.asList(uiManParameter3, uiManParameter4));
        uiManDataSetList2.setAttributes(Collections.singletonList(uiManAttribute2));

        //--------------------DSL1
        DataSetList dataSetList1 = new DataSetListImpl();
        datasetListId = UUID.randomUUID();
        dataSetList1.setId(datasetListId);
        dataSetList1.setName("DSL_LINK_ATTR");
        UiManDataSetList uiManDataSetList1 = new UiManDataSetList();
        uiManDataSetList1.setSource(dataSetList1);

        List<DataSet> dataSets1 = new ArrayList<>();
        List<UiManDataSet> uiManDataSets1 = new ArrayList<>();
        DataSet ds1 = new DataSetImpl();
        MixInId mId1 = new MixInIdImpl(ds1Id);
        ds1.setMixInId(mId1);
        ds1.setId(ds1Id);
        ds1.setName("DS1");
        UiManDataSet uds1 = new UiManDataSet();
        uds1.setSource(ds1);
        DataSet ds2 = new DataSetImpl();
        MixInId mId2 = new MixInIdImpl(ds2Id);
        ds2.setMixInId(mId2);
        ds2.setId(ds2Id);
        ds2.setName("DS2");
        UiManDataSet uds2 = new UiManDataSet();
        uds2.setSource(ds2);

        dataSets1.add(ds1);
        dataSets1.add(ds2);
        dataSetList1.setDataSets(dataSets1);
        uiManDataSets1.add(uds1);
        uiManDataSets1.add(uds2);
        uiManDataSetList1.setDataSets(uiManDataSets1);

        Attribute attribute1 = new AttributeImpl();
        attribute1.setType(attrType);
        attribute1.setName("Link to DSL2");
        attribute1.setDataSetListReference(dataSetList2);
        UiManAttribute uiManAttribute1 = new UiManAttribute();
        uiManAttribute1.setSource(attribute1);
        uiManAttribute1.setAttributes(Collections.singletonList(uiManAttribute2));

        Parameter parameter1 = new ParameterImpl();
        parameter1.setDataSet(ds1);
        parameter1.setDataSetReference(ds3);
        UiManParameter uiManParameter1 = new UiManParameter();
        uiManParameter1.setDataSet(ds1Id);
        uiManParameter1.setSource(parameter1);
        uiManParameter1.setValue(ds3.getName());
        uiManParameter1.setValueRef(ds3.getId());

        Parameter parameter2 = new ParameterImpl();
        parameter2.setDataSet(ds2);
        parameter2.setDataSetReference(ds4);
        UiManParameter uiManParameter2 = new UiManParameter();
        uiManParameter2.setDataSet(ds2Id);
        uiManParameter2.setSource(parameter2);
        uiManParameter2.setValue(ds4.getName());
        uiManParameter1.setValueRef(ds4.getId());

        attribute1.setParameters(Arrays.asList(parameter1, parameter2));
        dataSetList1.setAttributes(Collections.singletonList(attribute1));
        uiManAttribute1.setParameters(Arrays.asList(uiManParameter1, uiManParameter2));
        uiManDataSetList1.setAttributes(Collections.singletonList(uiManAttribute1));

        when(dslService.getAsTree(datasetListId, false)).thenReturn(uiManDataSetList1);
        when(factory.getAttributeExportConverter(any()))
                .thenReturn(new DatasetLinkAttributeExportConverter());
        Mockito.doReturn(new TextAttributeExportConverter()).when(factory)
                .getAttributeExportConverter(AttributeTypeConverterEnum.TEXT.getName());
    }

    @Test
    public void datasetListExportService_mapTextAttributeToRow_returnDocumentWithEqualStructure() {
        File erFile = new File(RESOURCES_PATH + EXCEL_FILE);
        File arFile = datasetListExportService.exportDataSetList(datasetListId);
        List<List<String>> erRows = ExcelRowsReader.read(erFile).collect(Collectors.toList());
        List<List<String>> arRows = ExcelRowsReader.read(arFile).collect(Collectors.toList());
        Assertions.assertEquals(erRows, arRows);
    }
}

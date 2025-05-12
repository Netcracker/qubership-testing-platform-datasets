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
public class TextAttributeExportConverterTest {

    private static final String RESOURCES_PATH = "src/test/resources/exportToFile/";
    private static final String EXCEL_FILE = "DSL_TEXT_ATTR.xlsx";
    private static final AttributeType attrType = AttributeType.TEXT;
    private static final String uuidText = "#UUID()";
    private static final String refText = "#REF_THIS(Text Attribute)";

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
        dataSetList.setName("DSL_TEXT_ATTR");
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
        attribute.setName("Text Attribute");
        UiManAttribute uiManAttribute = new UiManAttribute();
        uiManAttribute.setSource(attribute);

        Attribute macrosAttribute = new AttributeImpl();
        macrosAttribute.setType(attrType);
        macrosAttribute.setName("Macros Attribute");
        UiManAttribute uiManAttributeMacros = new UiManAttribute();
        uiManAttributeMacros.setSource(macrosAttribute);

        Parameter parameter = new ParameterImpl();
        parameter.setDataSet(ds2);
        parameter.setText("some_text");
        UiManParameter uiManParameter = new UiManParameter();
        uiManParameter.setDataSet(ds2Id);
        uiManParameter.setSource(parameter);
        uiManParameter.setValue(parameter.getText());

        Parameter macrosUuid = new ParameterImpl();
        macrosUuid.setDataSet(ds1);
        macrosUuid.setText(uuidText);
        UiManParameter uiManParameterMacrosUuid = new UiManParameter();
        uiManParameterMacrosUuid.setDataSet(ds1Id);
        uiManParameterMacrosUuid.setSource(macrosUuid);
        uiManParameterMacrosUuid.setValue(macrosUuid.getText());

        Parameter macrosRef = new ParameterImpl();
        macrosRef.setDataSet(ds2);
        macrosRef.setText(refText);
        UiManParameter uiManParameterMacrosRef = new UiManParameter();
        uiManParameterMacrosRef.setDataSet(ds2Id);
        uiManParameterMacrosRef.setSource(macrosRef);
        uiManParameterMacrosRef.setValue(macrosRef.getText());

        attribute.setParameters(Collections.singletonList(parameter));
        macrosAttribute.setParameters(Arrays.asList(macrosUuid, macrosRef));
        dataSetList.setAttributes(Arrays.asList(attribute, macrosAttribute));
        uiManAttribute.setParameters(Collections.singletonList(uiManParameter));
        uiManAttributeMacros.setParameters(Arrays.asList(uiManParameterMacrosUuid, uiManParameterMacrosRef));
        uiManDataSetList.setAttributes(Arrays.asList(uiManAttribute, uiManAttributeMacros));

        when(dslService.getAsTree(datasetListId, false)).thenReturn(uiManDataSetList);
        when(factory.getAttributeExportConverter(any())).thenReturn(new TextAttributeExportConverter());
    }

    @Test
    public void datasetListExportService_mapTextAttributeToRow_returnDocumentWithEqualStructure() {
        File erFile = new File(RESOURCES_PATH + EXCEL_FILE);

        File arFile = datasetListExportService.exportDataSetList(datasetListId);
        List<List<String>> erRows = ExcelRowsReader.read(erFile).collect(Collectors.toList());
        List<List<String>> arRows = ExcelRowsReader.read(arFile).collect(Collectors.toList());

        Assertions.assertEquals(erRows, arRows);
    }

    @Test
    public void exportDataSetList_exportMacros_macrosNotCalculated() {
        File erFile = new File(RESOURCES_PATH + EXCEL_FILE);
        List<List<String>> rows = ExcelRowsReader.read(erFile).collect(Collectors.toList());

        rows.forEach(row -> {
            if ("Macros Attribute".equalsIgnoreCase(row.get(0))) {
                Assertions.assertEquals(uuidText, row.get(2));
                Assertions.assertEquals(refText, row.get(3));
            }
        });
    }
}

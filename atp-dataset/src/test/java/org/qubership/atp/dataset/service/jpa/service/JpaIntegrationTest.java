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

package org.qubership.atp.dataset.service.jpa.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.service.jpa.ContextType;
import org.qubership.atp.dataset.service.jpa.delegates.Attribute;
import org.qubership.atp.dataset.service.jpa.delegates.DataSet;
import org.qubership.atp.dataset.service.jpa.delegates.DataSetList;
import org.qubership.atp.dataset.service.jpa.delegates.ListValue;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;
import org.qubership.atp.dataset.service.jpa.model.tree.ds.DataSetTree;
import org.qubership.atp.macros.core.clients.api.dto.macros.MacrosDto;

@Disabled
@SpringBootTest
@ContextConfiguration(classes = {TestConfiguration.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class JpaIntegrationTest extends AbstractJpaTest {

    @Mock
    MacrosDto macrosDto;

    private String EXPECTED_ATP_EVALUATED_CONTEXT =
            "{\"parameters\":{\"Attr 1\":{\"type\":\"TEXT\"},\"Attr 2\":{\"type\":\"TEXT\","
                    + "\"value\":\"32OK_MACRO_VALUE\"},\"Attr list 1\":{\"type\":\"LIST\",\"value\":\"Value 2\"}},"
                    + "\"groups\":{\"DSL reference\":{\"type\":\"DSL\",\"value\":\"Test DataSet2  2\",\"dsl\":\"Test "
                    + "DataSetList 2\",\"parameters\":{\"Attr2  1\":{\"type\":\"TEXT\","
                    + "\"value\":\"32OK_MACRO_VALUE\"},\"Attr2  2\":{\"type\":\"TEXT\",\"value\":\"Some overlap "
                    + "value\"},\"Attr2  list 1\":{\"type\":\"LIST\",\"value\":\"Value2 2\"}},\"groups\":{\"DSL "
                    + "reference 2\":{\"type\":\"DSL\",\"value\":\"Test DataSet3  3\",\"dsl\":\"Test DataSetList 3\","
                    + "\"parameters\":{\"Attr3  1\":{\"type\":\"TEXT\"},\"Attr3  2\":{\"type\":\"TEXT\","
                    + "\"value\":\"Some value3 4\"}},\"dataSetId\":\"b893b683-0b17-4f76-a96f-c997af88f7b2\"}},"
                    + "\"dataSetId\":\"96ba075a-1436-488a-90a0-30b3d58584f9\"}}}";
    private String EXPECTED_ATP_UNEVALUATED_CONTEXT =
            "{\"parameters\":{\"Attr 1\":{\"type\":\"TEXT\"},\"Attr 2\":{\"type\":\"TEXT\","
                    + "\"value\":\"32OK_MACRO_VALUE\"},\"Attr list 1\":{\"type\":\"LIST\","
                    + "\"value\":\"Value 2\"}},\"groups\":{\"DSL reference\":{\"type\":\"DSL\",\"value\":\"Test "
                    + "DataSet2  2\",\"dsl\":\"Test DataSetList 2\",\"parameters\":{\"Attr2  1\":{\"type\":\"TEXT\","
                    + "\"value\":\"32OK_MACRO_VALUE\"},\"Attr2  2\":{\"type\":\"TEXT\",\"value\":\"Some overlap "
                    + "value\"},\"Attr2  list 1\":{\"type\":\"LIST\",\"value\":\"Value2 2\"}},\"groups\":{\"DSL "
                    + "reference 2\":{\"type\":\"DSL\",\"value\":\"Test DataSet3  3\",\"dsl\":\"Test DataSetList 3\","
                    + "\"parameters\":{\"Attr3  1\":{\"type\":\"TEXT\"},\"Attr3  2\":{\"type\":\"TEXT\","
                    + "\"value\":\"Some value3 4\"}},\"dataSetId\":\"b893b683-0b17-4f76-a96f-c997af88f7b2\"}},"
                    + "\"dataSetId\":\"96ba075a-1436-488a-90a0-30b3d58584f9\"}}}";
    private String EXPECTED_ITF_CONTEXT =
            "{\"Attr 1\":\"\",\"Attr 2\":\"32OK_MACRO_VALUE\",\"Attr list 1\":\"Value 2\",\"DSL reference\":{\"Attr2 "
                    + " 1\":\"32OK_MACRO_VALUE\",\"Attr2  2\":\"Some overlap value\",\"Attr2  list 1\":\"Value2 2\","
                    + "\"DSL reference 2\":{\"Attr3  1\":\"\",\"Attr3  2\":\"Some value3 4\"}}}";

    private String FULL_ATP_EXPECTED_RESULT =
            "{\"parameters\":{\"Some text 1\":{\"type\":\"TEXT\",\"value\":\"32OK_MACRO_VALUE\"},\"Some text "
                    + "2\":{\"type\":\"TEXT\",\"value\":\"Val2\"}},\"groups\":{\"DSL reference\":{\"type\":\"DSL\","
                    + "\"dsl\":\"Child DSL\",\"parameters\":{\"Some var 1\":{\"type\":\"TEXT\"},\"Some var "
                    + "2\":{\"type\":\"TEXT\",\"value\":\"Some overlap value\"}}}}}";
    private String OBJECT_ATP_EXPECTED_RESULT =
            "{\"parameters\":{\"Some text 1\":{\"type\":\"TEXT\",\"value\":\"32OK_MACRO_VALUE\"},\"Some text "
                    + "2\":{\"type\":\"TEXT\",\"value\":\"Val2\"}},\"groups\":{\"DSL reference\":{\"type\":\"DSL\","
                    + "\"dsl\":\"Child DSL\"}}}";
    private String OPTIMIZED_ATP_EXPECTED_RESULT =
            "{\"parameters\":{\"Some text 1\":{\"type\":\"TEXT\",\"value\":\"32OK_MACRO_VALUE\"},\"Some text "
                    + "2\":{\"type\":\"TEXT\",\"value\":\"Val2\"}},\"groups\":{\"DSL reference\":{\"type\":\"DSL\","
                    + "\"dsl\":\"Child DSL\",\"parameters\":{\"Some var 2\":{\"type\":\"TEXT\",\"value\":\"Some "
                    + "overlap value\"}}}}}";
    private String OBJECT_EXTENDED_ATP_EXPECTED_RESULT =
            "{\"parameters\":{\"Some text 1\":{\"type\":\"TEXT\",\"value\":\"32OK_MACRO_VALUE\"},\"Some text "
                    + "2\":{\"type\":\"TEXT\",\"value\":\"Val2\"}},\"groups\":{\"DSL reference\":{\"type\":\"DSL\","
                    + "\"dsl\":\"Child DSL\",\"parameters\":{\"Some var 2\":{\"type\":\"TEXT\",\"value\":\"Some "
                    + "overlap value\"}}}}}";

    private static UUID dataSetToCheck = UUID.fromString("f05b4368-a1ff-41f6-bf8f-9c7f0f139e1c");
    private static UUID sourceAttributeEncNoParameterId = UUID.fromString("89520827-8daf-4f3a-ba05-d1369b63e636");

    private UUID visibilityAreaId = UUID.fromString("f261ed65-a491-44b0-af38-f0d97a46008c");
    private UUID sourceDsId = UUID.fromString("336e0cc9-dee2-458c-887e-bcdc406ae4be");
    private UUID sourceAttributeTextId = UUID.fromString("2e70a5cf-815a-40ee-b258-90a1b05f709b");
    private UUID sourceAttributeListId = UUID.fromString("36f8ff3d-3291-4274-bbee-5a468db5f7d2");
    private UUID sourceAttributeDslId = UUID.fromString("9923d9ec-81b8-4dca-b762-f663ed24595b");
    private UUID sourceAttributeChangeId = UUID.fromString("4079aab2-3d5e-4d86-9217-f66524808ec3");
    private UUID sourceAttributeEncId = UUID.fromString("81a84637-62a4-4d8b-8159-a6aeb20e52f5");
    private UUID sourceAttributeTextNoParameterId = UUID.fromString("6871e447-2f61-4fd7-ae4c-dc26aebdc607");
    private UUID sourceAttributeListNoParameterId = UUID.fromString("575e88a0-f83d-48d6-bf3a-adfda05d00f4");
    private UUID sourceAttributeDslNoParameterId = UUID.fromString("132eadd7-4f7e-4fe3-91b8-e36a5e36b672");
    private UUID sourceAttributeChangeNoParameterId = UUID.fromString("062276b4-29a6-48a5-8348-af3a1165fcd8");
    private UUID dataSetToCheckContextTypes = UUID.fromString("fe703972-1b4b-41d5-837b-ab3824de5269");

        @BeforeEach
    public void generateData() throws Exception {
        when(scriptMacrosCalculator.calculate(any(), any(), any())).thenReturn("OK_MACRO_VALUE");
        when(macrosFeignClient.findAllByProject(any(UUID.class)))
                .thenReturn(new ResponseEntity<>(Collections.singletonList(macrosDto), HttpStatus.OK));
        when(macrosDto.getName()).thenReturn("RANDOMBETWEEN");
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void checkAtpContextTypes_resultsMatches() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        DataSetTree fullTree = dataSetService.getDataSetTreeInAtpFormat(dataSetToCheckContextTypes, true, null, ContextType.FULL);
        String full = mapper.writeValueAsString(fullTree);
        Assertions.assertEquals(FULL_ATP_EXPECTED_RESULT, full);

        DataSetTree objectTree = dataSetService.getDataSetTreeInAtpFormat(dataSetToCheckContextTypes, true, null, ContextType.OBJECT);
        String object = mapper.writeValueAsString(objectTree);
        Assertions.assertEquals(OBJECT_ATP_EXPECTED_RESULT, object);

        DataSetTree objectExtendedTree = dataSetService.getDataSetTreeInAtpFormat(dataSetToCheckContextTypes, true, null, ContextType.OBJECT_EXTENDED);
        String objectExtended = mapper.writeValueAsString(objectExtendedTree);
        Assertions.assertEquals(OBJECT_EXTENDED_ATP_EXPECTED_RESULT, objectExtended);

        DataSetTree optimizedTree = dataSetService.getDataSetTreeInAtpFormat(dataSetToCheckContextTypes, true, null, ContextType.NO_NULL_VALUES);
        String optimized = mapper.writeValueAsString(optimizedTree);
        Assertions.assertEquals(OPTIMIZED_ATP_EXPECTED_RESULT, optimized);
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void checkAtpContextEvaluated_resultMatches() throws Exception {
        DataSetTree dataSetTree = dataSetService.getDataSetTreeInAtpFormat(dataSetToCheck, true, null, ContextType.FULL);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(dataSetTree);
        Assertions.assertEquals(EXPECTED_ATP_EVALUATED_CONTEXT, jsonString);
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void checkAtpContextUnevaluated_resultMatches() throws Exception {
        DataSetTree dataSetTree = dataSetService.getDataSetTreeInAtpFormat(dataSetToCheck, false, null, ContextType.FULL);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(dataSetTree);
        Assertions.assertEquals(EXPECTED_ATP_UNEVALUATED_CONTEXT, jsonString);
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void     checkItfContextEvaluated_resultMatches() throws Exception {
        String result = dataSetService.getDataSetTreeInItfFormat(dataSetToCheck);
        Assertions.assertEquals(EXPECTED_ITF_CONTEXT, result);
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void copyDsAttributeValue_withAllValues() throws Exception {
        // given
        DataSetList targetDslAllParametersEqTypes = dataSetListService.create("TargetDsl1", visibilityAreaId);
        UUID targetDslId = targetDslAllParametersEqTypes.getId();
        DataSet targetDsAllParametersEqTypes = dataSetService.create("TargetDS1", targetDslId);
        UUID targetDsId = targetDsAllParametersEqTypes.getId();

        Attribute target1AttributeText = attributeService.create("text_attr", AttributeTypeName.TEXT, targetDslId);
        Attribute target1AttributeList = attributeService.create("list_attr", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeDsl = attributeService.create("dsl_attr", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeChange = attributeService.create("change_attr", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeEnc = attributeService.create("enc_attr", AttributeTypeName.ENCRYPTED, targetDslId);

        Attribute target1AttributeText1 = attributeService.create("text_attr_without_parameter", AttributeTypeName.TEXT, targetDslId);
        Attribute target1AttributeList1 = attributeService.create("list_attr_without_parameter", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeDsl1 = attributeService.create("dsl_attr_without_parameter", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeChange1 = attributeService.create("change_attr_without_parameter", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeEnc1 = attributeService.create("enc_attr_without_parameter", AttributeTypeName.ENCRYPTED, targetDslId);

        ListValue targetListValue = attributeService.createListValue("2", target1AttributeList.getId());
        DataSetList targetDslReference = dataSetListService.create("targetDslParameter", visibilityAreaId);

        ListValue targetListValue1 = attributeService.createListValue("2", target1AttributeList1.getId());
        DataSetList targetDslReference1 = dataSetListService.create("targetDslParameter1", visibilityAreaId);

        parameterService.createParameter(targetDsId, target1AttributeText.getId(), "text2", null, null);
        parameterService.createParameter(targetDsId, target1AttributeList.getId(), null, null, targetListValue.getEntity().getId());
        parameterService.createParameter(targetDsId, target1AttributeDsl.getId(), null, targetDslReference.getId(), null);
        parameterService.createParameter(targetDsId, target1AttributeChange.getId(), "changeType2", null, null);
        parameterService.createParameter(targetDsId, target1AttributeEnc.getId(), "encType2", null, null);

        parameterService.createParameter(targetDsId, target1AttributeText1.getId(), "text3", null, null);
        parameterService.createParameter(targetDsId, target1AttributeList1.getId(), null, null, targetListValue1.getEntity().getId());
        parameterService.createParameter(targetDsId, target1AttributeDsl1.getId(), null, targetDslReference1.getId(), null);
        parameterService.createParameter(targetDsId, target1AttributeChange1.getId(), "changeType3", null, null);
        parameterService.createParameter(targetDsId, target1AttributeEnc1.getId(), "encType3", null, null);

        // when
        when(decryptor.decryptIfEncrypted(any(String.class))).thenAnswer(i -> i.getArguments()[0]);
        UUID newTargetTextId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextId, target1AttributeText.getId());
        UUID newTargetListId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListId, target1AttributeList.getId());
        UUID newTargetDslId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslId, target1AttributeDsl.getId());
        UUID newTargetChangeId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeId, target1AttributeChange.getId());
        UUID newTargetEncId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncId, target1AttributeEnc.getId());
        UUID newTargetText1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextNoParameterId, target1AttributeText1.getId());
        UUID newTargetList1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListNoParameterId, target1AttributeList1.getId());
        UUID newTargetDsl1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslNoParameterId, target1AttributeDsl1.getId());
        UUID newTargetChange1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeNoParameterId, target1AttributeChange1.getId());
        UUID newTargetEnc1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncNoParameterId, target1AttributeEnc1.getId());

        // then
        // new attribute must not be created
        Assertions.assertEquals(target1AttributeText.getId(), newTargetTextId);
        Assertions.assertEquals(target1AttributeList.getId(), newTargetListId);
        Assertions.assertEquals(target1AttributeDsl.getId(), newTargetDslId);
        Assertions.assertEquals(target1AttributeChange.getId(), newTargetChangeId);
        Assertions.assertEquals(target1AttributeEnc.getId(), newTargetEncId);
        Assertions.assertEquals(target1AttributeText1.getId(), newTargetText1Id);
        Assertions.assertEquals(target1AttributeList1.getId(), newTargetList1Id);
        Assertions.assertEquals(target1AttributeDsl1.getId(), newTargetDsl1Id);
        Assertions.assertEquals(target1AttributeChange1.getId(), newTargetChange1Id);
        Assertions.assertEquals(target1AttributeEnc1.getId(), newTargetEnc1Id);

        // values in target parameters should be equal source parameters
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeTextId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetTextId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeListId, sourceDsId).getListValue().getText(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetListId, targetDsId).getListValue().getText());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeDslId, sourceDsId).getDataSetReferenceId(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetDslId, targetDsId).getDataSetReferenceId());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeChangeId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetChangeId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeEncId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetEncId, targetDsId).getStringValue());
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetText1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetList1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetDsl1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetChange1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetEnc1Id, targetDsId));
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void copyDsAttributeValue_withAllValuesDiffTypes() throws Exception {
        // given
        DataSetList targetDslAllParametersDiffTypes = dataSetListService.create("TargetDsl2", visibilityAreaId);
        UUID targetDslId = targetDslAllParametersDiffTypes.getId();
        DataSet targetDsAllParametersDiffTypes = dataSetService.create("TargetDS2", targetDslId);
        UUID targetDsId = targetDsAllParametersDiffTypes.getId();

        Attribute target1AttributeText = attributeService.create("text_attr", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeList = attributeService.create("list_attr", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeDsl = attributeService.create("dsl_attr", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeChange = attributeService.create("change_attr", AttributeTypeName.ENCRYPTED, targetDslId);
        Attribute target1AttributeEnc = attributeService.create("enc_attr", AttributeTypeName.TEXT, targetDslId);

        ListValue targerListValue = attributeService.createListValue("2", target1AttributeList.getId());
        DataSetList targetDslReference = dataSetListService.create("targetDslParameter", visibilityAreaId);

        Attribute target1AttributeText1 = attributeService.create("text_attr_without_parameter", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeList1 = attributeService.create("list_attr_without_parameter", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeDsl1 = attributeService.create("dsl_attr_without_parameter", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeChange1 = attributeService.create("change_attr_without_parameter", AttributeTypeName.ENCRYPTED, targetDslId);
        Attribute target1AttributeEnc1 = attributeService.create("enc_attr_without_parameter", AttributeTypeName.TEXT, targetDslId);

        ListValue targetListValue1 = attributeService.createListValue("2", target1AttributeList1.getId());
        DataSetList targetDslReference1 = dataSetListService.create("targetDslParameter1", visibilityAreaId);

        Parameter textParam = parameterService.createParameter(targetDsId, target1AttributeText.getId(), null, null, targerListValue.getEntity().getId());
        Parameter listParam = parameterService.createParameter(targetDsId, target1AttributeList.getId(), null, targetDslReference.getId(), null);
        Parameter dslParam = parameterService.createParameter(targetDsId, target1AttributeDsl.getId(), "changeType2", null, null);
        Parameter changeParam = parameterService.createParameter(targetDsId, target1AttributeChange.getId(), "encType2", null, null);
        Parameter encParam = parameterService.createParameter(targetDsId, target1AttributeEnc.getId(), "textType2", null, null);

        Parameter p1 = parameterService.createParameter(targetDsId, target1AttributeText1.getId(), null, null, targetListValue1.getEntity().getId());
        Parameter p2 = parameterService.createParameter(targetDsId, target1AttributeList1.getId(), null, targetDslReference1.getId(), null);
        Parameter p3 = parameterService.createParameter(targetDsId, target1AttributeDsl1.getId(), "changeType3", null, null);
        Parameter p4 = parameterService.createParameter(targetDsId, target1AttributeChange1.getId(), "encType3", null, null);
        Parameter p5 = parameterService.createParameter(targetDsId, target1AttributeEnc1.getId(), "textType3", null, null);

        // when
        UUID newTargetTextId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextId, target1AttributeText.getId());
        UUID newTargetListId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListId, target1AttributeList.getId());
        UUID newTargetDslId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslId, target1AttributeDsl.getId());
        UUID newTargetChangeId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeId, target1AttributeChange.getId());
        UUID newTargetEncId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncId, target1AttributeEnc.getId());
        UUID newTargetText1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextNoParameterId, target1AttributeText1.getId());
        UUID newTargetList1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListNoParameterId, target1AttributeList1.getId());
        UUID newTargetDsl1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslNoParameterId, target1AttributeDsl1.getId());
        UUID newTargetChange1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeNoParameterId, target1AttributeChange1.getId());
        UUID newTargetEnc1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncNoParameterId, target1AttributeEnc1.getId());

        // then
        // new attribute must be created
        Assertions.assertNotEquals(target1AttributeText.getId(), newTargetTextId);
        Assertions.assertNotEquals(target1AttributeList.getId(), newTargetListId);
        Assertions.assertNotEquals(target1AttributeDsl.getId(), newTargetDslId);
        Assertions.assertNotEquals(target1AttributeChange.getId(), newTargetChangeId);
        Assertions.assertNotEquals(target1AttributeEnc.getId(), newTargetEncId);
        Assertions.assertNotEquals(target1AttributeText1.getId(), newTargetText1Id);
        Assertions.assertNotEquals(target1AttributeList1.getId(), newTargetList1Id);
        Assertions.assertNotEquals(target1AttributeDsl1.getId(), newTargetDsl1Id);
        Assertions.assertNotEquals(target1AttributeChange1.getId(), newTargetChange1Id);
        Assertions.assertNotEquals(target1AttributeEnc1.getId(), newTargetEnc1Id);

        // Old parameters should be deleted
        Assertions.assertNull(parameterService.getById(textParam.getId()));
        Assertions.assertNull(parameterService.getById(listParam.getId()));
        Assertions.assertNull(parameterService.getById(dslParam.getId()));
        Assertions.assertNull(parameterService.getById(changeParam.getId()));
        Assertions.assertNull(parameterService.getById(encParam.getId()));
        Assertions.assertNull(parameterService.getById(p1.getId()));
        Assertions.assertNull(parameterService.getById(p2.getId()));
        Assertions.assertNull(parameterService.getById(p3.getId()));
        Assertions.assertNull(parameterService.getById(p4.getId()));
        Assertions.assertNull(parameterService.getById(p5.getId()));

        // Old listValue should be deleted
        Assertions.assertNull(attributeService.getListValueById(targerListValue.getId()));

        // values in target parameters should be equal source parameters
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeTextId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetTextId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeListId, sourceDsId).getListValue().getText(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetListId, targetDsId).getListValue().getText());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeDslId, sourceDsId).getDataSetReferenceId(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetDslId, targetDsId).getDataSetReferenceId());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeChangeId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetChangeId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeEncId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetEncId, targetDsId).getStringValue());
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetText1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetList1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetDsl1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetChange1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetEnc1Id, targetDsId));
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void copyDsAttributeValue_noTargetParameters_parametersShouldBeCreated() throws Exception {
        // given
        DataSetList targetDslOnlyAttributes = dataSetListService.create("TargetDsl3", visibilityAreaId);
        UUID targetDslId = targetDslOnlyAttributes.getId();
        DataSet targetDsOnlyAttributes = dataSetService.create("TargetDS3", targetDslId);
        UUID targetDsId = targetDsOnlyAttributes.getId();

        Attribute target1AttributeText = attributeService.create("text_attr", AttributeTypeName.TEXT, targetDslId);
        Attribute target1AttributeList = attributeService.create("list_attr", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeDsl = attributeService.create("dsl_attr", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeChange = attributeService.create("change_attr", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeEnc = attributeService.create("enc_attr", AttributeTypeName.ENCRYPTED, targetDslId);

        Attribute target1AttributeText1 = attributeService.create("text_attr_without_parameter", AttributeTypeName.TEXT, targetDslId);
        Attribute target1AttributeList1 = attributeService.create("list_attr_without_parameter", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeDsl1 = attributeService.create("dsl_attr_without_parameter", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeChange1 = attributeService.create("change_attr_without_parameter", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeEnc1 = attributeService.create("enc_attr_without_parameter", AttributeTypeName.ENCRYPTED, targetDslId);

        // when
        UUID newTargetTextId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextId, target1AttributeText.getId());
        UUID newTargetListId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListId, target1AttributeList.getId());
        UUID newTargetDslId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslId, target1AttributeDsl.getId());
        UUID newTargetChangeId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeId, target1AttributeChange.getId());
        UUID newTargetEncId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncId, target1AttributeEnc.getId());
        UUID newTargetText1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextNoParameterId, target1AttributeText1.getId());
        UUID newTargetList1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListNoParameterId, target1AttributeList1.getId());
        UUID newTargetDsl1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslNoParameterId, target1AttributeDsl1.getId());
        UUID newTargetChange1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeNoParameterId, target1AttributeChange1.getId());
        UUID newTargetEnc1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncNoParameterId, target1AttributeEnc1.getId());

        // then
        // new attribute must not be created
        Assertions.assertEquals(target1AttributeText.getId(), newTargetTextId);
        Assertions.assertEquals(target1AttributeList.getId(), newTargetListId);
        Assertions.assertEquals(target1AttributeDsl.getId(), newTargetDslId);
        Assertions.assertEquals(target1AttributeChange.getId(), newTargetChangeId);
        Assertions.assertEquals(target1AttributeEnc.getId(), newTargetEncId);
        Assertions.assertEquals(target1AttributeText1.getId(), newTargetText1Id);
        Assertions.assertEquals(target1AttributeList1.getId(), newTargetList1Id);
        Assertions.assertEquals(target1AttributeDsl1.getId(), newTargetDsl1Id);
        Assertions.assertEquals(target1AttributeChange1.getId(), newTargetChange1Id);
        Assertions.assertEquals(target1AttributeEnc1.getId(), newTargetEnc1Id);

        // values in target parameters should be equal source parameters
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeTextId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetTextId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeListId, sourceDsId).getListValue().getText(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetListId, targetDsId).getListValue().getText());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeDslId, sourceDsId).getDataSetReferenceId(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetDslId, targetDsId).getDataSetReferenceId());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeChangeId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetChangeId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeEncId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetEncId, targetDsId).getStringValue());
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetText1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetList1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetDsl1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetChange1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetEnc1Id, targetDsId));
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void copyDsAttributeValue_noTargetAttributes() throws Exception {
        // given
        DataSetList targetDslNoAttributes = dataSetListService.create("TargetDsl4", visibilityAreaId);
        UUID targetDslId = targetDslNoAttributes.getId();
        DataSet targetDsNoAttributes = dataSetService.create("TargetDS4", targetDslId);
        UUID targetDsId = targetDsNoAttributes.getId();

        // when
        UUID newTargetTextId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextId, null);
        UUID newTargetListId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListId, null);
        UUID newTargetDslId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslId, null);
        UUID newTargetChangeId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeId, null);
        UUID newTargetEncId = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncId, null);
        UUID newTargetText1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeTextNoParameterId, null);
        UUID newTargetList1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeListNoParameterId, null);
        UUID newTargetDsl1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeDslNoParameterId, null);
        UUID newTargetChange1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeChangeNoParameterId, null);
        UUID newTargetEnc1Id = dataSetService.copyDsAttributeValue(sourceDsId, targetDsId, sourceAttributeEncNoParameterId, null);

        // then
        // values in target parameters should be equal source parameters
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeTextId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetTextId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeListId, sourceDsId).getListValue().getText(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetListId, targetDsId).getListValue().getText());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeDslId, sourceDsId).getDataSetReferenceId(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetDslId, targetDsId).getDataSetReferenceId());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeChangeId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetChangeId, targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeEncId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(newTargetEncId, targetDsId).getStringValue());
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetText1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetList1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetDsl1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetChange1Id, targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(newTargetEnc1Id, targetDsId));
    }

    @Test
    @Sql(scripts = "classpath:test_data/sql/jpa_integration_test/JpaIntegrationTest.sql")
    public void copyDsAttributeValueBulk_withAllValues() throws Exception {
//        generateData();
        // given
        DataSetList targetDslAllParametersEqTypes = dataSetListService.create("TargetDsl5", visibilityAreaId);
        UUID targetDslId = targetDslAllParametersEqTypes.getId();
        DataSet targetDsAllParametersEqTypes = dataSetService.create("TargetDS5", targetDslId);
        UUID targetDsId = targetDsAllParametersEqTypes.getId();

        Attribute target1AttributeText = attributeService.create("text_attr", AttributeTypeName.TEXT, targetDslId);
        Attribute target1AttributeList = attributeService.create("list_attr", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeDsl = attributeService.create("dsl_attr", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeChange = attributeService.create("change_attr", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeEnc = attributeService.create("enc_attr", AttributeTypeName.ENCRYPTED, targetDslId);

        Attribute target1AttributeText1 = attributeService.create("text_attr_without_parameter", AttributeTypeName.TEXT, targetDslId);
        Attribute target1AttributeList1 = attributeService.create("list_attr_without_parameter", AttributeTypeName.LIST, targetDslId);
        Attribute target1AttributeDsl1 = attributeService.create("dsl_attr_without_parameter", AttributeTypeName.DSL, targetDslId);
        Attribute target1AttributeChange1 = attributeService.create("change_attr_without_parameter", AttributeTypeName.CHANGE, targetDslId);
        Attribute target1AttributeEnc1 = attributeService.create("enc_attr_without_parameter", AttributeTypeName.ENCRYPTED, targetDslId);

        ListValue targetListValue = attributeService.createListValue("2", target1AttributeList.getId());
        DataSetList targetDslReference = dataSetListService.create("targetDslParameter", visibilityAreaId);

        ListValue targetListValue1 = attributeService.createListValue("2", target1AttributeList1.getId());
        DataSetList targetDslReference1 = dataSetListService.create("targetDslParameter1", visibilityAreaId);

        parameterService.createParameter(targetDsId, target1AttributeText.getId(), "text2", null, null);
        parameterService.createParameter(targetDsId, target1AttributeList.getId(), null, null, targetListValue.getEntity().getId());
        parameterService.createParameter(targetDsId, target1AttributeDsl.getId(), null, targetDslReference.getId(), null);
        parameterService.createParameter(targetDsId, target1AttributeChange.getId(), "changeType2", null, null);
        parameterService.createParameter(targetDsId, target1AttributeEnc.getId(), "encType2", null, null);

        parameterService.createParameter(targetDsId, target1AttributeText1.getId(), "text3", null, null);
        parameterService.createParameter(targetDsId, target1AttributeList1.getId(), null, null, targetListValue1.getEntity().getId());
        parameterService.createParameter(targetDsId, target1AttributeDsl1.getId(), null, targetDslReference1.getId(), null);
        parameterService.createParameter(targetDsId, target1AttributeChange1.getId(), "changeType3", null, null);
        parameterService.createParameter(targetDsId, target1AttributeEnc1.getId(), "encType3", null, null);

        // when
        when(decryptor.decryptIfEncrypted(any(String.class))).thenAnswer(i -> i.getArguments()[0]);
        dataSetService.copyDsAttributeValueBulk(sourceDsId, targetDsId);

        // then
        // values in target parameters should be equal source parameters
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeTextId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeText.getId(), targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeListId, sourceDsId).getListValue().getText(),
                parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeList.getId(), targetDsId).getListValue().getText());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeDslId, sourceDsId).getDataSetReferenceId(),
                parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeDsl.getId(), targetDsId).getDataSetReferenceId());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeChangeId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeChange.getId(), targetDsId).getStringValue());
        Assertions.assertEquals(parameterService.getParameterByAttributeIdAndDataSetId(sourceAttributeEncId, sourceDsId).getStringValue(),
                parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeEnc.getId(), targetDsId).getStringValue());
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeText1.getId(), targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeList1.getId(), targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeDsl1.getId(), targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeChange1.getId(), targetDsId));
        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(target1AttributeEnc1.getId(), targetDsId));
    }
}

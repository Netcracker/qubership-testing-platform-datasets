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

package org.qubership.atp.dataset.model.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;

public class ItfSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final CreationFacade CREATE = SimpleCreationFacade.INSTANCE;

    private static DataSet customerWithEmptyCart() {
        DataSet result = customer1();
        VisibilityArea va = result.getDataSetList().getVisibilityArea();
        DataSet emptyCart = CREATE.ds(va, "emptyCart", "emptyCart1");
        CREATE.refParam(result, "emptyCart1", emptyCart);
        return result;
    }

    private static DataSet customer1() {
        VisibilityArea va = CREATE.va("va");
        DataSet customer1Ds = CREATE.ds(va, "Customer", "Customer1");
        DataSet addressDs = CREATE.ds(va, "Address", "Address1");
        CREATE.textParam(addressDs, "province", "Ontario");
        CREATE.refParam(customer1Ds, "Address1", addressDs);
        return customer1Ds;
    }

    private static DataSet customerWithOverlapsHierarchy() {
        DataSet customer1 = customer1();
        VisibilityArea va = customer1.getDataSetList().getVisibilityArea();
        Parameter customer1Address1Ref = customer1.getParameters().get(0);
        DataSet address1 = customer1Address1Ref.getDataSetReference();
        //got all the necessary data from customer 1
        DataSet postalCode1 = CREATE.ds(va, "PostalCode", "PC1");
        Parameter firstPartParameter = CREATE.textParam(postalCode1, "firstPart", "A1A");
        Parameter address1PostalCode1Ref = CREATE.refParam(address1, "Postal Code", postalCode1);
        //+ reference address1->pc1 with firstPart=A1A
        CREATE.overrideParam(address1,
                firstPartParameter.getAttribute(),
                "Overlapped by Address1", null, null,
                null, address1PostalCode1Ref.getAttribute());
        //firstPart=Overlapped by Address1 now
        CREATE.overrideParam(customer1,
                firstPartParameter.getAttribute(),
                "Overlapped by Customer1", null, null,
                null, customer1Address1Ref.getAttribute(), address1PostalCode1Ref.getAttribute());
        //firstPart=Overlapped by Customer1 now
        return customer1;
    }

    private static DataSet customerWithAgeAndAddressAsHomeAndWorkWithOverlap() {
        VisibilityArea va = CREATE.va("va");
        DataSet customer = CREATE.ds(va, "Customer", "DefaultCustomer");
        Parameter age = CREATE.textParam(customer, "Age", "20");
        Parameter ageLV = CREATE.listParam(customer, "AgeLV", "20",
                "10", "20", "30", "40");
        DataSet address = CREATE.ds(va, "Address", "DefaultAddress");
        Parameter floor = CREATE.textParam(address, "Floor", "3");
        Parameter floorLV = CREATE.listParam(address, "FloorLV", "3",
                "1", "2", "3", "4", "5");
        Parameter customerToHome = CREATE.refParam(customer, "Home", address);
        Parameter customerToWork = CREATE.refParam(customer, "Work", address);
        CREATE.overrideParam(customer, floorLV.getAttribute(), null, "5", null,
                null, customerToWork.getAttribute());
        CREATE.overrideParam(customer, floor.getAttribute(), "5", null, null,
                null, customerToWork.getAttribute());
        return customer;
    }

    @Test
    public void dataSetParametersSerialized() throws Exception {
        String expectedJson = "{\"default\":\"text\",\"group\":{\"default\":\"text2\",\"overridden\":\"text3\"}}";
        DataSet ds = testDs();
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ObjectNode actual = Utils.serializeInItfWay(ds, MAPPER, EvaluatorMock.INSTANCE);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void dataSetParametersSerializedWhenOverlapsInHierarchy() throws Exception {
        String expectedJson = "{\"Address1\":{\"province\":\"Ontario\",\"Postal Code\":{\"firstPart\":\"Overlapped by Customer1\"}}}";
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ObjectNode actual = Utils.serializeInItfWay(customerWithOverlapsHierarchy(), MAPPER, EvaluatorMock.INSTANCE);
        Assertions.assertEquals(expected, actual);
    }

    /**
     * TAPLATFORM-3729
     */
    @Test
    public void dataSetParametersSerializedWhenRefDsIsEmpty() throws Exception {
        String expectedJson = "{\"Address1\":{\"province\":\"Ontario\"}}";
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ObjectNode actual = Utils.serializeInItfWay(customerWithEmptyCart(), MAPPER, EvaluatorMock.INSTANCE);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void dataSetParametersSerializedWhenEmpty() throws Exception {
        String expectedJson = "{}";
        DataSet targetDs = CREATE.ds("va", "dsl", "ds");
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ObjectNode actual = Utils.serializeInItfWay(targetDs, MAPPER, EvaluatorMock.INSTANCE);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void attributesListSerialized() throws Exception {
        String expectedJson = "[\"default\",\"group.default\",\"group.overridden\"]";
        DataSetList dsl = testDs().getDataSetList();
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ArrayNode actual = Utils.serializeAttrInItfWay(dsl, MAPPER);
        Assertions.assertEquals(expected, actual);
    }

    /**
     * TAPLATFORM-3729
     */
    @Test
    public void attributesListSerializedWhenRefDsIsEmpty() throws Exception {
        String expectedJson = "[\"Address1.province\"]";
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ArrayNode actual = Utils.serializeAttrInItfWay(customerWithEmptyCart().getDataSetList(), MAPPER);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void dataSetAttributesSerializedWhenNameOfRefIsDifferFromRefTargetName() throws Exception {
        //'Postal Code' and not 'PostalCode'
        String expectedJson = "[\"Address1.province\",\"Address1.Postal Code.firstPart\"]";
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ArrayNode actual = Utils.serializeAttrInItfWay(customerWithOverlapsHierarchy().getDataSetList(), MAPPER);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void serializeDataSet_SameParameterUsedTwiceAndOneIsOverlapped_NotOverlappedParameterHasDefaultValue() throws Exception {
        String expectedJson = "{\"Age\":\"20\",\"AgeLV\":\"20\",\"Home\":{\"Floor\":\"3\",\"FloorLV\":\"3\"},\"Work\":{\"Floor\":\"5\",\"FloorLV\":\"5\"}}";
        JsonNode expected = MAPPER.readTree(expectedJson);
        ObjectNode actual = Utils.serializeInItfWay(customerWithAgeAndAddressAsHomeAndWorkWithOverlap(), MAPPER, EvaluatorMock.INSTANCE);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void serializeDataSet_SameParameterUsedTwiceAndOneIsOverlappedInTheMiddleGroup_OverlappedParameterHasActualValueAtTopLayer() throws Exception {
        String expectedJson = "{\"Home\":{\"Postal Code\":{\"Code\":\"PC#OfHome\",\"CodeLV\":\"PC#OfHome\"}},\"Work\":{\"Postal Code\":{\"Code\":\"PC#OfWork\",\"CodeLV\":\"PC#OfWork\"}}}";
        JsonNode expected = MAPPER.readTree(expectedJson);
        ObjectNode actual = Utils.serializeInItfWay(TestData.customerWithAddressAndPostalCode(CREATE), MAPPER, EvaluatorMock.INSTANCE);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void attributesListSerializedWhenEmpty() throws Exception {
        String expectedJson = "[]";
        DataSet targetDs = CREATE.ds("va", "dsl", "ds");
        //validate
        JsonNode expected = MAPPER.readTree(expectedJson);
        ArrayNode actual = Utils.serializeAttrInItfWay(targetDs.getDataSetList(), MAPPER);
        Assertions.assertEquals(expected, actual);
    }

    private DataSet testDs() {
        //do main ds
        VisibilityArea va = CREATE.va("va1");
        DataSet ds1 = CREATE.ds(va, "dsl1", "dsl1_ds1");
        Parameter param1 = CREATE.textParam(ds1, "default", "text");
        ds1.getParameters().add(param1);
        //do inner ds
        DataSet ds2 = CREATE.ds(va, "dsl2", "dsl2_ds1");
        Parameter ds2Param = CREATE.textParam(ds2, "default", "text2");
        Parameter ds2TextParam2 = CREATE.textParam(ds2, "overridden", "should be overridden");
        ds2.getParameters().add(ds2TextParam2);
        ds2.getParameters().add(ds2Param);
        //place inner ds into main ds
        Parameter group = CREATE.refParam(ds1, "group", ds2);
        ds1.getParameters().add(group);
        //override inner ds text value
        Parameter text3 = CREATE.overrideParam(ds1, ds2TextParam2.getAttribute(),
                "text3", null, null,
                null, group.getAttribute());
        ds1.getParameters().add(text3);
        return ds1;
    }
}

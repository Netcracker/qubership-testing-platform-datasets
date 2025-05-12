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

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.qubership.atp.dataset.JsonMatcher;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.ListValue;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;

@ExtendWith(SpringExtension.class)
public class UiSerializationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final CreationFacade CREATE = SimpleCreationFacade.INSTANCE;

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("Ds ref parameter missing", concreteAddressNotSpecified()),
                Arguments.of("Ds ref missing in parameter", concreteAddressNotSpecifiedInParam()),
                Arguments.of("Ds ref parameter missing, but an overlap exists",
                        concreteAddressSpecifiedByOverlapWithNoDefault()),
                Arguments.of("Default ds ref is overlapped to a new value", defaultStreetRefIsOverlapped()));
    }

    private static UiDslTestData concreteAddressNotSpecified() {
        VisibilityArea va = CREATE.va("va");
        DataSet customer1 = CREATE.ds(va, "Customer", "Customer1");
        DataSet addr1 = CREATE.ds(va, "Address", "Address1");
        Parameter streetParam = CREATE.textParam(addr1, "Street", "Polevaya");
        Attribute custToAddr = CREATE.refAttr(customer1.getDataSetList(), "Address", addr1.getDataSetList());
        String erJsonStreet = "{\"id\":\"${attr: Street}\",\"name\":\"Street\",\"type\":\"TEXT\"}";
        String erJsonAddress = "{\"id\":\"${attr: Address}\",\"name\":\"Address\",\"type\":\"DSL\",\"dataSetListReference\":\"${dsl: Address}\",\"attributes\":["
                + erJsonStreet + "]}";
        String erJsonDsl = "{\"id\":\"${dsl: Customer}\",\"name\":\"Customer\",\"dataSets\":[{\"id\":\"${ds: "
                + "Customer1}\",\"name\":\"Customer1\",\"locked\":false}],\"attributes\":["
                + erJsonAddress + "]}";
        UiDslTestData td = new UiDslTestData(customer1.getDataSetList(), erJsonDsl);
        UiAttrPathTestData addressTd = new UiAttrPathTestData(customer1, erJsonAddress);
        addressTd.attrPath.add(custToAddr.getId());
        td.uiAttrPathTestData.add(addressTd);
        UiAttrPathTestData streetTd = new UiAttrPathTestData(customer1, erJsonStreet);
        streetTd.attrPath.add(custToAddr.getId());
        streetTd.attrPath.add(streetParam.getAttribute().getId());
        td.uiAttrPathTestData.add(streetTd);
        return td;
    }

    private static UiDslTestData concreteAddressNotSpecifiedInParam() {
        VisibilityArea va = CREATE.va("va");
        DataSet customer1 = CREATE.ds(va, "Customer", "Customer1");
        DataSet addr1 = CREATE.ds(va, "Address", "Address1");
        Parameter streetParam = CREATE.textParam(addr1, "Street", "Polevaya");
        Attribute custToAddr = CREATE.refAttr(customer1.getDataSetList(), "Address", addr1.getDataSetList());
        CREATE.refParam(customer1, custToAddr, null);
        String erJsonStreet = "{\"id\":\"${attr: Street}\",\"name\":\"Street\",\"type\":\"TEXT\"}";
        String erJsonAddress = "{\"id\":\"${attr: Address}\",\"name\":\"Address\",\"type\":\"DSL\",\"dataSetListReference\":\"${dsl: Address}\",\"attributes\":["
                + erJsonStreet + "]}";
        String erJsonDsl = "{\"id\":\"${dsl: Customer}\",\"name\":\"Customer\",\"dataSets\":[{\"id\":\"${ds: Customer1}\",\"name\":\"Customer1\",\"locked\":false}],\"attributes\":["
                + erJsonAddress + "]}";
        UiDslTestData td = new UiDslTestData(customer1.getDataSetList(), erJsonDsl);
        UiAttrPathTestData addressTd = new UiAttrPathTestData(customer1, erJsonAddress);
        addressTd.attrPath.add(custToAddr.getId());
        td.uiAttrPathTestData.add(addressTd);
        UiAttrPathTestData streetTd = new UiAttrPathTestData(customer1, erJsonStreet);
        streetTd.attrPath.add(custToAddr.getId());
        streetTd.attrPath.add(streetParam.getAttribute().getId());
        td.uiAttrPathTestData.add(streetTd);
        return td;
    }

    private static UiDslTestData concreteAddressSpecifiedByOverlapWithNoDefault() {
        VisibilityArea va = CREATE.va("va");
        DataSet customer1 = CREATE.ds(va, "Customer", "Customer1");
        DataSet addr1 = CREATE.ds(va, "Address", "Address1");
        Parameter streetParam = CREATE.textParam(addr1, "Street", "Polevaya");
        Attribute custToAddr = CREATE.refAttr(customer1.getDataSetList(), "Address", addr1.getDataSetList());
        CREATE.overrideParam(customer1, streetParam.getAttribute(), "Moskovskaya", null, null, null, custToAddr);
        String erJsonStreet = "{\"id\":\"${attr: Street}\",\"name\":\"Street\",\"type\":\"TEXT\",\"parameters\":[{\"dataSet\":\"${ds: Customer1}\",\"value\":\"Moskovskaya\",\"overlap\":true}]}";
        String erJsonAddress = "{\"id\":\"${attr: Address}\",\"name\":\"Address\",\"type\":\"DSL\",\"dataSetListReference\":\"${dsl: Address}\",\"attributes\":["
                + erJsonStreet + "]}";
        String erJsonDsl = "{\"id\":\"${dsl: Customer}\",\"name\":\"Customer\",\"dataSets\":[{\"id\":\"${ds: Customer1}\",\"name\":\"Customer1\",\"locked\":false}],\"attributes\":["
                + erJsonAddress + "]}";
        UiDslTestData td = new UiDslTestData(customer1.getDataSetList(), erJsonDsl);
        UiAttrPathTestData addressTd = new UiAttrPathTestData(customer1, erJsonAddress);
        addressTd.attrPath.add(custToAddr.getId());
        td.uiAttrPathTestData.add(addressTd);
        UiAttrPathTestData streetTd = new UiAttrPathTestData(customer1, erJsonStreet);
        streetTd.attrPath.add(custToAddr.getId());
        streetTd.attrPath.add(streetParam.getAttribute().getId());
        td.uiAttrPathTestData.add(streetTd);
        return td;
    }

    private static UiDslTestData defaultStreetRefIsOverlapped() {
        VisibilityArea va = CREATE.va("va");
        DataSet customer = CREATE.ds(va, "Customer", "Customer1");
        DataSet addr = CREATE.ds(va, "Address", "Address1");
        Parameter custToAddr = CREATE.refParam(customer, "Address", addr);
        DataSet street1 = CREATE.ds(va, "Street", "Street1");
        Parameter streetName1 = CREATE.textParam(street1, "Name", "Polevaya");
        Parameter addrToStreet = CREATE.refParam(addr, "Street", street1);
        DataSet street2 = CREATE.ds(street1.getDataSetList(), "Street2");
        CREATE.textParam(street2, streetName1.getAttribute(), "Moskovskaya");
        CREATE.overrideParam(customer, addrToStreet.getAttribute(), null, null, street2, null, custToAddr
                .getAttribute());
        String erJsonStreetName = "{\"id\":\"${attr: Name}\",\"name\":\"Name\",\"type\":\"TEXT\",\"parameters\":[{\"dataSet\":\"${ds: Customer1}\",\"value\":\"Moskovskaya\",\"overlap\":false}]}";
        String erJsonStreet = "{\"id\":\"${attr: Street}\",\"name\":\"Street\",\"type\":\"DSL\",\"dataSetListReference\":\"${dsl: Street}\",\"parameters\":[{\"dataSet\":\"${ds: Customer1}\",\"value\":\"Street2\",\"valueRef\":\"${ds: Street2}\",\"overlap\":true}],\"attributes\":["
                + erJsonStreetName + "]}";
        String erJsonAddress = "{\"id\":\"${attr: Address}\",\"name\":\"Address\",\"type\":\"DSL\",\"dataSetListReference\":\"${dsl: Address}\",\"parameters\":[{\"dataSet\":\"${ds: Customer1}\",\"value\":\"Address1\",\"valueRef\":\"${ds: Address1}\",\"overlap\":false}],\"attributes\":["
                + erJsonStreet + "]}";
        String erJsonDsl = "{\"id\":\"${dsl: Customer}\",\"name\":\"Customer\",\"dataSets\":[{\"id\":\"${ds: Customer1}\",\"name\":\"Customer1\",\"locked\":false}],\"attributes\":["
                + erJsonAddress + "]}";
        UiDslTestData td = new UiDslTestData(customer.getDataSetList(), erJsonDsl);
        UiAttrPathTestData addressTd = new UiAttrPathTestData(customer, erJsonAddress);
        addressTd.attrPath.add(custToAddr.getAttribute().getId());
        td.uiAttrPathTestData.add(addressTd);
        UiAttrPathTestData streetTd = new UiAttrPathTestData(customer, erJsonStreet);
        streetTd.attrPath.add(custToAddr.getAttribute().getId());
        streetTd.attrPath.add(addrToStreet.getAttribute().getId());
        td.uiAttrPathTestData.add(streetTd);
        UiAttrPathTestData streetNameTd = new UiAttrPathTestData(customer, erJsonStreetName);
        streetNameTd.attrPath.add(custToAddr.getAttribute().getId());
        streetNameTd.attrPath.add(addrToStreet.getAttribute().getId());
        streetNameTd.attrPath.add(streetName1.getAttribute().getId());
        td.uiAttrPathTestData.add(streetNameTd);
        return td;
    }

    private static String replaceIds(Identified source, String jsonTarget) {
        Iterator<Identified> items = Utils.allRefs(source).iterator();
        while (items.hasNext()) {
            Identified next = items.next();
            String id = next.getId().toString();
            String name;
            if (next instanceof Named) {
                name = ((Named) next).getName();
            } else {
                name = "not_supported";
            }
            String type;
            if (next instanceof DataSetList) {
                type = "dsl";
            } else if (next instanceof Attribute) {
                type = "attr";
            } else if (next instanceof DataSet) {
                type = "ds";
            } else if (next instanceof Parameter) {
                type = "param";
            } else if (next instanceof ListValue) {
                type = "lv";
            } else {
                type = next.getClass().getSimpleName();
            }
            jsonTarget = jsonTarget.replaceAll(id, String.format("\\$\\{%s: %s\\}", type, name));
        }
        return jsonTarget;
    }

    private static void assertEquals(Identified source, Object arConverted, String erJson) throws JsonProcessingException {
        String arJson = replaceIds(source, OBJECT_MAPPER.writeValueAsString(arConverted));
        assertThat(arJson, JsonMatcher.is(erJson));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("data")
    public void dslSerialization(String str, UiDslTestData uiDslTestData) throws JsonProcessingException {
        UiManDataSetList converted = Utils.doUiDs(uiDslTestData.dsl, EvaluatorMock.INSTANCE, true);
        assertEquals(uiDslTestData.dsl, converted, uiDslTestData.erJson);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("data")
    public void attrPathSerialization(String str, UiDslTestData uiDslTestData) throws IOException {
        for (UiAttrPathTestData data : uiDslTestData.uiAttrPathTestData) {
            UiManAttribute converted = Utils.doUiAttr(data.ds, EvaluatorMock.INSTANCE, data.attrPath);
            assertEquals(data.ds, converted, data.erJson);
        }
    }

    private static class UiDslTestData {

        private final DataSetList dsl;
        private final String erJson;
        private final List<UiAttrPathTestData> uiAttrPathTestData = Lists.newArrayList();

        private UiDslTestData(@Nonnull DataSetList dsl, @Nonnull String erJson) {
            this.dsl = dsl;
            this.erJson = erJson;
        }
    }

    private static class UiAttrPathTestData {

        private final DataSet ds;
        private final String erJson;
        private final List<UUID> attrPath = Lists.newArrayList();

        private UiAttrPathTestData(@Nonnull DataSet ds, @Nonnull String erJson) {
            this.ds = ds;
            this.erJson = erJson;
        }
    }
}

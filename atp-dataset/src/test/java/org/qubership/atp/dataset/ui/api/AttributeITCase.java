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

package org.qubership.atp.dataset.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Identified;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.SQLQueryFactory;

@Isolated
public class AttributeITCase extends AbstractApiIntegrationCase {

    @Autowired
    private SQLQueryFactory queryFactory;

    @Test
    public void getEagerByParentId_getAttributeListByDslId_VerifyThatOnlyOneSqlSelectWasExecuted() {
        TestData testData = createTestDataInstance(TestData::new);
        Mockito.reset(queryFactory);
        List<AttributePojo> attributesActual =
                baseUrl.path("/attribute/dsl").path(testData.ds.getDataSetList().getId().toString())
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(new GenericType<List<AttributePojo>>() {
                        });
        Mockito.verify(queryFactory).select((Expression<?>[]) any());
    }

    @Test
    public void getEagerByParentId_getAttributeListByDslId_CheckThatAttributesHasExpectedSortedListValues() {
        TestData testData = createTestDataInstance(TestData::new);
        Mockito.reset(queryFactory);
        List<AttributePojo> attributesActual =
                baseUrl.path("/attribute/dsl").path(testData.ds.getDataSetList().getId().toString())
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(new GenericType<List<AttributePojo>>() {
                        });
        List<AttributePojo> attributesExpected = getAttributePojosWithSortedListValues(testData);
        assertEquals(attributesExpected, attributesActual);
    }

    private List<AttributePojo> getAttributePojosWithSortedListValues(TestData testData) {
        List<ListValuePojo> listValuePojos = testData.attr.getListValues().stream()
                .map(ListValuePojo::convert).sorted(Comparator.comparing(ListValuePojo::getName))
                .collect(Collectors.toList());
        List<UUID> parametersUUIDs = testData.attr.getParameters().stream()
                .map(Identified::getId)
                .collect(Collectors.toList());
        AttributePojo attributePojo = new AttributePojo(testData.attr.getId(), testData.attr.getName(),
                testData.dsl.getId(), testData.attr.getType(), null,
                listValuePojos, parametersUUIDs);
        List<AttributePojo> attributePojos = new ArrayList<>();
        attributePojos.add(attributePojo);
        return attributePojos;
    }

    private static class TestData implements Supplier<VisibilityArea> {

        final VisibilityArea va;
        final DataSetList dsl;
        final DataSet ds;
        final Parameter param;
        final Attribute attr;

        TestData(CreationFacade create) {
            va = create.va("ATPII-4713");
            dsl = create.dsl(va, "DataSetList");
            ds = create.ds(dsl, "DataSet");
            attr = create.listAttr(dsl, "Attribute", "selectedLV", "LV1", "LV2");
            param = create.listParam(ds, attr, "selectedLV");
        }

        @Override
        public VisibilityArea get() {
            return va;
        }
    }
}

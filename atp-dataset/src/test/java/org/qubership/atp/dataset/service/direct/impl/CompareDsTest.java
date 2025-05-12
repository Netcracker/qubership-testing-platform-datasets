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

package org.qubership.atp.dataset.service.direct.impl;

import java.util.UUID;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.db.jpa.ModelsProvider;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsRequest;
import org.qubership.atp.dataset.model.api.DetailedComparisonDsResponse;
import org.qubership.atp.dataset.model.enums.CompareStatus;
import org.qubership.atp.dataset.model.enums.DetailedComparisonStatus;
import org.qubership.atp.dataset.model.impl.ComparedAttribute;
import org.qubership.atp.dataset.service.direct.EncryptionService;
import org.qubership.atp.dataset.service.direct.GridFsService;
import org.qubership.atp.dataset.service.jpa.model.AttributeTypeName;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {
        "feign.atp.macros.url=",
        "feign.atp.macros.route=",
        "atp-dataset.javers.enabled=false"
})
public class CompareDsTest extends DataSetBuilder {

    @SpyBean
    CompareDatasetServiceImpl compareDs;
    @Autowired
    protected ModelsProvider modelsProvider;
    @SpyBean
    GridFsService gridFsService;
    @SpyBean
    EncryptionService encryptionService;

    private UUID leftDs = UUID.fromString("527b9023-b6a8-4a64-99c1-af80b94449d2");
    private UUID identicalDs = UUID.fromString("1919170b-5733-4a9a-b67d-80a6ca027d7d");
    private UUID oneParamDs = UUID.fromString("d450e596-1910-4ad4-8c94-61c576296e4d");
    private UUID diffAttrTypeDs = UUID.fromString("23be800d-dd13-4ead-ae82-883a4a228a92");
    private UUID diffAttrNameDs = UUID.fromString("e8fe4a2b-815c-46ba-b32f-13a63204eb8a");
    private UUID diffStringValueDs = UUID.fromString("fa11fd87-4461-4908-9069-f0c2f4a2d058");
    private UUID diffDslValueDs = UUID.fromString("1ff48d2c-1819-4682-afc1-a1092a198929");
    private UUID diffListValueDs = UUID.fromString("6ab9a777-3b0d-478c-8340-4b12843342e7");
    private UUID leftDsNoParameters = UUID.fromString("3e2d9226-453a-452d-999e-f060627f8c6c");
    private UUID rightDsNoParameters = UUID.fromString("9b3d09e7-2ddf-4381-a711-187174d7ad73");
    @Test
    @Transactional
    public void compare_IdenticalDs_statusOk() {
        UUID left = UUID.fromString("e72db491-a854-469d-aec3-63669d13d15d");
        UUID right = UUID.fromString("47144c96-44f2-4875-96ad-a3db9939f841");

        CompareStatus status = compareDs.compare(left, right);

        Assertions.assertEquals(CompareStatus.OK, status);
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void compare_secondDsHaveOneParam_statusWarning() {
        CompareStatus status = compareDs.compare(leftDs, oneParamDs);

        Assertions.assertEquals(CompareStatus.WARNING, status);
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void compare_SameCountParametersDifferentType_statusWarning() {
        CompareStatus status = compareDs.compare(leftDs, diffAttrTypeDs);

        Assertions.assertEquals(CompareStatus.WARNING, status);
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void compare_SameCountParametersDifferentAttributeName_statusWarning() {
        CompareStatus status = compareDs.compare(leftDs, diffAttrNameDs);

        Assertions.assertEquals(CompareStatus.WARNING, status);
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void compare_SameCountParametersDiffValueText_statusWarning() {
        CompareStatus status = compareDs.compare(leftDs, diffStringValueDs);

        Assertions.assertEquals(CompareStatus.WARNING, status);
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void compare_SameCountParametersDiffValueRefDsName_statusWarning() {
        CompareStatus status = compareDs.compare(leftDs, diffDslValueDs);

        Assertions.assertEquals(CompareStatus.WARNING, status);
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void compare_SameCountParametersDiffListValue_statusWarning() {
        CompareStatus status = compareDs.compare(leftDs, diffListValueDs);

        Assertions.assertEquals(CompareStatus.WARNING, status);
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void detailedComparison_IdenticalDs() {
        DetailedComparisonDsResponse result = compareDs.detailedComparison(
                new DetailedComparisonDsRequest(leftDs, identicalDs, 0, 3));

        Assertions.assertEquals(3, result.getTotal());
        ComparedAttribute firstAttr = result.getAttributes().get(0);
        ComparedAttribute secondAttr = result.getAttributes().get(1);
        ComparedAttribute thirdAttr = result.getAttributes().get(2);
        Assertions.assertEquals(DetailedComparisonStatus.EQUAL, firstAttr.getStatus());
        Assertions.assertEquals(DetailedComparisonStatus.EQUAL, secondAttr.getStatus());
        Assertions.assertEquals(DetailedComparisonStatus.EQUAL, thirdAttr.getStatus());
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void detailedComparison_secondDsHaveOneParam() {
        DetailedComparisonDsResponse result = compareDs.detailedComparison(
                new DetailedComparisonDsRequest(leftDs, oneParamDs, 0, 3));

        Assertions.assertEquals(3, result.getTotal());
        ComparedAttribute firstAttr = result.getAttributes().get(0);
        Assertions.assertEquals(DetailedComparisonStatus.NOT_EQUAL, firstAttr.getStatus());
        Assertions.assertNotNull(firstAttr.getLeftAttributeId());
        Assertions.assertNull(firstAttr.getRightAttributeId());

        ComparedAttribute secondAttr = result.getAttributes().get(1);
        Assertions.assertEquals(DetailedComparisonStatus.NOT_EQUAL, secondAttr.getStatus());
        Assertions.assertNotNull(secondAttr.getLeftAttributeId());
        Assertions.assertNull(secondAttr.getRightAttributeId());

        ComparedAttribute thirdAttr = result.getAttributes().get(2);
        Assertions.assertEquals(DetailedComparisonStatus.EQUAL, thirdAttr.getStatus());
        Assertions.assertNotNull(thirdAttr.getLeftAttributeId());
        Assertions.assertNotNull(thirdAttr.getRightAttributeId());
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void detailedComparison_SameCountParametersDifferentType() {
        DetailedComparisonDsResponse result = compareDs.detailedComparison(
                new DetailedComparisonDsRequest(leftDs, diffAttrTypeDs, 0, 3));

        Assertions.assertEquals(3, result.getTotal());
        // Attributes with same name have different types
        ComparedAttribute firstAttr = result.getAttributes().get(0);
        Assertions.assertEquals(DetailedComparisonStatus.INCOMPATIBLE_TYPES, firstAttr.getStatus());
        Assertions.assertNotNull(firstAttr.getLeftAttributeId());
        Assertions.assertNotNull(firstAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.DSL, firstAttr.getLeftAttributeType());
        Assertions.assertEquals(AttributeTypeName.TEXT, firstAttr.getRightAttributeType());

        ComparedAttribute secondAttr = result.getAttributes().get(1);
        Assertions.assertEquals(DetailedComparisonStatus.EQUAL, secondAttr.getStatus());
        Assertions.assertNotNull(secondAttr.getLeftAttributeId());
        Assertions.assertNotNull(secondAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.LIST, secondAttr.getLeftAttributeType());
        Assertions.assertEquals(AttributeTypeName.LIST, secondAttr.getRightAttributeType());

        ComparedAttribute thirdAttr = result.getAttributes().get(2);
        Assertions.assertEquals(DetailedComparisonStatus.EQUAL, thirdAttr.getStatus());
        Assertions.assertNotNull(thirdAttr.getLeftAttributeId());
        Assertions.assertNotNull(thirdAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.TEXT, thirdAttr.getLeftAttributeType());
        Assertions.assertEquals(AttributeTypeName.TEXT, thirdAttr.getRightAttributeType());
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void detailedComparison_SameCountParametersDifferentAttributeName() {
        DetailedComparisonDsResponse result = compareDs.detailedComparison(
                new DetailedComparisonDsRequest(leftDs, diffAttrNameDs, 0, 5));

        Assertions.assertEquals(5, result.getTotal());

        // Attribute not found in left dataset
        ComparedAttribute firstAttr = result.getAttributes().get(0);
        Assertions.assertEquals(DetailedComparisonStatus.NOT_EQUAL, firstAttr.getStatus());
        Assertions.assertNull(firstAttr.getLeftAttributeId());
        Assertions.assertNotNull(firstAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.TEXT, firstAttr.getRightAttributeType());

        // Attribute not found in left dataset
        ComparedAttribute secondAttr = result.getAttributes().get(1);
        Assertions.assertEquals(DetailedComparisonStatus.NOT_EQUAL, secondAttr.getStatus());
        Assertions.assertNull(secondAttr.getLeftAttributeId());
        Assertions.assertNotNull(secondAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.DSL, secondAttr.getRightAttributeType());

        // Attribute not found in rightDataset
        ComparedAttribute thirdAttr = result.getAttributes().get(2);
        Assertions.assertEquals(DetailedComparisonStatus.NOT_EQUAL, thirdAttr.getStatus());
        Assertions.assertNotNull(thirdAttr.getLeftAttributeId());
        Assertions.assertNull(thirdAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.DSL, thirdAttr.getLeftAttributeType());

        // Different values
        ComparedAttribute fourthAttr = result.getAttributes().get(3);
        Assertions.assertEquals(DetailedComparisonStatus.NOT_EQUAL, fourthAttr.getStatus());
        Assertions.assertNotNull(fourthAttr.getLeftAttributeId());
        Assertions.assertNotNull(fourthAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.LIST, fourthAttr.getLeftAttributeType());
        Assertions.assertEquals(AttributeTypeName.LIST, fourthAttr.getRightAttributeType());
        Assertions.assertNotEquals(fourthAttr.getLeftAttributeValue(), fourthAttr.getRightAttributeType());

        // Attribute not found in right dataset
        ComparedAttribute fifthAttr = result.getAttributes().get(4);
        Assertions.assertEquals(DetailedComparisonStatus.NOT_EQUAL, fifthAttr.getStatus());
        Assertions.assertNotNull(fifthAttr.getLeftAttributeId());
        Assertions.assertNull(fifthAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.TEXT, fifthAttr.getLeftAttributeType());
    }

    @Test
    @Sql(scripts = {"classpath:test_data/sql/compare_ds/cleanDb.sql",
            "classpath:test_data/sql/compare_ds/compareDs.sql"})
    @Transactional
    public void detailedComparison_NoParametersButAttributes() {
        DetailedComparisonDsResponse result = compareDs.detailedComparison(
                new DetailedComparisonDsRequest(leftDsNoParameters, rightDsNoParameters, 0, 5));

        Assertions.assertEquals(2, result.getTotal());

        ComparedAttribute firstAttr = result.getAttributes().get(0);
        Assertions.assertEquals(DetailedComparisonStatus.EQUAL, firstAttr.getStatus());
        Assertions.assertNotNull(firstAttr.getLeftAttributeId());
        Assertions.assertNotNull(firstAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.TEXT, firstAttr.getLeftAttributeType());
        Assertions.assertEquals(AttributeTypeName.TEXT, firstAttr.getLeftAttributeType());
        Assertions.assertNull(firstAttr.getLeftAttributeValue());
        Assertions.assertNull(firstAttr.getRightAttributeValue());

        ComparedAttribute secondAttr = result.getAttributes().get(1);
        Assertions.assertEquals(DetailedComparisonStatus.INCOMPATIBLE_TYPES, secondAttr.getStatus());
        Assertions.assertNotNull(secondAttr.getLeftAttributeId());
        Assertions.assertNotNull(secondAttr.getRightAttributeId());
        Assertions.assertEquals(AttributeTypeName.TEXT, secondAttr.getLeftAttributeType());
        Assertions.assertEquals(AttributeTypeName.FILE, secondAttr.getRightAttributeType());
        Assertions.assertNull(secondAttr.getLeftAttributeValue());
        Assertions.assertNull(secondAttr.getRightAttributeValue());
    }

}

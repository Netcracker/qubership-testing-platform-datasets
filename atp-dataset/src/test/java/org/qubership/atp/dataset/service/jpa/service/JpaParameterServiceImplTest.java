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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.service.direct.impl.ClearCacheServiceImpl;
import org.qubership.atp.dataset.service.jpa.DataSetServiceException;
import org.qubership.atp.dataset.service.jpa.delegates.Parameter;

//@Isolated
@Disabled
@SpringBootTest
@ContextConfiguration(classes = {TestConfiguration.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class JpaParameterServiceImplTest extends AbstractJpaTest {

    @Autowired
    ClearCacheServiceImpl clearCacheService;

    private UUID visibilityAreaId = UUID.fromString("c904edfc-310d-4b01-8af2-5cc4f1d86fcd");
    private UUID dataSetList1 = UUID.fromString("b8444ae9-bb06-47b6-a895-eedd5f2f4f60");
    private UUID dataSet11 = UUID.fromString("2f27efc6-bfa0-4a00-9064-f20cb2afb3b8");
    private UUID dataSet12 = UUID.fromString("18a8b8db-4c8b-41a8-af5e-ca366dc99f91");
    private UUID attribute11 = UUID.fromString("beb2484b-fb54-44dd-84fd-d759329c163b");
    private UUID attribute12 = UUID.fromString("08582328-90b8-49c7-9ab8-e676e8b896f8");
    private UUID parameter1ds1 = UUID.fromString("4cfd2426-ad08-4d02-8501-dbcb83ce6825");
    private UUID parameter2ds2 = UUID.fromString("ccedbecc-deb6-456e-9b30-ae77ddfde157");

    @BeforeEach
    public void clearCache() throws InterruptedException {
        List<UUID> parameters = Arrays.asList(UUID.fromString("4cfd2426-ad08-4d02-8501-dbcb83ce6825"),
                UUID.fromString("57a9e15e-0cdb-4b85-ba17-d70812b0b1df"),
                UUID.fromString("210fb6ac-08dc-4e47-bfd7-2ae980bcf586"),
                UUID.fromString("ccedbecc-deb6-456e-9b30-ae77ddfde157"),
                UUID.fromString("fe2737d8-0a20-4a43-85ac-bea033a0396c"));
        parameters.forEach(param -> clearCacheService.evictParameterCache(param));

    }

    @Test()
    @Sql(scripts = "classpath:test_data/sql/jpa_parameter_service_impl_test/JpaParameterServceImplTest.sql")
    public void bulkUpdateValues_datasetLockedTrue_NotUpdateValuesThrowException() throws DataSetServiceException {
        dataSetService.lock(dataSetList1, Collections.singletonList(dataSet11), true);
        String updateValue = "txtUpdateValue";

        assertThrows(IllegalArgumentException.class, () ->
                parameterService.bulkUpdateValues(updateValue, null, null,
                        dataSetList1, null, Arrays.asList(parameter1ds1, parameter2ds2)));
        Assertions.assertNotEquals(updateValue, parameterService.getById(parameter1ds1).getStringValue());
        Assertions.assertNotEquals(updateValue, parameterService.getById(parameter2ds2).getStringValue());
        Assertions.assertEquals("Value 1", parameterService.getById(parameter1ds1).getStringValue());
        Assertions.assertEquals("Value 2", parameterService.getById(parameter2ds2).getStringValue());
    }

    @Test()
    @Sql(scripts = "classpath:test_data/sql/jpa_parameter_service_impl_test/JpaParameterServceImplTest.sql")
    public void bulkUpdateValues_datasetLockFalse_UpdateValuesSuccessfully() throws DataSetServiceException {
        String updateValue = "txtUpdateValue";

        Assertions.assertDoesNotThrow(() ->
                parameterService.bulkUpdateValues(updateValue, null, null,
                        dataSetList1, null, Arrays.asList(parameter1ds1, parameter2ds2)));

        Assertions.assertEquals(updateValue, parameterService.getById(parameter1ds1).getStringValue());
        Assertions.assertEquals(updateValue, parameterService.getById(parameter2ds2).getStringValue());
    }

    @Test()
    @Sql(scripts = "classpath:test_data/sql/jpa_parameter_service_impl_test/JpaParameterServceImplTest.sql")
    public void createParameter_datasetLockedTrue_NotCreatParameterThrowException() throws DataSetServiceException {
        dataSetService.lock(dataSetList1, Collections.singletonList(dataSet12), true);
        String newValue = "New text value";

        assertThrows(IllegalArgumentException.class, () ->
                parameterService.createParameter(dataSet12, attribute12,
                        newValue, null, null));

        Assertions.assertNull(parameterService.getParameterByAttributeIdAndDataSetId(attribute12, dataSet12));

    }

    @Test()
    @Sql(scripts = "classpath:test_data/sql/jpa_parameter_service_impl_test/JpaParameterServceImplTest.sql")
    public void createParameter_datasetLockedFalse_CreatTextParameterSuccessfully() throws DataSetServiceException {
        String newValue = "New text value";

        Parameter parameter = parameterService.createParameter(dataSet12, attribute12,
                newValue, null, null);

        Assertions.assertNotNull(parameterService.getParameterByAttributeIdAndDataSetId(attribute12, dataSet12));
        Assertions.assertEquals(newValue, parameter.getStringValue());
    }
}

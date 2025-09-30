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

package org.qubership.atp.dataset.service.direct;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.service.AbstractTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class ConcurrentModificationServiceTest extends AbstractTest {

    private static final String VA_NAME = "VA";
    private static final String DSL_NAME = "DSL";

    private DataSetList dataSetList;

    @Autowired
    private ConcurrentModificationService concurrentModificationService;

    @BeforeEach
    public void setUp() {
        va = visibilityAreaService.create(VA_NAME);
        dataSetList = dataSetListService.create(va.getId(), DSL_NAME, null);
    }

    /**
     * Should return {@link HttpStatus} OK when the input parameter entityId is null.
     */
    @Test
    public void testGetHttpStatus_shouldReturnsOk_whenEntityIdIsNull() {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(null, currentTimeMillis());
        assertThat(httpStatus, equalTo(HttpStatus.OK));
    }

    /**
     * Should return {@link HttpStatus} OK when the input parameter modifiedWhen is null.
     */
    @Test
    public void testGetHttpStatus_shouldReturnsOk_whenModifiedWhenIsNull() {
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(randomUUID(), null);
        assertThat(httpStatus, equalTo(HttpStatus.OK));
    }

    /**
     * Should return {@link HttpStatus} OK when the input parameter modifiedWhen is not null and
     * this parameter is equal to dataSetList modifiedWhen.
     */
    @Test
    public void testGetHttpStatus_shouldReturnsOk_whenModifiedWhenIsNotNullAndEquals() {
        HttpStatus httpStatus =
            concurrentModificationService.getHttpStatus(dataSetList.getId(), dataSetList.getModifiedWhen().getTime());
        assertThat(httpStatus, equalTo(HttpStatus.OK));
    }

    /**
     * Should return {@link HttpStatus} IM_USED when the input parameter modifiedWhen is not null and
     * this parameter is not equal to dataSetList modifiedWhen.
     */
    @Test
    public void testGetHttpStatus_shouldReturnsImUsed_whenModifiedWhenIsNotNullAndNotEquals()
            throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(500);
        HttpStatus httpStatus = concurrentModificationService.getHttpStatus(dataSetList.getId(), currentTimeMillis());
        assertThat(httpStatus, equalTo(HttpStatus.IM_USED));
    }
}

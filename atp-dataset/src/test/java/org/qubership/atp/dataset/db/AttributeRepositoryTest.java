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

package org.qubership.atp.dataset.db;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.exception.dataset.DataSetExistsException;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManAttribute;
import org.qubership.atp.dataset.service.ws.entities.Pair;
import lombok.SneakyThrows;

@Isolated
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {"atp-dataset.javers.enabled=false"})
public class AttributeRepositoryTest extends AbstractTest {
    private static final long DEFAULT_TIME_FOR_CREATE_ATTR = 1576741274989L;
    private static final long DEFAULT_TIME_FOR_UPDATE_ATTR = 1576741383850L;

    private Attribute attribute1;
    private Attribute attribute2;
    private Attribute attribute3;
    private VisibilityArea visibilityArea;
    private DataSetList testDsl;

    @Autowired
    private AttributeRepository repo;

    @BeforeEach
    @SneakyThrows(DataSetExistsException.class)
    public void setUp() {
        visibilityArea = visibilityAreaService.create("TestVA");
        testDsl = dataSetListService.create(visibilityArea.getId(), "TestDsl", null);
        DataSet dataSet = dataSetService.create(testDsl.getId(), "ds1");

        attribute1 = attributeService
                .create(testDsl.getId(), (int) ((DEFAULT_TIME_FOR_CREATE_ATTR - 30000) / 1000),
                        "TestAttr1", AttributeType.TEXT, null,
                        null);
        attribute2 = attributeService
                .create(testDsl.getId(), (int) ((DEFAULT_TIME_FOR_CREATE_ATTR - 20000) / 1000),
                        "TestAttr2", AttributeType.TEXT, null,
                        null);
        attribute3 = attributeService
                .create(testDsl.getId(), (int) ((DEFAULT_TIME_FOR_CREATE_ATTR - 10000) / 1000),
                        "TestAttr3", AttributeType.TEXT, null,
                        null);

        Parameter parameter1 = parameterService.create(dataSet.getId(), attribute1.getId(),
                "1", null, null);
        Parameter parameter2 = parameterService.create(dataSet.getId(), attribute2.getId(),
                "2", null, null);
        Parameter parameter3 = parameterService.create(dataSet.getId(), attribute3.getId(),
                "3", null, null);
    }

    @Test
    public void updateOrdering_ShouldBeValid() {
        List<Pair<UUID, Integer>> attributesOrdering = generatePairs();
        List<UiManAttribute> attributesBefore = dataSetListService.getAsTree(testDsl.getId(), false).getAttributes();
        repo.updateOrdering(attributesOrdering);
        List<UiManAttribute> attributesAfter = dataSetListService.getAsTree(testDsl.getId(), false).getAttributes();
        Assertions.assertNotEquals(attributesBefore, attributesAfter, "The new ordering is different from the old");
        Assertions.assertEquals(attribute1.getId(), attributesAfter.get(2).getId(),
                "Third element of new list of attr-s is TestAttr1");
        Assertions.assertEquals(attribute3.getId(), attributesAfter.get(1).getId(),
                "Second element of new list of attr-s is TestAttr3");
        Assertions.assertEquals(attribute2.getId(), attributesAfter.get(0).getId(),
                "First element of new list of attr-s is TestAttr2");

    }

    private List<Pair<UUID, Integer>> generatePairs() {
        Pair<UUID, Integer> pair1 = new Pair<>();
        pair1.setFirst(attribute1.getId());
        pair1.setSecond((int) ((DEFAULT_TIME_FOR_UPDATE_ATTR + 30000) / 1000));

        Pair<UUID, Integer> pair2 = new Pair<>();
        pair2.setFirst(attribute2.getId());
        pair2.setSecond((int) ((DEFAULT_TIME_FOR_UPDATE_ATTR + 10000) / 1000));

        Pair<UUID, Integer> pair3 = new Pair<>();
        pair3.setFirst(attribute3.getId());
        pair3.setSecond((int) ((DEFAULT_TIME_FOR_UPDATE_ATTR + 20000) / 1000));

        return Arrays.asList(pair1, pair2, pair3);
    }

    @After
    public void tearDown() throws Exception {
        visibilityAreaService.delete(visibilityArea.getId());
    }
}

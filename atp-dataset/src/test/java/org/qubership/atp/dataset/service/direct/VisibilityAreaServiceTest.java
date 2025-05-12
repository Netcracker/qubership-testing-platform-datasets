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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.Attribute;
import org.qubership.atp.dataset.model.AttributeType;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.DataSetList;
import org.qubership.atp.dataset.model.Parameter;
import org.qubership.atp.dataset.model.VisibilityArea;
import org.qubership.atp.dataset.service.AbstractTest;
@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
@TestPropertySource(properties = {
        "atp-dataset.javers.enabled=false"
})
public class VisibilityAreaServiceTest extends AbstractTest {

    @Test
    public void testCascadeDeleteVisibilityArea() throws DuplicateKeyException {
        VisibilityArea area = visibilityAreaService.create("V");
        DataSetList dsl = dataSetListService.create(area.getId(), "DSL", null);
        DataSet ds = dataSetService.create(dsl.getId(), "DS");
        Attribute attribute = attributeService
                .create(dsl.getId(), 0, "A", AttributeType.TEXT, null, null);
        Parameter parameter = parameterService
                .create(ds.getId(), attribute.getId(), "Text", null, null);
        visibilityAreaService.delete(area.getId());
        Assertions.assertFalse(visibilityAreaService.getAll().contains(area));
        Assertions.assertNull(dataSetListService.get(dsl.getId()));
        Assertions.assertNull(dataSetService.get(ds.getId()));
        Assertions.assertNull(attributeService.get(attribute.getId()));
        Assertions.assertNull(parameterService.get(parameter.getId()));
    }
}

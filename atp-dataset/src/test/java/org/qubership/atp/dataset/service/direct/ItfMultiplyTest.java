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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.qubership.atp.dataset.JsonMatcher;
import org.qubership.atp.dataset.config.MockJaversCommitEntityServiceConfiguration;
import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.model.DataSet;
import org.qubership.atp.dataset.model.Named;
import org.qubership.atp.dataset.model.utils.EvaluatorMock;
import org.qubership.atp.dataset.model.utils.MultiplyTestData;
import org.qubership.atp.dataset.model.utils.Utils;
import org.qubership.atp.dataset.service.AbstractTest;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class, MockJaversCommitEntityServiceConfiguration.class})
public class ItfMultiplyTest extends AbstractTest {

    @Autowired
    ObjectMapper MAPPER;

    @Test
    public void dsWithMixins_SerializeForUI_ContainMixinData() throws IOException {
        MultiplyTestData d = createTestDataInstance(MultiplyTestData::new);
        UiManDataSetList actual = Utils.doUiDs(d.mix.getDataSetList(), EvaluatorMock.INSTANCE, true);
        assertThat(MAPPER.writeValueAsString(actual), JsonMatcher.isMinified(d.expectedJson));
    }

    @Test
    public void dsWithMixins_GetAllEvaluated_HasValidNames() throws IOException {
        MultiplyTestData d = createTestDataInstance(MultiplyTestData::new);
        List<DataSet> children = dataSetListService.getChildren(d.mix.getDataSetList().getId(), true);
        List<String> childrenNames = children.stream().map(Named::getName).collect(Collectors.toList());
        Assertions.assertEquals(MultiplyTestData.DS_NAMES, childrenNames);
    }

    @Test
    public void dsWithMixins_GetAllEvaluated_SerializedProperly() throws IOException {
        MultiplyTestData d = createTestDataInstance(MultiplyTestData::new);
        List<DataSet> children = dataSetListService.getChildren(d.mix.getDataSetList().getId(), true);
        DataSet ds = children.stream().filter(named -> MultiplyTestData.TEST_NAME.equals(named.getName())).findAny().get();
        ObjectNode ar = dataSetService.getInItfFormat(ds.getMixInId());
        assertThat(MAPPER.writeValueAsString(ar), JsonMatcher.isMinified(MultiplyTestData.TEST_ER));
    }

    @Test
    public void dsWithMixins_GetAllEvaluated_everyDsIsDifferent() {
        MultiplyTestData d = createTestDataInstance(MultiplyTestData::new);
        List<DataSet> children = dataSetListService.getChildren(d.mix.getDataSetList().getId(), true);
        Set<DataSet> ar = Sets.newHashSet(children);
        assertThat(children, containsInAnyOrder(ar.toArray()));
    }
}

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

import org.junit.jupiter.api.Test;

import org.qubership.atp.dataset.JsonMatcher;
import org.qubership.atp.dataset.service.direct.helper.CreationFacade;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;

public class AtpSerializationTest {
    private static final CreationFacade CREATE = SimpleCreationFacade.INSTANCE;

    @Test
    public void dataSet_GroupInGroup_SerializedAsNestedGroups() throws Exception {
        AtpDsTestData.GroupInGroup testData = new AtpDsTestData.GroupInGroup(CREATE);
        String serialized = AtpDsSerializer.writeValueAsString(testData.ds, EvaluatorMock.INSTANCE);
        assertThat(serialized, JsonMatcher.isMinified(testData.erJson));
    }

    @Test
    public void dataSet_EmptyParameters_SerializedWithoutValue() throws Exception {
        AtpDsTestData.EmptyParameters testData = new AtpDsTestData.EmptyParameters(CREATE);
        String serialized = AtpDsSerializer.writeValueAsString(testData.ds, EvaluatorMock.INSTANCE);
        assertThat(serialized, JsonMatcher.isMinified(testData.erJson));
    }

    @Test
    public void dataSet_RegularParamsBetweenGroups_ParamsAreFirstAndGroupsAreNext() throws Exception {
        AtpDsTestData.ShuffleGroups testData = new AtpDsTestData.ShuffleGroups(CREATE);
        String serialized = AtpDsSerializer.writeValueAsString(testData.ds, EvaluatorMock.INSTANCE);
        assertThat(serialized, JsonMatcher.isMinified(testData.erJson));
    }

}

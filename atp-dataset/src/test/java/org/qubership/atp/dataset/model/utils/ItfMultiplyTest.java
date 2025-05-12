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
import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.dataset.JsonMatcher;
import org.qubership.atp.dataset.service.direct.helper.SimpleCreationFacade;
import org.qubership.atp.dataset.service.rest.dto.manager.UiManDataSetList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ItfMultiplyTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void dataSetWithMixins_serializeInItfWay_shouldContainMixinData() throws IOException {
        MultiplyTestData d = new MultiplyTestData(SimpleCreationFacade.INSTANCE);
        String expectedJson = "{\n"
                + "    \"Account\": "
                + String.format("\"MULTIPLY %s %s\"", d.resCA.getId(), d.b2bCA.getId())
                + "    ,\n"
                + "    \"Subscription\": "
                + String.format("\"MULTIPLY %s %s %s\"", d.baseCheck.getId(), d.proStart.getId(), d.base.getId())
                + "    }\n"
                + "}";
        ObjectNode actual = Utils.serializeInItfWay(d.mix, MAPPER, EvaluatorMock.INSTANCE);
        assertThat(MAPPER.writeValueAsString(actual), JsonMatcher.isMinified(expectedJson));
    }

    @Test
    public void dataSetWithMixins_serializeForUI_shouldContainMixinData() throws IOException {
        MultiplyTestData d = new MultiplyTestData(SimpleCreationFacade.INSTANCE);
        UiManDataSetList actual = Utils.doUiDs(d.mix.getDataSetList(), EvaluatorMock.INSTANCE, true);
        assertThat(MAPPER.writeValueAsString(actual), JsonMatcher.isMinified(d.expectedJson));
    }

    @Test
    public void multiplication_3x3_got9() {
        List<String> combinations = Utils.combinations(ImmutableList.of(
                ImmutableList.of("a", "b", "c"),
                ImmutableList.of("d", "e", "f")),
                Collectors.joining())
                .collect(Collectors.toList());
        Assertions.assertEquals(Lists.newArrayList("ad", "ae", "af", "bd", "be", "bf", "cd", "ce", "cf"),
                combinations);
    }
}

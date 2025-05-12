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

package org.qubership.atp.dataset.macros.impl;

import static org.qubership.atp.dataset.RegexpMatcher.matchesToRegExp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;

import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;

@Isolated
@ContextConfiguration(classes = {TestMacrosConfiguration.class})
public class RandomBetweenMacrosTest extends AbstractMacrosTest {

    @Test
    public void testRandomBetweenReturnsNumericValues() throws Exception {
        String evaluate = evaluator.evaluate("#RaNdOmBeTwEeN(1,10)", new DataSetListImpl(), new DataSetImpl());
        assertThat(evaluate, matchesToRegExp("\\d+"));
    }

    @Test
    public void testRandomBetweenGeneratesValuesInSpecifiedRange() throws Exception {
        String evaluate;
        for (int count = 0; count < 100; count++) {
            evaluate = evaluator.evaluate("#RaNdOmBeTwEeN(2, 10)", new DataSetListImpl(), new DataSetImpl());
            long result = Long.parseLong(evaluate);
            assertTrue(result >= 2 && result <= 10);
        }
    }
}

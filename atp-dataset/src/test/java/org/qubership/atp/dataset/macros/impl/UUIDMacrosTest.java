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

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import org.qubership.atp.dataset.config.TestMacrosConfiguration;
import org.qubership.atp.dataset.model.impl.DataSetImpl;
import org.qubership.atp.dataset.model.impl.DataSetListImpl;

@ContextConfiguration(classes = {TestMacrosConfiguration.class})
public class UUIDMacrosTest extends AbstractMacrosTest {

    @Test
    public void testUUIDMacros() throws Exception {
        String evaluate = evaluator.evaluate("#UUID()", new DataSetListImpl(), new DataSetImpl());
        assertThat(evaluate, matchesToRegExp("([a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12})"));
    }
}

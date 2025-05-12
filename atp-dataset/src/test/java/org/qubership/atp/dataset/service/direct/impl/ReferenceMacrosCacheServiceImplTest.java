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


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.test.context.ContextConfiguration;

import org.qubership.atp.dataset.config.TestConfiguration;
import org.qubership.atp.dataset.macros.cache.Cache;
import org.qubership.atp.dataset.macros.cache.SimpleCache;
import org.qubership.atp.dataset.service.AbstractTest;

@Isolated
@ContextConfiguration(classes = {TestConfiguration.class})
public class ReferenceMacrosCacheServiceImplTest extends AbstractTest {

    @Test
    public void testCacheReturnsSameDataForOneThread() {
        Cache cache = new SimpleCache();
        UUID parameterId = UUID.randomUUID();
        String expected = "TestText";
        cache.newKey(parameterId).cacheValue(expected);
        assertEquals(expected, cache.newKey(parameterId).lookupValue());
    }
}

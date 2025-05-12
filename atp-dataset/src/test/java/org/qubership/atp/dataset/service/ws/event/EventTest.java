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

package org.qubership.atp.dataset.service.ws.event;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventTest {
    private static final Logger LOG = LoggerFactory.getLogger(EventTest.class);

    @Test
    public void test() throws JsonProcessingException {
        Event event = new Event();
        event.setEventType(EventType.SELECT);
        event.setDataSetListId("123");
        event.setSessionId("ss");
        event.setAttributeId("param");
        String s = new ObjectMapper().writeValueAsString(event);
        LOG.info(s);
    }
}

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

package org.qubership.atp.dataset.service.ws;

import org.qubership.atp.dataset.mdc.MdcField;
import org.qubership.atp.dataset.service.ws.event.Event;
import org.qubership.atp.dataset.service.ws.event.EventExecutor;
import org.qubership.atp.dataset.service.ws.event.EventExecutorImpl;
import org.qubership.atp.dataset.service.ws.event.EventType;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WebSocketDsHandler extends TextWebSocketHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketDsHandler.class);
    private static final EventExecutor executor = EventExecutorImpl.getInstance();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage messageRequest) throws Exception {
        MDC.clear();
        String message = messageRequest.getPayload();
        LOGGER.debug("Received message from: {}\nmessage: {}", session.getLocalAddress(), message);
        Event event;
        try {
            event = OBJECT_MAPPER.readValue(message, Event.class);
            MdcUtils.put(MdcField.DATA_SET_LIST_ID.toString(), event.getDataSetListId());
            MdcUtils.put(MdcField.DATA_SET_ID.toString(), event.getDatasetId());
            MdcUtils.put(MdcField.ATTRIBUTE_ID.toString(), event.getAttributeId());
            executor.execute(session, event);
        } catch (Throwable e) {
            LOGGER.error("Unable to execute even:\n" + message, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOGGER.info("Disconnected remote user from: {}", session.getLocalAddress().getHostName());
        try {
            Event event = new Event();
            event.setEventType(EventType.CLOSE);
            executor.execute(session, event);
            MDC.clear();
        } catch (Throwable e) {
            LOGGER.error("Unable to execute event", e);
        }
        super.afterConnectionClosed(session, status);
    }
}

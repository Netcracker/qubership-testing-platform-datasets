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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.dataset.service.ws.entities.Pair;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventExecutorImpl implements EventExecutor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final EventHandler handler = new EventHandler();
    private static final EventExecutor instance = new EventExecutorImpl();

    public static EventExecutor getInstance() {
        return instance;
    }

    /**
     * Execution event action.
     */
    @Override
    public void execute(@Nullable WebSocketSession socket, @Nonnull Event event) {
        EventType eventType = event.getEventType();
        if (EventType.UNSELECT == eventType || EventType.CLOSE == eventType) {
            Pair<String, String> dsPath = handler.getDsPath(socket);
            event.setSessionId(dsPath.getSecond());
            event.setDataSetListId(dsPath.getFirst());
            handler.remove(event);
        }
        if (EventType.SELECT == eventType || EventType.GET_ALL == eventType) {
            handler.remove(event);
            handler.add(event, socket);
        }
        if (EventType.GET_USERS == eventType) {
            String hostsList = serialize(handler.getHosts(event.getSessionId(), event.getDataSetListId()));
            if (socket != null) {
                event.setValue(hostsList);
                sendEvent(event, socket);
            }
            return;
        }
        if (EventType.CONNECT == eventType && socket != null) {
            event.setValue(socket.getLocalAddress().getHostName());
        }
        broadCastEvent(socket, event);
    }

    private void broadCastEvent(WebSocketSession sourceSocket, @Nonnull Event event) {
        Collection<WebSocketSession> sockets = handler.getWebSockets(event);
        Stream<WebSocketSession> webSocketStream = sockets.stream().filter(webSocket -> webSocket != sourceSocket
                && webSocket.isOpen());
        List<WebSocketSession> collect = webSocketStream.collect(Collectors.toList());
        collect.forEach(socket -> sendEvent(event, socket));
    }

    private void sendEvent(@Nonnull Event event, WebSocketSession socket) {
        if (socket.isOpen()) {
            try {
                socket.sendMessage(new TextMessage(serialize(event)));
            } catch (IOException e) {
                LoggerFactory.getLogger(EventExecutorImpl.class).error("Failed to send event", e);
            }
        }
    }

    private String serialize(@Nonnull Object event) {
        try {
            return OBJECT_MAPPER.writeValueAsString(event);
        } catch (Exception e) {
            LoggerFactory.getLogger(EventExecutorImpl.class).error("Failed serialization object", e);
        }
        return "";
    }
}

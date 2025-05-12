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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.qubership.atp.dataset.service.ws.entities.Pair;
import org.springframework.web.socket.WebSocketSession;

public class EventHandler {

    /*<DSLId, JSESSIONID, WEBSOCKET>*/
    private static final Map<String, HashMap<String, WebSocketSession>> SOCKET_MAP = new ConcurrentHashMap<>();
    private static final Map<String, HashMap<String, String>> sessionHandler = new ConcurrentHashMap<>();

    /**
     * This method will store which DSL user would like to edit, user's JSESSIONID and web socket
     * connection of course.
     */
    public void add(Event event, WebSocketSession webSocket) {
        SOCKET_MAP.computeIfAbsent(event.getDataSetListId(), key -> new HashMap<>())
                .put(event.getSessionId(), webSocket);

        sessionHandler.computeIfAbsent(event.getDataSetListId(), key -> new HashMap<>())
                .put(event.getSessionId(), webSocket.getLocalAddress().getHostName());
    }

    /**
     * returns all websockets which working on current DSL.
     */
    public Collection<WebSocketSession> getWebSockets(Event event) {
        Map<String, WebSocketSession> socketMap = SOCKET_MAP.get(event.getDataSetListId());
        if (socketMap == null) {
            return Collections.emptyList();
        }
        return socketMap.values();
    }

    public void remove(Event event) {
        removeFromMap(event, SOCKET_MAP);
        removeFromMap(event, sessionHandler);
    }

    /**
     * remove session by websocket.
     */
    public void remove(WebSocketSession webSocket) {
        Pair<String, String> dsPath = getDsPath(webSocket);
        String dslId = dsPath.getFirst();
        String sessionId = dsPath.getSecond();
        removeFromMap(dslId, sessionId, SOCKET_MAP);
        removeFromMap(dslId, sessionId, sessionHandler);
    }

    private <U> void removeFromMap(Event event, Map<String, HashMap<String, U>> map) {
        removeFromMap(event.getDataSetListId(), event.getSessionId(), map);
    }

    private <U> void removeFromMap(String dslId, String sessionId, Map<String, HashMap<String, U>> map) {
        Map<String, U> subMap = map.get(dslId);
        if (subMap == null) {
            return;
        }
        subMap.remove(sessionId);
    }

    /**
     * returns DSL and Session.
     */
    public Pair<String, String> getDsPath(WebSocketSession socket) {
        Pair<String, String> pair = new Pair<>();
        Optional<Map.Entry<String, HashMap<String, WebSocketSession>>> optional = SOCKET_MAP
                .entrySet().stream().filter(entry -> {
                    Optional<Map.Entry<String, WebSocketSession>> first = entry.getValue()
                            .entrySet()
                            .stream()
                            .filter(socketEntry -> socketEntry.getValue() == socket).findFirst();
                    first.ifPresent(record -> pair.setSecond(record.getKey()));
                    return first.isPresent();
                }).findFirst();
        optional.ifPresent(stringHashMapEntry -> pair.setFirst(stringHashMapEntry.getKey()));
        return pair;
    }

    /**
     * returns the list of users who edit current dsl.
     */
    public Collection<String> getHosts(String sessionId, String dsl) {
        HashMap<String, String> dslMap = sessionHandler.get(dsl);
        if (dslMap.isEmpty()) {
            return Collections.emptyList();
        }
        return dslMap.entrySet().stream().filter(entry -> !entry.getKey().equals(sessionId))
                .map(entry -> entry.getKey() + ',' + entry.getValue()).collect(Collectors.toList());
    }
}

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.internal.util.reflection.FieldReader;
import org.springframework.web.socket.WebSocketSession;

import org.qubership.atp.dataset.service.ws.event.Event;
import org.qubership.atp.dataset.service.ws.event.EventHandler;

@Isolated
public class WebSocketHandlerTest {

    private EventHandler handler;
    private WebSocketSession webSocket;
    private String sessionId = "sessionId";
    private String dslId = "dslId";
    private Event event;

    @BeforeEach
    public void setUp() {
        handler = new EventHandler();
        webSocket = mock(WebSocketSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteHost()).thenReturn("wsmsa-026");
        when(webSocket.getLocalAddress()).thenReturn(new InetSocketAddress("testHost", 8080));
        event = new Event();
        event.setDataSetListId(dslId);
        event.setSessionId(sessionId);
        handler.add(event, webSocket);
    }

    @Test
    public void testEventIsPresentedInMapAfterAdding() {
        Map<String, Map<String, WebSocketSession>> map = getSocketMap();
        Assertions.assertSame(webSocket, map.get(dslId).get(sessionId));
    }

    @SuppressWarnings("unchecked") //Why Whitebox.getInternalState doesn't know about generics?
    private Map<String, Map<String, WebSocketSession>> getSocketMap() {
        try {
            Class clazz = handler.getClass();
            Field socketMap = clazz.getDeclaredField("SOCKET_MAP");
            FieldReader fieldReader = new FieldReader(handler, socketMap);
            return (Map<String, Map<String, WebSocketSession>>) fieldReader.read();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Error reading field SOCKET_MAP", e);
        }
    }

    @Test
    public void testEventsIsAccessibleByDslId() {
        Event event = new Event();
        event.setSessionId("1");
        event.setDataSetListId("1");
        WebSocketSession webSocketSession = mock(WebSocketSession.class);
        when(webSocketSession.getLocalAddress()).thenReturn(new InetSocketAddress("localhost", 8080));
        handler.add(event, webSocketSession);
        Collection<WebSocketSession> webSockets = handler.getWebSockets(this.event);
        Assertions.assertEquals(1, webSockets.size());
        Assertions.assertSame(webSocket, webSockets.iterator().next());
    }

    @Test
    public void testSocketIsRemovedFromHandlerBySocketInstance() {
        handler.remove(webSocket);
        Assertions.assertEquals(0, getSocketMap().get(dslId).size());
    }

    @Test
    public void testSocketIsRemovedFromHandlerByEvent() {
        handler.remove(event);
        Assertions.assertEquals(0, getSocketMap().get(dslId).size());
    }
}

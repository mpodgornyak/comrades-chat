package com.comrades.chat.websocket;

import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

public interface WebSocketSessionManager {

    void registerSession(UUID deviceId, WebSocketSession session);

    void removeSession(UUID deviceId);

    boolean isDeviceOnline(UUID deviceId);

    void pushToDevice(UUID deviceId, String jsonPayload);

    int getActiveSessionsCount();
}

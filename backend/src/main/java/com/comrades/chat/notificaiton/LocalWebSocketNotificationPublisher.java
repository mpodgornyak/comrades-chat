package com.comrades.chat.notificaiton;

import com.comrades.chat.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocalWebSocketNotificationPublisher implements NotificationPublisher {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void notifyNewMessage(UUID deviceId, NewMessageEvent event) {
        if (!sessionManager.isDeviceOnline(deviceId)) {
            log.debug("Device {} offline, skipping push (message in queue)", deviceId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "NEW_MESSAGE",
                    "messageId", event.getMessageId().toString(),
                    "senderId", event.getSenderId().toString(),
                    "clientTimestamp", event.getClientTimestamp()
            ));

            sessionManager.pushToDevice(deviceId, json);
            log.debug("Pushed NEW_MESSAGE notification to device {}", deviceId);
        } catch (Exception e) {
            log.error("Failed to push notification to device {}", deviceId, e);
        }
    }

    @Override
        public boolean isDeviceOnline(UUID deviceId) {
        return sessionManager.isDeviceOnline(deviceId);
    }
}
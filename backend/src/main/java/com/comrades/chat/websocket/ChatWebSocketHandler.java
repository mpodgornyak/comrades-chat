package com.comrades.chat.websocket;

import com.comrades.chat.repository.DeviceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик WebSocket соединений для чата.
 * Управляет жизненным циклом: подключение, сообщения, отключение.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Вызывается при успешном подключении.
     * deviceId передаётся через атрибуты сессии (установлены в HandshakeInterceptor).
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID deviceId = getDeviceIdFromSession(session);
        if (deviceId == null) {
            log.warn("WebSocket connection without deviceId, closing");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        sessionManager.registerSession(deviceId, session);
        updateDeviceLastSeen(deviceId);

        // Отправляем приветственное сообщение
        String welcome = objectMapper.writeValueAsString(Map.of(
                "type", "CONNECTED",
                "deviceId", deviceId.toString(),
                "timestamp", Instant.now().toString()
        ));
        session.sendMessage(new TextMessage(welcome));

        log.info("WebSocket connected: deviceId={}", deviceId);
    }

    /**
     * Вызывается при получении сообщения от клиента.
     * Обрабатываем PING и другие команды.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID deviceId = getDeviceIdFromSession(session);
        String payload = message.getPayload();

        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "";

            switch (type) {
                case "PING" -> handlePing(session);
                case "ACK" -> handleAck(deviceId, json);
                default -> log.warn("Unknown message type from device {}: {}", deviceId, type);
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message from device {}", deviceId, e);
        }
    }

    /**
     * Вызывается при закрытии соединения.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID deviceId = getDeviceIdFromSession(session);
        if (deviceId != null) {
            sessionManager.removeSession(deviceId);
            log.info("WebSocket disconnected: deviceId={}, status={}", deviceId, status);
        }
    }

    /**
     * Вызывается при ошибке транспорта.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        UUID deviceId = getDeviceIdFromSession(session);
        log.error("WebSocket transport error for device {}", deviceId, exception);
    }

    // ============================================
    // Приватные методы
    // ============================================

    private void handlePing(WebSocketSession session) throws IOException {
        String pong = "{\"type\":\"PONG\",\"timestamp\":" + System.currentTimeMillis() + "}";
        session.sendMessage(new TextMessage(pong));
    }

    private void handleAck(UUID deviceId, JsonNode json) {
        // Клиент может подтвердить получение push-уведомления
        log.debug("Received ACK from device {}", deviceId);
    }

    private UUID getDeviceIdFromSession(WebSocketSession session) {
        Object deviceIdAttr = session.getAttributes().get("deviceId");
        if (deviceIdAttr instanceof UUID) {
            return (UUID) deviceIdAttr;
        }
        if (deviceIdAttr instanceof String) {
            try {
                return UUID.fromString((String) deviceIdAttr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private void updateDeviceLastSeen(UUID deviceId) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setLastSeenAt(Instant.now());
            deviceRepository.save(device);
        });
    }
}

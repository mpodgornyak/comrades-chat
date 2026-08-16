package com.comrades.chat.websocket;

import com.comrades.chat.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;

import java.util.Map;
import java.util.UUID;

/**
 * Перехватывает WebSocket handshake для аутентификации.
 * Ожидает query параметры: ?token=<JWT>&deviceId=<UUID>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptorImpl implements WebSocketAuthInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            String deviceIdParam = servletRequest.getServletRequest().getParameter("deviceId");

            // 1. Проверяем наличие параметров
            if (token == null || deviceIdParam == null) {
                log.warn("WebSocket handshake rejected: missing token or deviceId");
                return false;
            }

            // 2. Валидируем JWT
            if (!jwtService.validateAccessToken(token)) {
                log.warn("WebSocket handshake rejected: invalid token");
                return false;
            }

            // 3. Парсим deviceId
            UUID deviceId;
            try {
                deviceId = UUID.fromString(deviceIdParam);
            } catch (IllegalArgumentException e) {
                log.warn("WebSocket handshake rejected: invalid deviceId format");
                return false;
            }

            // 4. Проверяем, что deviceId принадлежит пользователю из токена
            UUID userId = jwtService.extractUserId(token);
            // TODO: проверить что deviceId принадлежит userId через DeviceRepository
            // Для MVP пропускаем эту проверку

            // 5. Сохраняем в атрибуты сессии
            attributes.put("deviceId", deviceId);
            attributes.put("userId", userId);

            log.info("WebSocket handshake approved: userId={}, deviceId={}", userId, deviceId);
            return true;
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Ничего не делаем после handshake
    }
}
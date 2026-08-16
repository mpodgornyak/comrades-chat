package com.comrades.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Управляет активными WebSocket соединениями.
 * Использует ReentrantLock на каждую сессию для потокобезопасной отправки.
 */
@Component
@Slf4j
public class WebSocketSessionManagerImpl implements WebSocketSessionManager {

    /**
     * Таймаут ожидания блокировки при отправке сообщения.
     * Если не успели за это время — пропускаем отправку (сообщение в queue).
     */
    private static final long SEND_LOCK_TIMEOUT_MS = 1000;

    private final Map<UUID, SessionHolder> activeSessions = new ConcurrentHashMap<>();

    /**
     * Обёртка над WebSocketSession с собственным локом.
     * Каждая сессия имеет свой lock — отправки разным устройствам не блокируют друг друга.
     */
    private static class SessionHolder {
        final WebSocketSession session;
        final ReentrantLock sendLock = new ReentrantLock();

        SessionHolder(WebSocketSession session) {
            this.session = session;
        }
    }

    /**
     * Регистрирует сессию устройства.
     */
    public void registerSession(UUID deviceId, WebSocketSession session) {
        SessionHolder newHolder = new SessionHolder(session);
        SessionHolder oldHolder = activeSessions.put(deviceId, newHolder);

        if (oldHolder != null && oldHolder.session.isOpen()) {
            try {
                oldHolder.session.close();
                log.info("Closed old WebSocket session for device {}", deviceId);
            } catch (IOException e) {
                log.warn("Failed to close old session for device {}", deviceId, e);
            }
        }

        log.info("WebSocket session registered for device {}. Total sessions: {}",
                deviceId, activeSessions.size());
    }

    /**
     * Удаляет сессию устройства.
     */
    public void removeSession(UUID deviceId) {
        activeSessions.remove(deviceId);
        log.info("WebSocket session removed for device {}. Total sessions: {}",
                deviceId, activeSessions.size());
    }

    /**
     * Проверяет, онлайн ли устройство.
     */
    public boolean isDeviceOnline(UUID deviceId) {
        SessionHolder holder = activeSessions.get(deviceId);
        return holder != null && holder.session.isOpen();
    }

    /**
     * Отправляет сообщение на устройство.
     * Использует ReentrantLock с таймаутом для неблокирующей отправки.
     */
    public void pushToDevice(UUID deviceId, String jsonPayload) {
        SessionHolder holder = activeSessions.get(deviceId);

        if (holder == null) {
            log.debug("No session for device {}, skipping push", deviceId);
            return;
        }

        if (!holder.session.isOpen()) {
            log.debug("Session for device {} is closed, skipping push", deviceId);
            return;
        }

        // Пытаемся получить блокировку с таймаутом
        boolean lockAcquired = false;
        try {
            lockAcquired = holder.sendLock.tryLock(SEND_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!lockAcquired) {
                log.warn("Could not acquire send lock for device {} within {}ms, skipping push. Message stays in queue.",
                        deviceId, SEND_LOCK_TIMEOUT_MS);
                return;
            }

            // Повторная проверка после получения блокировки
            if (!holder.session.isOpen()) {
                log.debug("Session for device {} closed while waiting for lock", deviceId);
                return;
            }

            holder.session.sendMessage(new TextMessage(jsonPayload));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Send to device {} was interrupted", deviceId);
        } catch (IOException e) {
            log.error("Failed to push message to device {}", deviceId, e);
        } finally {
            if (lockAcquired) {
                holder.sendLock.unlock();
            }
        }
    }

    /**
     * Возвращает количество активных соединений.
     */
    public int getActiveSessionsCount() {
        return activeSessions.size();
    }
}
package com.comrades.chat.notificaiton;

import java.util.UUID;

public interface NotificationPublisher {

    void notifyNewMessage(UUID deviceId, NewMessageEvent event);

    boolean isDeviceOnline(UUID deviceId);

}

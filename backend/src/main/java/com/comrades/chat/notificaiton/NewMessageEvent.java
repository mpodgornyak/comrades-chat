package com.comrades.chat.notificaiton;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NewMessageEvent {
    private UUID messageId;
    private UUID senderId;
    private UUID recipientId;
    private long clientTimestamp;
}
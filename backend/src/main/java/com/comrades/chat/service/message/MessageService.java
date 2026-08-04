package com.comrades.chat.service.message;

import com.comrades.chat.dto.AcknowledgeRequest;
import com.comrades.chat.dto.InboxMessage;
import com.comrades.chat.dto.MessageRequest;
import com.comrades.chat.dto.MessageResponse;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    MessageResponse sendMessage(UUID senderId, MessageRequest request);

    List<InboxMessage> getInbox(UUID deviceId);

    void acknowledgeMessages(AcknowledgeRequest request);
}

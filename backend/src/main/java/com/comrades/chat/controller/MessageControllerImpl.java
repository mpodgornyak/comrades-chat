package com.comrades.chat.controller;

import com.comrades.chat.api.MessagesApi;
import com.comrades.chat.dto.AcknowledgeRequest;
import com.comrades.chat.dto.InboxMessage;
import com.comrades.chat.dto.MessageRequest;
import com.comrades.chat.dto.MessageResponse;
import com.comrades.chat.mapper.MessageMapper;
import com.comrades.chat.exception.IdempotentRequestException;
import com.comrades.chat.service.message.MessageService;
import com.comrades.chat.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageControllerImpl implements MessagesApi {

    private final MessageService messageService;
    private final MessageMapper messageMapper;

    @Override
    public ResponseEntity<MessageResponse> sendMessage(MessageRequest messageRequest) {
        UUID senderId = SecurityUtil.getCurrentUserId();
        try {
            var response = messageService.sendMessage(senderId, messageRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IdempotentRequestException exception) {
            log.info("Returning existing message. IdempotentKey: {}", exception.getExistingMessage().getIdempotencyKey());
            var existingMsg = messageMapper.toResponse(exception.getExistingMessage());
            return ResponseEntity.ok(existingMsg);
        }
    }

    @Override
    public ResponseEntity<Void> acknowledgeMessages(AcknowledgeRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        // TODO: добавить проверку принадлежности устройства

        messageService.acknowledgeMessages(request);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<List<InboxMessage>> getInbox(UUID deviceId) {
        // Проверяем, что устройство принадлежит текущему пользователю
        UUID userId = SecurityUtil.getCurrentUserId();
        // TODO: добавить проверку принадлежности устройства

        List<InboxMessage> messages = messageService.getInbox(deviceId);
        return ResponseEntity.ok(messages);
    }
}

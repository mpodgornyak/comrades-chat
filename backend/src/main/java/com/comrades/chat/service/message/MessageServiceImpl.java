package com.comrades.chat.service.message;

import com.comrades.chat.dto.AcknowledgeRequest;
import com.comrades.chat.dto.InboxMessage;
import com.comrades.chat.dto.MessageRequest;
import com.comrades.chat.dto.MessageResponse;
import com.comrades.chat.entity.device.Device;
import com.comrades.chat.entity.message.Message;
import com.comrades.chat.entity.message.MessageQueue;
import com.comrades.chat.exception.IdempotentRequestException;
import com.comrades.chat.mapper.MessageMapper;
import com.comrades.chat.repository.MessageQueueRepository;
import com.comrades.chat.repository.MessageRepository;
import com.comrades.chat.service.device.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageQueueRepository messageQueueRepository;
    private final DeviceService deviceService;
    private final MessageMapper messageMapper;

    @Transactional
    public MessageResponse sendMessage(UUID senderId, MessageRequest request) {
        log.info("Sending message from {} to {}", senderId, request.getRecipientId());

        messageRepository.findBySenderIdAndIdempotencyKey(senderId, request.getIdempotencyKey())
                .ifPresent(existingMessage -> {
                    log.info("Idempotent request detected, returning existing message: {}", existingMessage.getId());
                    throw new IdempotentRequestException(existingMessage);
                });

        Message message = messageMapper.toEntity(request);
        message.setSenderId(senderId);
        message.setChatId(resolveChatId(senderId, request.getRecipientId()));

        Message saved;
        try {
            saved = messageRepository.save(message);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate message detected (race condition): {}", request.getIdempotencyKey());
            return messageRepository.findBySenderIdAndIdempotencyKey(senderId, request.getIdempotencyKey())
                    .map(messageMapper::toResponse)
                    .orElseThrow(() -> new IllegalStateException("Message disappeared after duplicate error"));
        }

        fanOutToRecipientDevices(saved.getId(), request.getRecipientId());

        log.info("Message saved: id={}, chatId={}", saved.getId(), saved.getChatId());
        return messageMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InboxMessage> getInbox(UUID deviceId) {
        log.info("Fetching inbox for device {}", deviceId);

        // 1. Получаем записи из очереди
        List<MessageQueue> queueEntries = messageQueueRepository
                .findByRecipientDeviceIdAndDeliveredAtIsNull(deviceId);

        if (queueEntries.isEmpty()) {
            log.info("No pending messages for device {}", deviceId);
            return Collections.emptyList();
        }

        // 2. Извлекаем messageId из записей очереди
        List<UUID> messageIds = queueEntries.stream()
                .map(MessageQueue::getMessageId)
                .distinct()
                .toList();

        // 3. Получаем сами сообщения
        List<Message> messages = messageRepository.findByIdIn(messageIds);

        // 4. Создаём мапу для быстрого доступа
        Map<UUID, Message> messageMap = messages.stream()
                .collect(Collectors.toMap(Message::getId, Function.identity()));

        // 5. Маппим в InboxMessage DTO
        List<InboxMessage> inboxMessages = queueEntries.stream()
                .map(queueEntry -> {
                    Message message = messageMap.get(queueEntry.getMessageId());
                    if (message == null) {
                        log.warn("Message {} not found for queue entry {}", queueEntry.getMessageId(), queueEntry.getId());
                        return null;
                    }
                    return toInboxMessage(message);
                })
                .filter(Objects::nonNull)
                .toList();

        log.info("Returning {} messages for device {}", inboxMessages.size(), deviceId);
        return inboxMessages;
    }

    @Transactional
    public void acknowledgeMessages(AcknowledgeRequest request) {
        log.info("Acknowledging {} messages for device {}",
                request.getMessageIds().size(), request.getDeviceId());

        int deleted = messageQueueRepository.deleteByMessageIdsAndDeviceId(
                request.getMessageIds(),
                request.getDeviceId()
        );

        log.info("Acknowledged and removed {} queue entries", deleted);
    }


    /**
     * Создаёт записи в message_queue для каждого активного устройства получателя.
     */
    private void fanOutToRecipientDevices(UUID messageId, UUID recipientId) {
        List<Device> recipientDevices = deviceService.getActiveDevices(recipientId);

        if (recipientDevices.isEmpty()) {
            log.info("Recipient {} has no active devices, message stays in messages table", recipientId);
            return;
        }

        for (Device device : recipientDevices) {
            MessageQueue queueEntry = new MessageQueue();
            queueEntry.setMessageId(messageId);
            queueEntry.setRecipientDeviceId(device.getId());
            messageQueueRepository.save(queueEntry);
        }

        log.info("Message {} queued for {} devices", messageId, recipientDevices.size());
    }

    private UUID resolveChatId(UUID senderId, UUID recipientId) {
        // TODO: Реализовать в следующем шаге
        return recipientId;
    }

    private InboxMessage toInboxMessage(Message message) {
        InboxMessage inbox = new InboxMessage();
        inbox.setMessageId(message.getId());
        inbox.setSenderId(message.getSenderId());
        inbox.setPayload(message.getPayload());
        inbox.setClientTimestamp(message.getClientTimestamp());
        return inbox;
    }

}

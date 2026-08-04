package com.comrades.chat.mapper;

import com.comrades.chat.dto.MessageRequest;
import com.comrades.chat.dto.MessageResponse;
import com.comrades.chat.entity.message.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    /**
     * DTO → Entity.
     * Поля id, serverTimestamp, status генерируются в @PrePersist.
     * Поля chatId, senderId устанавливаются в сервисе.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "serverTimestamp", ignore = true)
    @Mapping(target = "chatId", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(source = "payload", target = "payload")
    @Mapping(source = "clientTimestamp", target = "clientTimestamp")
    @Mapping(source = "idempotencyKey", target = "idempotencyKey")
    Message toEntity(MessageRequest request);

    /**
     * Entity → DTO для ответа клиенту.
     */
    @Mapping(source = "id", target = "messageId")
    @Mapping(source = "status", target = "status")
    MessageResponse toResponse(Message message);
}

package com.comrades.chat.repository;

import com.comrades.chat.entity.message.MessageQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueue, UUID> {

    /**
     * Найти все недоставленные сообщения для устройства.
     */
    List<MessageQueue> findByRecipientDeviceIdAndDeliveredAtIsNull(UUID recipientDeviceId);

    /**
     * Найти все записи в очереди для конкретного сообщения.
     */
    List<MessageQueue> findByMessageId(UUID messageId);

    /**
     * Удалить записи из очереди по списку messageId для конкретного устройства.
     * Используется при acknowledge.
     */
    @Modifying
    @Query("DELETE FROM MessageQueue mq WHERE mq.messageId IN :messageIds AND mq.recipientDeviceId = :deviceId")
    int deleteByMessageIdsAndDeviceId(@Param("messageIds") List<UUID> messageIds, @Param("deviceId") UUID deviceId);

    /**
     * Подсчёт недоставленных сообщений для устройства.
     */
    long countByRecipientDeviceIdAndDeliveredAtIsNull(UUID recipientDeviceId);
}

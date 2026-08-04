package com.comrades.chat.entity.message;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochRandomGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueue {

    private static final TimeBasedEpochRandomGenerator UUID_V7_GENERATOR =
            Generators.timeBasedEpochRandomGenerator();

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "recipient_device_id", nullable = false)
    private UUID recipientDeviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @PrePersist
    public void onCreate() {
        if (id == null) {
            id = UUID_V7_GENERATOR.generate();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Помечает сообщение как доставленное.
     */
    public void markDelivered() {
        this.deliveredAt = Instant.now();
    }

    /**
     * Проверка: доставлено ли сообщение.
     */
    public boolean isDelivered() {
        return deliveredAt != null;
    }
}
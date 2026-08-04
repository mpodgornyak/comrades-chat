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
@Table(name = "messages")
@IdClass(MessageId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private static final TimeBasedEpochRandomGenerator UUID_V7_GENERATOR =
            Generators.timeBasedEpochRandomGenerator();

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Id
    @Column(name = "server_timestamp", nullable = false)
    private Instant serverTimestamp;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "client_timestamp", nullable = false)
    private Long clientTimestamp;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @PrePersist
    public void onCreate() {
        if (id == null) {
            id = UUID_V7_GENERATOR.generate(); // UUID v7: time-ordered
        }
        if (serverTimestamp == null) {
            serverTimestamp = Instant.now();
        }
        if (status == null) {
            status = "QUEUED";
        }
    }
}

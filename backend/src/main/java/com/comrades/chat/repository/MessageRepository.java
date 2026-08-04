package com.comrades.chat.repository;

import com.comrades.chat.entity.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Optional<Message> findBySenderIdAndIdempotencyKey(UUID senderId, UUID idempotencyKey);

    List<Message> findByIdIn(List<UUID> messageIds);
}




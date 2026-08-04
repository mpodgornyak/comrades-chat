package com.comrades.chat.entity.message;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MessageId implements Serializable {

    private UUID id;
    private Instant serverTimestamp;
}
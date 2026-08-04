package com.comrades.chat.exception;

import com.comrades.chat.entity.message.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IdempotentRequestException extends RuntimeException {
    private final Message existingMessage;
}

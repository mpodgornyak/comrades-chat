package com.comrades.chat.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final String clientIp;
    private final String path;

    public RateLimitExceededException(String clientIp, String path) {
        super(String.format("Rate limit exceeded for %s on %s", clientIp, path));
        this.clientIp = clientIp;
        this.path = path;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getPath() {
        return path;
    }
}
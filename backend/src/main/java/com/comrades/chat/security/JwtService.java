package com.comrades.chat.security;

import java.util.UUID;

public interface JwtService {

    String generateAccessToken(UUID userId);

    UUID extractUserId(String token);

    boolean validateAccessToken(String token);

    String generateRefreshToken(UUID userId);

    boolean validateRefreshToken(String token);

    long getAccessExpirationSeconds();
}

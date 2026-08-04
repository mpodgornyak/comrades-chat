package com.comrades.chat.service.authentication;

import com.comrades.chat.dto.AuthResponse;
import com.comrades.chat.dto.LoginRequest;
import com.comrades.chat.dto.RefreshTokenRequest;
import com.comrades.chat.dto.RegisterRequest;

public interface AuthenticationService {
    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    AuthResponse loginUser(LoginRequest loginRequest);
}

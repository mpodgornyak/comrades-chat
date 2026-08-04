package com.comrades.chat.controller;

import com.comrades.chat.api.AuthenticationApi;
import com.comrades.chat.dto.AuthResponse;
import com.comrades.chat.dto.LoginRequest;
import com.comrades.chat.dto.RefreshTokenRequest;
import com.comrades.chat.dto.RegisterRequest;
import com.comrades.chat.service.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthenticationControllerImpl implements AuthenticationApi {

    private final AuthenticationService authenticationService;


    @Override
    public ResponseEntity<AuthResponse> loginUser(LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.loginUser(loginRequest));
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }

    @Override
    public ResponseEntity<AuthResponse> registerUser(RegisterRequest registerRequest) {
        return ResponseEntity.ok(authenticationService.register(registerRequest));
    }
}

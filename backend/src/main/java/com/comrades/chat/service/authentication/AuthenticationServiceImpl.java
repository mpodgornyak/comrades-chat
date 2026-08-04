package com.comrades.chat.service.authentication;

import com.comrades.chat.dto.AuthResponse;
import com.comrades.chat.dto.LoginRequest;
import com.comrades.chat.dto.RefreshTokenRequest;
import com.comrades.chat.dto.RegisterRequest;
import com.comrades.chat.entity.user.User;
import com.comrades.chat.exception.InvalidCredentialsException;
import com.comrades.chat.exception.UsernameAlreadyExistsException;
import com.comrades.chat.repository.UserRepository;
import com.comrades.chat.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        userRepository.findByUsername(registerRequest.getUsername())
                .ifPresent(user -> {
                    throw new UsernameAlreadyExistsException("user [" + user.getUsername() + "] already exists");
                });

        var newUser = User.builder()
                .username(registerRequest.getUsername())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .publicKey(registerRequest.getPublicKey())
                .build();

        var savedUSer = userRepository.save(newUser);
        log.info("User registered: id={}, user={}", savedUSer.getId(), savedUSer.getUsername());

        return buildAuthResponse(savedUSer.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        log.info("User logged in: id={}, username={}", user.getId(), user.getUsername());

        return buildAuthResponse(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Refreshing token");
        var refreshToken = refreshTokenRequest.getRefreshToken();

        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        return buildAuthResponse(userId);
    }

    private AuthResponse buildAuthResponse(UUID userId) {
        String accessToken = jwtService.generateAccessToken(userId);
        String refreshToken = jwtService.generateRefreshToken(userId);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(userId);
        response.setExpiresIn((int) jwtService.getAccessExpirationSeconds());
        return response;
    }
}

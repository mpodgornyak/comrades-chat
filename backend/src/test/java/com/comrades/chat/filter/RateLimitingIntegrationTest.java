package com.comrades.chat.filter;

import com.comrades.chat.TestDatabaseConfiguration;
import com.comrades.chat.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestDatabaseConfiguration.class)
@ActiveProfiles("test-ratelimit")
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // ВКЛЮЧАЕМ rate limiting для этого теста
        registry.add("rate-limit.enabled", () -> "true");
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-testing");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
    }

    @Test
    @DisplayName("Rate limiting возвращает 429 после превышения лимита")
    void rateLimiting_returns429AfterLimitExceeded() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("ratelimit_test");
        request.setPassword("wrongpassword");

        String requestBody = objectMapper.writeValueAsString(request);

        // 5 запросов (лимит) — все 401
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        // 6-й запрос — 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests());
    }
}
package com.comrades.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Базовый класс для всех интеграционных тестов.
 * Использует Singleton PostgreSQL контейнер.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestDatabaseConfiguration.class)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Отключаем rate limiting в тестах
        registry.add("rate-limit.enabled", () -> "false");
        // JWT secret для тестов
        registry.add("jwt.secret", () -> "test-secret-key-at-least-256-bits-long-for-testing");
        // Hikari settings для стабильности
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "60000");
    }

    /**
     * Очищает таблицы перед каждым тестом для изоляции.
     * Порядок важен из-за foreign key constraints.
     */
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE message_receipts CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE message_queue CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE messages CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE chat_members CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE chats CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE devices CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }
}
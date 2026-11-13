package com.comrades.chat;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfiguration {

    /**
     * Singleton контейнер PostgreSQL на весь тестовый run.
     * Reuse гарантирует что контейнер не останавливается между тестами.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("comrades_chat_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);

        // Ждём пока PostgreSQL реально готов принимать соединения
        container.start();
        return container;
    }
}
package com.comrades.chat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DockerAvailabilityTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:alpine");

    @Test
    void dockerIsAvailable() {
        // Если Docker работает, контейнер запустится и тест пройдёт
        assert redis.isRunning();
        System.out.println("Docker is available! Container ID: " + redis.getContainerId());
    }
}
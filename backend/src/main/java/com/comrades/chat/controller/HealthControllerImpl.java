package com.comrades.chat.controller;

import com.comrades.chat.api.HealthApi;
import com.comrades.chat.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
public class HealthControllerImpl implements HealthApi {

    private static final Logger logger = LoggerFactory.getLogger(HealthControllerImpl.class);

    public ResponseEntity<HealthResponse> getHealth() {
        logger.debug("Health check endpoint called");
        return ResponseEntity.ok(new HealthResponse("UP", OffsetDateTime.now(ZoneOffset.UTC)));
    }

}

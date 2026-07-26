package com.comrades.chat.controller;

import com.comrades.chat.api.SystemApi;
import com.comrades.chat.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
public class SystemControllerImpl implements SystemApi {

    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(
                HealthResponse.builder()
                        .status("UP")
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .build()
        );
    }

}

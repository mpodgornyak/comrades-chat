package com.comrades.chat.controller;

import com.comrades.chat.api.DevicesApi;
import com.comrades.chat.dto.DeviceInfo;
import com.comrades.chat.dto.DeviceRegistrationRequest;
import com.comrades.chat.dto.DeviceRegistrationResponse;
import com.comrades.chat.security.SecurityUtil;
import com.comrades.chat.service.device.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DeviceControllerImpl implements DevicesApi {

    private final DeviceService deviceService;

    @Override
    public ResponseEntity<List<DeviceInfo>> listDevices() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<DeviceInfo> devices = deviceService.listDevices(userId);
        return ResponseEntity.ok(devices);
    }

    @Override
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(DeviceRegistrationRequest deviceRegistrationRequest) {
        UUID userId = SecurityUtil.getCurrentUserId();
        DeviceRegistrationResponse response = deviceService.registerDevice(userId, deviceRegistrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> unregisterDevice(UUID deviceId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        deviceService.unregisterDevice(userId, deviceId);
        return ResponseEntity.noContent().build();
    }
}

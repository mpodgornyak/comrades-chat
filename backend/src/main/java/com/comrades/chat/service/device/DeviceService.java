package com.comrades.chat.service.device;

import com.comrades.chat.dto.DeviceInfo;
import com.comrades.chat.dto.DeviceRegistrationRequest;
import com.comrades.chat.dto.DeviceRegistrationResponse;
import com.comrades.chat.entity.device.Device;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    DeviceRegistrationResponse registerDevice(UUID userId, DeviceRegistrationRequest request);

    List<DeviceInfo> listDevices(UUID userId);

    void unregisterDevice(UUID userId, UUID deviceId);

    List<Device> getActiveDevices(UUID userId);
}

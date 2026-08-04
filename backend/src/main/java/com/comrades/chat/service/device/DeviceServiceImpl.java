package com.comrades.chat.service.device;

import com.comrades.chat.dto.DeviceInfo;
import com.comrades.chat.dto.DeviceRegistrationRequest;
import com.comrades.chat.dto.DeviceRegistrationResponse;
import com.comrades.chat.entity.device.Device;
import com.comrades.chat.exception.DeviceNotFoundException;
import com.comrades.chat.mapper.DeviceMapper;
import com.comrades.chat.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    @Transactional
    public DeviceRegistrationResponse registerDevice(UUID userId, DeviceRegistrationRequest request) {
        log.info("Registering device {} for user {}", request.getDeviceId(), userId);

        // Проверяем, не зарегистрировано ли уже это устройство
        if (deviceRepository.existsByIdAndUserId(request.getDeviceId(), userId)) {
            log.info("Device {} already registered, updating lastSeenAt", request.getDeviceId());
            Device existing = deviceRepository.findByIdAndUserId(request.getDeviceId(), userId).orElseThrow();
            existing.setLastSeenAt(Instant.now());
            existing.setStatus("ACTIVE");
            deviceRepository.save(existing);
            return deviceMapper.toRegistrationResponse(existing);
        }

        // Создаём новое устройство через маппер
        Device device = deviceMapper.toEntity(request);
        device.setUserId(userId);
        device.setLastSeenAt(Instant.now());

        Device saved = deviceRepository.save(device);
        log.info("Device registered: id={}, type={}", saved.getId(), saved.getDeviceType());

        return deviceMapper.toRegistrationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DeviceInfo> listDevices(UUID userId) {
        log.info("Listing devices for user {}", userId);
        List<Device> devices = deviceRepository.findByUserId(userId);
        return deviceMapper.toDeviceInfoList(devices);
    }

    @Transactional
    public void unregisterDevice(UUID userId, UUID deviceId) {
        log.info("Unregistering device {} for user {}", deviceId, userId);

        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found: " + deviceId));

        deviceRepository.delete(device);
        log.info("Device unregistered: {}", deviceId);
    }

    @Transactional(readOnly = true)
    public List<Device> getActiveDevices(UUID userId) {
        return deviceRepository.findByUserIdAndStatus(userId, "ACTIVE");
    }
}
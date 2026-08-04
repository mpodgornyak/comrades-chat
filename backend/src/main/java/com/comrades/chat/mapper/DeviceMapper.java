package com.comrades.chat.mapper;

import com.comrades.chat.dto.DeviceInfo;
import com.comrades.chat.dto.DeviceRegistrationRequest;
import com.comrades.chat.dto.DeviceRegistrationResponse;
import com.comrades.chat.entity.device.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    /**
     * DTO → Entity для регистрации устройства.
     * id берётся из запроса (клиент генерирует UUID v4).
     */
    @Mapping(source = "deviceId", target = "id")
    @Mapping(target = "userId", ignore = true)       // устанавливается в сервисе
    @Mapping(source = "deviceName", target = "deviceName")
    @Mapping(source = "deviceType", target = "deviceType")
    @Mapping(target = "status", ignore = true)       // устанавливается в @PrePersist
    @Mapping(target = "createdAt", ignore = true)    // устанавливается в @PrePersist
    @Mapping(target = "lastSeenAt", ignore = true)   // устанавливается в сервисе
    Device toEntity(DeviceRegistrationRequest request);

    /**
     * Entity → DTO для ответа на регистрацию.
     */
    @Mapping(source = "id", target = "deviceId")
    @Mapping(source = "status", target = "status")
    DeviceRegistrationResponse toRegistrationResponse(Device device);

    /**
     * Entity → DTO для списка устройств.
     */
    @Mapping(source = "id", target = "deviceId")
    @Mapping(source = "deviceName", target = "deviceName")
    @Mapping(source = "deviceType", target = "deviceType")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "lastSeenAt", target = "lastSeenAt")
    DeviceInfo toDeviceInfo(Device device);

    List<DeviceInfo> toDeviceInfoList(List<Device> devices);

    /**
     * Конвертация Instant → OffsetDateTime для MapStruct.
     */
    default OffsetDateTime instantToOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}

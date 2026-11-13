package com.comrades.chat.service;

import com.comrades.chat.BaseIntegrationTest;
import com.comrades.chat.dto.DeviceRegistrationRequest;
import com.comrades.chat.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeviceServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String username) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("password123");
        request.setPublicKey("key");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    @DisplayName("Регистрация устройства возвращает 201")
    void registerDevice_returns201() throws Exception {
        String token = registerAndGetToken("device_user_" + System.currentTimeMillis());
        UUID deviceId = UUID.randomUUID();

        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setDeviceId(deviceId);
        request.setDeviceName("iPhone 15");
        request.setDeviceType(DeviceRegistrationRequest.DeviceTypeEnum.MOBILE);

        mockMvc.perform(post("/api/v1/devices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Список устройств возвращает зарегистрированные устройства")
    void listDevices_returnsRegisteredDevices() throws Exception {
        String token = registerAndGetToken("list_devices_" + System.currentTimeMillis());

        // Регистрируем два устройства
        for (int i = 0; i < 2; i++) {
            DeviceRegistrationRequest request = new DeviceRegistrationRequest();
            request.setDeviceId(UUID.randomUUID());
            request.setDeviceName("Device " + i);
            request.setDeviceType(DeviceRegistrationRequest.DeviceTypeEnum.WEB);

            mockMvc.perform(post("/api/v1/devices")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Проверяем список
        mockMvc.perform(get("/api/v1/devices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Удаление устройства возвращает 204")
    void unregisterDevice_returns204() throws Exception {
        String token = registerAndGetToken("delete_device_" + System.currentTimeMillis());
        UUID deviceId = UUID.randomUUID();

        // Регистрируем устройство
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setDeviceId(deviceId);
        request.setDeviceName("To Delete");
        request.setDeviceType(DeviceRegistrationRequest.DeviceTypeEnum.DESKTOP);

        mockMvc.perform(post("/api/v1/devices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Удаляем
        mockMvc.perform(delete("/api/v1/devices/" + deviceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Проверяем что список пуст
        mockMvc.perform(get("/api/v1/devices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
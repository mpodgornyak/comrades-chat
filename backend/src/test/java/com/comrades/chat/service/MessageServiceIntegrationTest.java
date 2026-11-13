package com.comrades.chat.service;

import com.comrades.chat.BaseIntegrationTest;
import com.comrades.chat.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessageServiceIntegrationTest extends BaseIntegrationTest {

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

    private String registerAndGetUserId(String username) throws Exception {
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
        return json.get("userId").asText();
    }

    @Test
    @DisplayName("Отправка сообщения возвращает 201 и messageId")
    void sendMessage_returns201WithMessageId() throws Exception {
        String senderToken = registerAndGetToken("sender_" + System.currentTimeMillis());
        String recipientId = registerAndGetUserId("recipient_" + System.currentTimeMillis());

        MessageRequest request = new MessageRequest();
        request.setRecipientId(UUID.fromString(recipientId));
        request.setPayload("Hello, World!");
        request.setClientTimestamp(System.currentTimeMillis());
        request.setIdempotencyKey(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @DisplayName("Повторная отправка с тем же idempotencyKey возвращает то же сообщение")
    void sendMessage_idempotent_returnsSameMessage() throws Exception {
        String senderToken = registerAndGetToken("idemp_sender_" + System.currentTimeMillis());
        String recipientId = registerAndGetUserId("idemp_recipient_" + System.currentTimeMillis());
        UUID idempotencyKey = UUID.randomUUID();

        MessageRequest request = new MessageRequest();
        request.setRecipientId(UUID.fromString(recipientId));
        request.setPayload("Idempotent message");
        request.setClientTimestamp(System.currentTimeMillis());
        request.setIdempotencyKey(idempotencyKey);

        String requestBody = objectMapper.writeValueAsString(request);

        // Первая отправка
        MvcResult first = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        // Повторная отправка с тем же idempotencyKey
        MvcResult second = mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()) // 200, не 201
                .andReturn();

        // Проверяем что messageId одинаковый
        String firstMessageId = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("messageId").asText();
        String secondMessageId = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("messageId").asText();

        assert firstMessageId.equals(secondMessageId) : "Message IDs should be the same";
    }

    @Test
    @DisplayName("Отправка без токена возвращает 401")
    void sendMessage_withoutToken_returns401() throws Exception {
        MessageRequest request = new MessageRequest();
        request.setRecipientId(UUID.randomUUID());
        request.setPayload("test");
        request.setClientTimestamp(System.currentTimeMillis());
        request.setIdempotencyKey(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Inbox возвращает сообщения для устройства")
    void getInbox_returnsMessages() throws Exception {
        String senderToken = registerAndGetToken("inbox_sender_" + System.currentTimeMillis());
        String recipientUsername = "inbox_recipient_" + System.currentTimeMillis();
        String recipientId = registerAndGetUserId(recipientUsername);
        String recipientToken = registerAndGetToken(recipientUsername + "_token");

        // Регистрируем устройство получателя
        UUID deviceId = UUID.randomUUID();
        DeviceRegistrationRequest deviceRequest = new DeviceRegistrationRequest();
        deviceRequest.setDeviceId(deviceId);
        deviceRequest.setDeviceName("Test Device");
        deviceRequest.setDeviceType(DeviceRegistrationRequest.DeviceTypeEnum.WEB);

        mockMvc.perform(post("/api/v1/devices")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deviceRequest)))
                .andExpect(status().isCreated());

        // Отправляем сообщение
        MessageRequest messageRequest = new MessageRequest();
        messageRequest.setRecipientId(UUID.fromString(recipientId));
        messageRequest.setPayload("Inbox test message");
        messageRequest.setClientTimestamp(System.currentTimeMillis());
        messageRequest.setIdempotencyKey(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isCreated());

        // Проверяем inbox
        mockMvc.perform(get("/api/v1/messages/inbox")
                        .param("deviceId", deviceId.toString())
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payload").value("Inbox test message"))
                .andExpect(jsonPath("$[0].senderId").isNotEmpty());
    }
}
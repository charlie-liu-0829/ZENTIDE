package com.zentide.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zentide.exception.BusinessException;
import okhttp3.MediaType;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Authenticated gateway from the web application to the local Python Agent API. */
@Service
public class ZentideAgentGatewayService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient;
    private final String endpoint;
    private final String internalToken;

    public ZentideAgentGatewayService(
            @Value("${zentide.agent.base-url:http://127.0.0.1:8090}") String endpoint,
            @Value("${zentide.agent.internal-token:}") String internalToken) {
        this.endpoint = normalizeEndpoint(endpoint);
        this.internalToken = internalToken == null ? "" : internalToken.trim();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(70, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public Call streamChat(String userId, Long sceneId, Long currentPostId,
                           String mode, String question, String conversationId,
                           Set<String> permittedVisibilities,
                           Consumer<Map<String, Object>> onEvent,
                           Consumer<Throwable> onError) {
        return streamChat(userId, sceneId, currentPostId, currentPostId == null ? List.of() : List.of(currentPostId), mode, question,
                conversationId, permittedVisibilities, onEvent, onError);
    }

    public Call streamChat(String userId, Long sceneId, Long currentPostId, List<Long> currentPostIds,
                           String mode, String question, String conversationId,
                           Set<String> permittedVisibilities,
                           Consumer<Map<String, Object>> onEvent,
                           Consumer<Throwable> onError) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scene_id", sceneId);
        payload.put("question", question.trim());
        payload.put("mode", mode);
        if (currentPostId != null) payload.put("current_post_id", currentPostId);
        if (currentPostIds != null && !currentPostIds.isEmpty()) payload.put("current_post_ids", currentPostIds);
        if (conversationId != null && !conversationId.isBlank()) payload.put("conversation_id", conversationId);
        payload.put("user_id", userId);
        payload.put("permitted_visibilities", List.copyOf(permittedVisibilities));
        byte[] requestBytes;
        try {
            requestBytes = objectMapper.writeValueAsBytes(payload);
        } catch (IOException error) {
            throw new BusinessException("社区小助手请求格式错误", error);
        }
        Request.Builder streamRequest = new Request.Builder()
                .url(endpoint + "/v1/chat/stream")
                .post(RequestBody.create(requestBytes, JSON));
        if (!internalToken.isBlank()) streamRequest.header("X-Zentide-Agent-Token", internalToken);
        Call call = httpClient.newCall(streamRequest.build());
        new Thread(() -> {
            try {
                try (okhttp3.Response response = call.execute()) {
                    if (!response.isSuccessful()) {
                        byte[] body = response.body() == null ? new byte[0] : response.body().bytes();
                        throw new BusinessException(readError(body));
                    }
                    if (response.body() == null) throw new IOException("Agent stream response is empty");
                    String line;
                    while ((line = response.body().source().readUtf8Line()) != null) {
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if (data.isBlank()) continue;
                        Map<String, Object> event = objectMapper.readValue(data, new TypeReference<>() {});
                        onEvent.accept(event);
                    }
                }
            } catch (Throwable error) {
                onError.accept(error);
            }
        }, "zentide-agent-stream").start();
        return call;
    }

    public Map<String, Object> chat(String userId, Long sceneId, Long currentPostId,
                                    String mode, String question, String conversationId,
                                    Set<String> permittedVisibilities) {
        return chat(userId, sceneId, currentPostId, currentPostId == null ? List.of() : List.of(currentPostId), mode, question,
                conversationId, permittedVisibilities);
    }

    public Map<String, Object> chat(String userId, Long sceneId, Long currentPostId, List<Long> currentPostIds,
                                    String mode, String question, String conversationId,
                                    Set<String> permittedVisibilities) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scene_id", sceneId);
        payload.put("question", question.trim());
        payload.put("mode", mode);
        if (currentPostId != null) payload.put("current_post_id", currentPostId);
        if (currentPostIds != null && !currentPostIds.isEmpty()) payload.put("current_post_ids", currentPostIds);
        if (conversationId != null && !conversationId.isBlank()) payload.put("conversation_id", conversationId);
        payload.put("user_id", userId);
        payload.put("permitted_visibilities", List.copyOf(permittedVisibilities));

        try {
            Request.Builder request = new Request.Builder()
                    .url(endpoint + "/v1/chat")
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(payload), JSON));
            if (!internalToken.isBlank()) request.header("X-Zentide-Agent-Token", internalToken);
            try (okhttp3.Response response = httpClient.newCall(request.build()).execute()) {
                byte[] body = response.body() == null ? new byte[0] : response.body().bytes();
                if (!response.isSuccessful()) throw new BusinessException(readError(body));
                Map<String, Object> result = objectMapper.readValue(body, new TypeReference<>() {});
                if (!(result.get("answer") instanceof String answer) || answer.isBlank()) {
                    throw new BusinessException("社区小助手没有返回有效回答");
                }
                return result;
            }
        } catch (BusinessException error) {
            throw error;
        } catch (IOException error) {
            throw new BusinessException("无法连接社区小助手，请确认 Python Agent API 已启动", error);
        }
    }

    private String readError(byte[] body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            String detail = json.path("detail").asText("").trim();
            if (!detail.isBlank() && detail.length() <= 300) return detail;
        } catch (Exception ignored) {
            // Do not expose an upstream HTML page or stack trace to the client.
        }
        return "社区小助手暂时无法回答，请稍后再试";
    }

    private String normalizeEndpoint(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("/+$", "");
        if (!normalized.matches("https?://[^/\\s]+(?::\\d+)?(?:/[^\\s]*)?")) {
            throw new IllegalArgumentException("zentide.agent.base-url 必须是 http(s) 地址");
        }
        return normalized;
    }
}

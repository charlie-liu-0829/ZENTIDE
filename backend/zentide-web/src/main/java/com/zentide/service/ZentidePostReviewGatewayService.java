package com.zentide.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zentide.exception.BusinessException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Trusted server-to-server boundary for the SmartPosting review service. */
@Service
public class ZentidePostReviewGatewayService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient;
    private final String endpoint;
    private final String internalToken;

    public ZentidePostReviewGatewayService(
            @Value("${zentide.post-review.base-url:http://127.0.0.1:8091}") String endpoint,
            @Value("${zentide.agent.internal-token:}") String internalToken) {
        this.endpoint = normalizeEndpoint(endpoint);
        this.internalToken = internalToken == null ? "" : internalToken.trim();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    public Map<String, Object> review(String userId, Long sceneId, String title, String content,
                                      String postType, List<Long> topicIds, String draftId,
                                      Set<String> permittedVisibilities, List<Map<String, Object>> availableTopics) {
        Map<String, Object> payload = payload(userId, sceneId, title, content, postType, topicIds,
                permittedVisibilities);
        if (draftId != null && !draftId.isBlank()) payload.put("draft_id", draftId.trim());
        payload.put("available_topics", availableTopics == null ? List.of() : availableTopics);
        return call("/v1/posts/review", payload);
    }

    public Map<String, Object> validate(String reviewId, String userId, Long sceneId, String title,
                                        String content, String postType, List<Long> topicIds,
                                        Set<String> permittedVisibilities) {
        Map<String, Object> payload = payload(userId, sceneId, title, content, postType, topicIds,
                permittedVisibilities);
        payload.put("review_id", reviewId);
        return call("/v1/posts/review/validate", payload);
    }

    public void trackEvent(String reviewId, String userId, Long sceneId, String event, Long targetId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("review_id", reviewId);
        payload.put("user_id", userId);
        payload.put("scene_id", sceneId);
        payload.put("event", event);
        if (targetId != null) payload.put("target_id", targetId);
        call("/v1/posts/review/events", payload);
    }

    private Map<String, Object> payload(String userId, Long sceneId, String title, String content,
                                        String postType, List<Long> topicIds,
                                        Set<String> permittedVisibilities) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user_id", userId);
        payload.put("scene_id", sceneId);
        payload.put("title", title == null ? "" : title.trim());
        payload.put("content", content == null ? "" : content.trim());
        payload.put("selected_post_type", postType);
        payload.put("selected_topic_ids", topicIds == null ? List.of() : topicIds);
        payload.put("permitted_visibilities", List.copyOf(permittedVisibilities));
        return payload;
    }

    private Map<String, Object> call(String path, Map<String, Object> payload) {
        try {
            Request.Builder request = new Request.Builder().url(endpoint + path)
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(payload), JSON));
            if (!internalToken.isBlank()) request.header("X-Zentide-Agent-Token", internalToken);
            try (okhttp3.Response response = httpClient.newCall(request.build()).execute()) {
                byte[] body = response.body() == null ? new byte[0] : response.body().bytes();
                if (!response.isSuccessful()) throw new BusinessException(readError(body));
                return objectMapper.readValue(body, new TypeReference<>() {});
            }
        } catch (BusinessException error) {
            throw error;
        } catch (IOException error) {
            throw new BusinessException("智能检查服务暂时不可用，请稍后重试", error);
        }
    }

    private String readError(byte[] body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            String detail = json.path("detail").asText("").trim();
            if (!detail.isBlank() && detail.length() <= 300) return detail;
        } catch (Exception ignored) {
            // Do not expose an upstream response or stack trace.
        }
        return "智能检查服务暂时不可用，请稍后重试";
    }

    private String normalizeEndpoint(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("/+$", "");
        if (!normalized.matches("https?://[^/\\s]+(?::\\d+)?(?:/[^\\s]*)?")) {
            throw new IllegalArgumentException("zentide.post-review.base-url 必须是 http(s) 地址");
        }
        return normalized;
    }
}

package com.zentide.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.concurrent.TimeUnit;

@Service
public class ZentideInterestInsightGatewayService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String endpoint;
    private final String internalToken;

    public ZentideInterestInsightGatewayService(
            @Value("${zentide.recommend.base-url:http://127.0.0.1:8092}") String endpoint,
            @Value("${zentide.agent.internal-token:}") String internalToken) {
        this.endpoint = endpoint == null ? "" : endpoint.trim().replaceAll("/+$", "");
        this.internalToken = internalToken == null ? "" : internalToken.trim();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS).build();
    }

    public Map<String, Object> insights(String userId, Long sceneId, List<String> keywords,
                                        List<Long> seenPostIds, List<Long> permittedPostIds, int maxItems) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (sceneId != null) payload.put("scene_id", sceneId);
        payload.put("keywords", keywords == null ? List.of() : keywords);
        payload.put("seen_post_ids", seenPostIds == null ? List.of() : seenPostIds);
        if (permittedPostIds != null) payload.put("permitted_post_ids", permittedPostIds);
        payload.put("max_items", maxItems);
        return call("/v1/users/insights", userId, payload);
    }

    public Map<String, Object> listKeywords(String userId) { return call("/v1/users/interest-keywords", userId, null, "GET"); }
    public Map<String, Object> saved(String userId) { return call("/v1/users/insights/saved", userId, null, "GET"); }
    public Map<String, Object> unsave(String userId, String itemId) { return call("/v1/users/insights/saved/" + itemId, userId, null, "DELETE"); }

    public Map<String, Object> addKeyword(String userId, String keyword) {
        return call("/v1/users/interest-keywords", userId, Map.of("keyword", keyword));
    }

    public Map<String, Object> updateKeyword(String userId, Long id, boolean enabled) {
        return call("/v1/users/interest-keywords/" + id, userId, Map.of("enabled", enabled), "PATCH");
    }

    public Map<String, Object> deleteKeyword(String userId, Long id) {
        return call("/v1/users/interest-keywords/" + id, userId, null, "DELETE");
    }

    public void feedback(String userId, String itemId, String action, Map<String, Object> item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("item_id", itemId);
        payload.put("action", action);
        if (item != null) payload.put("item", item);
        call("/v1/users/insights/feedback", userId, payload);
    }

    private Map<String, Object> call(String path, String userId, Map<String, Object> payload) {
        return call(path, userId, payload, "POST");
    }

    private Map<String, Object> call(String path, String userId, Map<String, Object> payload, String method) {
        try {
            Request.Builder builder = new Request.Builder().url(endpoint + path)
                    .header("X-User-Id", userId);
            if (!internalToken.isBlank()) builder.header("X-Internal-Token", internalToken);
            if ("GET".equals(method)) builder.get();
            else if ("DELETE".equals(method)) builder.delete();
            else builder.method(method, RequestBody.create(mapper.writeValueAsBytes(payload == null ? Map.of() : payload), JSON));
            try (okhttp3.Response response = client.newCall(builder.build()).execute()) {
                byte[] body = response.body() == null ? new byte[0] : response.body().bytes();
                if (!response.isSuccessful()) throw new BusinessException(readError(body));
                return mapper.readValue(body, new TypeReference<>() {});
            }
        } catch (BusinessException error) { throw error; }
        catch (IOException error) { throw new BusinessException("兴趣情报服务暂时不可用，请稍后重试", error); }
    }

    private String readError(byte[] body) {
        try {
            String detail = mapper.readTree(body).path("detail").asText("");
            if (!detail.isBlank() && detail.length() < 300) return detail;
        } catch (Exception ignored) { }
        return "兴趣情报服务暂时不可用，请稍后重试";
    }
}

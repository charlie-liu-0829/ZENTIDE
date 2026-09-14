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
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ZentideGovernanceGatewayService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();
    private final String endpoint;
    private final String token;

    public ZentideGovernanceGatewayService(@Value("${zentide.governance.base-url:http://127.0.0.1:8093}") String endpoint,
                                           @Value("${zentide.agent.internal-token:}") String token) {
        this.endpoint = endpoint.trim().replaceAll("/+$", "");
        this.token = token == null ? "" : token.trim();
    }

    public Map<String, Object> review(Map<String, Object> payload) { return call("/v1/governance/review", payload); }
    public Map<String, Object> scan(Map<String, Object> payload) { return call("/v1/governance/scan", payload); }
    public Map<String, Object> rules(Map<String, Object> payload) { return call("/v1/governance/rules", payload); }
    public Map<String, Object> createRule(Map<String, Object> payload) { return call("/v1/governance/rules", payload); }
    public Map<String, Object> feedback(Map<String, Object> payload) { return call("/v1/governance/feedback", payload); }

    private Map<String, Object> call(String path, Map<String, Object> payload) {
        try {
            Request.Builder builder = new Request.Builder().url(endpoint + path).post(RequestBody.create(mapper.writeValueAsBytes(payload), JSON));
            if (!token.isBlank()) builder.header("X-Zentide-Agent-Token", token);
            try (okhttp3.Response response = client.newCall(builder.build()).execute()) {
                byte[] body = response.body() == null ? new byte[0] : response.body().bytes();
                if (!response.isSuccessful()) throw new BusinessException("治理 Agent 暂时不可用");
                return mapper.readValue(body, new TypeReference<>() {});
            }
        } catch (BusinessException e) { throw e; }
        catch (IOException e) { throw new BusinessException("治理 Agent 暂时不可用，请稍后重试", e); }
    }
}

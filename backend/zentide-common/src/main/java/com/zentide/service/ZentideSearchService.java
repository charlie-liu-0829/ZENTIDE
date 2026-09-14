package com.zentide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zentide.entity.vo.ZentideSearchHistoryItem;
import com.zentide.entity.vo.ZentideSearchResult;
import com.zentide.mapper.UserAccountMapper;
import com.zentide.mapper.ZentideInterestMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

@Service
public class ZentideSearchService {
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ZentideInterestMapper mapper;
    private final UserAccountMapper userAccountMapper;
    private final OkHttpClient httpClient;
    private final String endpoint;
    private final String postsIndex;
    private final String hubsIndex;

    public ZentideSearchService(ZentideInterestMapper mapper, UserAccountMapper userAccountMapper,
                                @Value("${zentide.search.elasticsearch.url:}") String endpoint,
                                @Value("${zentide.search.elasticsearch.posts-index:zentide_posts}") String postsIndex,
                                @Value("${zentide.search.elasticsearch.hubs-index:zentide_hubs}") String hubsIndex) {
        this.mapper = mapper;
        this.userAccountMapper = userAccountMapper;
        this.endpoint = endpoint == null ? "" : endpoint.replaceAll("/$", "");
        this.postsIndex = postsIndex;
        this.hubsIndex = hubsIndex;
        this.httpClient = new OkHttpClient.Builder().connectTimeout(700, TimeUnit.MILLISECONDS).readTimeout(1200, TimeUnit.MILLISECONDS).build();
    }

    public List<ZentideSearchResult> search(String userId, String type, String query, int limit) {
        String normalizedType = "hub".equalsIgnoreCase(type) || "hubs".equalsIgnoreCase(type) ? "hub" : "post";
        String clean = query == null ? "" : query.trim();
        if (clean.isBlank()) return List.of();
        if (userId != null) recordSearch(userId, clean, normalizedType);
        List<ZentideSearchResult> esResults = searchElasticsearch(normalizedType, clean, limit);
        if (!esResults.isEmpty()) return esResults;
        return "hub".equals(normalizedType) ? fallbackHubs(clean, limit) : fallbackPosts(clean, limit);
    }

    public List<ZentideSearchHistoryItem> history(String userId) {
        return readHistory(userId);
    }

    public void clearHistory(String userId) {
        userAccountMapper.updateSearchHistory(userId, "[]");
    }

    private synchronized void recordSearch(String userId, String query, String type) {
        List<ZentideSearchHistoryItem> history = new ArrayList<>(readHistory(userId));
        history.removeIf(item -> type.equals(item.getType()) && query.equalsIgnoreCase(item.getQuery()));
        ZentideSearchHistoryItem latest = new ZentideSearchHistoryItem();
        latest.setQuery(query); latest.setType(type); latest.setSearchedAt(LocalDateTime.now().toString());
        history.add(0, latest);
        if (history.size() > 5) history = new ArrayList<>(history.subList(0, 5));
        try {
            userAccountMapper.updateSearchHistory(userId, objectMapper.writeValueAsString(history));
        } catch (Exception ignored) {
            // Search itself must remain available even if history persistence fails.
        }
    }

    private List<ZentideSearchHistoryItem> readHistory(String userId) {
        if (userId == null) return List.of();
        String json = userAccountMapper.findSearchHistory(userId);
        if (json == null || json.isBlank()) return List.of();
        try {
            List<ZentideSearchHistoryItem> items = objectMapper.readValue(json, new TypeReference<>() {});
            return items.size() > 5 ? items.subList(0, 5) : items;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<ZentideSearchResult> searchElasticsearch(String type, String query, int limit) {
        if (endpoint.isBlank()) return List.of();
        try {
            String index = "hub".equals(type) ? hubsIndex : postsIndex;
            Map<String, Object> body = Map.of(
                    "size", limit,
                    "query", Map.of("multi_match", Map.of("query", query, "fields", "hub".equals(type) ? List.of("name^3", "description") : List.of("title^3", "body", "hubName")))
            );
            Request request = new Request.Builder().url(endpoint + "/" + index + "/_search")
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(body), JSON_TYPE)).build();
            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return List.of();
                JsonNode hits = objectMapper.readTree(response.body().bytes()).path("hits").path("hits");
                List<ZentideSearchResult> results = new ArrayList<>();
                for (JsonNode hit : hits) {
                    JsonNode source = hit.path("_source");
                    ZentideSearchResult result = new ZentideSearchResult();
                    result.setType(type); result.setId(source.path("id").asText(hit.path("_id").asText()));
                    result.setTitle(source.path("title").asText(source.path("name").asText("")));
                    result.setSummary(source.path("summary").asText(source.path("description").asText("")));
                    result.setHubName(source.path("hubName").asText("")); result.setCategory(source.path("category").asText(null));
                    result.setScore(hit.path("_score").isNumber() ? hit.path("_score").asDouble() : null);
                    results.add(result);
                }
                return results;
            }
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<ZentideSearchResult> fallbackPosts(String query, int limit) {
        return mapper.searchPosts(query, limit).stream().map(row -> {
            ZentideSearchResult r = new ZentideSearchResult(); r.setType("post"); r.setId(String.valueOf(row.get("id")));
            r.setTitle(String.valueOf(row.getOrDefault("title", "社区分享"))); r.setSummary(String.valueOf(row.getOrDefault("summary", "")));
            r.setHubName(String.valueOf(value(row, "hub_name", "hubName", ""))); return r;
        }).toList();
    }

    private List<ZentideSearchResult> fallbackHubs(String query, int limit) {
        return mapper.searchHubs(query, limit).stream().map(row -> {
            ZentideSearchResult r = new ZentideSearchResult(); r.setType("hub"); r.setId(String.valueOf(row.get("id")));
            r.setTitle(String.valueOf(row.getOrDefault("title", "兴趣现场"))); r.setSummary(String.valueOf(row.getOrDefault("summary", "")));
            r.setCategory(String.valueOf(value(row, "category", "category", "GENERAL")));
            r.setMemberCount(number(value(row, "member_count", "memberCount", null))); r.setPostCount(number(value(row, "post_count", "postCount", null))); return r;
        }).toList();
    }

    private Object value(Map<String, Object> row, String first, String second, Object fallback) {
        Object value = row.get(first); return value == null ? row.getOrDefault(second, fallback) : value;
    }

    private Integer number(Object value) { return value instanceof Number n ? n.intValue() : null; }
}

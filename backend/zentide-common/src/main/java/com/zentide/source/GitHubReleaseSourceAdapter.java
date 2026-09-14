package com.zentide.source;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zentide.entity.po.ZentideSource;
import com.zentide.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubReleaseSourceAdapter implements SourceAdapter {
    @Override
    public String sourceType() {
        return "GITHUB_RELEASES";
    }

    @Override
    public List<NormalizedDocument> parse(ZentideSource source, SourceFetchResult result) {
        JSONArray releases;
        try {
            releases = JSONArray.parseArray(new String(result.body(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException("GitHub Release 响应不是有效 JSON 数组");
        }
        if (releases == null) return List.of();
        List<NormalizedDocument> documents = new ArrayList<>();
        for (Object value : releases) {
            if (!(value instanceof JSONObject release) || Boolean.TRUE.equals(release.getBoolean("draft"))) continue;
            String tag = safe(release.getString("tag_name"));
            String url = safe(release.getString("html_url"));
            if (tag.isBlank() || url.isBlank()) continue;
            String title = safe(release.getString("name"));
            if (title.isBlank()) title = tag;
            String content = "tag: " + tag + "\nurl: " + url + "\n\n" + safe(release.getString("body"));
            documents.add(new NormalizedDocument("github-release:" + tag, title, content,
                    ContentHash.sha256(content), time(release.getString("published_at")), url, safe(release.getString("body"))));
        }
        return List.copyOf(documents);
    }

    private LocalDateTime time(String value) {
        try {
            return value == null || value.isBlank() ? null : OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

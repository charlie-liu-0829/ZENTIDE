package com.zentide.service;

import com.zentide.entity.po.ZentideDocument;
import com.zentide.entity.po.ZentideDocumentVersion;
import com.zentide.entity.po.ZentideFetchAttempt;
import com.zentide.entity.po.ZentideRawSnapshot;
import com.zentide.entity.po.ZentideSource;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideSourceMapper;
import com.zentide.source.NormalizedDocument;
import com.zentide.source.SourceAdapter;
import com.zentide.source.SourceFetchResult;
import com.zentide.source.SourceFetcher;
import com.zentide.source.ZentideFetchException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ZentideSourceService {
    private final ZentideSourceMapper mapper;
    private final SourceFetcher fetcher;
    private final Map<String, SourceAdapter> adapters;
    private final ZentideChangeCandidateService candidateService;
    private final ZentideRetrievalIndexService retrievalIndexService;

    public ZentideSourceService(ZentideSourceMapper mapper, SourceFetcher fetcher, List<SourceAdapter> adapters,
                                ZentideChangeCandidateService candidateService, ZentideRetrievalIndexService retrievalIndexService) {
        this.mapper = mapper;
        this.fetcher = fetcher;
        this.candidateService = candidateService;
        this.retrievalIndexService = retrievalIndexService;
        Map<String, SourceAdapter> available = new LinkedHashMap<>();
        for (SourceAdapter adapter : adapters) available.put(adapter.sourceType(), adapter);
        this.adapters = Map.copyOf(available);
    }

    public List<ZentideSource> list() {
        return mapper.listSources();
    }

    public ZentideSource register(String sourceName, String sourceType, String canonicalUrl, String trustTier) {
        String name = required(sourceName, "Source 名称", 200);
        String type = required(sourceType, "Source 类型", 32).toUpperCase(Locale.ROOT);
        SourceAdapter adapter = adapters.get(type);
        if (adapter == null) throw new BusinessException("当前只支持 GITHUB_RELEASES 和 RSS_ATOM 来源");
        String url = canonicalUrl(canonicalUrl);
        ZentideSource source = new ZentideSource();
        source.setSourceName(name);
        source.setSourceType(type);
        source.setCanonicalUrl(url);
        source.setCanonicalUrlHash(sha256(url.getBytes(StandardCharsets.UTF_8)));
        source.setTrustTier(normalizeTrustTier(trustTier));
        source.setAdapterKey(adapter.sourceType());
        mapper.insertSource(source);
        return source;
    }

    public IngestionOutcome fetch(Long sourceId) {
        if (sourceId == null) throw new BusinessException("Source ID 不能为空");
        ZentideSource source = mapper.findSource(sourceId);
        if (source == null || !"ACTIVE".equals(source.getStatus())) throw new BusinessException("Source 不存在或未启用");
        SourceAdapter adapter = adapters.get(source.getSourceType());
        if (adapter == null) throw new BusinessException("Source 没有可用适配器");

        ZentideFetchAttempt attempt = new ZentideFetchAttempt();
        attempt.setSourceId(sourceId);
        attempt.setRequestUrl(source.getCanonicalUrl());
        attempt.setStartedAt(LocalDateTime.now());
        mapper.insertAttempt(attempt);
        try {
            SourceFetchResult result = fetcher.fetch(source);
            String rawBody = new String(result.body(), StandardCharsets.UTF_8);
            String hash = sha256(result.body());
            ZentideRawSnapshot snapshot = new ZentideRawSnapshot();
            snapshot.setSourceId(sourceId);
            snapshot.setAttemptId(attempt.getAttemptId());
            snapshot.setCapturedAt(LocalDateTime.now());
            snapshot.setContentType(result.contentType());
            snapshot.setContentHash(hash);
            snapshot.setBody(rawBody);
            mapper.insertSnapshot(snapshot);

            List<NormalizedDocument> documents = adapter.parse(source, result);
            int createdVersions = persistDocuments(source, snapshot.getRawSnapshotId(), documents);
            finish(attempt, "SUCCEEDED", result.httpStatus(), result.contentType(), hash, null, null);
            return new IngestionOutcome(sourceId, attempt.getAttemptId(), snapshot.getRawSnapshotId(), documents.size(), createdVersions, "SUCCEEDED");
        } catch (ZentideFetchException e) {
            finish(attempt, "FAILED", null, null, null, e.code(), message(e.getMessage()));
            throw new BusinessException("来源抓取失败：" + e.getMessage());
        } catch (BusinessException e) {
            finish(attempt, "PARSE_FAILED", null, null, null, "PARSE_FAILED", message(e.getMessage()));
            throw e;
        } catch (Exception e) {
            finish(attempt, "FAILED", null, null, null, "INGESTION_FAILED", "来源采集未完成");
            throw new BusinessException("来源采集未完成");
        }
    }

    private int persistDocuments(ZentideSource source, Long rawSnapshotId, List<NormalizedDocument> documents) {
        int created = 0;
        for (NormalizedDocument normalized : documents) {
            String key = required(normalized.canonicalKey(), "文档标识", 500);
            ZentideDocument document = mapper.findDocument(source.getSourceId(), key);
            if (document == null) {
                document = new ZentideDocument();
                document.setSourceId(source.getSourceId());
                document.setCanonicalKey(key);
                document.setCurrentVersionNo(0);
                mapper.insertDocument(document);
            }
            if (mapper.findVersionByHash(document.getDocumentId(), normalized.contentHash()) != null) continue;
            ZentideDocumentVersion version = new ZentideDocumentVersion();
            version.setDocumentId(document.getDocumentId());
            version.setVersionNo(document.getCurrentVersionNo() + 1);
            version.setRawSnapshotId(rawSnapshotId);
            version.setContentHash(normalized.contentHash());
            version.setTitle(required(normalized.title(), "文档标题", 500));
            version.setNormalizedContent(required(normalized.content(), "文档内容", 1_000_000));
            version.setPublishedAt(normalized.publishedAt());
            mapper.insertDocumentVersion(version);
            mapper.updateCurrentVersion(document.getDocumentId(), version.getVersionNo());
            candidateService.create(source, document, version, normalized);
            retrievalIndexService.index(version);
            document.setCurrentVersionNo(version.getVersionNo());
            created++;
        }
        return created;
    }

    private void finish(ZentideFetchAttempt attempt, String status, Integer httpStatus, String contentType,
                        String responseHash, String errorCode, String errorMessage) {
        attempt.setStatus(status);
        attempt.setHttpStatus(httpStatus);
        attempt.setContentType(contentType);
        attempt.setResponseHash(responseHash);
        attempt.setErrorCode(errorCode);
        attempt.setErrorMessage(errorMessage);
        attempt.setFinishedAt(LocalDateTime.now());
        mapper.finishAttempt(attempt);
    }

    private String canonicalUrl(String value) {
        try {
            URI uri = URI.create(required(value, "Source URL", 2048)).normalize();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new BusinessException("Source URL 必须是无凭据的 HTTP 或 HTTPS 地址");
            }
            if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) throw new BusinessException("Source URL 端口不允许");
            return uri.toASCIIString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Source URL 格式不正确");
        }
    }

    private String normalizeTrustTier(String value) {
        String tier = value == null || value.isBlank() ? "PRIMARY" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PRIMARY", "OFFICIAL", "COMMUNITY").contains(tier)) throw new BusinessException("不支持的来源可信等级");
        return tier;
    }

    private String required(String value, String name, int limit) {
        String result = value == null ? "" : value.trim();
        if (result.isBlank() || result.length() > limit) throw new BusinessException(name + "不能为空且不能超过 " + limit + " 个字符");
        return result;
    }

    private String message(String value) {
        if (value == null || value.isBlank()) return "来源处理失败";
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private String sha256(byte[] body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body);
            StringBuilder value = new StringBuilder(64);
            for (byte item : digest) value.append(String.format("%02x", item));
            return value.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    public record IngestionOutcome(Long sourceId, Long attemptId, Long rawSnapshotId,
                                   int documentsFound, int versionsCreated, String status) {
    }
}

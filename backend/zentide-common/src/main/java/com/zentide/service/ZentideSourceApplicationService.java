package com.zentide.service;

import com.zentide.entity.po.ZentideSource;
import com.zentide.entity.po.ZentideSourceApplication;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideSourceApplicationMapper;
import com.zentide.source.NormalizedDocument;
import com.zentide.source.SourceAdapter;
import com.zentide.source.SourceFetchResult;
import com.zentide.source.SourceFetcher;
import com.zentide.source.ZentideFetchException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ZentideSourceApplicationService {
    private final ZentideSourceApplicationMapper mapper;
    private final SourceFetcher fetcher;
    private final Map<String, SourceAdapter> adapters;

    public ZentideSourceApplicationService(ZentideSourceApplicationMapper mapper, SourceFetcher fetcher, List<SourceAdapter> adapters) {
        this.mapper = mapper;
        this.fetcher = fetcher;
        Map<String, SourceAdapter> available = new LinkedHashMap<>();
        for (SourceAdapter adapter : adapters) available.put(adapter.sourceType(), adapter);
        this.adapters = Map.copyOf(available);
    }

    @Transactional
    public ZentideSourceApplication submit(String userId, Long topicId, String sourceName, String sourceType, String canonicalUrl) {
        if (topicId == null || mapper.findActiveTopic(topicId) == null) throw new BusinessException("话题不存在或已停用");
        String type = required(sourceType, "来源类型", 32).toUpperCase(Locale.ROOT);
        if (!adapters.containsKey(type)) throw new BusinessException("当前只支持 GITHUB_RELEASES 和 RSS_ATOM 来源");
        ZentideSourceApplication application = new ZentideSourceApplication();
        application.setTopicId(topicId);
        application.setApplicantId(userId);
        application.setSourceName(required(sourceName, "来源名称", 200));
        application.setSourceType(type);
        application.setCanonicalUrl(canonicalUrl(canonicalUrl));
        application.setCanonicalUrlHash(sha256(application.getCanonicalUrl()));
        application.setTrustTier("COMMUNITY");
        mapper.insert(application);
        mapper.addAudit(application.getApplicationId(), "USER", userId, "SUBMITTED", null, "{\"topicId\":" + topicId + "}");
        return application;
    }

    public List<ZentideSourceApplication> list() { return mapper.list(); }

    @Transactional
    public void approve(Long applicationId, String adminAccount, String note) {
        requireStatus(applicationId, "SUBMITTED");
        if (mapper.review(applicationId, "SUBMITTED", "APPROVED", optional(note, 1000), adminAccount, LocalDateTime.now()) != 1) throw new BusinessException("申请状态已变化，请刷新后重试");
        mapper.addAudit(applicationId, "ADMIN", adminAccount, "APPROVED", optional(note, 1000), null);
    }

    @Transactional
    public void reject(Long applicationId, String adminAccount, String note) {
        requireStatus(applicationId, "SUBMITTED");
        String reason = required(note, "拒绝原因", 1000);
        if (mapper.review(applicationId, "SUBMITTED", "REJECTED", reason, adminAccount, LocalDateTime.now()) != 1) throw new BusinessException("申请状态已变化，请刷新后重试");
        mapper.addAudit(applicationId, "ADMIN", adminAccount, "REJECTED", reason, null);
    }

    public TrialOutcome runTrial(Long applicationId, String adminAccount) {
        ZentideSourceApplication application = require(applicationId);
        if (!("APPROVED".equals(application.getStatus()) || "TRIAL_FAILED".equals(application.getStatus()))) throw new BusinessException("只有已审核通过或试运行失败的申请可以试运行");
        if (mapper.startTrial(applicationId) != 1) throw new BusinessException("申请状态已变化，请刷新后重试");
        try {
            ZentideSource source = transientSource(application);
            SourceFetchResult result = fetcher.fetch(source);
            List<NormalizedDocument> documents = adapters.get(application.getSourceType()).parse(source, result);
            if (documents.isEmpty()) throw new BusinessException("来源没有解析出可用文档");
            String sampleTitle = documents.get(0).title() == null ? "" : documents.get(0).title().replace("\"", "'");
            TrialOutcome outcome = new TrialOutcome("TRIAL_PASSED", result.httpStatus(), result.contentType(), documents.size(), trim(sampleTitle, 200), null);
            mapper.finishTrial(applicationId, "TRIAL_PASSED", outcome.json(), LocalDateTime.now());
            mapper.addAudit(applicationId, "ADMIN", adminAccount, "TRIAL_PASSED", null, outcome.json());
            return outcome;
        } catch (ZentideFetchException | BusinessException e) {
            TrialOutcome outcome = new TrialOutcome("TRIAL_FAILED", null, null, 0, null, safeMessage(e.getMessage()));
            mapper.finishTrial(applicationId, "TRIAL_FAILED", outcome.json(), LocalDateTime.now());
            mapper.addAudit(applicationId, "ADMIN", adminAccount, "TRIAL_FAILED", outcome.error(), outcome.json());
            return outcome;
        } catch (Exception e) {
            TrialOutcome outcome = new TrialOutcome("TRIAL_FAILED", null, null, 0, null, "来源试运行未完成");
            mapper.finishTrial(applicationId, "TRIAL_FAILED", outcome.json(), LocalDateTime.now());
            mapper.addAudit(applicationId, "ADMIN", adminAccount, "TRIAL_FAILED", outcome.error(), outcome.json());
            return outcome;
        }
    }

    @Transactional
    public ZentideSource activate(Long applicationId, String adminAccount) {
        ZentideSourceApplication application = require(applicationId);
        if (!"TRIAL_PASSED".equals(application.getStatus())) throw new BusinessException("只有试运行通过的申请可以启用");
        ZentideSource source = mapper.findSourceByUrlHash(application.getCanonicalUrlHash());
        if (source == null) {
            source = transientSource(application);
            source.setTrustTier(application.getTrustTier());
            source.setPolicyJson("{\"admission\":\"application-trial\"}");
            mapper.insertActiveSource(source);
        }
        mapper.attachTopicSource(application.getTopicId(), source.getSourceId(), platformKey(source.getSourceType()), application.getApplicantId());
        if (mapper.activate(applicationId, source.getSourceId(), adminAccount, LocalDateTime.now()) != 1) throw new BusinessException("申请状态已变化，请刷新后重试");
        mapper.addAudit(applicationId, "ADMIN", adminAccount, "ACTIVATED", null, "{\"sourceId\":" + source.getSourceId() + "}");
        return source;
    }

    private ZentideSourceApplication require(Long applicationId) {
        if (applicationId == null) throw new BusinessException("申请 ID 不能为空");
        ZentideSourceApplication application = mapper.find(applicationId);
        if (application == null) throw new BusinessException("来源申请不存在");
        return application;
    }

    private void requireStatus(Long applicationId, String status) {
        if (!status.equals(require(applicationId).getStatus())) throw new BusinessException("申请状态不允许当前操作");
    }

    private ZentideSource transientSource(ZentideSourceApplication application) {
        ZentideSource source = new ZentideSource();
        source.setSourceName(application.getSourceName()); source.setSourceType(application.getSourceType());
        source.setCanonicalUrl(application.getCanonicalUrl()); source.setCanonicalUrlHash(application.getCanonicalUrlHash());
        source.setAdapterKey(application.getSourceType());
        return source;
    }

    private String canonicalUrl(String value) {
        try {
            URI uri = URI.create(required(value, "来源地址", 2048)).normalize();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null || uri.getUserInfo() != null) throw new BusinessException("来源地址必须是无凭据的 HTTP 或 HTTPS 地址");
            if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) throw new BusinessException("来源地址端口不允许");
            return uri.toASCIIString();
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException("来源地址格式不正确"); }
    }
    private String required(String value, String name, int limit) { String result = optional(value, limit); if (result.isBlank()) throw new BusinessException(name + "不能为空且不能超过 " + limit + " 个字符"); return result; }
    private String optional(String value, int limit) { String result = value == null ? "" : value.trim(); if (result.length() > limit) throw new BusinessException("内容不能超过 " + limit + " 个字符"); return result; }
    private String sha256(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(64); for (byte b : bytes) out.append(String.format("%02x", b)); return out.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String platformKey(String type) { return "GITHUB_RELEASES".equals(type) ? "GITHUB" : "RSS"; }
    private String safeMessage(String value) { return trim(value == null || value.isBlank() ? "来源试运行失败" : value, 500); }
    private String trim(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }

    public record TrialOutcome(String status, Integer httpStatus, String contentType, int documentsFound, String sampleTitle, String error) {
        String json() { return "{\"status\":\"" + status + "\",\"httpStatus\":" + (httpStatus == null ? "null" : httpStatus) + ",\"contentType\":" + quote(contentType) + ",\"documentsFound\":" + documentsFound + ",\"sampleTitle\":" + quote(sampleTitle) + ",\"error\":" + quote(error) + "}"; }
        private static String quote(String value) { return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") + "\""; }
    }
}

package com.zentide.service;

import com.zentide.entity.po.ZentideEvidence;
import com.zentide.entity.po.ZentideEvidenceSubmission;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideCommunityEvidenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

@Service
public class ZentideCommunityEvidenceService {
    private final ZentideCommunityEvidenceMapper mapper;

    public ZentideCommunityEvidenceService(ZentideCommunityEvidenceMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public ZentideEvidenceSubmission submit(String authorId, Long changeId, String requestedSourceUrl, String requestedExcerpt, String requestedNote) {
        requirePublishedChange(changeId);
        ZentideEvidenceSubmission submission = new ZentideEvidenceSubmission();
        submission.setChangeId(changeId);
        submission.setAuthorId(authorId);
        submission.setSourceUrl(validateSourceUrl(requestedSourceUrl));
        submission.setExcerpt(required(requestedExcerpt, "证据摘录", 4_000, 20));
        submission.setNote(optional(requestedNote, "说明", 500));
        mapper.insertSubmission(submission);
        submission.setStatus("CANDIDATE");
        return submission;
    }

    public List<ZentideEvidenceSubmission> listCandidates(Integer requestedLimit) {
        int limit = requestedLimit == null ? 50 : Math.max(1, Math.min(200, requestedLimit));
        return mapper.listCandidates(limit);
    }

    @Transactional
    public void approve(String actorId, Long submissionId, Long documentVersionId, String requestedReviewNote) {
        ZentideEvidenceSubmission submission = candidate(submissionId);
        String reviewNote = optional(requestedReviewNote, "审核说明", 500);
        if (documentVersionId == null) throw new BusinessException("需要选择已采集的文档版本");
        ZentideEvidence material = mapper.findActiveSourceMaterial(documentVersionId);
        if (material == null || mapper.countExcerptInDocumentVersion(documentVersionId, submission.getExcerpt()) != 1) {
            throw new BusinessException("摘录未在指定的已采集文档版本中核对通过");
        }
        material.setChangeId(submission.getChangeId());
        material.setDocumentVersionId(documentVersionId);
        material.setLocator("community-submission:" + submissionId);
        material.setExcerpt(submission.getExcerpt());
        material.setExcerptHash(sha256(submission.getExcerpt()));
        mapper.insertEvidence(material);
        if (mapper.approve(submissionId, actorId, reviewNote, material.getEvidenceId()) != 1) {
            throw new BusinessException("投稿状态已变化，请刷新后重试");
        }
        mapper.insertAudit(actorId, "APPROVE_COMMUNITY_EVIDENCE", submissionId, submission.getChangeId(), documentVersionId);
    }

    @Transactional
    public void reject(String actorId, Long submissionId, String requestedReviewNote) {
        ZentideEvidenceSubmission submission = candidate(submissionId);
        String reviewNote = required(requestedReviewNote, "驳回说明", 500, 2);
        if (mapper.reject(submissionId, actorId, reviewNote) != 1) {
            throw new BusinessException("投稿状态已变化，请刷新后重试");
        }
        mapper.insertAudit(actorId, "REJECT_COMMUNITY_EVIDENCE", submissionId, submission.getChangeId(), null);
    }

    private ZentideEvidenceSubmission candidate(Long submissionId) {
        if (submissionId == null) throw new BusinessException("投稿 ID 不能为空");
        ZentideEvidenceSubmission submission = mapper.findCandidate(submissionId);
        if (submission == null) throw new BusinessException("投稿不存在或已处理");
        return submission;
    }

    private void requirePublishedChange(Long changeId) {
        if (changeId == null || mapper.countPublishedChange(changeId) != 1) {
            throw new BusinessException("Change 不存在或尚未验证发布");
        }
    }

    private String validateSourceUrl(String requested) {
        try {
            URI uri = URI.create(required(requested, "来源链接", 2048, 1)).normalize();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new BusinessException("来源链接必须是无凭据的 HTTP 或 HTTPS 地址");
            }
            return uri.toASCIIString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("来源链接格式不正确");
        }
    }

    private String required(String value, String name, int maximum, int minimum) {
        String safe = value == null ? "" : value.trim();
        if (safe.length() < minimum || safe.length() > maximum) throw new BusinessException(name + "长度不正确");
        return safe;
    }

    private String optional(String value, String name, int maximum) {
        if (value == null || value.isBlank()) return null;
        return required(value, name, maximum, 1);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}

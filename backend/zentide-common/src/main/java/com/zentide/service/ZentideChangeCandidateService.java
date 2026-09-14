package com.zentide.service;

import com.zentide.entity.po.ZentideChange;
import com.zentide.entity.po.ZentideClaim;
import com.zentide.entity.po.ZentideDocument;
import com.zentide.entity.po.ZentideDocumentVersion;
import com.zentide.entity.po.ZentideEvidence;
import com.zentide.entity.po.ZentideSource;
import com.zentide.entity.po.ZentideSignal;
import com.zentide.mapper.ZentideChangeCandidateMapper;
import com.zentide.mapper.ZentideTopicMapper;
import com.zentide.messaging.OutboxService;
import com.zentide.source.NormalizedDocument;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ZentideChangeCandidateService {
    private final ZentideChangeCandidateMapper mapper;
    private final ZentideTopicMapper topicMapper;
    private final OutboxService outboxService;

    public ZentideChangeCandidateService(ZentideChangeCandidateMapper mapper, ZentideTopicMapper topicMapper) {
        this(mapper, topicMapper, null);
    }

    @Autowired
    public ZentideChangeCandidateService(ZentideChangeCandidateMapper mapper, ZentideTopicMapper topicMapper, OutboxService outboxService) {
        this.mapper = mapper;
        this.topicMapper = topicMapper;
        this.outboxService = outboxService;
    }

    @Transactional
    public Long create(ZentideSource source, ZentideDocument document, ZentideDocumentVersion version,
                       NormalizedDocument normalized) {
        ZentideSignal signal = new ZentideSignal();
        signal.setSourceId(source.getSourceId());
        signal.setDocumentVersionId(version.getDocumentVersionId());
        signal.setSignalKey("document-version:" + version.getDocumentVersionId());
        signal.setSignalType(version.getVersionNo() == 1 ? "NEW_DOCUMENT" : "DOCUMENT_UPDATED");
        signal.setTitle(normalized.title());
        signal.setSummary(excerpt(normalized.excerpt(), normalized.content()));
        signal.setStatus("OBSERVING");
        signal.setConfidenceScore(new BigDecimal("50.00"));
        signal.setDiscoveredAt(LocalDateTime.now());
        mapper.insertSignal(signal);
        mapper.attachSignalTopics(signal.getSignalId(), source.getSourceId());
        mapper.insertActivity(signal.getSignalId(), null, source.getSourceId(), "DISCOVERED", "COMPLETED",
                "发现新的来源版本，形成一条浪花");

        ZentideChange change = new ZentideChange();
        change.setEntityType("DOCUMENT");
        change.setEntityKey(document.getCanonicalKey());
        change.setDocumentVersionId(version.getDocumentVersionId());
        change.setChangeKey("document-version:" + version.getDocumentVersionId());
        change.setChangeType(version.getVersionNo() == 1 ? "DISCOVERED" : "UPDATED");
        change.setTitle(normalized.title());
        change.setSummary(excerpt(normalized.excerpt(), normalized.content()));
        change.setWhatHappened("检测到来源文档的" + (version.getVersionNo() == 1 ? "首次发布" : "新版本") + "，等待证据验证后发布。");
        change.setStatus("CANDIDATE");
        change.setVerificationStatus("UNVERIFIED");
        change.setOccurredAt(normalized.publishedAt());
        change.setPublishedAt(normalized.publishedAt());
        mapper.insertChange(change);

        ZentideClaim claim = new ZentideClaim();
        claim.setChangeId(change.getChangeId());
        claim.setClaimType("FACT");
        claim.setClaimText("来源发布了文档：《" + normalized.title() + "》");
        claim.setConfidence(new BigDecimal("1.00"));
        claim.setStatus("CANDIDATE");
        mapper.insertClaim(claim);

        ZentideEvidence evidence = new ZentideEvidence();
        evidence.setChangeId(change.getChangeId());
        evidence.setSourceId(source.getSourceId());
        evidence.setDocumentVersionId(version.getDocumentVersionId());
        evidence.setClaimId(claim.getClaimId());
        evidence.setSourceName(source.getSourceName());
        evidence.setSourceUrl(normalized.sourceUrl() == null || normalized.sourceUrl().isBlank() ? source.getCanonicalUrl() : normalized.sourceUrl());
        evidence.setLocator(document.getCanonicalKey());
        evidence.setExcerpt(excerpt(normalized.excerpt(), normalized.content()));
        evidence.setTrustTier(source.getTrustTier());
        evidence.setExcerptHash(sha256(evidence.getExcerpt()));
        mapper.insertEvidence(evidence);
        topicMapper.attachChangeForSource(source.getSourceId(), change.getChangeId());
        mapper.linkSignalToChange(signal.getSignalId(), change.getChangeId());
        mapper.insertActivity(signal.getSignalId(), change.getChangeId(), source.getSourceId(), "SOURCE_CHECK", "COMPLETED",
                "已定位原始潮源，正在判断是否形成潮变");
        mapper.insertActivity(signal.getSignalId(), change.getChangeId(), source.getSourceId(), "CHANGE_ASSESSMENT", "IN_PROGRESS",
                "正在比对变化内容，等待最终校准");
        if (outboxService != null) {
            outboxService.enqueue("CHANGE_CANDIDATE_CREATED", "CHANGE", String.valueOf(change.getChangeId()), 1,
                    "zentide-change", Map.of("changeId", change.getChangeId(), "sourceId", source.getSourceId(),
                            "documentVersionId", version.getDocumentVersionId(), "changeType", change.getChangeType()));
        }
        return change.getChangeId();
    }

    private String excerpt(String preferred, String fallback) {
        String value = preferred == null || preferred.isBlank() ? fallback : preferred;
        return value.length() > 4_000 ? value.substring(0, 4_000) : value;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}

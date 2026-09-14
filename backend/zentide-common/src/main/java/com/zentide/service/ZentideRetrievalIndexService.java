package com.zentide.service;

import com.zentide.entity.po.ZentideDocumentVersion;
import com.zentide.entity.po.ZentideRetrievalChunk;
import com.zentide.mapper.ZentideRetrievalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class ZentideRetrievalIndexService {
    private static final int CHUNK_SIZE = 900;
    private static final int CHUNK_OVERLAP = 160;

    private final ZentideRetrievalMapper mapper;

    public ZentideRetrievalIndexService(ZentideRetrievalMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public int index(ZentideDocumentVersion version) {
        if (version == null || version.getDocumentVersionId() == null) {
            throw new IllegalArgumentException("文档版本不能为空");
        }
        String content = version.getNormalizedContent() == null ? "" : version.getNormalizedContent().trim();
        mapper.deleteByDocumentVersionId(version.getDocumentVersionId());
        if (content.isBlank()) return 0;

        int chunkNo = 0;
        for (int start = 0; start < content.length();) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            if (end < content.length()) end = preferredBoundary(content, start, end);
            String text = content.substring(start, end).trim();
            if (!text.isBlank()) {
                ZentideRetrievalChunk chunk = new ZentideRetrievalChunk();
                chunk.setDocumentVersionId(version.getDocumentVersionId());
                chunk.setChunkNo(chunkNo++);
                chunk.setContentStart(start);
                chunk.setContentEnd(end);
                chunk.setChunkText(text);
                chunk.setContentHash(sha256(text));
                mapper.insertChunk(chunk);
            }
            if (end >= content.length()) break;
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunkNo;
    }

    public int rebuildMissing(Integer requestedLimit) {
        int limit = requestedLimit == null ? 100 : Math.max(1, Math.min(500, requestedLimit));
        List<ZentideDocumentVersion> versions = mapper.listUnindexedDocumentVersions(limit);
        for (ZentideDocumentVersion version : versions) index(version);
        return versions.size();
    }

    private int preferredBoundary(String content, int start, int suggestedEnd) {
        int floor = Math.max(start + CHUNK_SIZE / 2, suggestedEnd - 160);
        for (int cursor = suggestedEnd; cursor > floor; cursor--) {
            char value = content.charAt(cursor - 1);
            if (value == '\n' || value == '。' || value == '！' || value == '？' || value == '.' || value == '!' || value == '?') return cursor;
        }
        return suggestedEnd;
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

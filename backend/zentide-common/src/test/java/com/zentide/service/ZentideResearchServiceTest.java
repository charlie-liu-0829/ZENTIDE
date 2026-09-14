package com.zentide.service;

import com.zentide.entity.po.ZentideDocumentVersion;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideRetrievalMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZentideResearchServiceTest {
    @Test
    void researchSearchIsBoundedAndDelegatesOnlyTheQuestion() {
        ZentideRetrievalMapper mapper = mock(ZentideRetrievalMapper.class);

        new ZentideResearchService(mapper).search("Spring Boot 4", 100);

        verify(mapper).searchVerified("Spring Boot 4", 20);
    }

    @Test
    void researchSearchRejectsUnusableQuestions() {
        ZentideRetrievalMapper mapper = mock(ZentideRetrievalMapper.class);

        assertThrows(BusinessException.class, () -> new ZentideResearchService(mapper).search("x", null));
        verify(mapper, never()).searchVerified(any(), anyInt());
    }

    @Test
    void indexingCreatesOverlappingTraceableChunks() {
        ZentideRetrievalMapper mapper = mock(ZentideRetrievalMapper.class);
        ZentideDocumentVersion version = new ZentideDocumentVersion();
        version.setDocumentVersionId(8L);
        version.setNormalizedContent("a".repeat(1_900));

        int count = new ZentideRetrievalIndexService(mapper).index(version);

        assertEquals(3, count);
        verify(mapper).deleteByDocumentVersionId(8L);
        verify(mapper, times(3)).insertChunk(any());
    }

    @Test
    void rebuildOnlyVisitsVersionsWithoutAnActiveIndex() {
        ZentideRetrievalMapper mapper = mock(ZentideRetrievalMapper.class);
        ZentideDocumentVersion version = new ZentideDocumentVersion();
        version.setDocumentVersionId(11L);
        version.setNormalizedContent("A verified source document.");
        when(mapper.listUnindexedDocumentVersions(500)).thenReturn(java.util.List.of(version));

        int rebuilt = new ZentideRetrievalIndexService(mapper).rebuildMissing(999);

        assertEquals(1, rebuilt);
        verify(mapper).listUnindexedDocumentVersions(500);
        verify(mapper).insertChunk(any());
    }
}

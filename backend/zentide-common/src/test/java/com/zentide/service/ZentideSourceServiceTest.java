package com.zentide.service;

import com.zentide.entity.po.ZentideDocument;
import com.zentide.entity.po.ZentideFetchAttempt;
import com.zentide.entity.po.ZentideRawSnapshot;
import com.zentide.entity.po.ZentideSource;
import com.zentide.entity.po.ZentideDocumentVersion;
import com.zentide.mapper.ZentideSourceMapper;
import com.zentide.source.GitHubReleaseSourceAdapter;
import com.zentide.source.SourceFetchResult;
import com.zentide.source.SourceFetcher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZentideSourceServiceTest {
    @Test
    void persistsSnapshotAndOnlyCreatesANewDocumentVersionForNewContent() {
        ZentideSourceMapper mapper = mock(ZentideSourceMapper.class);
        SourceFetcher fetcher = mock(SourceFetcher.class);
        ZentideSource source = new ZentideSource();
        source.setSourceId(5L);
        source.setSourceType("GITHUB_RELEASES");
        source.setStatus("ACTIVE");
        source.setCanonicalUrl("https://api.github.com/repos/acme/demo/releases");
        when(mapper.findSource(5L)).thenReturn(source);
        doAnswer(invocation -> { invocation.<ZentideFetchAttempt>getArgument(0).setAttemptId(11L); return 1; }).when(mapper).insertAttempt(any());
        doAnswer(invocation -> { invocation.<ZentideRawSnapshot>getArgument(0).setRawSnapshotId(12L); return 1; }).when(mapper).insertSnapshot(any());
        when(mapper.findDocument(eq(5L), eq("github-release:v1.2.0"))).thenReturn(null);
        doAnswer(invocation -> { invocation.<ZentideDocument>getArgument(0).setDocumentId(13L); return 1; }).when(mapper).insertDocument(any());
        when(fetcher.fetch(source)).thenReturn(new SourceFetchResult(source.getCanonicalUrl(), 200, "application/json",
                "[{\"tag_name\":\"v1.2.0\",\"html_url\":\"https://github.com/acme/demo/releases/v1.2.0\",\"body\":\"Notes\"}]".getBytes(StandardCharsets.UTF_8)));

        doAnswer(invocation -> { invocation.<ZentideDocumentVersion>getArgument(0).setDocumentVersionId(14L); return 1; }).when(mapper).insertDocumentVersion(any());
        ZentideRetrievalIndexService indexService = mock(ZentideRetrievalIndexService.class);
        ZentideSourceService.IngestionOutcome result = new ZentideSourceService(mapper, fetcher, List.of(new GitHubReleaseSourceAdapter()), mock(ZentideChangeCandidateService.class), indexService).fetch(5L);

        assertEquals("SUCCEEDED", result.status());
        assertEquals(1, result.documentsFound());
        assertEquals(1, result.versionsCreated());
        verify(mapper).insertDocumentVersion(any());
        verify(indexService).index(any(ZentideDocumentVersion.class));
        verify(mapper).finishAttempt(argThat(attempt -> "SUCCEEDED".equals(attempt.getStatus())));
    }

    @Test
    void doesNotCreateAnotherVersionWhenTheDocumentHashAlreadyExists() {
        ZentideSourceMapper mapper = mock(ZentideSourceMapper.class);
        SourceFetcher fetcher = mock(SourceFetcher.class);
        ZentideSource source = new ZentideSource();
        source.setSourceId(5L);
        source.setSourceType("GITHUB_RELEASES");
        source.setStatus("ACTIVE");
        source.setCanonicalUrl("https://api.github.com/repos/acme/demo/releases");
        ZentideDocument document = new ZentideDocument();
        document.setDocumentId(13L);
        document.setCurrentVersionNo(1);
        when(mapper.findSource(5L)).thenReturn(source);
        doAnswer(invocation -> { invocation.<ZentideFetchAttempt>getArgument(0).setAttemptId(11L); return 1; }).when(mapper).insertAttempt(any());
        doAnswer(invocation -> { invocation.<ZentideRawSnapshot>getArgument(0).setRawSnapshotId(12L); return 1; }).when(mapper).insertSnapshot(any());
        when(mapper.findDocument(eq(5L), eq("github-release:v1.2.0"))).thenReturn(document);
        when(mapper.findVersionByHash(eq(13L), any())).thenReturn(new ZentideDocumentVersion());
        when(fetcher.fetch(source)).thenReturn(new SourceFetchResult(source.getCanonicalUrl(), 200, "application/json",
                "[{\"tag_name\":\"v1.2.0\",\"html_url\":\"https://github.com/acme/demo/releases/v1.2.0\",\"body\":\"Notes\"}]".getBytes(StandardCharsets.UTF_8)));

        ZentideSourceService.IngestionOutcome result = new ZentideSourceService(mapper, fetcher, List.of(new GitHubReleaseSourceAdapter()), mock(ZentideChangeCandidateService.class), mock(ZentideRetrievalIndexService.class)).fetch(5L);

        assertEquals(0, result.versionsCreated());
        verify(mapper, never()).insertDocumentVersion(any());
    }
}

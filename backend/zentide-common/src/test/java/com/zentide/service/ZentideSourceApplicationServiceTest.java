package com.zentide.service;

import com.zentide.entity.po.ZentideSource;
import com.zentide.entity.po.ZentideSourceApplication;
import com.zentide.mapper.ZentideSourceApplicationMapper;
import com.zentide.source.GitHubReleaseSourceAdapter;
import com.zentide.source.SourceFetchResult;
import com.zentide.source.SourceFetcher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZentideSourceApplicationServiceTest {
    @Test
    void onlyActivatesAfterSuccessfulTrialAndAttachesTheTopic() {
        ZentideSourceApplicationMapper mapper = mock(ZentideSourceApplicationMapper.class);
        SourceFetcher fetcher = mock(SourceFetcher.class);
        ZentideSourceApplication application = new ZentideSourceApplication();
        application.setApplicationId(7L); application.setTopicId(3L); application.setApplicantId("U1");
        application.setSourceName("Demo releases"); application.setSourceType("GITHUB_RELEASES");
        application.setCanonicalUrl("https://api.github.com/repos/acme/demo/releases"); application.setCanonicalUrlHash("hash");
        application.setTrustTier("COMMUNITY"); application.setStatus("APPROVED");
        when(mapper.find(7L)).thenReturn(application);
        when(mapper.startTrial(7L)).thenReturn(1);
        when(fetcher.fetch(any())).thenReturn(new SourceFetchResult(application.getCanonicalUrl(), 200, "application/json",
                "[{\"tag_name\":\"v1.0.0\",\"html_url\":\"https://github.com/acme/demo/releases/v1.0.0\",\"body\":\"Notes\"}]".getBytes(StandardCharsets.UTF_8)));
        ZentideSourceApplicationService service = new ZentideSourceApplicationService(mapper, fetcher, List.of(new GitHubReleaseSourceAdapter()));

        ZentideSourceApplicationService.TrialOutcome outcome = service.runTrial(7L, "admin");
        assertEquals("TRIAL_PASSED", outcome.status());
        verify(mapper).finishTrial(eq(7L), eq("TRIAL_PASSED"), any(), any());

        application.setStatus("TRIAL_PASSED");
        when(mapper.findSourceByUrlHash("hash")).thenReturn(null);
        doAnswer(invocation -> { invocation.<ZentideSource>getArgument(0).setSourceId(9L); return 1; }).when(mapper).insertActiveSource(any());
        when(mapper.activate(eq(7L), eq(9L), eq("admin"), any())).thenReturn(1);
        ZentideSource source = service.activate(7L, "admin");

        assertEquals(9L, source.getSourceId());
        verify(mapper).attachTopicSource(3L, 9L, "GITHUB", "U1");
    }
}

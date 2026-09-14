package com.zentide.source;

import com.zentide.entity.po.ZentideSource;
import com.zentide.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceAdapterTest {
    @Test
    void parsesPublishedGitHubReleasesAndSkipsDrafts() {
        String payload = "[{\"tag_name\":\"v1.2.0\",\"name\":\"Release 1.2\",\"html_url\":\"https://github.com/acme/demo/releases/v1.2.0\",\"body\":\"Fixed a security issue\",\"published_at\":\"2026-08-26T01:00:00Z\"},{\"tag_name\":\"v1.3.0\",\"draft\":true,\"html_url\":\"https://github.com/acme/demo/releases/v1.3.0\"}]";

        List<NormalizedDocument> documents = new GitHubReleaseSourceAdapter().parse(source("GITHUB_RELEASES"),
                new SourceFetchResult("https://api.github.com/repos/acme/demo/releases", 200, "application/json", payload.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, documents.size());
        assertEquals("github-release:v1.2.0", documents.getFirst().canonicalKey());
        assertEquals("Release 1.2", documents.getFirst().title());
    }

    @Test
    void parsesAtomEntriesWithAlternateLinks() {
        String payload = "<feed xmlns=\"http://www.w3.org/2005/Atom\"><entry><id>tag:example.org,2026:1</id><title>Runtime update</title><link rel=\"alternate\" href=\"https://example.org/update\"/><updated>2026-08-26T01:00:00Z</updated><summary>Version updated</summary></entry></feed>";

        List<NormalizedDocument> documents = new RssAtomSourceAdapter().parse(source("RSS_ATOM"),
                new SourceFetchResult("https://example.org/feed.xml", 200, "application/atom+xml", payload.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, documents.size());
        assertEquals("feed-entry:tag:example.org,2026:1", documents.getFirst().canonicalKey());
    }

    @Test
    void prefersAtomAlternateLinkWhenEnclosureAppearsFirst() {
        String payload = "<feed xmlns=\"http://www.w3.org/2005/Atom\"><entry><id>tag:example.org,2026:2</id><title>Update</title><link rel=\"enclosure\" href=\"https://cdn.example.org/update.tgz\"/><link rel=\"alternate\" href=\"https://example.org/update\"/><summary>Details</summary></entry></feed>";
        List<NormalizedDocument> documents = new RssAtomSourceAdapter().parse(source("RSS_ATOM"),
                new SourceFetchResult("https://example.org/feed.xml", 200, "application/atom+xml", payload.getBytes(StandardCharsets.UTF_8)));
        assertEquals("https://example.org/update", documents.getFirst().sourceUrl());
    }

    @Test
    void rejectsExternalEntityXml() {
        String payload = "<!DOCTYPE feed [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><feed><entry><title>&xxe;</title></entry></feed>";

        assertThrows(BusinessException.class, () -> new RssAtomSourceAdapter().parse(source("RSS_ATOM"),
                new SourceFetchResult("https://example.org/feed.xml", 200, "application/xml", payload.getBytes(StandardCharsets.UTF_8))));
    }

    private ZentideSource source(String type) {
        ZentideSource source = new ZentideSource();
        source.setSourceType(type);
        return source;
    }
}

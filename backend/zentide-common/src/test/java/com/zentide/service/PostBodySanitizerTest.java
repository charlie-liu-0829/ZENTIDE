package com.zentide.service;

import com.zentide.exception.BusinessException;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostBodySanitizerTest {
    @Test
    void preservesSupportedRichTextStructure() {
        String clean = PostBodySanitizer.sanitize("<h2>现场观察</h2><p>这是<strong>重点</strong>和<em>补充</em>。</p><ul><li>第一点</li></ul>");

        assertTrue(clean.contains("<h2>现场观察</h2>"));
        assertTrue(clean.contains("<strong>重点</strong>"));
        assertTrue(clean.contains("<ul><li>第一点</li></ul>"));
    }

    @Test
    void removesScriptsHandlersAndUnsupportedMedia() {
        String clean = PostBodySanitizer.sanitize("<p onclick=\"alert(1)\">安全正文<script>alert(1)</script><img src=x onerror=alert(1)></p>");

        assertEquals("安全正文", Jsoup.parseBodyFragment(clean).body().text());
        assertFalse(clean.contains("script"));
        assertFalse(clean.contains("onclick"));
        assertFalse(clean.contains("img"));
    }

    @Test
    void keepsOnlySafeLinksAndAddsIsolationAttributes() {
        String clean = PostBodySanitizer.sanitize("<p><a href=\"javascript:alert(1)\">危险</a> <a href=\"https://example.com/a\">来源</a></p>");

        assertFalse(clean.contains("javascript:"));
        assertTrue(clean.contains("href=\"https://example.com/a\""));
        assertTrue(clean.contains("target=\"_blank\""));
        assertTrue(clean.contains("rel=\"nofollow noopener noreferrer\""));
    }

    @Test
    void convertsLegacyPlainTextIntoParagraphsAndBreaks() {
        String clean = PostBodySanitizer.sanitize("第一行\n第二行\n\n第二段");

        assertEquals("<p>第一行<br>第二行</p><p>第二段</p>", clean);
    }

    @Test
    void keepsOnlyUploadedInlineImagesAndVideos() {
        String clean = PostBodySanitizer.sanitize("<p>现场记录</p><img src=\"/api/file/getResource?sourceName=202608%2Fabcdefghijklmnopqrstuvwx123456.jpg\"><video autoplay src=\"/api/file/getResource?sourceName=202608%2Fabcdefghijklmnopqrstuvwx123456.mp4\"></video><img src=\"https://evil.example/x.jpg\"><video src=\"javascript:alert(1)\"></video>");

        assertTrue(clean.contains("202608%2Fabcdefghijklmnopqrstuvwx123456.jpg"));
        assertTrue(clean.contains("loading=\"lazy\""));
        assertTrue(clean.contains("202608%2Fabcdefghijklmnopqrstuvwx123456.mp4"));
        assertTrue(clean.contains("controls"));
        assertFalse(clean.contains("evil.example"));
        assertFalse(clean.contains("javascript:"));
        assertFalse(clean.contains("autoplay"));
    }

    @Test
    void rejectsFormattingWithoutMeaningfulContent() {
        assertThrows(BusinessException.class, () -> PostBodySanitizer.sanitize("<p><strong> </strong><br></p>"));
    }
}

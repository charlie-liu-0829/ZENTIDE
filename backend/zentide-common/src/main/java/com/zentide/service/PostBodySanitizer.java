package com.zentide.service;

import com.zentide.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class PostBodySanitizer {
    private static final int MAX_TEXT_LENGTH = 8_000;
    private static final int MAX_HTML_BYTES = 60_000;
    private static final int MAX_REQUEST_LENGTH = 120_000;
    private static final Pattern HTML_TAG = Pattern.compile("(?is).*<\\s*/?\\s*[a-z][^>]*>.*");
    private static final Pattern STORED_MEDIA = Pattern.compile("\\d{6}/[A-Za-z0-9_-]{8,80}\\.[A-Za-z0-9]{2,8}");
    private static final String RESOURCE_PREFIX = "/api/file/getResource?sourceName=";
    private static final Set<String> IMAGE_SUFFIXES = Set.of("jpg", "jpeg", "png", "gif", "webp", "avif");
    private static final Set<String> VIDEO_SUFFIXES = Set.of("mp4", "webm", "mov");
    private static final Safelist ALLOWED = Safelist.none()
            .addTags("p", "br", "h2", "h3", "strong", "b", "em", "i", "u", "s", "strike",
                    "blockquote", "ul", "ol", "li", "code", "pre", "a", "img", "video")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("img", "src", "alt", "title", "loading", "decoding")
            .addAttributes("video", "src", "controls", "preload", "playsinline")
            .addProtocols("a", "href", "http", "https");

    private PostBodySanitizer() {
    }

    static String sanitize(String requested) {
        String source = requested == null ? "" : requested.trim();
        if (source.length() > MAX_REQUEST_LENGTH) {
            throw new BusinessException("正文内容过长");
        }
        if (!source.isEmpty() && !HTML_TAG.matcher(source).matches()) {
            source = plainTextToHtml(source);
        }

        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        String cleanHtml = Jsoup.clean(source, "", ALLOWED, outputSettings);
        Document fragment = Jsoup.parseBodyFragment(cleanHtml);
        fragment.outputSettings().prettyPrint(false);
        for (Element link : fragment.select("a")) {
            if (!link.hasAttr("href")) {
                link.removeAttr("target").removeAttr("rel");
                continue;
            }
            link.attr("target", "_blank");
            link.attr("rel", "nofollow noopener noreferrer");
        }
        for (Element image : fragment.select("img")) {
            if (!isInternalMedia(image.attr("src"), IMAGE_SUFFIXES)) {
                image.remove();
                continue;
            }
            image.attr("loading", "lazy");
            image.attr("decoding", "async");
        }
        for (Element video : fragment.select("video")) {
            if (!isInternalMedia(video.attr("src"), VIDEO_SUFFIXES)) {
                video.remove();
                continue;
            }
            video.attr("controls", "");
            video.attr("preload", "metadata");
            video.attr("playsinline", "");
        }

        String normalizedHtml = fragment.body().html().trim();
        String plainText = fragment.body().text().replace('\u00A0', ' ').trim();
        if (plainText.length() < 2 || plainText.length() > MAX_TEXT_LENGTH) {
            throw new BusinessException("内容需要是 2 到 8000 个字符");
        }
        if (normalizedHtml.getBytes(StandardCharsets.UTF_8).length > MAX_HTML_BYTES) {
            throw new BusinessException("正文格式过于复杂，请适当减少格式后重试");
        }
        return normalizedHtml;
    }

    private static boolean isInternalMedia(String source, Set<String> allowedSuffixes) {
        if (source == null || !source.startsWith(RESOURCE_PREFIX)) return false;
        String encodedName = source.substring(RESOURCE_PREFIX.length());
        if (encodedName.isBlank() || encodedName.contains("&") || encodedName.contains("#")) return false;
        String storedName;
        try {
            storedName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!STORED_MEDIA.matcher(storedName).matches()) return false;
        int suffixIndex = storedName.lastIndexOf('.');
        return suffixIndex > 0 && allowedSuffixes.contains(storedName.substring(suffixIndex + 1).toLowerCase(Locale.ROOT));
    }

    private static String plainTextToHtml(String source) {
        Document document = Document.createShell("");
        document.outputSettings().prettyPrint(false);
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        for (String paragraph : normalized.split("\\n{2,}")) {
            Element paragraphElement = document.body().appendElement("p");
            String[] lines = paragraph.split("\\n", -1);
            for (int index = 0; index < lines.length; index++) {
                paragraphElement.appendChild(new TextNode(lines[index]));
                if (index < lines.length - 1) paragraphElement.appendElement("br");
            }
        }
        return document.body().html();
    }
}

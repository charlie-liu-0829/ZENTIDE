package com.zentide.source;

import com.zentide.entity.po.ZentideSource;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class SafeHttpSourceFetcher implements SourceFetcher {
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/json", "application/rss+xml", "application/atom+xml", "application/xml", "text/xml"
    );

    @Override
    public SourceFetchResult fetch(ZentideSource source) {
        URI uri = validateUrl(source.getCanonicalUrl());
        List<InetAddress> pinned = resolvePublic(uri.getHost());
        Dns dns = hostname -> hostname.equalsIgnoreCase(uri.getHost()) ? pinned : resolvePublic(hostname);
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .dns(dns)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(Duration.ofSeconds(8))
                .readTimeout(Duration.ofSeconds(20))
                .callTimeout(Duration.ofSeconds(25))
                .build();
        Request request = new Request.Builder().url(uri.toString())
                .header("User-Agent", "ZENTIDE/1.0 (+evidence-first-source-fetch)")
                .header("Accept", "application/vnd.github+json,application/json,application/rss+xml,application/atom+xml,application/xml,text/xml")
                .get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new ZentideFetchException("HTTP_STATUS", "来源返回 HTTP " + response.code());
            String contentType = mediaType(response);
            if (!ALLOWED_TYPES.contains(contentType)) {
                throw new ZentideFetchException("CONTENT_TYPE_NOT_ALLOWED", "来源响应不是允许的 JSON 或 Feed 类型");
            }
            return new SourceFetchResult(uri.toString(), response.code(), contentType, readBody(response.body()));
        } catch (ZentideFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new ZentideFetchException("FETCH_ERROR", "来源抓取失败");
        }
    }

    private URI validateUrl(String value) {
        try {
            URI uri = URI.create(value).normalize();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!("https".equals(scheme) || "http".equals(scheme)) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new ZentideFetchException("INVALID_URL", "来源 URL 必须是无凭据的 HTTP 或 HTTPS 公开地址");
            }
            if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) {
                throw new ZentideFetchException("INVALID_URL", "来源 URL 只允许使用 80 或 443 端口");
            }
            return uri;
        } catch (ZentideFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new ZentideFetchException("INVALID_URL", "来源 URL 格式不正确");
        }
    }

    private List<InetAddress> resolvePublic(String hostname) {
        try {
            List<InetAddress> addresses = new ArrayList<>();
            for (InetAddress address : InetAddress.getAllByName(hostname)) {
                if (!isPublic(address)) throw new ZentideFetchException("PRIVATE_OR_RESERVED_IP", "来源域名解析到非公开地址");
                addresses.add(address);
            }
            if (addresses.isEmpty()) throw new ZentideFetchException("DNS_EMPTY", "来源域名没有可用公开地址");
            return List.copyOf(addresses);
        } catch (ZentideFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new ZentideFetchException("DNS_ERROR", "来源域名解析失败");
        }
    }

    private boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            if (first == 0 || first == 10 || first == 127 || first >= 224 || (first == 100 && second >= 64 && second <= 127)) return false;
            if (first == 169 && second == 254 || first == 172 && second >= 16 && second <= 31 || first == 192 && second == 168) return false;
            return !(first == 192 && second == 0) && !(first == 198 && (second == 18 || second == 19)) && !(first == 198 && second == 51 && third == 100);
        }
        if (address instanceof Inet6Address && bytes.length == 16) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return !((first & 0xfe) == 0xfc) && !(first == 0xfe && (second & 0xc0) == 0x80);
        }
        return false;
    }

    private String mediaType(Response response) {
        return response.body() == null || response.body().contentType() == null ? "" : response.body().contentType().type() + "/" + response.body().contentType().subtype();
    }

    private byte[] readBody(ResponseBody body) throws Exception {
        if (body == null) throw new ZentideFetchException("EMPTY_RESPONSE", "来源没有返回内容");
        if (body.contentLength() > MAX_RESPONSE_BYTES) throw new ZentideFetchException("RESPONSE_TOO_LARGE", "来源响应超过 2MB 限制");
        try (InputStream input = body.byteStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (output.size() + count > MAX_RESPONSE_BYTES) throw new ZentideFetchException("RESPONSE_TOO_LARGE", "来源响应超过 2MB 限制");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
}

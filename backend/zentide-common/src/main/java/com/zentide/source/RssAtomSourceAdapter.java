package com.zentide.source;

import com.zentide.entity.po.ZentideSource;
import com.zentide.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class RssAtomSourceAdapter implements SourceAdapter {
    private static final int MAX_ITEMS = 200;

    @Override
    public String sourceType() {
        return "RSS_ATOM";
    }

    @Override
    public List<NormalizedDocument> parse(ZentideSource source, SourceFetchResult result) {
        Document document = parseXml(result.body());
        Element root = document.getDocumentElement();
        String rootName = root.getLocalName() == null ? root.getNodeName() : root.getLocalName();
        return "feed".equalsIgnoreCase(rootName) ? atom(root) : rss(root);
    }

    private List<NormalizedDocument> rss(Element root) {
        return documents(root.getElementsByTagName("item"), false);
    }

    private List<NormalizedDocument> atom(Element root) {
        return documents(root.getElementsByTagNameNS("*", "entry"), true);
    }

    private List<NormalizedDocument> documents(NodeList items, boolean atom) {
        List<NormalizedDocument> documents = new ArrayList<>();
        for (int index = 0; index < Math.min(items.getLength(), MAX_ITEMS); index++) {
            Element item = (Element) items.item(index);
            String title = child(item, "title");
            String id = atom ? child(item, "id") : child(item, "guid");
            String link = atom ? atomLink(item) : child(item, "link");
            if (id.isBlank()) id = link;
            if (id.isBlank() || title.isBlank()) continue;
            String summary = atom ? child(item, "summary") : child(item, "description");
            String content = "title: " + title + "\nurl: " + link + "\n\n" + summary;
            String time = atom ? first(item, "updated", "published") : first(item, "pubDate", "date");
            documents.add(new NormalizedDocument("feed-entry:" + id, title, content,
                    ContentHash.sha256(content), parseTime(time), link, summary));
        }
        return List.copyOf(documents);
    }

    private Document parseXml(byte[] body) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            return builder.parse(new ByteArrayInputStream(body));
        } catch (Exception e) {
            throw new BusinessException("RSS / Atom 响应不是安全有效的 XML");
        }
    }

    private String atomLink(Element item) {
        NodeList links = item.getElementsByTagNameNS("*", "link");
        String fallback = "";
        for (int index = 0; index < links.getLength(); index++) {
            Element link = (Element) links.item(index);
            String href = link.getAttribute("href").trim();
            if (href.isBlank()) continue;
            if (fallback.isBlank()) fallback = href;
            if ("alternate".equalsIgnoreCase(link.getAttribute("rel").trim())) return href;
        }
        return fallback;
    }

    private String first(Element item, String first, String second) {
        String value = child(item, first);
        return value.isBlank() ? child(item, second) : value;
    }

    private String child(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            String childName = child.getLocalName() == null ? child.getNodeName() : child.getLocalName();
            if (name.equalsIgnoreCase(childName)) return child.getTextContent().trim();
        }
        return "";
    }

    private LocalDateTime parseTime(String value) {
        try {
            return value == null || value.isBlank() ? null : OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return DateTimeFormatter.RFC_1123_DATE_TIME.parse(value, OffsetDateTime::from).toLocalDateTime();
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }
}

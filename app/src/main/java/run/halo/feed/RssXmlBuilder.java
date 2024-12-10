package run.halo.feed;

import com.google.common.base.Throwables;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import run.halo.feed.telemetry.TelemetryEndpoint;

@Slf4j
public class RssXmlBuilder {
    private RSS2 rss2;
    private String generator = "Halo v2.0";
    private String extractRssTags;
    private Instant lastBuildDate = Instant.now();
    private String externalUrl;

    public RssXmlBuilder withRss2(RSS2 rss2) {
        this.rss2 = rss2;
        return this;
    }

    /**
     * For test.
     */
    RssXmlBuilder withGenerator(String generator) {
        this.generator = generator;
        return this;
    }

    public RssXmlBuilder withExtractRssTags(String extractRssTags) {
        this.extractRssTags = extractRssTags;
        return this;
    }

    /**
     * For test.
     */
    RssXmlBuilder withLastBuildDate(Instant lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
        return this;
    }

    RssXmlBuilder withExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
        return this;
    }

    public String toXmlString() {
        Document document = DocumentHelper.createDocument();

        Element root = DocumentHelper.createElement("rss");
        root.addAttribute("version", "2.0");
        root.addNamespace("media", "http://search.yahoo.com/mrss/");
        document.setRootElement(root);

        Element channel = root.addElement("channel");
        channel.addElement("title").addText(rss2.getTitle());
        channel.addElement("link").addText(rss2.getLink());

        var description = StringUtils.defaultIfBlank(rss2.getDescription(), rss2.getTitle());
        var secureDescription = XmlCharUtils.removeInvalidXmlChar(description);
        channel.addElement("description").addText(secureDescription);
        channel.addElement("generator").addText(generator);

        channel.addElement("language")
            .addText(StringUtils.defaultIfBlank(rss2.getLanguage(), "zh-cn"));

        if (StringUtils.isNotBlank(rss2.getImage())) {
            Element imageElement = channel.addElement("image");
            imageElement.addElement("url").addText(rss2.getImage());
            imageElement.addElement("title").addText(rss2.getTitle());
            imageElement.addElement("link").addText(rss2.getLink());
        }
        channel.addElement("lastBuildDate")
            .addText(instantToString(lastBuildDate));

        if (StringUtils.isNotBlank(extractRssTags)) {
            try {
                var extractRssTagsElement = parseXmlString(extractRssTags);
                for (Element element : extractRssTagsElement.elements()) {
                    Element newElement = channel.addElement(element.getName());
                    copyAttributesAndChildren(newElement, element);
                }
            } catch (Throwable e) {
                // ignore
                log.error("无法注入自定义的 RSS 标签, 确保是正确的 XML 格式",
                    Throwables.getRootCause(e));
            }
        }

        var items = rss2.getItems();
        createItemElementsToChannel(channel, items);

        return document.asXML();
    }

    private void copyAttributesAndChildren(Element target, Element source) {
        for (var attribute : source.attributes()) {
            target.addAttribute(attribute.getName(), attribute.getValue());
        }
        if (source.getTextTrim() != null) {
            target.setText(source.getTextTrim());
        }
        for (Element child : source.elements()) {
            Element newChild = target.addElement(child.getName());
            copyAttributesAndChildren(newChild, child);
        }
    }

    private Element parseXmlString(String xml) throws DocumentException {
        // for SAXReader to load class from current classloader
        var originalContextClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(new StringReader("""
                <root>
                %s
                </root>
                """.formatted(xml)));
            return document.getRootElement();
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassloader);
        }
    }

    private void createItemElementsToChannel(Element channel, List<RSS2.Item> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        items.forEach(item -> createItemElementToChannel(channel, item));
    }

    private void createItemElementToChannel(Element channel, RSS2.Item item) {
        Element itemElement = channel.addElement("item");
        itemElement.addElement("title").addCDATA(item.getTitle());
        itemElement.addElement("link").addText(item.getLink());
        var description = getDescriptionWithTelemetry(item);
        itemElement.addElement("description").addCDATA(description);
        itemElement.addElement("guid")
            .addAttribute("isPermaLink", "false")
            .addText(item.getGuid());

        if (StringUtils.isNotBlank(item.getAuthor())) {
            itemElement.addElement("author").addText(item.getAuthor());
        }

        if (StringUtils.isNotBlank(item.getEnclosureUrl())) {
            itemElement.addElement("enclosure")
                .addAttribute("url", item.getEnclosureUrl())
                .addAttribute("length", item.getEnclosureLength())
                .addAttribute("type", item.getEnclosureType());
        }

        nullSafeList(item.getCategories())
            .forEach(category -> itemElement.addElement("category").addText(category));

        itemElement.addElement("pubDate")
            .addText(instantToString(item.getPubDate()));

        // support for media:content
        nullSafeList(item.getMediaContents()).forEach(mediaContent -> {
            Element mediaElement = itemElement.addElement("media:content")
                .addAttribute("url", mediaContent.getUrl())
                .addAttribute("type", mediaContent.getType());

            if (mediaContent.getMediaType() != null) {
                mediaElement.addAttribute("medium",
                    mediaContent.getMediaType().name().toLowerCase());
            }

            if (StringUtils.isNotBlank(mediaContent.getFileSize())) {
                mediaElement.addAttribute("fileSize", mediaContent.getFileSize());
            }
            if (StringUtils.isNotBlank(mediaContent.getDuration())) {
                mediaElement.addAttribute("duration", mediaContent.getDuration());
            }
            if (StringUtils.isNotBlank(mediaContent.getHeight())) {
                mediaElement.addAttribute("height", mediaContent.getHeight());
            }
            if (StringUtils.isNotBlank(mediaContent.getWidth())) {
                mediaElement.addAttribute("width", mediaContent.getWidth());
            }

            // add nested media:thumbnail
            var thumbnail = mediaContent.getThumbnail();
            if (thumbnail != null) {
                Element thumbnailElement = mediaElement.addElement("media:thumbnail")
                    .addAttribute("url", thumbnail.getUrl());

                if (StringUtils.isNotBlank(thumbnail.getHeight())) {
                    thumbnailElement.addAttribute("height", thumbnail.getHeight());
                }

                if (StringUtils.isNotBlank(thumbnail.getWidth())) {
                    thumbnailElement.addAttribute("width", thumbnail.getWidth());
                }
            }
        });
    }

    private String getDescriptionWithTelemetry(RSS2.Item item) {
        if (StringUtils.isBlank(externalUrl)) {
            return item.getDescription();
        }
        var uri = UriComponentsBuilder.fromUriString(item.getLink())
            .build();
        var telemetryBaseUri = externalUrl + TelemetryEndpoint.TELEMETRY_PATH;
        var telemetryUri = UriComponentsBuilder.fromUriString(telemetryBaseUri)
            .queryParam("title", UriUtils.encode(item.getTitle(), StandardCharsets.UTF_8))
            .queryParam("url", uri.getPath())
            .build(true)
            .toUriString();

        // Build the telemetry image HTML
        var telemetryImageHtml = String.format(
            "<img src=\"%s\" width=\"1\" height=\"1\" alt=\"\" style=\"opacity:0;\" />",
            telemetryUri
        );

        // Append telemetry image to description
        return telemetryImageHtml + item.getDescription();
    }

    static <T> List<T> nullSafeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    static String instantToString(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}

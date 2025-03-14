package run.halo.feed;

import com.google.common.base.Throwables;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import run.halo.feed.telemetry.TelemetryEndpoint;

@Slf4j
public class RssXmlBuilder {
    static final String UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/131.0.0.0 Safari/537.36";
    private final WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
            .responseTimeout(Duration.ofSeconds(1))
            .followRedirect(true))
        )
        .build();

    private RSS2 rss2;
    private String generator = "Halo v2.0";
    private String extractRssTags;
    private Instant lastBuildDate = Instant.now();
    private String requestPath;
    private String externalUrl;

    public RssXmlBuilder withRss2(RSS2 rss2) {
        this.rss2 = rss2;
        return this;
    }

    public RssXmlBuilder withRequestPath(String requestPath) {
        this.requestPath = requestPath;
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

    public Mono<String> toXmlString() {
        Document document = DocumentHelper.createDocument();

        Element root = DocumentHelper.createElement("rss");
        root.addAttribute("version", "2.0");
        root.addNamespace("dc", "http://purl.org/dc/elements/1.1/");
        root.addNamespace("atom", "http://www.w3.org/2005/Atom");
        root.addNamespace("media", "http://search.yahoo.com/mrss/");
        document.setRootElement(root);

        Element channel = root.addElement("channel");
        channel.addElement("title").addText(rss2.getTitle());
        channel.addElement("link").addText(rss2.getLink());
        if (StringUtils.isNotBlank(requestPath)) {
            channel.addElement("atom:link")
                .addAttribute("href", UriComponentsBuilder.fromUriString(rss2.getLink())
                    .path(requestPath).toUriString()
                )
                .addAttribute("rel", "self")
                .addAttribute("type", "application/rss+xml");
        }

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

        return createItemElementsToChannel(channel, items)
            .thenReturn(document)
            .map(Document::asXML);
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

    private Mono<Void> createItemElementsToChannel(Element channel, List<RSS2.Item> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Mono.empty();
        }
        return Flux.fromIterable(items)
            .flatMap(item -> createItemElementToChannel(channel, item))
            .then();
    }

    private Mono<Void> createItemElementToChannel(Element channel, RSS2.Item item) {
        Element itemElement = channel.addElement("item");
        itemElement.addElement("title")
            .addCDATA(XmlCharUtils.removeInvalidXmlChar(item.getTitle()));
        itemElement.addElement("link").addText(item.getLink());

        var description = Optional.of(getDescriptionWithTelemetry(item))
            .map(content -> {
                if (externalUrl != null) {
                    return new RelativeLinkProcessor(externalUrl)
                        .processForHtml(content);
                }
                return content;
            })
            .map(XmlCharUtils::removeInvalidXmlChar)
            .orElseThrow();
        itemElement.addElement("description").addCDATA(description);
        itemElement.addElement("guid")
            .addAttribute("isPermaLink", "false")
            .addText(item.getGuid());

        if (StringUtils.isNotBlank(item.getAuthor())) {
            // https://www.rssboard.org/rss-validator/docs/error/InvalidContact.html
            itemElement.addElement("dc:creator")
                .addText(item.getAuthor());
        }

        Mono<Void> handleEnclosure = Mono.empty();
        if (StringUtils.isNotBlank(item.getEnclosureUrl())) {
            var enclosureElement = itemElement.addElement("enclosure")
                .addAttribute("url", item.getEnclosureUrl())
                .addAttribute("type", item.getEnclosureType());
            var enclosureLength = item.getEnclosureLength();
            enclosureElement.addAttribute("length", enclosureLength);
            if (StringUtils.isBlank(enclosureLength)) {
                // https://www.rssboard.org/rss-validator/docs/error/MissingAttribute.html
                handleEnclosure = getFileSizeBytes(item.getEnclosureUrl())
                    .doOnNext(fileBytes -> enclosureElement.addAttribute("length", String.valueOf(fileBytes)))
                    .then();
            }
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
        return handleEnclosure;
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


    @NonNull
    Mono<Long> getFileSizeBytes(String url) {
        return webClient.get()
            .uri(URI.create(url))
            .header(HttpHeaders.USER_AGENT, UA)
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range
            .header(HttpHeaders.RANGE, "bytes=0-0")
            .headers(headers -> {
                if (StringUtils.isNotBlank(externalUrl)) {
                    // For Referrer anti-hotlinking
                    headers.set(HttpHeaders.REFERER, externalUrl);
                }
            })
            .retrieve()
            .toBodilessEntity()
            .map(HttpEntity::getHeaders)
            .mapNotNull(headers -> headers.getFirst(HttpHeaders.CONTENT_RANGE))
            .mapNotNull(RssXmlBuilder::parseLengthOfContentRange)
            .doOnError(e -> log.debug("Failed to get file size from url: {}", url,
                Throwables.getRootCause(e))
            )
            .onErrorReturn(0L)
            .defaultIfEmpty(0L);
    }

    @Nullable
    static Long parseLengthOfContentRange(@Nullable String contentRange) {
        if (contentRange == null) {
            return null;
        }
        // Refer to https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range#syntax
        var range = contentRange.split("/");
        if (range.length != 2) {
            return null;
        }
        var lengthStr = range[1];
        if ("*".equals(lengthStr)) {
            return null;
        }
        return Long.parseLong(range[1]);
    }

}

package run.halo.feed;

import static run.halo.feed.RssUtils.genRelativeThumbUri;

import com.google.common.base.Throwables;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;
import run.halo.app.core.attachment.ThumbnailSize;
import run.halo.app.infra.utils.PathUtils;
import run.halo.feed.telemetry.TelemetryEndpoint;

@Slf4j
public class RelativeLinkProcessor {
    private final URI externalUri;

    public RelativeLinkProcessor(String externalUrl) {
        Assert.notNull(externalUrl, "External URL must not be null");
        this.externalUri = URI.create(externalUrl);
    }

    public String processForHtml(String html) {
        try {
            return doProcessForHtml(html);
        } catch (Throwable e) {
            log.warn("Failed to process relative links for HTML", Throwables.getRootCause(e));
        }
        return html;
    }

    private String doProcessForHtml(String html) {
        var document = Jsoup.parse(html);

        // Process all links
        var links = document.select("a[href]");
        processElementAttr(links, "href", false);
        // process all images
        var images = document.select("img[src]");
        processElementAttr(images, "src", true);
        // video/audio source src
        var sources = document.select("source[src]");
        processElementAttr(sources, "src", false);
        // video src
        var videos = document.select("video[src]");
        processElementAttr(videos, "src", false);
        // link href
        var linksHref = document.select("link[href]");
        processElementAttr(linksHref, "href", false);
        // script src
        var scripts = document.select("script[src]");
        processElementAttr(scripts, "src", false);
        // iframe src
        var iframes = document.select("iframe[src]");
        processElementAttr(iframes, "src", false);
        // frame src
        var frames = document.select("frame[src]");
        processElementAttr(frames, "src", false);
        // embed src
        var embeds = document.select("embed[src]");
        processElementAttr(embeds, "src", false);

        return document.body().html();
    }

    private void processElementAttr(Elements elements, String attrKey, boolean canThumb) {
        for (Element link : elements) {
            String src = link.attr(attrKey);
            if (canThumb && isNotTelemetryLink(src)) {
                var thumb = genThumbUrl(src, ThumbnailSize.M);
                var absoluteUrl = processLink(thumb);
                link.attr(attrKey, absoluteUrl);
            } else {
                var absoluteUrl = processLink(src);
                link.attr(attrKey, absoluteUrl);
            }
        }
    }

    boolean isNotTelemetryLink(String uri) {
        return uri != null && !uri.contains(TelemetryEndpoint.TELEMETRY_PATH);
    }

    private String genThumbUrl(String url, ThumbnailSize size) {
        return processLink(genRelativeThumbUri(url, size));
    }

    private String processLink(String link) {
        if (StringUtils.isBlank(link) || PathUtils.isAbsoluteUri(link)) {
            return link;
        }
        var contextPath = StringUtils.defaultIfBlank(externalUri.getPath(), "/");
        var linkUri = UriComponentsBuilder.fromUriString(URI.create(link).toASCIIString())
            .build(true);
        var builder = UriComponentsBuilder.fromUriString(externalUri.toString());
        if (shouldAppendPath(contextPath, link)) {
            builder.pathSegment(linkUri.getPathSegments().toArray(new String[0]));
        } else {
            builder.replacePath(linkUri.getPath());
        }
        return builder.query(linkUri.getQuery())
            .fragment(linkUri.getFragment())
            .build(true)
            .toUri()
            .toString();
    }

    private static boolean shouldAppendPath(String contextPath, String link) {
        return !"/".equals(contextPath) && !link.startsWith(contextPath);
    }
}

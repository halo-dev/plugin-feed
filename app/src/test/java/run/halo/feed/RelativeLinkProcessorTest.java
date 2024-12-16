package run.halo.feed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RelativeLinkProcessor}.
 *
 * @author guqing
 * @since 1.4.1
 */
class RelativeLinkProcessorTest {
    private final RelativeLinkProcessor linkProcessor =
            new RelativeLinkProcessor("http://localhost:8090");

    @Test
    void textContent() {
        var content = "hello world";
        var processed = linkProcessor.processForHtml(content);
        assertThat(processed).isEqualTo(content);
    }

    @Test
    void testProcessForHtmlIncludeATag() {
        var content = "<a href=\"/hello\">hello</a>";
        var processed = linkProcessor.processForHtml(content);
        assertThat(processed).isEqualTo("<a href=\"http://localhost:8090/hello\">hello</a>");
    }

    @Test
    void processForHtmlIncludeImgTag() {
        var content = "<img src=\"/hello.jpg\"/>";
        var processed = linkProcessor.processForHtml(content);
        assertThat(processed).isEqualTo(
                "<img src=\"http://localhost:8090/apis/api.storage.halo"
                        + ".run/v1alpha1/thumbnails/-/via-uri?uri=%2Fhello.jpg&amp;size=m\">");
    }
}

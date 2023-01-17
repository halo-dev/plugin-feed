package run.halo.feed;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RSS2Test {

    @Test
    void toXmlString() {
        String result = RSS2.builder()
                .title("title")
                .description("description")
                .link("link")
                .items(Arrays.asList(
                        RSS2.Item.builder()
                                .title("title1")
                                .description("description1")
                                .link("link1")
                                .pubDate(Instant.EPOCH)
                                .guid("guid1")
                                .build(),
                        RSS2.Item.builder()
                                .title("title2")
                                .description("description2")
                                .link("link2")
                                .pubDate(Instant.ofEpochSecond(2208988800L))
                                .guid("guid2")
                                .build()
                ))
                .build()
                .toXmlString();

        String standard = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<rss version=\"2.0\">" +
                "<channel>" +
                "<title>title</title>" +
                "<link>link</link>" +
                "<description>description</description>" +
                "<item><title><![CDATA[title1]]></title><link>link1</link>" +
                "<description><![CDATA[description1]]></description><guid>guid1</guid>" +
                "<pubDate>Thu, 1 Jan 1970 00:00:00 GMT</pubDate></item>" +
                "<item><title><![CDATA[title2]]></title><link>link2</link>" +
                "<description><![CDATA[description2]]></description><guid>guid2</guid>" +
                "<pubDate>Sun, 1 Jan 2040 00:00:00 GMT</pubDate></item>" +
                "</channel>" +
                "</rss>";

        assertEquals(standard, result);
    }
}
package run.halo.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RSS2Test {

    @Test
    void toXmlString() {
        var rss = RSS2.builder()
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
            .build();

        var instant = Instant.now();
        var rssXml = new RssXmlBuilder()
            .withRss2(rss)
            .withGenerator("Halo")
            .withLastBuildDate(instant)
            .toXmlString();

        var lastBuildDate = RssXmlBuilder.instantToString(instant);

        // language=xml
        var expected = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
            	<channel>
            		<title>title</title>
            		<link>link</link>
            		<description>description</description>
            		<generator>Halo</generator>
            		<language>zh-cn</language>
            		<lastBuildDate>%s</lastBuildDate>
            		<item>
            			<title>
            				<![CDATA[title1]]>
            			</title>
            			<link>link1</link>
            			<description>
            				<![CDATA[description1]]>
            			</description>
            			<guid isPermaLink="false">guid1</guid>
            			<pubDate>Thu, 1 Jan 1970 00:00:00 GMT</pubDate>
            		</item>
            		<item>
            			<title>
            				<![CDATA[title2]]>
            			</title>
            			<link>link2</link>
            			<description>
            				<![CDATA[description2]]>
            			</description>
            			<guid isPermaLink="false">guid2</guid>
            			<pubDate>Sun, 1 Jan 2040 00:00:00 GMT</pubDate>
            		</item>
            	</channel>
            </rss>
            """.formatted(lastBuildDate);
        assertThat(rssXml).isEqualToIgnoringWhitespace(expected);
    }

    @Test
    void extractRssTagsTest() {
        var instant = Instant.now();
        var rss = RSS2.builder()
            .title("title")
            .description("description")
            .link("link")
            .build();

        var lastBuildDate = RssXmlBuilder.instantToString(instant);
        var rssXml = new RssXmlBuilder()
            .withRss2(rss)
            .withLastBuildDate(instant)
            .withGenerator("Halo")
            .withExtractRssTags("""
                <user>
                    <name>John</name>
                    <age>20</age>
                </user>
                <user>
                    <name>Tom</name>
                    <age>30</age>
                </user>
                """)
            .toXmlString();
        // language=xml
        var expected = """
            <?xml version="1.0" encoding="UTF-8"?>
             <rss
                xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
                <channel>
                    <title>title</title>
                    <link>link</link>
                    <description>description</description>
                    <generator>Halo</generator>
                    <language>zh-cn</language>
                    <lastBuildDate>%s</lastBuildDate>
                    <user>
                        <name>John</name>
                        <age>20</age>
                    </user>
                    <user>
                        <name>Tom</name>
                        <age>30</age>
                    </user>
                </channel>
             </rss>
            """.formatted(lastBuildDate);
        assertThat(rssXml).isEqualToIgnoringWhitespace(expected);
    }
}
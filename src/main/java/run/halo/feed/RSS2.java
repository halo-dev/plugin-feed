package run.halo.feed;

import lombok.Builder;
import lombok.Data;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
public class RSS2 {
    private String title;

    private String link;

    private String description;

    private List<Item> items;

    private String customContent;

    @Data
    @Builder
    public static class Item {
        private String title;

        private String link;

        private String description;

        private Instant pubDate;

        private String guid;
    }

    public String toXmlString() {
        Document document = DocumentHelper.createDocument();

        Element root = DocumentHelper.createElement("rss");
        root.addAttribute("version", "2.0");
        document.setRootElement(root);

        Element channel = root.addElement("channel");
        channel.addElement("title").addText(title);
        channel.addElement("link").addText(link);
        channel.addElement("description").addText(description);

        // TODO lastBuildDate need upgrade halo dependency version

        if (items != null) {
            items.forEach(item -> {
                Element itemElement = channel.addElement("item");
                itemElement.addElement("title").addCDATA(item.getTitle());
                itemElement.addElement("link").addText(item.getLink());
                itemElement.addElement("description").addCDATA(item.getDescription());
                itemElement.addElement("guid").addText(item.getGuid());
                itemElement.addElement("pubDate")
                        .addText(item.pubDate.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
            });
        }

        // 只有当 customContent 不为空且不为空白字符时才添加
        if (StringUtils.hasText(customContent)) {
            channel.addElement("custom").addCDATA(customContent);
        }

        return document.asXML();
    }
}

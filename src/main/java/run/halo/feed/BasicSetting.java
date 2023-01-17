package run.halo.feed;


import lombok.Data;

@Data
public class BasicSetting {
    public static final String CONFIG_MAP_NAME = "plugin-feed-config";
    public static final String GROUP = "basic";

    private Boolean enableCategories = Boolean.TRUE;

    private Boolean enableAuthors = Boolean.TRUE;

    private DescriptionType descriptionType = DescriptionType.excerpt;

    private Integer outputNum = 20;


    enum DescriptionType {
        /**
         * 全文
         */
        content,

        /**
         * 摘要
         */
        excerpt

    }
}

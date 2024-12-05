package run.halo.feed;

import lombok.Data;
import lombok.experimental.Accessors;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Data
@Accessors(chain = true)
public class BasicProp {
    private boolean enableCategories = Boolean.TRUE;
    private boolean enableAuthors = Boolean.TRUE;
    private String descriptionType;
    private Integer outputNum;
    private String rssExtraTags;

    public boolean isExcerptDescriptionType() {
        return DescriptionType.EXCERPT.name().equalsIgnoreCase(descriptionType);
    }

    public enum DescriptionType {
        EXCERPT,
        CONTENT
    }

    public static Mono<BasicProp> getBasicProp(ReactiveSettingFetcher settingFetcher) {
        return settingFetcher.fetch("basic", BasicProp.class)
            .defaultIfEmpty(new BasicProp())
            .doOnNext(prop -> {
                if (prop.getOutputNum() == null) {
                    prop.setOutputNum(20);
                }
                if (prop.getDescriptionType() == null) {
                    prop.setDescriptionType(DescriptionType.EXCERPT.name());
                }
            });
    }
}

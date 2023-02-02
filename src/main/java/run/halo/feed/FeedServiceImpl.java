package run.halo.feed;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ListResult;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemSetting;

import java.net.URI;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {
    private final ReactiveSettingFetcher settingFetcher;

    private final ExternalUrlSupplier externalUrlSupplier;

    private final FeedSourceFinder feedSourceFinder;

    private static final int FIRST_PAGE = 1;

    @Override
    public Mono<ServerResponse> allFeed() {
        return getFeedContext()
                .flatMap(feedContext -> {
                    var rss2 = buildBaseRss(feedContext);

                    return postListResultToXmlServerResponse(
                            feedSourceFinder.listPosts(FIRST_PAGE, feedContext.basicPluginSetting.getOutputNum()),
                            feedContext, rss2);
                });
    }

    @Override
    public Mono<ServerResponse> categoryFeed(String category) {
        return getFeedContext()
                .filter(feedContext -> BooleanUtils.isTrue(feedContext.basicPluginSetting.getEnableCategories()))
                .flatMap(feedContext -> {
                    var rss2 = buildBaseRss(feedContext);
                    // Get category metadata name by category slug
                    return feedSourceFinder.getCategoriesContentBySlug(category)
                            .next()
                            .map(categoryContent -> {
                                // Set category info
                                rss2.setTitle("分类：" + categoryContent.getSpec().getDisplayName() +
                                        " - " + rss2.getTitle());
                                rss2.setLink(categoryContent.getStatusOrDefault().getPermalink());
                                if (StringUtils.hasText(categoryContent.getSpec().getDescription())) {
                                    rss2.setDescription(categoryContent.getSpec().getDescription());
                                }
                                return categoryContent.getMetadata().getName();
                            })
                            .flatMap(categoryMetadataName -> {
                                // Get posts by category metadata name
                                var listResultMono = feedSourceFinder.listPostsByCategory(
                                        FIRST_PAGE,
                                        feedContext.basicPluginSetting.getOutputNum(),
                                        categoryMetadataName);
                                return postListResultToXmlServerResponse(listResultMono, feedContext, rss2);
                            });
                })
                .onErrorResume(error -> {
                    log.error("Failed to get category feed", error);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                })
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    @Override
    public Mono<ServerResponse> authorFeed(String author) {
        return getFeedContext()
                .filter(feedContext -> BooleanUtils.isTrue(feedContext.basicPluginSetting.getEnableAuthors()))
                .flatMap(feedContext -> {
                    var rss2 = buildBaseRss(feedContext);
                    // Get author display name by author metadata name
                    return feedSourceFinder.getUserByName(author)
                            .flatMap(user -> {
                                rss2.setTitle("作者：" + user.getSpec().getDisplayName() + " - " + rss2.getTitle());
                                return postListResultToXmlServerResponse(
                                        feedSourceFinder.listPostsByAuthor(FIRST_PAGE,
                                                feedContext.basicPluginSetting.getOutputNum(), author),
                                        feedContext, rss2);
                            });
                })
                .onErrorResume(error -> {
                    log.error("Failed to get author feed", error);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                })
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<FeedContext> getFeedContext() {
        return Mono
                .zip(settingFetcher.fetch(BasicSetting.CONFIG_MAP_NAME, BasicSetting.GROUP, BasicSetting.class)
                                .switchIfEmpty(Mono.just(new BasicSetting())),
                        settingFetcher.fetch(SystemSetting.SYSTEM_CONFIG, SystemSetting.Basic.GROUP,
                                SystemSetting.Basic.class))
                .map(tuple -> {
                    var basicPluginSetting = tuple.getT1();
                    var systemBasicSetting = tuple.getT2();
                    // Assert basic plugin setting
                    Assert.notNull(basicPluginSetting.getOutputNum(), "outputNum cannot be null");
                    Assert.isTrue(basicPluginSetting.getOutputNum() > 0,
                            "OutputNum must be greater than zero");
                    Assert.notNull(basicPluginSetting.getDescriptionType(), "descriptionType cannot be null");

                    var externalUrl = externalUrlSupplier.get();
                    // Build feed context
                    return new FeedContext(basicPluginSetting, systemBasicSetting, externalUrl);
                });
    }

    private Mono<ServerResponse> postListResultToXmlServerResponse(Mono<ListResult<Post>> postListResult,
                                                                   FeedContext feedContext, RSS2 rss2) {
        return postListResult
                .flatMapIterable(ListResult::getItems)
                .flatMap(post -> {
                    // Create item
                    var itemBuilder = RSS2.Item.builder()
                            .title(post.getSpec().getTitle())
                            .link(post.getStatusOrDefault().getPermalink())
                            .pubDate(post.getSpec().getPublishTime())
                            .guid(post.getStatusOrDefault().getPermalink());

                    // TODO lastBuildDate need upgrade halo dependency version

                    // Set description
                    if (Objects.equals(feedContext.basicPluginSetting.getDescriptionType(),
                            BasicSetting.DescriptionType.content)) {
                        // Set releaseSnapshot as description
                        var releaseSnapshot = post.getSpec().getReleaseSnapshot();
                        var baseSnapshot = post.getSpec().getBaseSnapshot();
                        return feedSourceFinder.getPostContent(releaseSnapshot, baseSnapshot)
                                .map(contentWrapper -> itemBuilder
                                        .description(contentWrapper.getContent())
                                        .build());
                    } else {
                        // Set excerpt as description
                        return Mono.just(itemBuilder
                                // Prevent parsing excerpt as html
                                .description(StringEscapeUtils.escapeXml10(post.getStatusOrDefault().getExcerpt()))
                                .build());
                    }
                })
                .collectList()
                .map(items -> {
                    rss2.setItems(items);
                    return rss2.toXmlString();
                })
                .flatMap(xml -> ServerResponse.ok().contentType(MediaType.TEXT_XML).bodyValue(xml));
    }

    private RSS2 buildBaseRss(FeedContext feedContext) {
        return RSS2.builder()
                .title(feedContext.systemBasicSetting.getTitle())
                .description(StringUtils.hasText(feedContext.systemBasicSetting.getSubtitle()) ?
                        feedContext.systemBasicSetting.getSubtitle() : feedContext.systemBasicSetting.getTitle())
                .link(feedContext.externalUrl.toString())
                .build();
    }

    record FeedContext(BasicSetting basicPluginSetting, SystemSetting.Basic systemBasicSetting, URI externalUrl) {
    }
}

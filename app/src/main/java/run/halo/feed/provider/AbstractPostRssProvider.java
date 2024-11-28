package run.halo.feed.provider;

import java.util.List;
import java.util.function.Function;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.content.ContentWrapper;
import run.halo.app.content.PostContentService;
import run.halo.app.core.attachment.ThumbnailSize;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.feed.BasicProp;
import run.halo.feed.RSS2;
import run.halo.feed.service.PostService;

@RequiredArgsConstructor
public abstract class AbstractPostRssProvider {
    protected final PostService postService;
    protected final ReactiveExtensionClient client;
    protected final ExternalLinkProcessor externalLinkProcessor;
    protected final ReactiveSettingFetcher settingFetcher;
    protected final PostContentService postContentService;
    protected final ExternalUrlSupplier externalUrlSupplier;
    protected final SystemInfoGetter systemInfoGetter;

    public Mono<RSS2> handler(ServerRequest request) {
        var builder = RSS2.builder();
        var rssMono = systemInfoGetter.get()
            .doOnNext(systemInfo -> builder
                .title(systemInfo.getTitle())
                .image(externalLinkProcessor.processLink(systemInfo.getLogo()))
                .description(
                    StringUtils.defaultIfBlank(systemInfo.getSubtitle(), systemInfo.getTitle()))
                .link(externalUrlSupplier.getURL(request.exchange().getRequest()).toString())
            )
            .subscribeOn(Schedulers.boundedElastic());

        var itemsMono = listPosts(request)
            .concatMap(postWithContent -> {
                var post = postWithContent.getPost();
                var permalink = post.getStatusOrDefault().getPermalink();
                var itemBuilder = RSS2.Item.builder()
                    .title(post.getSpec().getTitle())
                    .link(externalLinkProcessor.processLink(permalink))
                    .pubDate(post.getSpec().getPublishTime())
                    .guid(post.getStatusOrDefault().getPermalink())
                    .description(postWithContent.getContent());

                if (StringUtils.isNotBlank(post.getSpec().getCover())) {
                    itemBuilder.enclosureUrl(genThumbUrl(post.getSpec().getCover()))
                        .enclosureType("image/jpeg");
                }

                var ownerName = post.getSpec().getOwner();
                var userMono = fetchUser(ownerName)
                    .map(user -> user.getSpec().getDisplayName())
                    .doOnNext(itemBuilder::author);

                var categoryMono = fetchCategoryDisplayName(post.getSpec().getCategories())
                    .collectList()
                    .doOnNext(itemBuilder::categories);

                return Mono.when(userMono, categoryMono)
                    .then(Mono.fromSupplier(itemBuilder::build));
            })
            .collectList()
            .doOnNext(builder::items)
            .subscribeOn(Schedulers.boundedElastic());

        return Mono.when(rssMono, itemsMono)
            .then(Mono.fromSupplier(builder::build));
    }

    private String genThumbUrl(String url) {
        return externalLinkProcessor.processLink(
            "/apis/api.storage.halo.run/v1alpha1/thumbnails/-/via-uri?uri=" + url + "&size="
                + ThumbnailSize.M.name().toLowerCase()
        );
    }

    protected Flux<PostWithContent> listPosts(ServerRequest request) {
        return listPostsByFunc(basicProp -> postService.listPosts(basicProp.getOutputNum())
            .flatMapIterable(ListResult::getItems));
    }

    protected Flux<PostWithContent> listPostsByFunc(Function<BasicProp, Flux<Post>> postListFunc) {
        return BasicProp.getBasicProp(settingFetcher)
            .flatMapMany(basicProp -> postListFunc.apply(basicProp)
                .concatMap(post -> {
                    var postWithContent = new PostWithContent()
                        .setPost(post);
                    if (basicProp.isExcerptDescriptionType()) {
                        // Prevent parsing excerpt as html
                        var escapedContent =
                            StringEscapeUtils.escapeXml10(post.getStatusOrDefault().getExcerpt());
                        postWithContent.setContent(escapedContent);
                        return Mono.just(postWithContent);
                    }
                    return postContentService.getReleaseContent(post.getMetadata().getName())
                        .map(ContentWrapper::getContent)
                        .doOnNext(content -> postWithContent.setContent(
                            StringUtils.defaultString(content))
                        )
                        .thenReturn(postWithContent);
                })
            );
    }

    protected Mono<User> fetchUser(String username) {
        return client.fetch(User.class, username)
            .switchIfEmpty(Mono.error(new ServerWebInputException("User not found")))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<String> fetchCategoryDisplayName(List<String> categoryNames) {
        if (CollectionUtils.isEmpty(categoryNames)) {
            return Flux.empty();
        }
        return client.listAll(Category.class, ListOptions.builder()
                .fieldQuery(QueryFactory.in("metadata.name", categoryNames))
                .build(), ExtensionUtil.defaultSort())
            .map(category -> category.getSpec().getDisplayName())
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Data
    @Accessors(chain = true)
    protected static class PostWithContent {
        private Post post;
        private String content;
    }
}

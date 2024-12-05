package run.halo.feed.provider;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.content.PostContentService;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.feed.BasicProp;
import run.halo.feed.RSS2;
import run.halo.feed.RssRouteItem;
import run.halo.feed.service.PostService;

@Component
public class TagPostRssProvider extends AbstractPostRssProvider implements RssRouteItem {

    public TagPostRssProvider(PostService postService, ReactiveExtensionClient client,
        ExternalLinkProcessor externalLinkProcessor, ReactiveSettingFetcher settingFetcher,
        PostContentService postContentService, ExternalUrlSupplier externalUrlSupplier,
        SystemInfoGetter systemInfoGetter) {
        super(postService, client, externalLinkProcessor, settingFetcher, postContentService,
            externalUrlSupplier, systemInfoGetter);
    }

    @Override
    public Mono<String> pathPattern() {
        return BasicProp.getBasicProp(settingFetcher)
            .filter(BasicProp::isEnableCategories)
            .map(prop -> "/tags/{slug}.xml");
    }

    @Override
    @NonNull
    public String displayName() {
        return "标签文章订阅";
    }

    @Override
    public String description() {
        return "按标签订阅站点文章";
    }

    public Mono<RSS2> handler(ServerRequest request) {
        var slug = request.pathVariable("slug");
        return super.handler(request)
            .flatMap(rss2 -> postService.getTagBySlug(slug)
                .doOnNext(tag -> {
                    var displayName = tag.getSpec().getDisplayName();
                    var permalink = tag.getStatusOrDefault().getPermalink();
                    rss2.setTitle("标签：" + displayName + " - " + rss2.getTitle());
                    rss2.setLink(externalLinkProcessor.processLink(permalink));
                })
                .thenReturn(rss2)
            );
    }

    @Override
    protected Flux<PostWithContent> listPosts(ServerRequest request) {
        var tagSlug = request.pathVariable("slug");
        return listPostsByFunc(basicProp ->
            postService.listPostByTagSlug(basicProp.getOutputNum(), tagSlug)
                .flatMapIterable(ListResult::getItems)
        );
    }
}

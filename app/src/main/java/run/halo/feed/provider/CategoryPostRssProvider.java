package run.halo.feed.provider;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.content.PostContentService;
import run.halo.app.core.extension.content.Category;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.QueryFactory;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.feed.BasicProp;
import run.halo.feed.RSS2;
import run.halo.feed.RssRouteItem;
import run.halo.feed.service.PostService;

@Component
public class CategoryPostRssProvider extends AbstractPostRssProvider implements RssRouteItem {

    public CategoryPostRssProvider(PostService postService, ReactiveExtensionClient client,
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
            .map(prop -> "/categories/{slug}.xml");
    }

    @Override
    @NonNull
    public String displayName() {
        return "分类文章订阅";
    }

    @Override
    public String description() {
        return "按分类订阅站点文章";
    }

    @Override
    public String example() {
        return "https://exmaple.com/feed/categories/fake.xml";
    }

    @Override
    public Mono<RSS2> handler(ServerRequest request) {
        var slug = request.pathVariable("slug");
        return super.handler(request)
            .flatMap(rss2 -> getCategoryDisplayName(slug)
                .doOnNext(category -> {
                    var displayName = category.getSpec().getDisplayName();
                    var permalink = category.getStatusOrDefault().getPermalink();
                    rss2.setTitle("分类：" + displayName + " - " + rss2.getTitle());
                    rss2.setLink(externalLinkProcessor.processLink(permalink));
                })
                .thenReturn(rss2)
            );
    }

    private Mono<Category> getCategoryDisplayName(String categorySlug) {
        return client.listBy(Category.class, ListOptions.builder()
                    .fieldQuery(QueryFactory.equal("spec.slug", categorySlug))
                    .build(),
                PageRequestImpl.ofSize(1)
            )
            .flatMapIterable(ListResult::getItems)
            .next()
            .switchIfEmpty(Mono.error(new ServerWebInputException("Category not found")));
    }

    @Override
    protected Flux<PostWithContent> listPosts(ServerRequest request) {
        var categorySlug = request.pathVariable("slug");
        return listPostsByFunc(basicProp ->
            postService.listPostByCategorySlug(basicProp.getOutputNum(), categorySlug)
                .flatMapIterable(ListResult::getItems)
        );
    }
}

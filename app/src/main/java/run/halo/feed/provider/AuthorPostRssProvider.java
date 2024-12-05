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
public class AuthorPostRssProvider extends AbstractPostRssProvider implements RssRouteItem {

    public AuthorPostRssProvider(PostService postService, ReactiveExtensionClient client,
        ExternalLinkProcessor externalLinkProcessor, ReactiveSettingFetcher settingFetcher,
        PostContentService postContentService, ExternalUrlSupplier externalUrlSupplier,
        SystemInfoGetter systemInfoGetter) {
        super(postService, client, externalLinkProcessor, settingFetcher, postContentService,
            externalUrlSupplier, systemInfoGetter);
    }

    @Override
    public Mono<String> pathPattern() {
        return BasicProp.getBasicProp(settingFetcher)
            .filter(BasicProp::isEnableAuthors)
            .map(prop -> "/authors/{author}.xml");
    }

    @Override
    @NonNull
    public String displayName() {
        return "作者文章订阅";
    }

    @Override
    public String description() {
        return "按作者订阅站点文章";
    }

    @Override
    public String example() {
        return "https://exmaple.com/feed/authors/fake.xml";
    }

    @Override
    public Mono<RSS2> handler(ServerRequest request) {
        var authorName = request.pathVariable("author");
        return super.handler(request)
            .flatMap(rss2 -> fetchUser(authorName)
                .doOnNext(author -> {
                    var displayName = author.getSpec().getDisplayName();
                    rss2.setTitle("作者：" + displayName + " - " + rss2.getTitle());

                    if (author.getStatus() != null) {
                        rss2.setLink(
                            externalLinkProcessor.processLink(author.getStatus().getPermalink()));
                    }
                })
                .thenReturn(rss2)
            );
    }

    @Override
    protected Flux<PostWithContent> listPosts(ServerRequest request) {
        return listPostsByFunc(basicProp -> {
            var author = request.pathVariable("author");
            return postService.listPostByAuthor(basicProp.getOutputNum(), author)
                .flatMapIterable(ListResult::getItems);
        });
    }
}

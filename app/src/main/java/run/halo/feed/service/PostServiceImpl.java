package run.halo.feed.service;

import static run.halo.app.extension.index.query.QueryFactory.and;
import static run.halo.app.extension.index.query.QueryFactory.equal;
import static run.halo.app.extension.index.query.QueryFactory.in;
import static run.halo.app.extension.index.query.QueryFactory.isNull;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Tag;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Query;

@Component
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final ReactiveExtensionClient client;

    @Override
    public Mono<ListResult<Post>> listPosts(int size) {
        return client.listBy(Post.class, buildPostListOptions(),
            PageRequestImpl.ofSize(size).withSort(defaultSort()));
    }

    @Override
    public Mono<ListResult<Post>> listPostByCategorySlug(int size, String categorySlug) {
        return getCategoryBySlug(categorySlug)
            .flatMap(category -> {
                var categoryName = category.getMetadata().getName();
                return client.listBy(Post.class,
                    buildPostListOptions(in("spec.categories", categoryName)),
                    PageRequestImpl.ofSize(size).withSort(defaultSort()));
            });
    }

    @Override
    public Mono<ListResult<Post>> listPostByTagSlug(int size, String slug) {
        return getTagBySlug(slug)
            .flatMap(tag -> {
                var tagName = tag.getMetadata().getName();
                return client.listBy(Post.class,
                    buildPostListOptions(in("spec.tags", tagName)),
                    PageRequestImpl.ofSize(size).withSort(defaultSort()));
            });
    }

    @Override
    public Mono<ListResult<Post>> listPostByAuthor(int size, String author) {
        return client.listBy(Post.class, buildPostListOptions(in("spec.owner", author)),
            PageRequestImpl.ofSize(size).withSort(defaultSort()));
    }

    @Override
    public Mono<Category> getCategoryBySlug(String categorySlug) {
        return client.listBy(Category.class, ListOptions.builder()
                    .fieldQuery(equal("spec.slug", categorySlug))
                    .build(),
                PageRequestImpl.ofSize(1)
            )
            .flatMap(listResult -> Mono.justOrEmpty(ListResult.first(listResult)));
    }

    @Override
    public Mono<Tag> getTagBySlug(String slug) {
        return client.listBy(Tag.class, ListOptions.builder()
                    .fieldQuery(equal("spec.slug", slug))
                    .build(),
                PageRequestImpl.ofSize(1)
            )
            .flatMap(listResult -> Mono.justOrEmpty(ListResult.first(listResult)));
    }

    ListOptions buildPostListOptions() {
        return buildPostListOptions(null);
    }

    ListOptions buildPostListOptions(Query query) {
        var builder = ListOptions.builder()
            .labelSelector()
            .eq(Post.PUBLISHED_LABEL, "true")
            .end()
            .fieldQuery(and(
                isNull("metadata.deletionTimestamp"),
                equal("spec.deleted", "false"),
                equal("spec.visible", Post.VisibleEnum.PUBLIC.name())
            ));
        if (query != null) {
            builder.fieldQuery(query);
        }
        return builder.build();
    }

    static Sort defaultSort() {
        return Sort.by(
            Sort.Order.desc("spec.publishTime"),
            Sort.Order.asc("metadata.name")
        );
    }
}

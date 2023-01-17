package run.halo.feed;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.comparator.Comparators;
import org.thymeleaf.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.content.ContentWrapper;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Snapshot;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Ref;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
@AllArgsConstructor
public class FeedSourceFinderImpl implements FeedSourceFinder {
    private final ReactiveExtensionClient client;

    public static final Predicate<Post> FIXED_PREDICATE = post -> post.isPublished()
            && Objects.equals(false, post.getSpec().getDeleted())
            && Post.VisibleEnum.PUBLIC.equals(post.getSpec().getVisible());

    static Comparator<Post> defaultComparator() {
        Function<Post, Instant> publishTime =
                post -> post.getSpec().getPublishTime();
        Function<Post, String> name = post -> post.getMetadata().getName();
        return Comparator.comparing(publishTime, Comparators.nullsLow())
                .thenComparing(name)
                .reversed();
    }

    @Override
    public Mono<ListResult<Post>> listPosts(Integer page, Integer size) {
        return listPost(page, size, null, defaultComparator());
    }

    @Override
    public Mono<ListResult<Post>> listPostsByCategory(Integer page, Integer size, String category) {
        return listPost(page, size, post -> contains(post.getSpec().getCategories(), category), defaultComparator());
    }

    @Override
    public Mono<ListResult<Post>> listPostsByAuthor(Integer page, Integer size, String author) {
        return listPost(page, size, post -> post.getSpec().getOwner().equals(author), defaultComparator());
    }

    private boolean contains(List<String> c, String key) {
        if (org.apache.commons.lang3.StringUtils.isBlank(key) || c == null) {
            return false;
        }
        return c.contains(key);
    }

    private Mono<ListResult<Post>> listPost(Integer page, Integer size,
                                            Predicate<Post> postPredicate,
                                            Comparator<Post> comparator) {
        Predicate<Post> predicate = FIXED_PREDICATE
                .and(postPredicate == null ? post -> true : postPredicate);
        return client.list(Post.class, predicate, comparator, page, size);
    }

    @Override
    public Mono<ContentWrapper> getPostsContent(String name) {
        return client.fetch(Snapshot.class, name)
                .flatMap(snapshot -> getBaseSnapshot(snapshot.getSpec().getSubjectRef())
                        .map(snapshot::applyPatch));
    }

    @Override
    public Flux<Category> getCategoriesContentBySlug(String slug) {
        return client.list(Category.class, category -> StringUtils.equals(category.getSpec().getSlug(), slug),
                Comparator.naturalOrder());
    }

    public Mono<Snapshot> getBaseSnapshot(Ref subjectRef) {
        return listSnapshots(subjectRef)
                .sort(createTimeReversedComparator().reversed())
                .filter(p -> StringUtils.equals(Boolean.TRUE.toString(),
                        ExtensionUtil.nullSafeAnnotations(p).get(Snapshot.KEEP_RAW_ANNO)))
                .next();
    }

    public Flux<Snapshot> listSnapshots(Ref subjectRef) {
        Assert.notNull(subjectRef, "The subjectRef must not be null.");
        return client.list(Snapshot.class, snapshot -> subjectRef.equals(snapshot.getSpec()
                .getSubjectRef()), null);
    }

    Comparator<Snapshot> createTimeReversedComparator() {
        Function<Snapshot, String> name = snapshot -> snapshot.getMetadata().getName();
        Function<Snapshot, Instant> createTime = snapshot -> snapshot.getMetadata()
                .getCreationTimestamp();
        return Comparator.comparing(createTime)
                .thenComparing(name)
                .reversed();
    }


}

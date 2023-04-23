package run.halo.feed;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.comparator.Comparators;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Snapshot;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.ReactiveExtensionClient;

@Component
@AllArgsConstructor
public class FeedSourceFinderImpl implements FeedSourceFinder {
    private final ReactiveExtensionClient client;

    public static final Predicate<Post> FIXED_PREDICATE = post -> post.isPublished()
                                                                  && Objects.equals(false,
        post.getSpec().getDeleted())
                                                                  && Post.VisibleEnum.PUBLIC.equals(
        post.getSpec().getVisible())
                                                                  && post.getMetadata()
                                                                         .getDeletionTimestamp() ==
                                                                     null;

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
        return listPost(page, size, post -> contains(post.getSpec().getCategories(), category),
            defaultComparator());
    }

    @Override
    public Mono<ListResult<Post>> listPostsByAuthor(Integer page, Integer size, String author) {
        return listPost(page, size, post -> post.getSpec().getOwner().equals(author),
            defaultComparator());
    }

    private boolean contains(List<String> c, String key) {
        if (StringUtils.isBlank(key) || c == null) {
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
    public Mono<ContentWrapper> getPostContent(String snapshotName, String baseSnapshotName) {
        return client.fetch(Snapshot.class, baseSnapshotName)
            .doOnNext(this::checkBaseSnapshot)
            .flatMap(baseSnapshot -> {
                if (StringUtils.equals(snapshotName, baseSnapshotName)) {
                    var wrapper = ContentWrapper.patchSnapshot(baseSnapshot, baseSnapshot);
                    return Mono.just(wrapper);
                }
                return client.fetch(Snapshot.class, snapshotName)
                    .map(snapshot -> ContentWrapper.patchSnapshot(snapshot, baseSnapshot));
            });
    }

    private void checkBaseSnapshot(Snapshot snapshot) {
        Assert.notNull(snapshot, "The snapshot must not be null.");
        String keepRawAnno =
            MetadataUtil.nullSafeAnnotations(snapshot).get(Snapshot.KEEP_RAW_ANNO);
        if (!StringUtils.equals(Boolean.TRUE.toString(), keepRawAnno)) {
            throw new IllegalArgumentException(
                String.format("The snapshot [%s] is not a base snapshot.",
                    snapshot.getMetadata().getName()));
        }
    }

    @Override
    public Flux<Category> getCategoriesContentBySlug(String slug) {
        return client.list(Category.class,
            category -> StringUtils.equals(category.getSpec().getSlug(), slug),
            Comparator.naturalOrder());
    }

    @Override
    public Mono<User> getUserByName(String name) {
        return client.fetch(User.class, name);
    }

}

package run.halo.feed;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.content.ContentWrapper;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ListResult;

public interface FeedSourceFinder {

    /**
     * List all posts.
     *
     * @param page page number
     * @param size page size
     * @return post list result
     */
    Mono<ListResult<Post>> listPosts(Integer page, Integer size);

    /**
     * List posts by category.
     *
     * @param page     page number
     * @param size     page size
     * @param category category metadata name
     * @return post list result
     */
    Mono<ListResult<Post>> listPostsByCategory(Integer page, Integer size, String category);

    /**
     * List posts by author.
     *
     * @param page   page number
     * @param size   page size
     * @param author author slug name
     * @return post list result
     */
    Mono<ListResult<Post>> listPostsByAuthor(Integer page, Integer size, String author);

    /**
     * Get post snapshot post content.
     *
     * @param snapshotName     snapshot name
     * @param baseSnapshotName base snapshot name
     * @return post content
     */
    Mono<ContentWrapper> getPostContent(String snapshotName, String baseSnapshotName);

    /**
     * Get categories by category slug.
     *
     * @param slug category slug
     * @return category
     */
    Flux<Category> getCategoriesContentBySlug(String slug);

    /**
     * Get user by user metadata name.
     *
     * @param name user metadata name
     * @return user
     */
    Mono<User> getUserByName(String name);
}

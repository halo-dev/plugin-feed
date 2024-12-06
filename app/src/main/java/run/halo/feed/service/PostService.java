package run.halo.feed.service;

import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Category;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.Tag;
import run.halo.app.extension.ListResult;

public interface PostService {
    Mono<ListResult<Post>> listPosts(int size);

    Mono<ListResult<Post>> listPostByCategorySlug(int size, String categorySlug);

    Mono<ListResult<Post>> listPostByTagSlug(int size, String tagSlug);

    Mono<ListResult<Post>> listPostByAuthor(int size, String author);

    Mono<Category> getCategoryBySlug(String categorySlug);

    Mono<Tag> getTagBySlug(String slug);
}

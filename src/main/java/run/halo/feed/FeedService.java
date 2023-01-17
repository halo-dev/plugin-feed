package run.halo.feed;

import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface FeedService {
    /**
     * Get all posts feed response
     * @return feed response
     */
    Mono<ServerResponse> allFeed();

    /**
     * Get category posts feed response
     * @param category category name
     * @return feed response
     */
    Mono<ServerResponse> categoryFeed(String category);

    /**
     * Get author posts feed response
     * @param author author metadata name
     * @return feed response
     */
    Mono<ServerResponse> authorFeed(String author);
}

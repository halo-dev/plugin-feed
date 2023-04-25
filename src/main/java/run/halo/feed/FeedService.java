package run.halo.feed;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface FeedService {
    /**
     * Get all posts feed response
     *
     * @return feed response
     */
    Mono<ServerResponse> allFeed(ServerRequest request);

    /**
     * Get category posts feed response
     *
     * @param request
     * @param category category name
     * @return feed response
     */
    Mono<ServerResponse> categoryFeed(ServerRequest request, String category);

    /**
     * Get author posts feed response
     *
     * @param request
     * @param author author metadata name
     * @return feed response
     */
    Mono<ServerResponse> authorFeed(ServerRequest request, String author);
}

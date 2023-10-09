package run.halo.feed;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * Feed plugin endpoint.
 * Router function configuration.
 */
@Component
@AllArgsConstructor
public class FeedPluginEndpoint {
    private final FeedService feedService;


    @Bean
    RouterFunction<ServerResponse> sitemapRouterFunction() {
        RequestPredicate requestPredicate = accept(
            MediaType.TEXT_XML,
            MediaType.APPLICATION_RSS_XML
        );
        return RouterFunctions.route(GET("/feed.xml").and(requestPredicate),
                feedService::allFeed)
            .andRoute(GET("/rss.xml").and(requestPredicate),
                feedService::allFeed)
            .andRoute(GET("/feed/categories/{category}.xml").and(requestPredicate),
                request -> feedService.categoryFeed(request, request.pathVariable("category")))
            .andRoute(GET("/feed/authors/{author}.xml").and(requestPredicate),
                request -> feedService.authorFeed(request, request.pathVariable("author")));
    }
}
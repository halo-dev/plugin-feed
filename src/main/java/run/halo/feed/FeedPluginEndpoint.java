package run.halo.feed;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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
        return RouterFunctions
                .route(GET("/feed.xml").and(accept(MediaType.TEXT_XML)), request -> feedService.allFeed())
                .andRoute(GET("/rss.xml").and(accept(MediaType.TEXT_XML)), request -> feedService.allFeed())
                .andRoute(GET("/feed/categories/{category}.xml").and(accept(MediaType.TEXT_XML)),
                        request -> feedService.categoryFeed(request.pathVariable("category")))
                .andRoute(GET("/feed/authors/{author}.xml").and(accept(MediaType.TEXT_XML)),
                        request -> feedService.authorFeed(request.pathVariable("author")));
    }
}
package run.halo.feed;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;
import run.halo.feed.provider.PostRssProvider;

@Component
@AllArgsConstructor
public class FeedPluginEndpoint {
    static final RequestPredicate ACCEPT_PREDICATE = accept(
        MediaType.TEXT_XML,
        MediaType.APPLICATION_RSS_XML
    );

    private final PostRssProvider postRssProvider;
    private final ExtensionGetter extensionGetter;
    private final RssCacheManager rssCacheManager;

    @Bean
    RouterFunction<ServerResponse> rssRouterFunction() {
        return RouterFunctions.route()
            .GET(path("/feed.xml").or(path("/rss.xml")).and(ACCEPT_PREDICATE),
                request -> rssCacheManager.get("/rss.xml", postRssProvider.handler(request))
                    .flatMap(this::buildResponse)
            )
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> additionalRssRouter() {
        var pathMatcher = ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/feed/**");
        return new RouterFunction<>() {
            @Override
            @NonNull
            public Mono<HandlerFunction<ServerResponse>> route(@NonNull ServerRequest request) {
                return pathMatcher.matches(request.exchange())
                    .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                    .flatMap(matched -> handleRequest(request));
            }

            private Mono<HandlerFunction<ServerResponse>> handleRequest(ServerRequest request) {
                return extensionGetter.getEnabledExtensions(RssRouteItem.class)
                    .concatMap(routeItem -> buildRequestPredicate(routeItem)
                        .map(requestPredicate -> new RouteItem(requestPredicate,
                            buildHandleFunction(routeItem))
                        )
                    )
                    .filter(route -> route.requestPredicate.test(request))
                    .next()
                    .map(RouteItem::handler);
            }

            record RouteItem(RequestPredicate requestPredicate,
                             HandlerFunction<ServerResponse> handler) {
            }

            private HandlerFunction<ServerResponse> buildHandleFunction(RssRouteItem routeItem) {
                return request -> rssCacheManager.get(request.path(), routeItem.handler(request))
                    .flatMap(item -> buildResponse(item));
            }

            private Mono<RequestPredicate> buildRequestPredicate(RssRouteItem routeItem) {
                return routeItem.pathPattern()
                    .map(pathPattern -> path(
                        buildPathPattern(pathPattern, routeItem.namespace()))
                        .and(ACCEPT_PREDICATE)
                    );
            }

            private String buildPathPattern(String pathPattern, String namespace) {
                var sb = new StringBuilder("/feed/");

                if (StringUtils.isNotBlank(namespace)) {
                    sb.append(namespace);
                    if (!namespace.endsWith("/")) {
                        sb.append("/");
                    }
                }

                if (pathPattern.startsWith("/")) {
                    pathPattern = pathPattern.substring(1);
                }
                sb.append(pathPattern);

                return sb.toString();
            }
        };
    }

    private Mono<ServerResponse> buildResponse(String xml) {
        return ServerResponse.ok().contentType(MediaType.TEXT_XML)
            .bodyValue(xml);
    }
}

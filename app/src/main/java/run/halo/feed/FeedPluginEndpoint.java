package run.halo.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;
import run.halo.feed.provider.PostRssProvider;

import java.util.ArrayList;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

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
    private final ExternalUrlSupplier externalUrlSupplier;

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
    RouterFunction<ServerResponse> rssSourcesListerRouter() {
        return RouterFunctions.route()
            .GET("/apis/api.feed.halo.run/v1alpha1/rss-sources", this::listRssSources)
            .build();
    }

    private Mono<ServerResponse> listRssSources(ServerRequest request) {
        var externalUrl = externalUrlSupplier.getURL(request.exchange().getRequest()).toString();
        return extensionGetter.getEnabledExtensions(RssRouteItem.class)
            .concatMap(item -> {
                var rssSource = RssSource.builder()
                    .displayName(item.displayName())
                    .description(item.description())
                    .example(item.example());
                return item.pathPattern()
                    .map(pattern -> buildPathPattern(pattern, item.namespace()))
                    .doOnNext(path -> rssSource.pattern(externalUrl + path))
                    .then(Mono.fromSupplier(rssSource::build));
            })
            .collectList()
            .flatMap(result -> {
                var allPosts = RssSource.builder()
                    .pattern(externalUrl + "/rss.xml")
                    .displayName("订阅所有文章")
                    .description("会根据设置的文章数量返回最新的文章")
                    .example("https://example.com/rss.xml")
                    .build();
                var sources = new ArrayList<>();
                sources.add(allPosts);
                sources.addAll(result);
                return ServerResponse.ok().bodyValue(sources);
            });
    }

    @Builder
    record RssSource(String displayName, String description, String pattern, String example) {
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
        };
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

    private Mono<ServerResponse> buildResponse(String xml) {
        return ServerResponse.ok().contentType(MediaType.TEXT_XML)
            .bodyValue(xml);
    }
}

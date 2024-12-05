package run.halo.feed;

import org.pf4j.ExtensionPoint;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

public interface RssRouteItem extends ExtensionPoint {

    /**
     * <p>Path pattern of this route.</p>
     * <p>If return {@link Mono#empty()}, the route will be ignored.</p>
     * <p>Otherwise, the route will be registered with the returned path pattern by rule: {@code
     * /feed/[namespace]/{pathPattern}}.</p>
     */
    Mono<String> pathPattern();

    @NonNull
    String displayName();

    default String description() {
        return "";
    }

    /**
     * An example URI for this route.
     */
    default String example() {
        return "";
    }

    /**
     * The namespace of this route to avoid conflicts.
     */
    default String namespace() {
        return "";
    }

    Mono<RSS2> handler(ServerRequest request);
}

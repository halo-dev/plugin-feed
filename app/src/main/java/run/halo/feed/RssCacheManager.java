package run.halo.feed;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.event.post.PostUpdatedEvent;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.PluginConfigUpdatedEvent;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
@RequiredArgsConstructor
public class RssCacheManager {
    private final Duration expireMinutes = Duration.ofMinutes(60);
    private final Cache<String, String> cache = CacheBuilder.newBuilder()
        .expireAfterWrite(expireMinutes)
        .build();

    private final SystemInfoGetter systemInfoGetter;
    private final ReactiveSettingFetcher settingFetcher;

    public Mono<String> get(String key, Mono<RSS2> loader) {
        return Mono.fromCallable(() -> cache.get(key, () -> generateRssXml(loader)
                .doOnNext(xml -> cache.put(key, xml))
                .block()
            ))
            .cache()
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> generateRssXml(Mono<RSS2> loader) {
        var builder = new RssXmlBuilder()
            .withGenerator("Halo v2.0");

        var rssMono = loader.doOnNext(builder::withRss2);

        var generatorMono = systemInfoGetter.get()
            .doOnNext(info -> builder.withExternalUrl(info.getUrl().toString())
                .withGenerator("Halo v" + info.getVersion().toStableVersion().toString())
            );

        var extractTagsMono = BasicProp.getBasicProp(settingFetcher)
            .doOnNext(prop -> builder.withExtractRssTags(prop.getRssExtraTags()));

        return Mono.when(rssMono, generatorMono, extractTagsMono)
            .then(Mono.fromSupplier(builder::toXmlString));
    }

    @EventListener(PluginConfigUpdatedEvent.class)
    public void onPluginConfigUpdated() {
        cache.invalidateAll();
    }

    @EventListener(RssCacheClearRequested.class)
    public void onCacheClearRequested(RssCacheClearRequested event) {
        if (event.isApplyToAll()) {
            cache.invalidateAll();
            return;
        }
        event.getRules().forEach(rule -> {
            switch (rule.type()) {
                case PREFIX:
                    invalidateCache(key -> key.startsWith(rule.value()));
                    break;
                case EXACT:
                    invalidateCache(key -> key.equals(rule.value()));
                    break;
                case CONTAINS:
                    invalidateCache(key -> key.contains(rule.value()));
                    break;
                default:
                    break;
            }
        });
    }

    @EventListener(PostUpdatedEvent.class)
    public void onPostUpdated() {
        invalidateCache(key -> key.equals("/rss.xml")
            || key.startsWith("/feed/authors/")
            || key.startsWith("/feed/categories/")
        );
    }

    private void invalidateCache(Predicate<String> predicate) {
        cache.asMap().keySet().removeIf(predicate);
    }
}

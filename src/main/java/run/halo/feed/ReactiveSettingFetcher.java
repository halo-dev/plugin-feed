package run.halo.feed;

import lombok.AllArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.utils.JsonUtils;

import java.util.Map;

@Component
@AllArgsConstructor
public class ReactiveSettingFetcher {
    private final ReactiveExtensionClient extensionClient;

    public <T> Mono<T> fetch(String configMapName, String group, Class<T> clazz) {
        return getValuesInternal(configMapName)
                .filter(map -> map.containsKey(group))
                .map(map -> map.get(group))
                .mapNotNull(stringValue -> JsonUtils.jsonToObject(stringValue, clazz));
    }


    @NonNull
    private Mono<Map<String, String>> getValuesInternal(String configMapName) {
        return getConfigMap(configMapName)
                .filter(configMap -> configMap.getData() != null)
                .map(ConfigMap::getData)
                .defaultIfEmpty(Map.of());
    }

    private Mono<ConfigMap> getConfigMap(String configMapName) {
        return extensionClient.fetch(ConfigMap.class, configMapName);
    }
}

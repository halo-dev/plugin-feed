# 开发文档

> 参考 [Halo 插件开发文档](https://docs.halo.run/developer-guide/plugin/hello-world) 运行插件。

## 如何扩展 RSS 源

> 从 feed 插件 v1.4.0 版本开始，支持扩展 RSS 功能。

`feed` 插件提供了扩展点，允许其他插件扩展 RSS 源。

### 步骤 1：在插件中引入 feed 依赖

在你的插件项目中添加 `feed` 插件的依赖：

```groovy
dependencies {
    // ...
    compileOnly "run.halo.feed:api:{version}"
}
```

将 `{version}` 替换为实际的 `feed` 插件版本号。

### 步骤 2：实现 `RssRouteItem` 扩展点接口

创建一个类实现 `run.halo.feed.RssRouteItem` 接口，提供自定义的 RSS 数据源。例如：

```java
public class MomentRssProvider implements RssRouteItem {
    // 实现具体的 RSS 提供逻辑
}
```

你可以参考 [PostRssProvider](./app/src/main/java/run/halo/feed/provider/PostRssProvider.java) 示例。

### 步骤 3：声明扩展点

在 `src/main/resources/extensions`
目录下，声明你的扩展。你可以参考 [ext-definition.yaml](app/src/main/resources/extensions/ext-definition.yaml) 文件来完成此步骤。

### 步骤 4：定义配置类并清理 RSS 缓存

在插件中定义一个配置类，使用 `@ConditionalOnClass` 注解确保只有在 `run.halo.feed.RssRouteItem` 类存在时才会创建对应的
Bean。同时，定义事件监听器来清理缓存。

`@ConditionalOnClass` 注解只能使用 name 属性来指定类全限定名，不支持使用 value 属性。

示例代码：

```java

@Configuration
@ConditionalOnClass(name = "run.halo.feed.RssRouteItem")
@RequiredArgsConstructor
public class RssAutoConfiguration {
    private final ApplicationEventPublisher eventPublisher;

    @Bean
    public MomentRssProvider momentRssProvider() {
        return new MomentRssProvider();
    }

    @Async
    @EventListener({MomentUpdatedEvent.class, MomentDeletedEvent.class})
    public void onMomentUpdatedOrDeleted() {
        var rule = CacheClearRule.forExact("/feed/moments/rss.xml");
        var event = RssCacheClearRequested.forRule(this, rule);
        eventPublisher.publishEvent(event);
    }
}
```

此配置确保了当 `RssRouteItem` 接口存在时，插件才会自动创建 `MomentRssProvider` 并监听相关事件来清理缓存。

### 步骤 5：声明插件依赖

在 `plugin.yaml` 文件中声明 `feed` 插件为可选依赖，确保当 `feed` 插件存在并启用时，插件能够自动注册 RSS 源。

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: Plugin
metadata:
    name: moment
spec:
    pluginDependencies:
        "PluginFeed?": ">=1.4.0"
```

这样，当 `feed` 插件可用时，插件会自动注册自定义的 RSS 源。
# RSS 订阅插件

Halo 2.0 的 RSS 订阅链接生成插件

## 如何使用

RSS 订阅插件提供了用于生成 RSS 订阅链接的功能， 默认提供了以下订阅链接类型：

1. 全站订阅：`/feed.xml` 或者 `/rss.xml`
2. 按照分类订阅：`/feed/categories/{slug}.xml`
3. 按照标签订阅文章：`/feed/tags/{name}.xml`
4. 按照作者订阅：`/feed/authors/{name}.xml`

可在插件列表中点击 RSS 插件进入插件设置页面，查看支持的订阅链接列表。

如果安装并启用了适配此 RSS 插件的其他插件，例如 [瞬间插件](https://www.halo.run/store/apps/app-SnwWD) 则会提供更多的订阅链接类型。

你可以将订阅链接添加到 [Feedly](https://feedly.com/)、[Inoreader](https://www.inoreader.com/)、[Reeder](https://reederapp.com/)、[NetNewsWire](https://ranchero.com/netnewswire/)、[Follow](https://follow.is)
等 RSS 订阅工具中，订阅你感兴趣的内容，但需要注意的是，链接中的 `{slug}`、`{name}` 等为占位符，需要替换为实际的类目名称，slug
表示资源别名，name 表示资源唯一标识名。

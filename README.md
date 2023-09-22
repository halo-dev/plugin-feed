# plugin-feed

Halo 2.0 的 RSS 订阅链接生成插件

## 开发环境

```bash
git clone git@github.com:halo-dev/plugin-feed.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/plugin-feed.git
```

```bash
cd path/to/plugin-feed
```

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

修改 Halo 配置文件：

```yaml
halo:
  plugin:
    runtime-mode: development
    classes-directories:
      - "build/classes"
      - "build/resources"
    lib-directories:
      - "libs"
    fixedPluginPath:
      - "/path/to/plugin-feed"
```

## 使用方式

1. 在 [Releases](https://github.com/halo-dev/plugin-feed/releases) 下载最新的 JAR 文件。
2. 在 Halo 后台的插件管理上传 JAR 文件进行安装。

目前提供了以下订阅链接类型：

1. 全站订阅：`/feed.xml` 或者 `/rss.xml`
2. 按照分类订阅（可以在插件设置中关闭）：`/feed/categories/{slug}.xml`
3. 按照作者订阅（可以在插件设置中关闭）：`/feed/authors/{name}.xml`

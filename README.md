# Rss Halo Plugin

RSS插件 Halo 博客版本

![](https://img.shields.io/badge/%E6%96%B0%E7%96%86%E8%90%8C%E6%A3%AE%E8%BD%AF%E4%BB%B6%E5%BC%80%E5%8F%91%E5%B7%A5%E4%BD%9C%E5%AE%A4-%E6%8F%90%E4%BE%9B%E6%8A%80%E6%9C%AF%E6%94%AF%E6%8C%81-blue)

## 订阅管理

+ 全站订阅
    + 无扩展： `/feed` 或 `/rss`
    + 含扩展： `/feed.xml` 或 `/rss.xml`
+ 文章分类订阅
    + `/feed/categories/{slug}.xml`
    + `slug` 分类名称(配置中的别名)
+ 文章作者订阅
    + `/feed/authors/{name}.xml`
    + `name` 作者用户名(点击文章的作者，在作者主页地址栏最后一段为用户名) 
+ Follow 认证支持
+ 封面支持

## 安装说明

请先关闭自带 `RSS` 插件
![Halo RSS](https://github.com/user-attachments/assets/b7a1a195-350f-491f-a506-56824f4fa96b)

1. 下载安装包：[Releases 发布库](https://github.com/QYG2297248353/rss-plugin-halo/releases)
2. 通过 本地安装
![image](https://github.com/user-attachments/assets/aaf1d4eb-de9c-4c36-932a-151037cd4943)
3. 完成安装
![image](https://github.com/user-attachments/assets/15ef4e0b-59af-4efd-959b-62075907746f)

## 配置说明
![image](https://github.com/user-attachments/assets/8c136199-d079-4e24-bb9b-f5a1b84b5b5d)

## 常见问题

### Follow订阅后为什么没有封面？
由于 Follow 数据取自其自己的后端，当您订阅后发现，文章列表没有封面，说明您的 rss链接 被其他人订阅过。

您可以新发布一篇文章，便会出现文章封面。

> 前提：您在文章的设置中配置了封面。

其他解决方案：由于提供了四个订阅地址 `rss` `feed` `rss.xml` `feed.xml` 您可以选择其他订阅地址。

### Follow 认证ID 是什么？
通过 Follow 客户端订阅后，在您的博客上右键会出现 `认证` `Claim` 按钮，点击后会出现认证方式

+ 选择 RSS 标签
    + `feedId` ==> 认证ID
    + `userId` ==> 用户ID

![image](https://github.com/user-attachments/assets/8fdce636-c1a2-4fae-87dd-df2be52bac37)




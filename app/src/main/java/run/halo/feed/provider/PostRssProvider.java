package run.halo.feed.provider;

import org.springframework.stereotype.Component;
import run.halo.app.content.PostContentService;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.ExternalUrlSupplier;
import run.halo.app.infra.SystemInfoGetter;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.feed.service.PostService;

@Component
public class PostRssProvider extends AbstractPostRssProvider {

    public PostRssProvider(PostService postService, ReactiveExtensionClient client,
        ExternalLinkProcessor externalLinkProcessor, ReactiveSettingFetcher settingFetcher,
        PostContentService postContentService, ExternalUrlSupplier externalUrlSupplier,
        SystemInfoGetter systemInfoGetter) {
        super(postService, client, externalLinkProcessor, settingFetcher, postContentService,
            externalUrlSupplier, systemInfoGetter);
    }
}

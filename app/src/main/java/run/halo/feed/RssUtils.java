package run.halo.feed;

import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.springframework.web.util.UriUtils;
import run.halo.app.core.attachment.ThumbnailSize;

@UtilityClass
public class RssUtils {

    public static String genRelativeThumbUri(String url, ThumbnailSize size) {
        return "/apis/api.storage.halo.run/v1alpha1/thumbnails/-/via-uri?uri="
            + UriUtils.encode(url, StandardCharsets.UTF_8)
            + "&size=" + size.name().toLowerCase();
    }
}

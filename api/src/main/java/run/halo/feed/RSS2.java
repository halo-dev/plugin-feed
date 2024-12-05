package run.halo.feed;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class RSS2 {
    /**
     * (Recommended) The name of the feed, which should be plain text only
     */
    @NotBlank
    private String title;

    /**
     * (Recommended) The URL of the website associated with the feed, which should link to a
     * human-readable website
     */
    @NotBlank
    private String link;

    /**
     * (Optional) The summary of the feed, which should be plain text only
     */
    private String description;

    /**
     * The primary language of the feed, which should be a value from
     * <a href="https://www.rssboard.org/rss-language-codes">RSS Language Codes</a> or ISO
     * 639 language codes
     */
    private String language;

    /**
     * (Recommended) The URL of the image that represents the channel, which should be relatively
     * large and square
     */
    private String image;

    @Singular
    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        /**
         * (Required) The title of the item, which should be plain text only
         */
        @NotBlank
        private String title;

        /**
         * (Recommended) The URL of the item, which should link to a human-readable website
         */
        @NotBlank
        private String link;

        /**
         * <p>(Recommended) The content of the item. For an Atom feed, it's the atom:content
         * element.</p>
         * <p>For a JSON feed, it's the content_html field.</p>
         */
        @NotBlank
        private String description;

        /**
         * (Optional) The author of the item
         */
        private String author;

        /**
         * (Optional) The category of the item. You can use a plain string or an array of strings
         */
        @Singular
        private List<String> categories;

        /**
         * (Recommended) The publication
         * <a href="https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/Date">date </a> of the item, which should be a Date object
         * following <a href="https://docs.rsshub.app/joinus/advanced/pub-date">the standard</a>
         */
        private Instant pubDate;

        /**
         * (Optional) The unique identifier of the item
         */
        private String guid;

        /**
         * (Optional) The URL of an enclosure associated with the item
         */
        private String enclosureUrl;

        /**
         * (Optional) The size of the enclosure file in byte, which should be a number
         */
        private String enclosureLength;

        /**
         * (Optional) The MIME type of the enclosure file, which should be a string
         */
        private String enclosureType;

        /**
         * (Optional) Media content, represented by the <media:content> element.
         */
        @Singular
        private List<MediaContent> mediaContents;
    }

    @Data
    @Builder
    public static class MediaContent {
        /**
         * URL of the media object.
         */
        private String url;

        /**
         * Type of the media, such as "image/jpeg", "audio/mpeg", "video/mp4".
         */
        private String type;

        /**
         * The general type of media: image, audio, video.
         */
        private MediaType mediaType;

        /**
         * File size of the media object in bytes.
         */
        private String fileSize;

        /**
         * Duration of the media object in seconds (for audio and video).
         */
        private String duration;

        /**
         * Height of the media object in pixels (for image and video).
         */
        private String height;

        /**
         * Width of the media object in pixels (for image and video).
         */
        private String width;

        /**
         * Bitrate of the media (for audio and video).
         */
        private String bitrate;

        /**
         * Thumbnail associated with this media content.
         */
        private MediaThumbnail thumbnail;

        public enum MediaType {
            IMAGE, AUDIO, VIDEO, DOCUMENT
        }
    }

    @Data
    @Builder
    public static class MediaThumbnail {
        /**
         * URL of the thumbnail.
         */
        private String url;

        /**
         * Height of the thumbnail in pixels.
         */
        private String height;

        /**
         * Width of the thumbnail in pixels.
         */
        private String width;
    }
}

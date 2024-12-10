package run.halo.feed;

import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;

@Data
@Accessors(chain = true)
public class TelemetryEventInfo {
    private String pageUrl;
    private String screen;
    private String language;
    private String languageRegion;
    private String title;
    private String referrer;
    private String ip;
    private String userAgent;
    private String browser;
    private String os;

    @Getter(onMethod_ = @NonNull)
    private HttpHeaders headers;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TelemetryEventInfo that = (TelemetryEventInfo) o;
        return Objects.equals(pageUrl, that.pageUrl) && Objects.equals(title, that.title)
            && Objects.equals(referrer, that.referrer) && Objects.equals(ip, that.ip)
            && Objects.equals(userAgent, that.userAgent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageUrl, title, referrer, ip, userAgent);
    }
}

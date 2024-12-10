package run.halo.feed.telemetry;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.halo.feed.TelemetryEventInfo;

@Component
@RequiredArgsConstructor
public class TelemetryEndpoint {
    public static final String TELEMETRY_PATH = "/plugins/feed/assets/telemetry.gif";
    static final Resource ONE_PIXEL;
    private final TelemetryRecorderDelegator telemetryRecorderDelegator;

    static {
        // RSS readers may thumbnail images, and using base64 images may cause RSS readers to
        // fail to parse correctly.
        ONE_PIXEL = new ClassPathResource("1pixel.png", TelemetryEndpoint.class.getClassLoader());
    }

    @Bean
    public RouterFunction<ServerResponse> telemetryImageRouter() {
        return RouterFunctions.route()
            .GET(TELEMETRY_PATH, request -> {
                telemetryRecorderDelegator.record(createEventInfo(request));
                return ServerResponse.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_GIF_VALUE)
                    .cacheControl(CacheControl.noCache())
                    .bodyValue(ONE_PIXEL);
            })
            .build();
    }

    private TelemetryEventInfo createEventInfo(ServerRequest request) {
        var userAgent = request.headers().firstHeader(HttpHeaders.USER_AGENT);
        var browser = BrowserDetector.detectBrowser(userAgent);
        var eventInfo = new TelemetryEventInfo()
            .setTitle(queryParamOrNull(request, "title"))
            .setPageUrl(queryParamOrNull(request, "url"))
            .setBrowser(browser.nameVersion())
            .setOs(browser.os())
            .setIp(IpAddressUtils.getIpAddress(request))
            .setReferrer(request.headers().firstHeader(HttpHeaders.REFERER))
            .setScreen(browser.screen())
            .setUserAgent(userAgent)
            .setHeaders(request.headers().asHttpHeaders());

        var acceptLang = request.headers().firstHeader(HttpHeaders.ACCEPT_LANGUAGE);
        var languages = AcceptLanguageParser.parseAcceptLanguage(acceptLang);
        if (!CollectionUtils.isEmpty(languages)) {
            var lang = languages.get(0);
            eventInfo.setLanguage(languages.get(0).code());
            if (lang.region() != null) {
                eventInfo.setLanguageRegion(lang.code() + "-" + lang.region());
            }
        }
        return eventInfo;
    }

    private static String queryParamOrNull(ServerRequest request, String name) {
        return request.queryParam(name).orElse(null);
    }
}

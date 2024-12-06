package run.halo.feed.telemetry;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.halo.feed.TelemetryEventInfo;

@Component
@RequiredArgsConstructor
public class TelemetryEndpoint {
    public static final String TELEMETRY_PATH = "/plugins/feed/assets/telemetry.gif";
    static final String ONE_PIXEL_GIF_BASE64 =
        "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
    private final TelemetryRecorderDelegator telemetryRecorderDelegator;

    @Bean
    public RouterFunction<ServerResponse> telemetryImageRouter() {
        return RouterFunctions.route()
            .GET(TELEMETRY_PATH, request -> {
                telemetryRecorderDelegator.record(createEventInfo(request));
                return ServerResponse.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_GIF_VALUE)
                    .cacheControl(CacheControl.noCache())
                    .bodyValue(ONE_PIXEL_GIF_BASE64);
            })
            .build();
    }

    private TelemetryEventInfo createEventInfo(ServerRequest request) {
        var userAgent = request.headers().firstHeader(HttpHeaders.USER_AGENT);
        var browser = BrowserDetector.detectBrowser(userAgent);
        var acceptLang = request.headers().firstHeader(HttpHeaders.ACCEPT_LANGUAGE);
        return new TelemetryEventInfo()
            .setTitle(queryParamOrNull(request, "title"))
            .setPageUrl(queryParamOrNull(request, "url"))
            .setBrowser(browser.nameVersion())
            .setOs(browser.os())
            .setIp(IpAddressUtils.getIpAddress(request))
            .setReferrer(request.headers().firstHeader(HttpHeaders.REFERER))
            .setScreen(browser.screen())
            .setUserAgent(userAgent)
            .setLanguage(parseLanguage(acceptLang))
            .setHeaders(request.headers().asHttpHeaders());
    }

    private String parseLanguage(String acceptLanguage) {
        var languages = AcceptLanguageParser.parseAcceptLanguage(acceptLanguage);
        return languages.isEmpty() ? null : languages.get(0).code();
    }

    private static String queryParamOrNull(ServerRequest request, String name) {
        return request.queryParam(name).orElse(null);
    }
}

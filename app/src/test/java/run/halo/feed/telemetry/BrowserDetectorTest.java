package run.halo.feed.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BrowserDetectorTest {

    @ParameterizedTest
    @MethodSource("provideUserAgentTestCases")
    void detectorBrowserTest(String userAgent, String expectedBrowser, String expectedVersion) {
        var result = BrowserDetector.detectBrowser(userAgent);

        assertEquals(expectedBrowser, result.name(), "Browser name mismatch");
        assertEquals(expectedVersion, result.version(), "Version mismatch");
    }

    static Stream<Arguments> provideUserAgentTestCases() {
        return Stream.of(
            Arguments.of(
                "NetNewsWire (RSS Reader; https://netnewswire.com/)",
                "NetNewsWire",
                "RSS Reader"),
            Arguments.of(
                "NetNewsWire/5.1",
                "NetNewsWire/5.1",
                null),
            Arguments.of(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
                "Mozilla/5.0",
                "Macintosh"),
            Arguments.of(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like "
                    + "Gecko) Chrome/131.0.0.0 Safari/537.36",
                "Chrome",
                "131.0.0.0"
            ),
            Arguments.of(
                "SomeApp (Version 1.0; Details Here)",
                "SomeApp",
                "Version 1.0"),
            Arguments.of(
                "UnknownApp",
                "UnknownApp",
                null)
        );
    }
}
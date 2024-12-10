package run.halo.feed.telemetry;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class BrowserDetector {
    static final String UNKNOWN = "Unknown";

    private static final List<OperatingSystemRule> OPERATING_SYSTEM_RULES = Arrays.asList(
        new OperatingSystemRule("iOS", Pattern.compile("iP(hone|od|ad)")),
        new OperatingSystemRule("Android OS", Pattern.compile("Android")),
        new OperatingSystemRule("Windows 10", Pattern.compile("Windows NT 10.0")),
        new OperatingSystemRule("Windows 8.1", Pattern.compile("Windows NT 6.3")),
        new OperatingSystemRule("Windows 8", Pattern.compile("Windows NT 6.2")),
        new OperatingSystemRule("Windows 7", Pattern.compile("Windows NT 6.1")),
        new OperatingSystemRule("Windows Vista", Pattern.compile("Windows NT 6.0")),
        new OperatingSystemRule("Windows XP", Pattern.compile("Windows NT 5.1|Windows XP")),
        new OperatingSystemRule("Windows 2000", Pattern.compile("Windows NT 5.0|Windows 2000")),
        new OperatingSystemRule("Mac OS", Pattern.compile("Macintosh.*Mac OS X ([0-9_]+)")),
        new OperatingSystemRule("Chrome OS", Pattern.compile("CrOS")),
        new OperatingSystemRule("Linux", Pattern.compile("(Linux|X11)")),
        new OperatingSystemRule("BlackBerry OS", Pattern.compile("BlackBerry|BB10")),
        new OperatingSystemRule("Windows CE", Pattern.compile("Windows CE|WinCE")),
        new OperatingSystemRule("QNX", Pattern.compile("QNX")),
        new OperatingSystemRule("BeOS", Pattern.compile("BeOS")),
        new OperatingSystemRule("Open BSD", Pattern.compile("OpenBSD")),
        new OperatingSystemRule("Sun OS", Pattern.compile("SunOS"))
    );

    // High-priority patterns with associated application names
    private static final Map<Pattern, String> HIGH_PRIORITY_PATTERNS = new LinkedHashMap<>();

    static {
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("AOLShield/([0-9._]+)"), "AOLShield");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("(?!Chrom.*OPR)Chrom(?:e|ium)/([0-9.]+)"),
            "Chrome");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("Version/([0-9._]+).*Safari"), "Safari");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("Firefox/([0-9.]+)"), "Firefox");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("Edge/([0-9.]+)"), "Edge");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("OPR/([0-9.]+)"), "Opera");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("CriOS/([0-9.]+)"), "Chrome iOS");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("EdgiOS/([0-9.]+)"), "Edge iOS");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("YaBrowser/([0-9.]+)"), "Yandex Browser");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("KAKAOTALK\\s([0-9.]+)"), "KakaoTalk");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("SamsungBrowser/([0-9.]+)"), "Samsung Browser");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("\\bSilk/([0-9._-]+)\\b"), "Silk");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("MiuiBrowser/([0-9.]+)$"), "Miui Browser");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("BeakerBrowser/([0-9.]+)"), "Beaker Browser");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("PhantomJS/([0-9.]+)"), "PhantomJS");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("^curl/([0-9.]+)$"), "Curl");
        HIGH_PRIORITY_PATTERNS.put(Pattern.compile("bot|crawler|spider|crawl(er|ing)"), "Bot");
    }

    // Fallback general patterns
    private static final List<Pattern> GENERAL_PATTERNS = Arrays.asList(
        // Match "ApplicationName/Version (Additional Info)" format
        Pattern.compile("([a-zA-Z0-9]+(?:/[0-9.]+)?)\\s*\\(([^;]+)(?:;\\s(.+))?\\)"),
        // Match "ApplicationName/Version"
        Pattern.compile("([a-zA-Z0-9]+(?:/[0-9.]+)?)"),
        // Match general application name (last resort)
        Pattern.compile("([a-zA-Z0-9]+(?:\\s[a-zA-Z0-9]+)*)")
    );

    public static BrowserInfo detectBrowser(String userAgent) {
        UserAgentInfo userAgentInfo = parseUserAgent(userAgent);
        String browser = userAgentInfo.application();
        String version = userAgentInfo.version();
        String os = detectOsInternal(userAgent);
        String screen = guessScreen(browser, os).toString();
        return new BrowserInfo(browser, version, os, screen);
    }

    private static UserAgentInfo parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return new UserAgentInfo("Unknown", null, null);
        }

        // Try high-priority patterns first
        for (Map.Entry<Pattern, String> entry : HIGH_PRIORITY_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(userAgent);
            if (matcher.find()) {
                String application = entry.getValue();
                String version =
                    matcher.group(1); // Most high-priority patterns have version in group 1
                return new UserAgentInfo(application, version, null);
            }
        }

        // Try general patterns as a fallback
        for (Pattern pattern : GENERAL_PATTERNS) {
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                String application = matcher.group(1);
                String version = matcher.groupCount() > 1 ? matcher.group(2) : null;
                String additionalInfo = matcher.groupCount() > 2 ? matcher.group(3) : null;
                return new UserAgentInfo(application, version, additionalInfo);
            }
        }

        // Fallback for unrecognized user agents
        return new UserAgentInfo(userAgent, null, null);
    }

    private static String detectOsInternal(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return null;
        }
        String osName = null;
        for (OperatingSystemRule rule : OPERATING_SYSTEM_RULES) {
            Matcher matcher = rule.regex.matcher(userAgent);
            if (matcher.find()) {
                osName = rule.os;
                break;
            }
        }
        return osName;
    }

    public static ScreenResolution guessScreen(String browser, String os) {
        if (browser == null || os == null) {
            // Default fallback resolution
            return new ScreenResolution(1920, 1080);
        }

        if (StringUtils.isBlank(os)) {
            // Default fallback for unknown cases
            // Assume Full HD as a safe default
            return ScreenResolution.defaultResolution();
        }

        // Common resolutions based on OS and Browser
        if (os.contains("Windows") || os.contains("Mac OS")) {
            // Desktop OS
            if (browser.contains("chrome") || browser.contains("firefox") || browser.contains(
                "edge")) {
                // Full HD is most common
                return new ScreenResolution(1920, 1080);
            } else if (browser.contains("safari")) {
                // Many Mac users have Retina displays
                return new ScreenResolution(2560, 1440);
            }
        } else if (os.contains("Android OS")) {
            // Mobile OS
            // Full HD in portrait mode
            return new ScreenResolution(1080, 1920);
        } else if (os.contains("iOS")) {
            // iOS devices (e.g., iPhone, iPad)
            if (browser.contains("safari") || browser.contains("crios")) {
                // iPhone 12 resolution
                return new ScreenResolution(1170, 2532);
            } else {
                // iPad Pro resolution
                return new ScreenResolution(2048, 2732);
            }
        } else if (os.contains("Linux") || os.contains("Chrome OS")) {
            // Common for Chromebooks
            return new ScreenResolution(1366, 768);
        }
        return ScreenResolution.defaultResolution();
    }

    public record ScreenResolution(int width, int height) {
        @Override
        public String toString() {
            return width + "x" + height;
        }

        public static ScreenResolution defaultResolution() {
            return new ScreenResolution(1920, 1080);
        }
    }

    private record UserAgentRule(String browser, Pattern regex) {
    }

    private record OperatingSystemRule(String os, Pattern regex) {
    }

    public record UserAgentInfo(String application, String version, String additionalInfo) {
        @Override
        public String toString() {
            return "UserAgentInfo{" +
                "application='" + application + '\'' +
                ", version='" + version + '\'' +
                ", additionalInfo='" + additionalInfo + '\'' +
                '}';
        }
    }

    public record BrowserInfo(String name, String version, String os, String screen) {
        public BrowserInfo {
            if (name == null) {
                name = UNKNOWN;
            }
            if (os == null) {
                os = UNKNOWN;
            }
        }

        public String nameVersion() {
            if (StringUtils.isBlank(version)) {
                return name;
            }
            if (!UNKNOWN.equals(name)) {
                return name + " " + version;
            }
            return name;
        }

        @Override
        public String toString() {
            return "BrowserInfo{name='" + name + "', version='" + version + "', os='" + os + "'}";
        }
    }
}

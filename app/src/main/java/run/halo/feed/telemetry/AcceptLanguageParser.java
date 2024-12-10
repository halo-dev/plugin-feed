package run.halo.feed.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;

@UtilityClass
public class AcceptLanguageParser {

    public record Language(String code, String script, String region, double quality) {
        @Override
        public String toString() {
            return "Language{" +
                "code='" + code + '\'' +
                ", script='" + script + '\'' +
                ", region='" + region + '\'' +
                ", quality=" + quality +
                '}';
        }
    }

    private static final Pattern REGEX = Pattern.compile(
        "((([a-zA-Z]+(-[a-zA-Z0-9]+){0,2})|\\*)(;q=[0-1](\\.[0-9]+)?)?)*");

    @NonNull
    public static List<Language> parseAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return Collections.emptyList();
        }

        List<Language> languages = new ArrayList<>();
        Matcher matcher = REGEX.matcher(acceptLanguage);

        while (matcher.find()) {
            String match = matcher.group();
            if (match == null || match.isEmpty()) {
                continue;
            }

            String[] parts = match.split(";");
            String ietfTag = parts[0];
            String[] ietfComponents = ietfTag.split("-");
            String code = ietfComponents[0];
            String script = ietfComponents.length == 3 ? ietfComponents[1] : null;
            String region = ietfComponents.length == 3 ? ietfComponents[2]
                : ietfComponents.length == 2 ? ietfComponents[1] : null;

            double quality = 1.0;
            if (parts.length > 1 && parts[1].startsWith("q=")) {
                try {
                    quality = Double.parseDouble(parts[1].substring(2));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            languages.add(new Language(code, script, region, quality));
        }

        return languages.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingDouble((Language l) -> l.quality).reversed())
            .collect(Collectors.toList());
    }
}

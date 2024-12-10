package run.halo.feed.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static run.halo.feed.telemetry.AcceptLanguageParser.parseAcceptLanguage;

import org.junit.jupiter.api.Test;

class AcceptLanguageParserTest {

    @Test
    void parseLangTest() {
        String acceptLanguage = "en-US;q=0.9,fr-CA,fr;q=0.8,en;q=0.7,*;q=0.5";
        var languages = parseAcceptLanguage(acceptLanguage);
        assertThat(languages).hasSize(5);
        assertThat(languages.get(0).code()).isEqualTo("fr");
    }
}
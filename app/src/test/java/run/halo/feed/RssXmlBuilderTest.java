package run.halo.feed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static run.halo.feed.RssXmlBuilder.parseLengthOfContentRange;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RssXmlBuilderTest {

    static Stream<Arguments> testParseLengthOfContentRange() {
        return Stream.of(
            arguments("bytes 0-0/123", 123L),
            arguments("bytes */567", 567L),
            arguments("bytes 0-0", null),
            arguments("bytes 0-0/*", null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testParseLengthOfContentRange(String contentRange, Long expected) {
        assertEquals(expected, parseLengthOfContentRange(contentRange));
    }
}
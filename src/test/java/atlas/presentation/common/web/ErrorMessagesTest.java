package atlas.presentation.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ErrorMessagesTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final Pattern DOMAIN_CODE = Pattern.compile("\"([A-Z][A-Za-z]+\\.[A-Za-z]+)\"");
    private static final Pattern TRANSLATED_CODE = Pattern.compile("(?m)^\\s+'?([A-Za-z][A-Za-z0-9.]*)'?:");

    @Test
    void shouldTranslateEveryErrorCodeDefinedByTheDomain() {
        var codes = domainErrorCodes();
        var translated = translatedCodes();

        assertThat(codes).isNotEmpty();
        assertThat(translated).containsAll(codes);
    }

    private static Set<String> domainErrorCodes() {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files
                .filter(file -> file.getFileName().toString().endsWith("Errors.java"))
                .flatMap(ErrorMessagesTest::codesIn)
                .collect(Collectors.toSet());
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static Stream<String> codesIn(Path file) {
        return DOMAIN_CODE.matcher(read(file)).results().map(match -> match.group(1));
    }

    private static Set<String> translatedCodes() {
        var script = StaticResources.read("/web-shared/error-messages.js");

        return TRANSLATED_CODE.matcher(script).results()
            .map(match -> match.group(1))
            .collect(Collectors.toSet());
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}

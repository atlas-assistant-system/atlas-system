package atlas.presentation.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WebAssetsTest {

    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final List<String> EXTERNAL_HOSTS = List.of(
        "cdn.jsdelivr.net", "storage.googleapis.com", "unpkg.com", "cdnjs.cloudflare.com");

    @Test
    void shouldNotLoadAnythingFromOutsideAtlas() throws IOException {
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            var offenders = files
                .filter(Files::isRegularFile)
                .filter(WebAssetsTest::isWebAsset)
                .filter(WebAssetsTest::referencesAnExternalHost)
                .map(RESOURCES::relativize)
                .map(Path::toString)
                .toList();

            assertThat(offenders).isEmpty();
        }
    }

    private static boolean isWebAsset(Path file) {
        var name = file.getFileName().toString();
        if (name.equals("swagger.html")) {
            return false;
        }

        return name.endsWith(".html") || name.endsWith(".js") || name.endsWith(".css");
    }

    private static boolean referencesAnExternalHost(Path file) {
        try {
            var content = Files.readString(file);

            return EXTERNAL_HOSTS.stream().anyMatch(content::contains);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + file, exception);
        }
    }
}

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

    /**
     * Un espejo colgado en la pared no puede depender de un tercero para arrancar: sin red, una
     * sola de estas URLs deja la pantalla sin modelos, sin gestos y sin forma de autenticarse.
     * Los ficheros los baja el build y los sirve Atlas desde {@code /vendor}.
     */
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

    /**
     * Las paginas de Swagger quedan fuera a proposito: son documentacion de la API que se abre
     * desde un portatil, no forman parte del arranque del espejo y su CDN caido no impide
     * autenticarse ni ver la hora. Todo lo que si pinta la pantalla entra.
     */
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

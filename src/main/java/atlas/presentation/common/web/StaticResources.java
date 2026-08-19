package atlas.presentation.common.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class StaticResources {

    private StaticResources() {}

    public static String read(String path) {
        try (var stream = StaticResources.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new UncheckedIOException(new IOException("Missing classpath resource: " + path));
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

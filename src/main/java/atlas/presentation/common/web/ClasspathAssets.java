package atlas.presentation.common.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Sirve ficheros binarios del classpath: los modelos de Human, el runtime de MediaPipe y el
 * reconocedor de gestos. {@link StaticResources} no vale para esto porque lee a {@code String} en
 * UTF-8, y aqui hay {@code .wasm} y {@code .bin} de hasta doce megas.
 *
 * <p>
 * El cuerpo va en trozos y sin pasar por memoria entera: el espejo arranca con 96 MB de heap y un
 * solo {@code byte[]} del wasm se comeria un octavo.
 */
public final class ClasspathAssets implements HttpHandler {

    /** Clavados a una version en el build, asi que nunca cambian bajo la misma URL. */
    public static final String CACHE_FOREVER = "public, max-age=31536000, immutable";

    private static final Pattern SAFE_PATH = Pattern.compile("(/[A-Za-z0-9._-]+)+");
    private static final String DEFAULT_TYPE = "application/octet-stream";
    private static final Map<String, String> TYPES = Map.of(
        "js", "text/javascript; charset=utf-8",
        "mjs", "text/javascript; charset=utf-8",
        "json", "application/json; charset=utf-8",
        "wasm", "application/wasm",
        "css", "text/css; charset=utf-8",
        "html", "text/html; charset=utf-8");

    private final String prefix;
    private final String root;

    public ClasspathAssets(String prefix, String root) {
        this.prefix = prefix;
        this.root = root;
    }

    /**
     * El punto en el nombre esta permitido pero {@code ..} no: sin esa comprobacion, un
     * {@code /vendor/../../etc} se convertiria en cualquier recurso del jar.
     */
    static Optional<String> resolve(String prefix, String root, String path) {
        if (!path.startsWith(prefix)) {
            return Optional.empty();
        }
        var relative = path.substring(prefix.length());
        if (relative.isEmpty() || !SAFE_PATH.matcher(relative).matches() || relative.contains("..")) {
            return Optional.empty();
        }

        return Optional.of(root + relative);
    }

    static String contentTypeOf(String path) {
        var dot = path.lastIndexOf('.');

        return dot < 0 ? DEFAULT_TYPE : TYPES.getOrDefault(path.substring(dot + 1), DEFAULT_TYPE);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            var resource = resolve(prefix, root, exchange.getRequestURI().getPath());
            if (resource.isEmpty()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            try (var stream = ClasspathAssets.class.getResourceAsStream(resource.get())) {
                if (stream == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                exchange.getResponseHeaders().set("Content-Type", contentTypeOf(resource.get()));
                exchange.getResponseHeaders().set("Cache-Control", CACHE_FOREVER);
                exchange.sendResponseHeaders(200, 0);
                stream.transferTo(exchange.getResponseBody());
            }
        }
    }
}

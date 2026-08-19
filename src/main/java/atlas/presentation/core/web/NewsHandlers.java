package atlas.presentation.core.web;

import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class NewsHandlers {

    private static final Duration CACHE_TIME = Duration.ofMinutes(30);
    private static final Pattern TITLE = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"");
    private static final List<Source> SOURCES = List.of(
        new Source("Tecnología", "tech"),
        new Source("IA", "ai"),
        new Source("Desarrollo", "dev"),
        new Source("Datos", "data"),
        new Source("DevOps", "devops"),
        new Source("Seguridad", "infosec"),
        new Source("IT", "it"),
        new Source("Hardware", "hardware"));

    private static String cached = "[]";
    private static Instant expiresAt = Instant.EPOCH;

    private NewsHandlers() {}

    public static synchronized HttpResponse latest(HttpRequest request) {
        if (Instant.now().isAfter(expiresAt)) {
            var items = new ArrayList<Map<String, String>>();
            for (var source : SOURCES) {
                fetch(source).ifPresent(items::add);
            }
            if (!items.isEmpty()) {
                cached = Json.write(items);
            }
            expiresAt = Instant.now().plus(CACHE_TIME);
        }

        return HttpResponse.ok(cached);
    }

    static String extractTitle(String html) {
        var match = TITLE.matcher(html);
        if (!match.find()) {
            return "";
        }

        return decode(match.group(1));
    }

    static String extractArchiveTitle(String slug, String html) {
        var pattern = Pattern.compile(
            "<a href=\"/" + Pattern.quote(slug) + "/(\\d{4}-\\d{2}-\\d{2})\"><div[^>]*>(.*?)</div></a>",
            Pattern.DOTALL);
        var match = pattern.matcher(html);

        return match.find() ? decode(match.group(2).replaceAll("<[^>]+>", "")) : "";
    }

    private static String decode(String value) {
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'");
    }

    private static Optional<Map<String, String>> fetch(Source source) {
        try {
            var connection = open("https://tldr.tech/api/latest/" + source.slug());
            try (var input = connection.getInputStream()) {
                var title = extractTitle(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                if (title.isBlank()) {
                    return Optional.empty();
                }
                var url = connection.getURL().toString();
                var publishedAt = url.substring(url.lastIndexOf('/') + 1);

                return Optional.of(Map.of(
                    "category", source.name(),
                    "title", title,
                    "url", url,
                    "publishedAt", publishedAt));
            } finally {
                connection.disconnect();
            }
        } catch (IOException exception) {
            return fetchArchive(source);
        }
    }

    private static Optional<Map<String, String>> fetchArchive(Source source) {
        try {
            var connection = open("https://tldr.tech/" + source.slug() + "/archives");
            try (var input = connection.getInputStream()) {
                var html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                var title = extractArchiveTitle(source.slug(), html);
                if (title.isBlank()) {
                    return Optional.empty();
                }
                var pattern = Pattern.compile("href=\"/" + Pattern.quote(source.slug()) + "/(\\d{4}-\\d{2}-\\d{2})\"");
                var date = pattern.matcher(html);
                if (!date.find()) {
                    return Optional.empty();
                }
                var publishedAt = date.group(1);

                return Optional.of(Map.of(
                    "category", source.name(),
                    "title", title,
                    "url", "https://tldr.tech/" + source.slug() + "/" + publishedAt,
                    "publishedAt", publishedAt));
            } finally {
                connection.disconnect();
            }
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(3_000);
        connection.setReadTimeout(5_000);
        connection.setRequestProperty("User-Agent", "Atlas/1.0 (+http://localhost)");

        return connection;
    }

    private record Source(String name, String slug) {}
}

package atlas.presentation.home.web;

import atlas.domain.home.enums.NewsCategory;
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
    private static final List<NewsCategory> SOURCES = List.of(NewsCategory.values());
    private static final int ISSUES_PER_CATEGORY = 8;

    private static List<NewsItem> cached = List.of();
    private static Instant expiresAt = Instant.EPOCH;

    private NewsHandlers() {}

    public static synchronized HttpResponse latest(HttpRequest request) {
        if (Instant.now().isAfter(expiresAt)) {
            var items = new ArrayList<NewsItem>();
            for (var category : SOURCES) {
                items.addAll(fetch(category));
            }
            if (!items.isEmpty()) {
                cached = List.copyOf(items);
            }
            expiresAt = Instant.now().plus(CACHE_TIME);
        }

        var selected = Values.categories(request.queryParam("categories").orElse(null));
        var response = cached.stream()
            .filter(item -> selected.contains(item.category()))
            .map(NewsHandlers::response)
            .toList();

        return HttpResponse.ok(Json.write(response));
    }

    static String extractTitle(String html) {
        var match = TITLE.matcher(html);
        return match.find() ? decode(match.group(1)) : "";
    }

    /**
     * Las ediciones del archivo, de la más reciente a la más antigua. La pantalla necesita varias
     * por tema para llenar la columna: con dos temas elegidos caben muchas más de cada uno que con
     * seis, y una sola por tema dejaba el hueco a medias.
     */
    static List<NewsItem> archiveIssues(NewsCategory category, String html) {
        var pattern = Pattern.compile(
            "<a href=\"/" + Pattern.quote(category.slug())
                + "/(\\d{4}-\\d{2}-\\d{2})\"><div[^>]*>(.*?)</div></a>",
            Pattern.DOTALL);
        var match = pattern.matcher(html);
        var issues = new ArrayList<NewsItem>();
        while (match.find() && issues.size() < ISSUES_PER_CATEGORY) {
            var title = decode(match.group(2).replaceAll("<[^>]+>", ""));
            if (!title.isBlank()) {
                issues.add(issue(category, title, match.group(1)));
            }
        }

        return List.copyOf(issues);
    }

    private static NewsItem issue(NewsCategory category, String title, String publishedAt) {
        return new NewsItem(
            category, title, "https://tldr.tech/" + category.slug() + "/" + publishedAt, publishedAt);
    }

    private static String decode(String value) {
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'");
    }

    /** La edición viva primero y detrás las del archivo, sin repetir la del mismo día. */
    private static List<NewsItem> fetch(NewsCategory category) {
        var issues = new ArrayList<NewsItem>();
        latestIssue(category).ifPresent(issues::add);
        for (var older : archive(category)) {
            if (issues.stream().noneMatch(kept -> kept.publishedAt().equals(older.publishedAt()))) {
                issues.add(older);
            }
        }

        return issues;
    }

    private static Optional<NewsItem> latestIssue(NewsCategory category) {
        try {
            var connection = open("https://tldr.tech/api/latest/" + category.slug());
            try (var input = connection.getInputStream()) {
                var title = extractTitle(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                if (title.isBlank()) {
                    return Optional.empty();
                }
                var url = connection.getURL().toString();

                return Optional.of(new NewsItem(
                    category, title, url, url.substring(url.lastIndexOf('/') + 1)));
            } finally {
                connection.disconnect();
            }
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static List<NewsItem> archive(NewsCategory category) {
        try {
            var connection = open("https://tldr.tech/" + category.slug() + "/archives");
            try (var input = connection.getInputStream()) {
                var html = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                return archiveIssues(category, html);
            } finally {
                connection.disconnect();
            }
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static Map<String, String> response(NewsItem item) {
        return Map.of(
            "category", item.category().label(),
            "title", item.title(),
            "url", item.url(),
            "publishedAt", item.publishedAt());
    }

    private static HttpURLConnection open(String url) throws IOException {
        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(3_000);
        connection.setReadTimeout(5_000);
        connection.setRequestProperty("User-Agent", "Atlas/1.0 (+http://localhost)");

        return connection;
    }
}

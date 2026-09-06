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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class NewsHandlers implements AutoCloseable {

    public static final Duration REFRESH_EVERY = Duration.ofMinutes(30);

    private static final Pattern TITLE = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"");
    private static final List<NewsCategory> SOURCES = List.of(NewsCategory.values());
    private static final int ISSUES_PER_CATEGORY = 8;
    private static final System.Logger LOG = System.getLogger("home.news");

    private volatile List<NewsItem> cached = List.of();

    private ScheduledExecutorService scheduler;

    /**
     * Ocho temas por dos peticiones cada uno, con cinco segundos de espera de lectura, es hasta
     * un minuto largo de red. Hacerlo dentro del handler dejaba una peticion del espejo colgada
     * cada media hora y encolaba detras a las demas, asi que se refresca aparte y la vista lee
     * siempre lo ya guardado, aunque sea de hace un rato.
     */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "news-refresher");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(
            this::refreshSafely, 0, REFRESH_EVERY.toMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public void close() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public HttpResponse latest(HttpRequest request) {
        var selected = Values.categories(request.queryParam("categories").orElse(null));
        var response = cached.stream()
            .filter(item -> selected.contains(item.category()))
            .map(NewsHandlers::response)
            .toList();

        return HttpResponse.ok(Json.write(response));
    }

    /**
     * Los ocho temas a la vez: son ocho esperas de red independientes, y en fila costaban la suma
     * en vez del maximo. Un tema que falle vuelve vacio y no se lleva por delante a los demas.
     */
    void refresh() {
        var items = new ArrayList<NewsItem>();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var pending = SOURCES.stream().map(category -> pool.submit(() -> fetch(category))).toList();
            for (var issues : pending) {
                try {
                    items.addAll(issues.get());
                } catch (ExecutionException exception) {
                    LOG.log(System.Logger.Level.WARNING, "A news source failed.", exception.getCause());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // Sin red se conserva lo anterior: un titular de ayer dice mas que un hueco vacio.
        if (!items.isEmpty()) {
            cached = List.copyOf(items);
        }
    }

    private void refreshSafely() {
        try {
            refresh();
        } catch (RuntimeException exception) {
            LOG.log(System.Logger.Level.ERROR, "News refresh failed.", exception);
        }
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

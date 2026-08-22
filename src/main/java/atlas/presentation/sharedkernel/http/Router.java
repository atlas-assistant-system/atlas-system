package atlas.presentation.sharedkernel.http;

import atlas.application.sharedkernel.logging.CorrelationContext;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.infrastructure.sharedkernel.SecureTokens;
import atlas.presentation.sharedkernel.errors.ApiError;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class Router implements HttpHandler {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final int MAX_BODY_BYTES = 1024 * 1024;
    public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String REQUEST_TOO_LARGE = "REQUEST_TOO_LARGE";

    private static final System.Logger LOG = System.getLogger("sharedkernel.http");
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final List<Route> routes;

    Router(List<Route> routes) {
        this.routes = routes;
    }

    public static RouterBuilder builder() {
        return new RouterBuilder();
    }

    public HttpResponse dispatch(HttpRequest request) {
        ObjectGuard.notNull(request, "request");

        var correlationId = safeCorrelationId(request.header(CORRELATION_HEADER).orElse(null));

        return CorrelationContext.callWith(correlationId, () -> route(request));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var headers = headersOf(exchange);
        var correlationId = safeCorrelationId(headers.get(CORRELATION_HEADER));

        headers.put(CORRELATION_HEADER, correlationId);

        HttpResponse response;

        try {
            if (declaredContentLength(headers) > MAX_BODY_BYTES) {
                throw new BodyTooLargeException();
            }

            response = dispatch(requestOf(exchange, headers));
        } catch (BodyTooLargeException e) {
            response = HttpResponse.error(
                new ApiError(413, REQUEST_TOO_LARGE, "The request body is too large.", correlationId, List.of()));
        }

        write(exchange, response, correlationId);
    }

    private HttpResponse route(HttpRequest request) {
        var segments = PathSegments.of(request.path());
        var allowed = new TreeSet<String>();

        for (var route : routes) {
            if (!route.matchesPath(segments)) {
                continue;
            }

            if (!route.method().equals(request.method())) {
                allowed.add(route.method());
                continue;
            }

            return invoke(route, request.withPathParams(route.extractParams(segments)));
        }

        if (!allowed.isEmpty()) {
            return methodNotAllowed(allowed);
        }

        return HttpResponse.error(apiError(404, ROUTE_NOT_FOUND, "No route matches the requested path."));
    }

    private HttpResponse invoke(Route route, HttpRequest request) {
        try {
            return route.handler().handle(request);
        } catch (RuntimeException e) {
            var error = ApiError.from(e);

            if (error.status() >= 500) {
                LOG.log(Level.ERROR, "Unhandled exception in " + request.method() + " " + request.path(), e);
            } else {
                LOG.log(Level.DEBUG, "Rejected " + request.method() + " " + request.path() + ": " + e.getMessage());
            }

            return HttpResponse.error(error);
        }
    }

    private static HttpResponse methodNotAllowed(TreeSet<String> allowed) {
        return HttpResponse
            .error(apiError(405, METHOD_NOT_ALLOWED, "The requested method is not allowed on this path."))
            .withHeader("Allow", String.join(", ", allowed));
    }

    private static ApiError apiError(int status, String code, String message) {
        return new ApiError(status, code, message, CorrelationContext.current().orElse(null), List.of());
    }

    private static HttpRequest requestOf(HttpExchange exchange, Map<String, String> headers) throws IOException {
        return new HttpRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getRawPath(),
            Map.of(),
            queryParamsOf(exchange),
            headers,
            readBody(exchange));
    }

    private static String safeCorrelationId(String candidate) {
        if (candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches()) {
            return candidate;
        }

        return SecureTokens.newCorrelationId();
    }

    private static Map<String, String> headersOf(HttpExchange exchange) {
        var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        exchange.getRequestHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.getFirst());
            }
        });

        return headers;
    }

    private static Map<String, String> queryParamsOf(HttpExchange exchange) {
        var params = new LinkedHashMap<String, String>();
        var query = exchange.getRequestURI().getRawQuery();

        if (query == null || query.isEmpty()) {
            return params;
        }

        for (var pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }

            var separator = pair.indexOf('=');
            var name = separator < 0 ? pair : pair.substring(0, separator);
            var value = separator < 0 ? "" : pair.substring(separator + 1);

            params.put(decode(name), decode(value));
        }

        return params;
    }

    private static long declaredContentLength(Map<String, String> headers) {
        try {
            return Long.parseLong(headers.getOrDefault("Content-Length", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (var input = exchange.getRequestBody()) {
            var bytes = input.readNBytes(MAX_BODY_BYTES + 1);

            if (bytes.length > MAX_BODY_BYTES) {
                throw new BodyTooLargeException();
            }

            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void write(HttpExchange exchange, HttpResponse response, String correlationId) throws IOException {
        var bytes = response.body().getBytes(StandardCharsets.UTF_8);
        var headers = exchange.getResponseHeaders();

        response.headers().forEach(headers::set);

        if (response.contentType() != null && !response.contentType().isBlank()) {
            headers.set("Content-Type", response.contentType());
        }

        headers.set(CORRELATION_HEADER, correlationId);

        try {
            if (bytes.length == 0) {
                exchange.sendResponseHeaders(response.status(), -1);
                return;
            }

            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

}

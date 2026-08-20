package atlas.presentation.common.web;

import atlas.presentation.sharedkernel.http.HttpResponse;
import atlas.presentation.sharedkernel.http.RouteHandler;
import com.sun.net.httpserver.HttpHandler;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class SessionGuard {

    private final BooleanSupplier hasActiveSession;

    public SessionGuard(BooleanSupplier hasActiveSession) {
        this.hasActiveSession = hasActiveSession;
    }

    public RouteHandler protect(RouteHandler handler) {
        return request -> hasActiveSession.getAsBoolean() ? handler.handle(request) : authenticationRequired();
    }

    public HttpHandler protect(HttpHandler handler) {
        return exchange -> {
            if (hasActiveSession.getAsBoolean()) {
                handler.handle(exchange);
                return;
            }

            var body = "Authentication required.".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(401, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        };
    }

    private static HttpResponse authenticationRequired() {
        return HttpResponse.json(401, Json.write(Map.of(
            "code", "Authentication.Required",
            "message", "An active session is required.")));
    }
}

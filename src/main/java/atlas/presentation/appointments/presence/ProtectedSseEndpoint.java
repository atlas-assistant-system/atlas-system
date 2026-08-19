package atlas.presentation.appointments.presence;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ProtectedSseEndpoint implements HttpHandler {

    private final PresenceAccess presence;
    private final HttpHandler endpoint;

    public ProtectedSseEndpoint(PresenceAccess presence, HttpHandler endpoint) {
        this.presence = presence;
        this.endpoint = endpoint;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (presence.authenticated()) {
            endpoint.handle(exchange);
            return;
        }

        var body = "Authentication required.".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(401, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}

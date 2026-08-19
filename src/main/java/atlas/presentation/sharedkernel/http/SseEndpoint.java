package atlas.presentation.sharedkernel.http;

import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.presentation.sharedkernel.sse.SseHeaders;
import atlas.presentation.sharedkernel.sse.SseHub;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SseEndpoint implements HttpHandler {

    public static final String HANDSHAKE = ": connected\n\n";

    private final SseHub hub;

    public SseEndpoint(SseHub hub) {
        this.hub = ObjectGuard.notNull(hub, "hub");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();

            return;
        }

        SseHeaders.forStream().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
        exchange.sendResponseHeaders(200, 0);

        var output = exchange.getResponseBody();

        output.write(HANDSHAKE.getBytes(StandardCharsets.UTF_8));
        output.flush();

        var client = hub.register(output);

        try {
            client.awaitClose();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            hub.disconnect(client.id());
        }
    }
}

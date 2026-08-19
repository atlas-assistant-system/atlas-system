package atlas.presentation.sharedkernel.sse;

import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.infrastructure.sharedkernel.SecureTokens;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SseHub {

    private static final System.Logger LOG = System.getLogger("sharedkernel.sse");

    private final Map<String, SseClient> clients = new ConcurrentHashMap<>();
    private final AtomicLong nextEventId = new AtomicLong(1);

    public SseClient register(OutputStream output) {
        var client = new SseClient(SecureTokens.newCorrelationId(), output);
        clients.put(client.id(), client);
        LOG.log(Level.DEBUG, "SSE client " + client.id() + " connected (" + clients.size() + " open)");

        return client;
    }

    public int broadcast(SseEvent event) {
        ObjectGuard.notNull(event, "event");

        var stamped = event.withId(String.valueOf(nextEventId.getAndIncrement()));

        return deliver(client -> client.send(stamped));
    }

    public int sendHeartbeat() {
        return deliver(client -> client.comment("ping"));
    }

    public int connectedCount() {
        return clients.size();
    }

    public void disconnect(String clientId) {
        var client = clients.remove(clientId);

        if (client != null) {
            client.close();
        }
    }

    public void closeAll() {
        for (var clientId : Map.copyOf(clients).keySet()) {
            disconnect(clientId);
        }
    }

    private int deliver(Delivery delivery) {
        var delivered = 0;

        for (var entry : clients.entrySet()) {
            try {
                delivery.to(entry.getValue());
                delivered++;
            } catch (IOException e) {
                LOG.log(Level.DEBUG, "SSE client " + entry.getKey() + " went away: " + e.getMessage());
                disconnect(entry.getKey());
            }
        }

        return delivered;
    }

    @FunctionalInterface
    private interface Delivery {

        void to(SseClient client) throws IOException;
    }
}

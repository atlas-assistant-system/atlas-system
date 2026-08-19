package atlas.app;

import java.io.IOException;
import sharedkernel.application.cqrs.SimpleCommandBus;
import sharedkernel.application.cqrs.SimpleQueryBus;
import sharedkernel.application.events.SimpleDomainEventPublisher;
import sharedkernel.application.logging.LogEntryRenderer;
import sharedkernel.presentation.http.Router;
import sharedkernel.presentation.http.SseEndpoint;
import sharedkernel.presentation.http.WebServer;
import sharedkernel.presentation.sse.SseHub;

public final class Application {

    public static final String EVENT_STREAM_PATH = "/events";

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final SimpleDomainEventPublisher events;
    private final SseHub hub;
    private final Router router;

    private WebServer server;

    private Application(
        SimpleCommandBus commands,
        SimpleQueryBus queries,
        SimpleDomainEventPublisher events,
        SseHub hub,
        Router router) {
        this.commands = commands;
        this.queries = queries;
        this.events = events;
        this.hub = hub;
        this.router = router;
    }

    public static Application wire(LogEntryRenderer renderer) {
        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        var hub = new SseHub();

        registerHandlers(commands, queries, events, renderer);

        return new Application(commands, queries, events, hub, routes(commands, queries));
    }

    public Application start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", router)
            .mount(EVENT_STREAM_PATH, new SseEndpoint(hub))
            .start();

        return this;
    }

    public void stop() {
        hub.closeAll();

        if (server != null) {
            server.close();
        }
    }

    public int port() {
        return server.port();
    }

    public SimpleCommandBus commands() {
        return commands;
    }

    public SimpleQueryBus queries() {
        return queries;
    }

    public SimpleDomainEventPublisher events() {
        return events;
    }

    public SseHub hub() {
        return hub;
    }

    private static Router routes(SimpleCommandBus commands, SimpleQueryBus queries) {
        return Router.builder().build();
    }

    private static void registerHandlers(
        SimpleCommandBus commands,
        SimpleQueryBus queries,
        SimpleDomainEventPublisher events,
        LogEntryRenderer renderer) {}
}

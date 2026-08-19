package atlas.app;

import atlas.app.presence.PresenceApplication;
import atlas.app.presence.PresenceSettings;
import atlas.app.routines.RoutinesApplication;
import java.io.IOException;
import java.time.Clock;
import sharedkernel.application.logging.LogEntryRenderer;
import sharedkernel.presentation.http.WebServer;

public final class Application {

    public static final String EVENT_STREAM_PATH = "/events";
    public static final String ROUTINES_PATH = "/routines";
    public static final String ROUTINES_EVENT_STREAM_PATH = "/events/routines";

    private final PresenceApplication presence;
    private final RoutinesApplication routines;

    private WebServer server;

    private Application(PresenceApplication presence, RoutinesApplication routines) {
        this.presence = presence;
        this.routines = routines;
    }

    public static Application wire(LogEntryRenderer renderer, PresenceSettings presenceSettings, Clock clock) {
        return new Application(
            PresenceApplication.wire(renderer, presenceSettings, clock),
            RoutinesApplication.wire(renderer, presenceSettings.dataDirectory(), clock));
    }

    public Application start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", presence.router())
            .mount(EVENT_STREAM_PATH, presence.eventStream())
            .mount(ROUTINES_PATH, routines.router())
            .mount(ROUTINES_EVENT_STREAM_PATH, routines.eventStream())
            .start();

        presence.startBackgroundTasks();

        return this;
    }

    public void stop() {
        if (server != null) {
            server.close();
        }

        presence.stop();
        routines.stop();
    }

    public int port() {
        return server.port();
    }

    public PresenceApplication presence() {
        return presence;
    }

    public RoutinesApplication routines() {
        return routines;
    }
}

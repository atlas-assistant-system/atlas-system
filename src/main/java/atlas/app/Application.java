package atlas.app;

import atlas.app.presence.PresenceApplication;
import atlas.app.presence.PresenceSettings;
import java.io.IOException;
import java.time.Clock;
import sharedkernel.application.logging.LogEntryRenderer;
import sharedkernel.presentation.http.WebServer;

public final class Application {

    public static final String EVENT_STREAM_PATH = "/events";

    private final PresenceApplication presence;

    private WebServer server;

    private Application(PresenceApplication presence) {
        this.presence = presence;
    }

    public static Application wire(LogEntryRenderer renderer, PresenceSettings presenceSettings, Clock clock) {
        return new Application(PresenceApplication.wire(renderer, presenceSettings, clock));
    }

    public Application start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", presence.router())
            .mount(EVENT_STREAM_PATH, presence.eventStream())
            .start();

        presence.startBackgroundTasks();

        return this;
    }

    public void stop() {
        if (server != null) {
            server.close();
        }

        presence.stop();
    }

    public int port() {
        return server.port();
    }

    public PresenceApplication presence() {
        return presence;
    }
}

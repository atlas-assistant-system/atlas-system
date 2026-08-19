package atlas.app;

import atlas.app.appointments.AppointmentsApplication;
import atlas.app.presence.PresenceApplication;
import atlas.app.presence.PresenceSettings;
import atlas.app.routines.RoutinesApplication;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.presentation.sharedkernel.http.WebServer;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;

public final class Application {

    private final AppointmentsApplication appointments;
    private final PresenceApplication presence;
    private final RoutinesApplication routines;

    private WebServer server;

    private Application(
        AppointmentsApplication appointments, PresenceApplication presence, RoutinesApplication routines) {
        this.appointments = appointments;
        this.presence = presence;
        this.routines = routines;
    }

    public static Application wire(
        LogEntryRenderer renderer, PresenceSettings presenceSettings, Clock clock, int port) {
        var data = presenceSettings.dataDirectory();

        // ponytail: appointments sigue hablando con presence por HTTP contra este mismo proceso.
        // Ya comparten JVM, asi que sobra el salto de loopback: implementar el acceso a la sesion
        // en memoria y pasarselo en vez de la URL.
        var self = URI.create("http://localhost:" + port);

        return new Application(
            AppointmentsApplication.wire(renderer, data, clock, self),
            PresenceApplication.wire(renderer, presenceSettings, clock),
            RoutinesApplication.wire(renderer, data, clock));
    }

    public Application start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", appointments.router())
            .mount("/events", appointments.eventStream())
            .mount("/routines", routines.router())
            .mount("/events/routines", routines.eventStream())
            .mount("/authentication", presence.router())
            .mount("/challenges", presence.router())
            .mount("/interactions", presence.router())
            .mount("/gestures", presence.router())
            .mount("/profiles", presence.router())
            .mount("/sessions", presence.router())
            .mount("/attempts", presence.router())
            .mount("/events/presence", presence.eventStream())
            .start();

        appointments.startBackgroundTasks();
        presence.startBackgroundTasks();

        return this;
    }

    public void stop() {
        if (server != null) {
            server.close();
        }

        appointments.stop();
        presence.stop();
        routines.stop();
    }

    public int port() {
        return server.port();
    }

    public AppointmentsApplication appointments() {
        return appointments;
    }

    public PresenceApplication presence() {
        return presence;
    }

    public RoutinesApplication routines() {
        return routines;
    }
}

package atlas.app;

import atlas.app.appointments.AppointmentsApplication;
import atlas.app.core.CoreApplication;
import atlas.app.core.CoreSettings;
import atlas.app.economy.EconomyApplication;
import atlas.app.presence.PresenceApplication;
import atlas.app.presence.PresenceSettings;
import atlas.app.routines.RoutinesApplication;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.presentation.sharedkernel.http.WebServer;
import java.io.IOException;
import java.time.Clock;

public final class Application {

    private final CoreApplication core;
    private final AppointmentsApplication appointments;
    private final PresenceApplication presence;
    private final RoutinesApplication routines;
    private final EconomyApplication economy;

    private WebServer server;

    private Application(
        CoreApplication core,
        AppointmentsApplication appointments,
        PresenceApplication presence,
        RoutinesApplication routines,
        EconomyApplication economy) {
        this.core = core;
        this.appointments = appointments;
        this.presence = presence;
        this.routines = routines;
        this.economy = economy;
    }

    public static Application wire(LogEntryRenderer renderer, PresenceSettings presenceSettings, Clock clock) {
        var data = presenceSettings.dataDirectory();
        var presence = PresenceApplication.wire(renderer, presenceSettings, clock);

        return new Application(
            CoreApplication.wire(CoreSettings.fromEnvironment()),
            AppointmentsApplication.wire(renderer, data, clock, presence::hasActiveSession),
            presence,
            RoutinesApplication.wire(renderer, data, clock),
            EconomyApplication.wire(renderer, data, clock, presence::hasActiveSession));
    }

    public Application start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", core.router())
            .mount("/appointments", appointments.router())
            .mount("/reminders", appointments.router())
            .mount("/docs", appointments.router())
            .mount("/openapi.json", appointments.router())
            .mount("/events", appointments.eventStream())
            .mount("/routines", routines.router())
            .mount("/events/routines", routines.eventStream())
            .mount("/economy", economy.router())
            .mount("/events/economy", economy.eventStream())
            .mount("/authentication", presence.router())
            .mount("/interactions", presence.router())
            .mount("/profiles", presence.router())
            .mount("/sessions", presence.router())
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
        economy.stop();
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

    public EconomyApplication economy() {
        return economy;
    }
}

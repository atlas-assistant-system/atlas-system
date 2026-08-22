package atlas.app;

import atlas.app.appointments.AppointmentsApplication;
import atlas.app.core.CoreApplication;
import atlas.app.economy.EconomyApplication;
import atlas.app.home.HomeApplication;
import atlas.app.nutrition.NutritionApplication;
import atlas.app.presence.PresenceApplication;
import atlas.app.presence.PresenceSettings;
import atlas.app.routines.RoutinesApplication;
import atlas.app.training.TrainingApplication;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.domain.presence.events.SessionClosedEvent;
import atlas.domain.presence.events.SessionExpiredEvent;
import atlas.presentation.sharedkernel.http.WebServer;
import java.io.IOException;
import java.time.Clock;

public final class Application {

    private final CoreApplication core;
    private final HomeApplication home;
    private final AppointmentsApplication appointments;
    private final PresenceApplication presence;
    private final RoutinesApplication routines;
    private final EconomyApplication economy;
    private final NutritionApplication nutrition;
    private final TrainingApplication training;

    private WebServer server;

    private Application(
        CoreApplication core,
        HomeApplication home,
        AppointmentsApplication appointments,
        PresenceApplication presence,
        RoutinesApplication routines,
        EconomyApplication economy,
        NutritionApplication nutrition,
        TrainingApplication training) {
        this.core = core;
        this.home = home;
        this.appointments = appointments;
        this.presence = presence;
        this.routines = routines;
        this.economy = economy;
        this.nutrition = nutrition;
        this.training = training;
    }

    public static Application wire(LogEntryRenderer renderer, PresenceSettings presenceSettings, Clock clock) {
        var data = presenceSettings.dataDirectory();
        var presence = PresenceApplication.wire(renderer, presenceSettings, clock);

        var application = new Application(
            CoreApplication.wire(),
            HomeApplication.wire(
                renderer, data, clock, presenceSettings.maintenanceMode(), presence::activeProfileId),
            AppointmentsApplication.wire(renderer, data, clock, presence::hasActiveSession),
            presence,
            RoutinesApplication.wire(renderer, data, clock, presence::hasActiveSession),
            EconomyApplication.wire(renderer, data, clock, presence::hasActiveSession),
            NutritionApplication.wire(renderer, data, clock, presence::hasActiveSession),
            TrainingApplication.wire(renderer, data, clock, presence::hasActiveSession));

        presence.events().subscribe(SessionClosedEvent.class, event -> application.closeContextStreams());
        presence.events().subscribe(SessionExpiredEvent.class, event -> application.closeContextStreams());

        return application;
    }

    private void closeContextStreams() {
        appointments.hub().closeAll();
        routines.hub().closeAll();
        economy.hub().closeAll();
        nutrition.hub().closeAll();
        training.hub().closeAll();
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
            .mount("/nutrition", nutrition.router())
            .mount("/events/nutrition", nutrition.eventStream())
            .mount("/training", training.router())
            .mount("/events/training", training.eventStream())
            .mount("/home", home.router())
            .mount("/news", home.router())
            .mount("/authentication", presence.router())
            .mount("/interactions", presence.router())
            .mount("/profiles", presence.router())
            .mount("/sessions", presence.router())
            .mount("/presence", presence.router())
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
        nutrition.stop();
        training.stop();
        home.stop();
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

    public NutritionApplication nutrition() {
        return nutrition;
    }

    public TrainingApplication training() {
        return training;
    }

    public HomeApplication home() {
        return home;
    }
}

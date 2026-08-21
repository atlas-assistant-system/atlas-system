package atlas.app.home;

import atlas.application.home.commands.configurehome.ConfigureHomeCommand;
import atlas.application.home.commands.configurehome.ConfigureHomeCommandHandler;
import atlas.application.home.ports.HomeUnitOfWork;
import atlas.application.home.queries.gethomeprofile.GetHomeProfileQuery;
import atlas.application.home.queries.gethomeprofile.GetHomeProfileQueryHandler;
import atlas.application.sharedkernel.cqrs.SimpleCommandBus;
import atlas.application.sharedkernel.cqrs.SimpleQueryBus;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LoggingCommandHandler;
import atlas.application.sharedkernel.logging.LoggingQueryHandler;
import atlas.infrastructure.home.persistence.SqliteHomeUnitOfWork;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import atlas.presentation.home.handlers.HomeHandlers;
import atlas.presentation.home.web.NewsHandlers;
import atlas.presentation.sharedkernel.http.Router;
import atlas.presentation.sharedkernel.http.Routes;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;

public final class HomeApplication {

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final Router router;
    private final Connection connection;

    private HomeApplication(
        SimpleCommandBus commands,
        SimpleQueryBus queries,
        Router router,
        Connection connection) {
        this.commands = commands;
        this.queries = queries;
        this.router = router;
        this.connection = connection;
    }

    public static HomeApplication wire(
        LogEntryRenderer renderer,
        Path databaseDirectory,
        Clock clock,
        boolean maintenanceMode,
        Supplier<Optional<String>> activeProfileId) {

        var connection = SqliteConnections.openForContext(databaseDirectory, "home");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            HomeApplication.class, "/db-migrations/home", "V001__create_home_profiles.sql"));

        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        HomeUnitOfWork unitOfWork = new SqliteHomeUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(events)));

        commands.register(ConfigureHomeCommand.class, new LoggingCommandHandler<>(
            new ConfigureHomeCommandHandler(unitOfWork, clock), renderer));
        queries.register(GetHomeProfileQuery.class, new LoggingQueryHandler<>(
            new GetHomeProfileQueryHandler(unitOfWork.profiles()), renderer));

        var handlers = new HomeHandlers(commands, queries, profileId -> maintenanceMode
            || activeProfileId.get().map(profileId.value()::equals).orElse(false));

        return new HomeApplication(commands, queries, routes(handlers), connection);
    }

    public Router router() {
        return router;
    }

    public SimpleCommandBus commands() {
        return commands;
    }

    public SimpleQueryBus queries() {
        return queries;
    }

    public void stop() {
        try {
            connection.close();
        } catch (SQLException exception) {
            System.getLogger("home").log(System.Logger.Level.WARNING, "Failed to close the database.", exception);
        }
    }

    private static Router routes(HomeHandlers handlers) {
        return Router.builder()
            .mount(Routes.at("/home/profiles")
                .get("/{profileId}", handlers::get)
                .put("/{profileId}", handlers::configure))
            .mount(Routes.at("/news")
                .get("/", NewsHandlers::latest))
            .build();
    }
}

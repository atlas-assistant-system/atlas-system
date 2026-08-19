package atlas.app.routines;

import atlas.application.routines.commands.archiveroutine.ArchiveRoutineCommand;
import atlas.application.routines.commands.archiveroutine.ArchiveRoutineCommandHandler;
import atlas.application.routines.commands.changeroutinedetails.ChangeRoutineDetailsCommand;
import atlas.application.routines.commands.changeroutinedetails.ChangeRoutineDetailsCommandHandler;
import atlas.application.routines.commands.changeroutineschedule.ChangeRoutineScheduleCommand;
import atlas.application.routines.commands.changeroutineschedule.ChangeRoutineScheduleCommandHandler;
import atlas.application.routines.commands.clearday.ClearDayCommand;
import atlas.application.routines.commands.clearday.ClearDayCommandHandler;
import atlas.application.routines.commands.defineroutine.DefineRoutineCommand;
import atlas.application.routines.commands.defineroutine.DefineRoutineCommandHandler;
import atlas.application.routines.commands.deleteroutine.DeleteRoutineCommand;
import atlas.application.routines.commands.deleteroutine.DeleteRoutineCommandHandler;
import atlas.application.routines.commands.logprogress.LogProgressCommand;
import atlas.application.routines.commands.logprogress.LogProgressCommandHandler;
import atlas.application.routines.commands.unarchiveroutine.UnarchiveRoutineCommand;
import atlas.application.routines.commands.unarchiveroutine.UnarchiveRoutineCommandHandler;
import atlas.application.routines.ports.RoutineEntryIdGenerator;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.application.routines.queries.getcompliancestats.GetComplianceStatsQuery;
import atlas.application.routines.queries.getcompliancestats.GetComplianceStatsQueryHandler;
import atlas.application.routines.queries.gethistory.GetHistoryQuery;
import atlas.application.routines.queries.gethistory.GetHistoryQueryHandler;
import atlas.application.routines.queries.getperiodprogress.GetPeriodProgressQuery;
import atlas.application.routines.queries.getperiodprogress.GetPeriodProgressQueryHandler;
import atlas.application.routines.queries.getroutine.GetRoutineQuery;
import atlas.application.routines.queries.getroutine.GetRoutineQueryHandler;
import atlas.application.routines.queries.getstreak.GetStreakQuery;
import atlas.application.routines.queries.getstreak.GetStreakQueryHandler;
import atlas.application.routines.queries.gettoday.GetTodayQuery;
import atlas.application.routines.queries.gettoday.GetTodayQueryHandler;
import atlas.application.routines.queries.listroutines.ListRoutinesQuery;
import atlas.application.routines.queries.listroutines.ListRoutinesQueryHandler;
import atlas.application.sharedkernel.cqrs.SimpleCommandBus;
import atlas.application.sharedkernel.cqrs.SimpleQueryBus;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LoggingCommandHandler;
import atlas.application.sharedkernel.logging.LoggingQueryHandler;
import atlas.domain.routines.services.RoutineProgress;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.common.UuidRoutineEntryIdGenerator;
import atlas.infrastructure.routines.persistence.SqliteRoutineReadModel;
import atlas.infrastructure.routines.persistence.SqliteRoutineUnitOfWork;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import atlas.presentation.routines.handlers.RoutineHandlers;
import atlas.presentation.routines.sse.RoutineEventsBroadcaster;
import atlas.presentation.routines.web.DocsHandlers;
import atlas.presentation.routines.web.UiHandlers;
import atlas.presentation.sharedkernel.http.Router;
import atlas.presentation.sharedkernel.http.Routes;
import atlas.presentation.sharedkernel.http.SseEndpoint;
import atlas.presentation.sharedkernel.http.WebServer;
import atlas.presentation.sharedkernel.sse.SseHub;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;

public final class RoutinesApplication {

    public static final String EVENT_STREAM_PATH = "/events";

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final SimpleDomainEventPublisher events;
    private final SseHub hub;
    private final Router router;
    private final Connection connection;

    private WebServer server;

    private RoutinesApplication(
        SimpleCommandBus commands,
        SimpleQueryBus queries,
        SimpleDomainEventPublisher events,
        SseHub hub,
        Router router,
        Connection connection) {
        this.commands = commands;
        this.queries = queries;
        this.events = events;
        this.hub = hub;
        this.router = router;
        this.connection = connection;
    }

    public static RoutinesApplication wire(LogEntryRenderer renderer, Path databaseDirectory, Clock clock) {
        var connection = SqliteConnections.openForContext(databaseDirectory, "routines");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            RoutinesApplication.class, "/db-migrations/routines",
            "V001__create_routines.sql", "V002__create_routine_entries.sql",
            "V003__create_sequences.sql", "V004__index_entries_by_routine_and_day.sql"));

        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        var hub = new SseHub();

        var unitOfWork = new SqliteRoutineUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(events)),
            new SqliteSequenceGenerator(connection));
        var readModel = new SqliteRoutineReadModel(connection);
        var progress = new RoutineProgress();
        var entryIds = new UuidRoutineEntryIdGenerator();

        registerCommandHandlers(commands, unitOfWork, progress, entryIds, clock, renderer);
        registerQueryHandlers(queries, readModel, progress, clock, renderer);
        RoutineEventsBroadcaster.subscribeAll(events, hub);

        var handlers = new RoutineHandlers(commands, queries, clock);

        return new RoutinesApplication(commands, queries, events, hub, routes(handlers), connection);
    }

    public RoutinesApplication start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", router)
            .mount(EVENT_STREAM_PATH, new SseEndpoint(hub))
            .start();

        return this;
    }

    public HttpHandler eventStream() {
        return new SseEndpoint(hub);
    }

    public Router router() {
        return router;
    }

    public void stop() {
        hub.closeAll();

        if (server != null) {
            server.close();
        }

        try {
            connection.close();
        } catch (SQLException e) {
            System.getLogger("routines").log(System.Logger.Level.WARNING, "Failed to close the database.", e);
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

    private static Router routes(RoutineHandlers handlers) {
        return Router.builder()
            .mount(Routes.at("/routines")
                .post("/", handlers::define)
                .get("/", handlers::list)
                .get("/today", handlers::today)
                .get("/stats", handlers::complianceStats)
                .get("/{id}", handlers::detail)
                .delete("/{id}", handlers::delete)
                .put("/{id}/details", handlers::changeDetails)
                .put("/{id}/schedule", handlers::changeSchedule)
                .post("/{id}/archive", handlers::archive)
                .post("/{id}/unarchive", handlers::unarchive)
                .get("/{id}/progress", handlers::progress)
                .get("/{id}/history", handlers::history)
                .get("/{id}/streak", handlers::streak)
                .post("/{id}/entries", handlers::logProgress)
                .delete("/{id}/entries/{day}", handlers::clearDay))
            .mount(Routes.at("/")
                .get("/", UiHandlers::index)
                .get("/app.css", UiHandlers::styles)
                .get("/app.js", UiHandlers::script)
                .get("/docs", DocsHandlers::docs)
                .get("/openapi.json", DocsHandlers::openapi))
            .build();
    }

    private static void registerCommandHandlers(
        SimpleCommandBus commands,
        RoutineUnitOfWork unitOfWork,
        RoutineProgress progress,
        RoutineEntryIdGenerator entryIds,
        Clock clock,
        LogEntryRenderer renderer) {

        commands.register(DefineRoutineCommand.class, new LoggingCommandHandler<>(
            new DefineRoutineCommandHandler(unitOfWork, clock), renderer));
        commands.register(ChangeRoutineDetailsCommand.class, new LoggingCommandHandler<>(
            new ChangeRoutineDetailsCommandHandler(unitOfWork, clock), renderer));
        commands.register(ChangeRoutineScheduleCommand.class, new LoggingCommandHandler<>(
            new ChangeRoutineScheduleCommandHandler(unitOfWork, clock), renderer));
        commands.register(ArchiveRoutineCommand.class, new LoggingCommandHandler<>(
            new ArchiveRoutineCommandHandler(unitOfWork, clock), renderer));
        commands.register(UnarchiveRoutineCommand.class, new LoggingCommandHandler<>(
            new UnarchiveRoutineCommandHandler(unitOfWork, clock), renderer));
        commands.register(DeleteRoutineCommand.class, new LoggingCommandHandler<>(
            new DeleteRoutineCommandHandler(unitOfWork, clock), renderer));
        commands.register(LogProgressCommand.class, new LoggingCommandHandler<>(
            new LogProgressCommandHandler(unitOfWork, progress, entryIds, clock), renderer));
        commands.register(ClearDayCommand.class, new LoggingCommandHandler<>(
            new ClearDayCommandHandler(unitOfWork, clock), renderer));
    }

    private static void registerQueryHandlers(
        SimpleQueryBus queries,
        RoutineReadModel readModel,
        RoutineProgress progress,
        Clock clock,
        LogEntryRenderer renderer) {

        queries.register(GetTodayQuery.class, new LoggingQueryHandler<>(
            new GetTodayQueryHandler(readModel, progress, clock), renderer));
        queries.register(ListRoutinesQuery.class, new LoggingQueryHandler<>(
            new ListRoutinesQueryHandler(readModel), renderer));
        queries.register(GetRoutineQuery.class, new LoggingQueryHandler<>(
            new GetRoutineQueryHandler(readModel), renderer));
        queries.register(GetPeriodProgressQuery.class, new LoggingQueryHandler<>(
            new GetPeriodProgressQueryHandler(readModel, progress, clock), renderer));
        queries.register(GetHistoryQuery.class, new LoggingQueryHandler<>(
            new GetHistoryQueryHandler(readModel), renderer));
        queries.register(GetStreakQuery.class, new LoggingQueryHandler<>(
            new GetStreakQueryHandler(readModel, progress, clock), renderer));
        queries.register(GetComplianceStatsQuery.class, new LoggingQueryHandler<>(
            new GetComplianceStatsQueryHandler(readModel, progress, clock), renderer));
    }
}

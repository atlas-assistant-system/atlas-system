package atlas.app.nutrition;

import atlas.application.nutrition.commands.adjustplan.AdjustPlanCommand;
import atlas.application.nutrition.commands.adjustplan.AdjustPlanCommandHandler;
import atlas.application.nutrition.commands.archiveplan.ArchivePlanCommand;
import atlas.application.nutrition.commands.archiveplan.ArchivePlanCommandHandler;
import atlas.application.nutrition.commands.correctintake.CorrectIntakeCommand;
import atlas.application.nutrition.commands.correctintake.CorrectIntakeCommandHandler;
import atlas.application.nutrition.commands.correctweighin.CorrectWeighInCommand;
import atlas.application.nutrition.commands.correctweighin.CorrectWeighInCommandHandler;
import atlas.application.nutrition.commands.defineplan.DefinePlanCommand;
import atlas.application.nutrition.commands.defineplan.DefinePlanCommandHandler;
import atlas.application.nutrition.commands.deleteintake.DeleteIntakeCommand;
import atlas.application.nutrition.commands.deleteintake.DeleteIntakeCommandHandler;
import atlas.application.nutrition.commands.deleteweighin.DeleteWeighInCommand;
import atlas.application.nutrition.commands.deleteweighin.DeleteWeighInCommandHandler;
import atlas.application.nutrition.commands.recordintake.RecordIntakeCommand;
import atlas.application.nutrition.commands.recordintake.RecordIntakeCommandHandler;
import atlas.application.nutrition.commands.recordweighin.RecordWeighInCommand;
import atlas.application.nutrition.commands.recordweighin.RecordWeighInCommandHandler;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.nutrition.ports.WeighInReadModel;
import atlas.application.nutrition.queries.getactiveplan.GetActivePlanQuery;
import atlas.application.nutrition.queries.getactiveplan.GetActivePlanQueryHandler;
import atlas.application.nutrition.queries.getday.GetDayQuery;
import atlas.application.nutrition.queries.getday.GetDayQueryHandler;
import atlas.application.nutrition.queries.getintake.GetIntakeQuery;
import atlas.application.nutrition.queries.getintake.GetIntakeQueryHandler;
import atlas.application.nutrition.queries.getprogress.GetProgressQuery;
import atlas.application.nutrition.queries.getprogress.GetProgressQueryHandler;
import atlas.application.nutrition.queries.listdays.ListDaysQuery;
import atlas.application.nutrition.queries.listdays.ListDaysQueryHandler;
import atlas.application.nutrition.queries.listintakes.ListIntakesQuery;
import atlas.application.nutrition.queries.listintakes.ListIntakesQueryHandler;
import atlas.application.nutrition.queries.listweighins.ListWeighInsQuery;
import atlas.application.nutrition.queries.listweighins.ListWeighInsQueryHandler;
import atlas.application.sharedkernel.cqrs.SimpleCommandBus;
import atlas.application.sharedkernel.cqrs.SimpleQueryBus;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LoggingCommandHandler;
import atlas.application.sharedkernel.logging.LoggingQueryHandler;
import atlas.domain.nutrition.services.PlanProgress;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.nutrition.persistence.SqliteIntakeReadModel;
import atlas.infrastructure.nutrition.persistence.SqliteNutritionUnitOfWork;
import atlas.infrastructure.nutrition.persistence.SqlitePlanReadModel;
import atlas.infrastructure.nutrition.persistence.SqliteWeighInReadModel;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import atlas.presentation.common.web.SessionGuard;
import atlas.presentation.nutrition.handlers.NutritionHandlers;
import atlas.presentation.nutrition.sse.NutritionEventsBroadcaster;
import atlas.presentation.nutrition.web.DocsHandlers;
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
import java.util.function.BooleanSupplier;

public final class NutritionApplication {

    public static final String EVENT_STREAM_PATH = "/events/nutrition";

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final SimpleDomainEventPublisher events;
    private final SseHub hub;
    private final Router router;
    private final SessionGuard sessions;
    private final Connection connection;

    private WebServer server;

    private NutritionApplication(
        SimpleCommandBus commands,
        SimpleQueryBus queries,
        SimpleDomainEventPublisher events,
        SseHub hub,
        Router router,
        SessionGuard sessions,
        Connection connection) {
        this.commands = commands;
        this.queries = queries;
        this.events = events;
        this.hub = hub;
        this.router = router;
        this.sessions = sessions;
        this.connection = connection;
    }

    public static NutritionApplication wire(LogEntryRenderer renderer, Path databaseDirectory, Clock clock) {
        return wire(renderer, databaseDirectory, clock, () -> true);
    }

    public static NutritionApplication wire(
        LogEntryRenderer renderer, Path databaseDirectory, Clock clock, BooleanSupplier hasActiveSession) {
        var connection = SqliteConnections.openForContext(databaseDirectory, "nutrition");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            NutritionApplication.class, "/db-migrations/nutrition",
            "V001__create_plans.sql", "V002__index_single_active_plan.sql",
            "V003__create_intakes.sql", "V004__index_intakes_by_date.sql",
            "V005__create_sequences.sql", "V006__create_weigh_ins.sql"));

        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        var hub = new SseHub();

        var unitOfWork = new SqliteNutritionUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(events)),
            new SqliteSequenceGenerator(connection));
        var intakes = new SqliteIntakeReadModel(connection);
        var plans = new SqlitePlanReadModel(connection);
        var weighIns = new SqliteWeighInReadModel(connection);

        registerCommandHandlers(commands, unitOfWork, clock, renderer);
        registerQueryHandlers(queries, intakes, plans, weighIns, clock, renderer);
        NutritionEventsBroadcaster.subscribeAll(events, hub);

        var handlers = new NutritionHandlers(commands, queries);
        var sessions = new SessionGuard(hasActiveSession);

        return new NutritionApplication(
            commands, queries, events, hub, routes(handlers, sessions), sessions, connection);
    }

    public NutritionApplication start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", router)
            .mount(EVENT_STREAM_PATH, sessions.protect(new SseEndpoint(hub)))
            .start();

        return this;
    }

    public HttpHandler eventStream() {
        return sessions.protect(new SseEndpoint(hub));
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
            System.getLogger("nutrition").log(System.Logger.Level.WARNING, "Failed to close the database.", e);
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

    private static Router routes(NutritionHandlers handlers, SessionGuard sessions) {
        return Router.builder()
            .mount(Routes.at("/nutrition")
                .post("/plan", sessions.protect(handlers::definePlan))
                .get("/plan", sessions.protect(handlers::activePlan))
                .put("/plan", sessions.protect(handlers::adjustPlan))
                .delete("/plan", sessions.protect(handlers::archivePlan))
                .post("/intakes", sessions.protect(handlers::recordIntake))
                .get("/intakes", sessions.protect(handlers::listIntakes))
                .get("/today", sessions.protect(handlers::today))
                .get("/days", sessions.protect(handlers::days))
                .post("/weigh-ins", sessions.protect(handlers::recordWeighIn))
                .get("/weigh-ins", sessions.protect(handlers::weighIns))
                .get("/progress", sessions.protect(handlers::progress))
                .get("/docs", sessions.protect(DocsHandlers::docs))
                .get("/openapi.json", sessions.protect(DocsHandlers::openapi))
                .get("/intakes/{id}", sessions.protect(handlers::intakeDetail))
                .put("/intakes/{id}", sessions.protect(handlers::correctIntake))
                .delete("/intakes/{id}", sessions.protect(handlers::deleteIntake))
                .get("/days/{date}", sessions.protect(handlers::day))
                .put("/weigh-ins/{id}", sessions.protect(handlers::correctWeighIn))
                .delete("/weigh-ins/{id}", sessions.protect(handlers::deleteWeighIn)))
            .build();
    }

    private static void registerCommandHandlers(
        SimpleCommandBus commands, NutritionUnitOfWork unitOfWork, Clock clock, LogEntryRenderer renderer) {

        commands.register(DefinePlanCommand.class, new LoggingCommandHandler<>(
            new DefinePlanCommandHandler(unitOfWork, clock), renderer));
        commands.register(AdjustPlanCommand.class, new LoggingCommandHandler<>(
            new AdjustPlanCommandHandler(unitOfWork, clock), renderer));
        commands.register(ArchivePlanCommand.class, new LoggingCommandHandler<>(
            new ArchivePlanCommandHandler(unitOfWork, clock), renderer));
        commands.register(RecordIntakeCommand.class, new LoggingCommandHandler<>(
            new RecordIntakeCommandHandler(unitOfWork, clock), renderer));
        commands.register(CorrectIntakeCommand.class, new LoggingCommandHandler<>(
            new CorrectIntakeCommandHandler(unitOfWork, clock), renderer));
        commands.register(DeleteIntakeCommand.class, new LoggingCommandHandler<>(
            new DeleteIntakeCommandHandler(unitOfWork, clock), renderer));
        commands.register(RecordWeighInCommand.class, new LoggingCommandHandler<>(
            new RecordWeighInCommandHandler(unitOfWork, clock), renderer));
        commands.register(CorrectWeighInCommand.class, new LoggingCommandHandler<>(
            new CorrectWeighInCommandHandler(unitOfWork, clock), renderer));
        commands.register(DeleteWeighInCommand.class, new LoggingCommandHandler<>(
            new DeleteWeighInCommandHandler(unitOfWork, clock), renderer));
    }

    private static void registerQueryHandlers(
        SimpleQueryBus queries,
        IntakeReadModel intakes,
        PlanReadModel plans,
        WeighInReadModel weighIns,
        Clock clock,
        LogEntryRenderer renderer) {

        queries.register(GetActivePlanQuery.class, new LoggingQueryHandler<>(
            new GetActivePlanQueryHandler(plans), renderer));
        queries.register(GetDayQuery.class, new LoggingQueryHandler<>(
            new GetDayQueryHandler(intakes, plans, clock), renderer));
        queries.register(GetIntakeQuery.class, new LoggingQueryHandler<>(
            new GetIntakeQueryHandler(intakes), renderer));
        queries.register(ListIntakesQuery.class, new LoggingQueryHandler<>(
            new ListIntakesQueryHandler(intakes, clock), renderer));
        queries.register(ListDaysQuery.class, new LoggingQueryHandler<>(
            new ListDaysQueryHandler(intakes, plans, clock), renderer));
        queries.register(ListWeighInsQuery.class, new LoggingQueryHandler<>(
            new ListWeighInsQueryHandler(weighIns, clock), renderer));
        queries.register(GetProgressQuery.class, new LoggingQueryHandler<>(
            new GetProgressQueryHandler(plans, weighIns, new PlanProgress(), clock), renderer));
    }
}

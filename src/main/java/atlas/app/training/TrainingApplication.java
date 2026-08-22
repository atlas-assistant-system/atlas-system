package atlas.app.training;

import atlas.application.sharedkernel.cqrs.SimpleCommandBus;
import atlas.application.sharedkernel.cqrs.SimpleQueryBus;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LoggingCommandHandler;
import atlas.application.sharedkernel.logging.LoggingQueryHandler;
import atlas.application.training.commands.addset.AddSetCommand;
import atlas.application.training.commands.addset.AddSetCommandHandler;
import atlas.application.training.commands.archiveexercise.ArchiveExerciseCommand;
import atlas.application.training.commands.archiveexercise.ArchiveExerciseCommandHandler;
import atlas.application.training.commands.archiveworkout.ArchiveWorkoutCommand;
import atlas.application.training.commands.archiveworkout.ArchiveWorkoutCommandHandler;
import atlas.application.training.commands.defineexercise.DefineExerciseCommand;
import atlas.application.training.commands.defineexercise.DefineExerciseCommandHandler;
import atlas.application.training.commands.defineworkout.DefineWorkoutCommand;
import atlas.application.training.commands.defineworkout.DefineWorkoutCommandHandler;
import atlas.application.training.commands.discardworkoutlog.DiscardWorkoutLogCommand;
import atlas.application.training.commands.discardworkoutlog.DiscardWorkoutLogCommandHandler;
import atlas.application.training.commands.recordset.RecordSetCommand;
import atlas.application.training.commands.recordset.RecordSetCommandHandler;
import atlas.application.training.commands.removeset.RemoveSetCommand;
import atlas.application.training.commands.removeset.RemoveSetCommandHandler;
import atlas.application.training.commands.renameexercise.RenameExerciseCommand;
import atlas.application.training.commands.renameexercise.RenameExerciseCommandHandler;
import atlas.application.training.commands.renameworkout.RenameWorkoutCommand;
import atlas.application.training.commands.renameworkout.RenameWorkoutCommandHandler;
import atlas.application.training.commands.scheduleworkout.ScheduleWorkoutCommand;
import atlas.application.training.commands.scheduleworkout.ScheduleWorkoutCommandHandler;
import atlas.application.training.commands.setworkoutplan.SetWorkoutPlanCommand;
import atlas.application.training.commands.setworkoutplan.SetWorkoutPlanCommandHandler;
import atlas.application.training.commands.startworkoutlog.StartWorkoutLogCommand;
import atlas.application.training.commands.startworkoutlog.StartWorkoutLogCommandHandler;
import atlas.application.training.commands.unarchiveexercise.UnarchiveExerciseCommand;
import atlas.application.training.commands.unarchiveexercise.UnarchiveExerciseCommandHandler;
import atlas.application.training.ports.ExerciseReadModel;
import atlas.application.training.ports.IdGenerator;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.application.training.ports.WorkoutLogReadModel;
import atlas.application.training.ports.WorkoutReadModel;
import atlas.application.training.queries.gettodayworkout.GetTodayWorkoutQuery;
import atlas.application.training.queries.gettodayworkout.GetTodayWorkoutQueryHandler;
import atlas.application.training.queries.getworkout.GetWorkoutQuery;
import atlas.application.training.queries.getworkout.GetWorkoutQueryHandler;
import atlas.application.training.queries.getworkoutlog.GetWorkoutLogQuery;
import atlas.application.training.queries.getworkoutlog.GetWorkoutLogQueryHandler;
import atlas.application.training.queries.listexercises.ListExercisesQuery;
import atlas.application.training.queries.listexercises.ListExercisesQueryHandler;
import atlas.application.training.queries.listworkoutlogs.ListWorkoutLogsQuery;
import atlas.application.training.queries.listworkoutlogs.ListWorkoutLogsQueryHandler;
import atlas.application.training.queries.listworkouts.ListWorkoutsQuery;
import atlas.application.training.queries.listworkouts.ListWorkoutsQueryHandler;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import atlas.infrastructure.training.persistence.SqliteExerciseReadModel;
import atlas.infrastructure.training.persistence.SqliteTrainingUnitOfWork;
import atlas.infrastructure.training.persistence.SqliteWorkoutLogReadModel;
import atlas.infrastructure.training.persistence.SqliteWorkoutReadModel;
import atlas.presentation.common.web.SessionGuard;
import atlas.presentation.sharedkernel.http.Router;
import atlas.presentation.sharedkernel.http.Routes;
import atlas.presentation.sharedkernel.http.SseEndpoint;
import atlas.presentation.sharedkernel.http.WebServer;
import atlas.presentation.sharedkernel.sse.SseHub;
import atlas.presentation.training.handlers.TrainingHandlers;
import atlas.presentation.training.sse.TrainingEventsBroadcaster;
import atlas.presentation.training.web.DocsHandlers;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class TrainingApplication {

    public static final String EVENT_STREAM_PATH = "/events/training";

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final SimpleDomainEventPublisher events;
    private final SseHub hub;
    private final Router router;
    private final SessionGuard sessions;
    private final Connection connection;

    private WebServer server;

    private TrainingApplication(
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

    public static TrainingApplication wire(
        LogEntryRenderer renderer, Path databaseDirectory, Clock clock) {

        return wire(renderer, databaseDirectory, clock, () -> true);
    }

    public static TrainingApplication wire(
        LogEntryRenderer renderer,
        Path databaseDirectory,
        Clock clock,
        BooleanSupplier hasActiveSession) {

        var connection = SqliteConnections.openForContext(databaseDirectory, "training");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            TrainingApplication.class, "/db-migrations/training",
            "V001__create_exercises.sql", "V002__index_exercises_by_name.sql",
            "V003__create_workouts.sql", "V004__create_workout_exercises.sql",
            "V005__create_workout_logs.sql", "V006__create_set_logs.sql",
            "V007__index_set_logs_by_exercise.sql", "V008__index_workout_logs_by_date.sql",
            "V009__create_sequences.sql", "V010__add_workout_days.sql"));

        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        var hub = new SseHub();
        var sequences = new SqliteSequenceGenerator(connection);

        var unitOfWork = new SqliteTrainingUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(events)), sequences);
        var exercises = new SqliteExerciseReadModel(connection);
        var workouts = new SqliteWorkoutReadModel(connection, sequences);
        var logs = new SqliteWorkoutLogReadModel(connection);

        registerCommandHandlers(commands, unitOfWork, clock, UUID::randomUUID, renderer);
        registerQueryHandlers(queries, exercises, workouts, logs, clock, renderer);
        TrainingEventsBroadcaster.subscribeAll(events, hub);

        var handlers = new TrainingHandlers(commands, queries);
        var sessions = new SessionGuard(hasActiveSession);

        return new TrainingApplication(
            commands, queries, events, hub, routes(handlers, sessions), sessions, connection);
    }

    public TrainingApplication start(int port) throws IOException {
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
            System.getLogger("training")
                .log(System.Logger.Level.WARNING, "Failed to close the database.", e);
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

    private static Router routes(TrainingHandlers handlers, SessionGuard sessions) {
        return Router.builder()
            .mount(Routes.at("/training")
                .post("/exercises", sessions.protect(handlers::defineExercise))
                .get("/exercises", sessions.protect(handlers::listExercises))
                .post("/workouts", sessions.protect(handlers::defineWorkout))
                .get("/workouts", sessions.protect(handlers::listWorkouts))
                .post("/logs", sessions.protect(handlers::startWorkoutLog))
                .get("/logs", sessions.protect(handlers::listWorkoutLogs))
                .get("/today", sessions.protect(handlers::today))
                .get("/docs", sessions.protect(DocsHandlers::docs))
                .get("/openapi.json", sessions.protect(DocsHandlers::openapi))
                .put("/exercises/{id}", sessions.protect(handlers::renameExercise))
                .delete("/exercises/{id}", sessions.protect(handlers::archiveExercise))
                .post("/exercises/{id}/restore", sessions.protect(handlers::restoreExercise))
                .get("/workouts/{id}", sessions.protect(handlers::workoutDetail))
                .put("/workouts/{id}", sessions.protect(handlers::renameWorkout))
                .delete("/workouts/{id}", sessions.protect(handlers::archiveWorkout))
                .put("/workouts/{id}/plan", sessions.protect(handlers::setWorkoutPlan))
                .put("/workouts/{id}/schedule", sessions.protect(handlers::scheduleWorkout))
                .get("/logs/{id}", sessions.protect(handlers::workoutLogDetail))
                .delete("/logs/{id}", sessions.protect(handlers::discardWorkoutLog))
                .post("/logs/{id}/sets", sessions.protect(handlers::addSet))
                .put("/logs/{id}/sets/{setId}", sessions.protect(handlers::recordSet))
                .delete("/logs/{id}/sets/{setId}", sessions.protect(handlers::removeSet)))
            .build();
    }

    private static void registerCommandHandlers(
        SimpleCommandBus commands,
        TrainingUnitOfWork unitOfWork,
        Clock clock,
        IdGenerator ids,
        LogEntryRenderer renderer) {

        commands.register(DefineExerciseCommand.class, new LoggingCommandHandler<>(
            new DefineExerciseCommandHandler(unitOfWork, clock), renderer));
        commands.register(RenameExerciseCommand.class, new LoggingCommandHandler<>(
            new RenameExerciseCommandHandler(unitOfWork, clock), renderer));
        commands.register(ArchiveExerciseCommand.class, new LoggingCommandHandler<>(
            new ArchiveExerciseCommandHandler(unitOfWork, clock), renderer));
        commands.register(UnarchiveExerciseCommand.class, new LoggingCommandHandler<>(
            new UnarchiveExerciseCommandHandler(unitOfWork, clock), renderer));

        commands.register(DefineWorkoutCommand.class, new LoggingCommandHandler<>(
            new DefineWorkoutCommandHandler(unitOfWork, clock), renderer));
        commands.register(RenameWorkoutCommand.class, new LoggingCommandHandler<>(
            new RenameWorkoutCommandHandler(unitOfWork, clock), renderer));
        commands.register(SetWorkoutPlanCommand.class, new LoggingCommandHandler<>(
            new SetWorkoutPlanCommandHandler(unitOfWork, clock, ids), renderer));
        commands.register(ScheduleWorkoutCommand.class, new LoggingCommandHandler<>(
            new ScheduleWorkoutCommandHandler(unitOfWork, clock), renderer));
        commands.register(ArchiveWorkoutCommand.class, new LoggingCommandHandler<>(
            new ArchiveWorkoutCommandHandler(unitOfWork, clock), renderer));

        commands.register(StartWorkoutLogCommand.class, new LoggingCommandHandler<>(
            new StartWorkoutLogCommandHandler(unitOfWork, clock, ids), renderer));
        commands.register(RecordSetCommand.class, new LoggingCommandHandler<>(
            new RecordSetCommandHandler(unitOfWork, clock), renderer));
        commands.register(AddSetCommand.class, new LoggingCommandHandler<>(
            new AddSetCommandHandler(unitOfWork, clock, ids), renderer));
        commands.register(RemoveSetCommand.class, new LoggingCommandHandler<>(
            new RemoveSetCommandHandler(unitOfWork, clock), renderer));
        commands.register(DiscardWorkoutLogCommand.class, new LoggingCommandHandler<>(
            new DiscardWorkoutLogCommandHandler(unitOfWork, clock), renderer));
    }

    private static void registerQueryHandlers(
        SimpleQueryBus queries,
        ExerciseReadModel exercises,
        WorkoutReadModel workouts,
        WorkoutLogReadModel logs,
        Clock clock,
        LogEntryRenderer renderer) {

        queries.register(ListExercisesQuery.class, new LoggingQueryHandler<>(
            new ListExercisesQueryHandler(exercises), renderer));
        queries.register(ListWorkoutsQuery.class, new LoggingQueryHandler<>(
            new ListWorkoutsQueryHandler(workouts), renderer));
        queries.register(GetWorkoutQuery.class, new LoggingQueryHandler<>(
            new GetWorkoutQueryHandler(workouts), renderer));
        queries.register(ListWorkoutLogsQuery.class, new LoggingQueryHandler<>(
            new ListWorkoutLogsQueryHandler(logs, clock), renderer));
        queries.register(GetWorkoutLogQuery.class, new LoggingQueryHandler<>(
            new GetWorkoutLogQueryHandler(logs), renderer));
        queries.register(GetTodayWorkoutQuery.class, new LoggingQueryHandler<>(
            new GetTodayWorkoutQueryHandler(logs, clock), renderer));
    }
}

package atlas.app.economy;

import atlas.application.economy.commands.abandonsavingsgoal.AbandonSavingsGoalCommand;
import atlas.application.economy.commands.abandonsavingsgoal.AbandonSavingsGoalCommandHandler;
import atlas.application.economy.commands.changebudgetlimit.ChangeBudgetLimitCommand;
import atlas.application.economy.commands.changebudgetlimit.ChangeBudgetLimitCommandHandler;
import atlas.application.economy.commands.changesavingsgoal.ChangeSavingsGoalCommand;
import atlas.application.economy.commands.changesavingsgoal.ChangeSavingsGoalCommandHandler;
import atlas.application.economy.commands.correctmovement.CorrectMovementCommand;
import atlas.application.economy.commands.correctmovement.CorrectMovementCommandHandler;
import atlas.application.economy.commands.definebudget.DefineBudgetCommand;
import atlas.application.economy.commands.definebudget.DefineBudgetCommandHandler;
import atlas.application.economy.commands.deletemovement.DeleteMovementCommand;
import atlas.application.economy.commands.deletemovement.DeleteMovementCommandHandler;
import atlas.application.economy.commands.recategorizemovement.RecategorizeMovementCommand;
import atlas.application.economy.commands.recategorizemovement.RecategorizeMovementCommandHandler;
import atlas.application.economy.commands.recordmovement.RecordMovementCommand;
import atlas.application.economy.commands.recordmovement.RecordMovementCommandHandler;
import atlas.application.economy.commands.removebudget.RemoveBudgetCommand;
import atlas.application.economy.commands.removebudget.RemoveBudgetCommandHandler;
import atlas.application.economy.commands.setsavingsgoal.SetSavingsGoalCommand;
import atlas.application.economy.commands.setsavingsgoal.SetSavingsGoalCommandHandler;
import atlas.application.economy.ports.BudgetReadModel;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.ports.SavingsGoalReadModel;
import atlas.application.economy.queries.getbalance.GetBalanceQuery;
import atlas.application.economy.queries.getbalance.GetBalanceQueryHandler;
import atlas.application.economy.queries.getbreakdown.GetBreakdownQuery;
import atlas.application.economy.queries.getbreakdown.GetBreakdownQueryHandler;
import atlas.application.economy.queries.getmovement.GetMovementQuery;
import atlas.application.economy.queries.getmovement.GetMovementQueryHandler;
import atlas.application.economy.queries.listbudgets.ListBudgetsQuery;
import atlas.application.economy.queries.listbudgets.ListBudgetsQueryHandler;
import atlas.application.economy.queries.listmovements.ListMovementsQuery;
import atlas.application.economy.queries.listmovements.ListMovementsQueryHandler;
import atlas.application.economy.queries.listsavingsgoals.ListSavingsGoalsQuery;
import atlas.application.economy.queries.listsavingsgoals.ListSavingsGoalsQueryHandler;
import atlas.application.sharedkernel.cqrs.SimpleCommandBus;
import atlas.application.sharedkernel.cqrs.SimpleQueryBus;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LoggingCommandHandler;
import atlas.application.sharedkernel.logging.LoggingQueryHandler;
import atlas.domain.economy.services.BudgetPace;
import atlas.domain.economy.services.SavingsProjection;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.economy.persistence.SqliteBudgetReadModel;
import atlas.infrastructure.economy.persistence.SqliteEconomyUnitOfWork;
import atlas.infrastructure.economy.persistence.SqliteMovementReadModel;
import atlas.infrastructure.economy.persistence.SqliteSavingsGoalReadModel;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import atlas.presentation.common.web.SessionGuard;
import atlas.presentation.economy.handlers.EconomyHandlers;
import atlas.presentation.economy.sse.EconomyEventsBroadcaster;
import atlas.presentation.economy.web.DocsHandlers;
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

public final class EconomyApplication {

    public static final String EVENT_STREAM_PATH = "/events";

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final SimpleDomainEventPublisher events;
    private final SseHub hub;
    private final Router router;
    private final Connection connection;

    private WebServer server;

    private EconomyApplication(
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

    public static EconomyApplication wire(LogEntryRenderer renderer, Path databaseDirectory, Clock clock) {
        return wire(renderer, databaseDirectory, clock, () -> true);
    }

    public static EconomyApplication wire(
        LogEntryRenderer renderer, Path databaseDirectory, Clock clock, BooleanSupplier hasActiveSession) {
        var connection = SqliteConnections.openForContext(databaseDirectory, "economy");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            EconomyApplication.class, "/db-migrations/economy",
            "V001__create_movements.sql", "V002__create_sequences.sql",
            "V003__index_movements_by_date.sql", "V004__create_budgets.sql", "V005__create_savings_goals.sql"));

        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        var hub = new SseHub();

        var unitOfWork = new SqliteEconomyUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(events)),
            new SqliteSequenceGenerator(connection));
        var readModel = new SqliteMovementReadModel(connection);
        var budgets = new SqliteBudgetReadModel(connection);
        var goals = new SqliteSavingsGoalReadModel(connection);

        registerCommandHandlers(commands, unitOfWork, clock, renderer);
        registerQueryHandlers(queries, readModel, budgets, goals, clock, renderer);
        EconomyEventsBroadcaster.subscribeAll(events, hub);

        var handlers = new EconomyHandlers(commands, queries);
        var sessions = new SessionGuard(hasActiveSession);

        return new EconomyApplication(commands, queries, events, hub, routes(handlers, sessions), connection);
    }

    public EconomyApplication start(int port) throws IOException {
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
            System.getLogger("economy").log(System.Logger.Level.WARNING, "Failed to close the database.", e);
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

    private static Router routes(EconomyHandlers handlers, SessionGuard sessions) {
        return Router.builder()
            .mount(Routes.at("/economy")
                .post("/movements", sessions.protect(handlers::record))
                .get("/movements", sessions.protect(handlers::list))
                .get("/balance", sessions.protect(handlers::balance))
                .get("/breakdown", sessions.protect(handlers::breakdown))
                .post("/budgets", sessions.protect(handlers::defineBudget))
                .get("/budgets", sessions.protect(handlers::budgets))
                .put("/budgets/{id}", sessions.protect(handlers::changeBudgetLimit))
                .delete("/budgets/{id}", sessions.protect(handlers::removeBudget))
                .post("/goals", sessions.protect(handlers::setGoal))
                .get("/goals", sessions.protect(handlers::goals))
                .put("/goals/{id}", sessions.protect(handlers::changeGoal))
                .delete("/goals/{id}", sessions.protect(handlers::abandonGoal))
                .get("/docs", sessions.protect(DocsHandlers::docs))
                .get("/openapi.json", sessions.protect(DocsHandlers::openapi))
                .get("/movements/{id}", sessions.protect(handlers::detail))
                .put("/movements/{id}", sessions.protect(handlers::correct))
                .delete("/movements/{id}", sessions.protect(handlers::delete))
                .put("/movements/{id}/category", sessions.protect(handlers::recategorize)))
            .build();
    }

    private static void registerCommandHandlers(
        SimpleCommandBus commands, EconomyUnitOfWork unitOfWork, Clock clock, LogEntryRenderer renderer) {

        commands.register(RecordMovementCommand.class, new LoggingCommandHandler<>(
            new RecordMovementCommandHandler(unitOfWork, clock), renderer));
        commands.register(CorrectMovementCommand.class, new LoggingCommandHandler<>(
            new CorrectMovementCommandHandler(unitOfWork, clock), renderer));
        commands.register(RecategorizeMovementCommand.class, new LoggingCommandHandler<>(
            new RecategorizeMovementCommandHandler(unitOfWork, clock), renderer));
        commands.register(DeleteMovementCommand.class, new LoggingCommandHandler<>(
            new DeleteMovementCommandHandler(unitOfWork, clock), renderer));
        commands.register(DefineBudgetCommand.class, new LoggingCommandHandler<>(
            new DefineBudgetCommandHandler(unitOfWork, clock), renderer));
        commands.register(ChangeBudgetLimitCommand.class, new LoggingCommandHandler<>(
            new ChangeBudgetLimitCommandHandler(unitOfWork, clock), renderer));
        commands.register(RemoveBudgetCommand.class, new LoggingCommandHandler<>(
            new RemoveBudgetCommandHandler(unitOfWork, clock), renderer));
        commands.register(SetSavingsGoalCommand.class, new LoggingCommandHandler<>(
            new SetSavingsGoalCommandHandler(unitOfWork, clock), renderer));
        commands.register(ChangeSavingsGoalCommand.class, new LoggingCommandHandler<>(
            new ChangeSavingsGoalCommandHandler(unitOfWork, clock), renderer));
        commands.register(AbandonSavingsGoalCommand.class, new LoggingCommandHandler<>(
            new AbandonSavingsGoalCommandHandler(unitOfWork, clock), renderer));
    }

    private static void registerQueryHandlers(
        SimpleQueryBus queries,
        MovementReadModel readModel,
        BudgetReadModel budgets,
        SavingsGoalReadModel goals,
        Clock clock,
        LogEntryRenderer renderer) {

        queries.register(GetMovementQuery.class, new LoggingQueryHandler<>(
            new GetMovementQueryHandler(readModel), renderer));
        queries.register(ListMovementsQuery.class, new LoggingQueryHandler<>(
            new ListMovementsQueryHandler(readModel), renderer));
        queries.register(GetBalanceQuery.class, new LoggingQueryHandler<>(
            new GetBalanceQueryHandler(readModel, clock), renderer));
        queries.register(GetBreakdownQuery.class, new LoggingQueryHandler<>(
            new GetBreakdownQueryHandler(readModel, clock), renderer));
        queries.register(ListBudgetsQuery.class, new LoggingQueryHandler<>(
            new ListBudgetsQueryHandler(budgets, readModel, new BudgetPace(), clock), renderer));
        queries.register(ListSavingsGoalsQuery.class, new LoggingQueryHandler<>(
            new ListSavingsGoalsQueryHandler(goals, readModel, new SavingsProjection(), clock), renderer));
    }
}

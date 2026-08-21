package atlas.app.presence;

import atlas.application.presence.commands.addfacetemplate.AddFaceTemplateCommand;
import atlas.application.presence.commands.addfacetemplate.AddFaceTemplateCommandHandler;
import atlas.application.presence.commands.beginauthentication.BeginAuthenticationCommand;
import atlas.application.presence.commands.beginauthentication.BeginAuthenticationCommandHandler;
import atlas.application.presence.commands.closesession.CloseSessionCommand;
import atlas.application.presence.commands.closesession.CloseSessionCommandHandler;
import atlas.application.presence.commands.completeauthentication.CompleteAuthenticationCommand;
import atlas.application.presence.commands.completeauthentication.CompleteAuthenticationCommandHandler;
import atlas.application.presence.commands.deleteprofile.DeleteProfileCommand;
import atlas.application.presence.commands.deleteprofile.DeleteProfileCommandHandler;
import atlas.application.presence.commands.enrollprofile.EnrollProfileCommand;
import atlas.application.presence.commands.enrollprofile.EnrollProfileCommandHandler;
import atlas.application.presence.commands.expirestalesessions.ExpireStaleSessionsCommand;
import atlas.application.presence.commands.expirestalesessions.ExpireStaleSessionsCommandHandler;
import atlas.application.presence.commands.refreshsession.RefreshSessionCommand;
import atlas.application.presence.commands.refreshsession.RefreshSessionCommandHandler;
import atlas.application.presence.commands.removefacetemplate.RemoveFaceTemplateCommand;
import atlas.application.presence.commands.removefacetemplate.RemoveFaceTemplateCommandHandler;
import atlas.application.presence.ports.AuthenticationAttemptReadModel;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.presence.queries.getactivesession.GetActiveSessionQuery;
import atlas.application.presence.queries.getactivesession.GetActiveSessionQueryHandler;
import atlas.application.presence.queries.getauthenticationstate.GetAuthenticationStateQuery;
import atlas.application.presence.queries.getauthenticationstate.GetAuthenticationStateQueryHandler;
import atlas.application.presence.queries.getprofile.GetProfileQuery;
import atlas.application.presence.queries.getprofile.GetProfileQueryHandler;
import atlas.application.presence.queries.listauthenticationattempts.ListAuthenticationAttemptsQuery;
import atlas.application.presence.queries.listauthenticationattempts.ListAuthenticationAttemptsQueryHandler;
import atlas.application.presence.queries.listprofiles.ListProfilesQuery;
import atlas.application.presence.queries.listprofiles.ListProfilesQueryHandler;
import atlas.application.sharedkernel.cqrs.SimpleCommandBus;
import atlas.application.sharedkernel.cqrs.SimpleQueryBus;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LoggingCommandHandler;
import atlas.application.sharedkernel.logging.LoggingQueryHandler;
import atlas.domain.presence.services.FaceMatcher;
import atlas.domain.presence.services.LivenessPolicy;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.common.UuidFaceTemplateIdGenerator;
import atlas.infrastructure.presence.memory.InMemoryLivenessChallengeRepository;
import atlas.infrastructure.presence.persistence.AuthenticationAuditDelivery;
import atlas.infrastructure.presence.persistence.PresenceMigrations;
import atlas.infrastructure.presence.persistence.SqliteAuthenticationAttemptReadModel;
import atlas.infrastructure.presence.persistence.SqlitePresenceUnitOfWork;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import atlas.presentation.common.web.ErrorMessagesHandler;
import atlas.presentation.presence.handlers.InteractionHandlers;
import atlas.presentation.presence.handlers.PresenceHandlers;
import atlas.presentation.presence.sse.PresenceEventsBroadcaster;
import atlas.presentation.presence.sse.PresencePoller;
import atlas.presentation.presence.web.DocsHandlers;
import atlas.presentation.presence.web.UiHandlers;
import atlas.presentation.sharedkernel.http.Router;
import atlas.presentation.sharedkernel.http.Routes;
import atlas.presentation.sharedkernel.http.SseEndpoint;
import atlas.presentation.sharedkernel.http.WebServer;
import atlas.presentation.sharedkernel.sse.SseHub;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;

public final class PresenceApplication {

    public static final String EVENT_STREAM_PATH = "/events";

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final SimpleDomainEventPublisher events;
    private final SseHub hub;
    private final Router router;
    private final PresencePoller poller;
    private final Connection connection;

    private WebServer server;

    private PresenceApplication(
        SimpleCommandBus commands,
        SimpleQueryBus queries,
        SimpleDomainEventPublisher events,
        SseHub hub,
        Router router,
        PresencePoller poller,
        Connection connection) {
        this.commands = commands;
        this.queries = queries;
        this.events = events;
        this.hub = hub;
        this.router = router;
        this.poller = poller;
        this.connection = connection;
    }

    public static PresenceApplication wire(LogEntryRenderer renderer, PresenceSettings settings, Clock clock) {
        var connection = SqliteConnections.openForContext(settings.dataDirectory(), "presence");
        new SchemaMigrator(connection, clock).migrate(PresenceMigrations.load());

        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        var hub = new SseHub();
        var dispatcher = new PendingEventDispatcher(events);
        var unitOfWork = new SqlitePresenceUnitOfWork(
            connection,
            new AuthenticationAuditDelivery(connection, dispatcher),
            new SqliteSequenceGenerator(connection));
        var attempts = new SqliteAuthenticationAttemptReadModel(connection);
        var challenges = new InMemoryLivenessChallengeRepository();

        registerCommandHandlers(commands, unitOfWork, challenges, settings, clock, renderer);
        registerQueryHandlers(queries, unitOfWork, attempts, clock, renderer);
        PresenceEventsBroadcaster.subscribeAll(events, hub);

        var handlers = new PresenceHandlers(commands, queries, settings.maintenanceMode());
        var interactions = new InteractionHandlers(commands, queries, hub);
        var poller = new PresencePoller(commands, challenges, hub, clock);

        return new PresenceApplication(commands, queries, events, hub, routes(handlers, interactions), poller,
            connection);
    }

    public PresenceApplication start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", router)
            .mount(EVENT_STREAM_PATH, new SseEndpoint(hub))
            .start();
        poller.start();
        return this;
    }

    public HttpHandler eventStream() {
        return new SseEndpoint(hub);
    }

    public void startBackgroundTasks() {
        poller.start();
    }

    public Router router() {
        return router;
    }

    public boolean hasActiveSession() {
        return queries.dispatch(new GetActiveSessionQuery()).value().isPresent();
    }

    public Optional<String> activeProfileId() {
        return queries.dispatch(new GetActiveSessionQuery()).value().map(session -> session.profileId());
    }

    public void stop() {
        poller.close();
        hub.closeAll();
        if (server != null) {
            server.close();
        }

        try {
            connection.close();
        } catch (SQLException e) {
            System.getLogger("presence").log(System.Logger.Level.WARNING, "Failed to close the database.", e);
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

    private static Router routes(PresenceHandlers handlers, InteractionHandlers interactions) {
        return Router.builder()
            .mount(Routes.at("/profiles")
                .post("/", handlers::enrollProfile)
                .get("/", handlers::listProfiles)
                .get("/{id}", handlers::getProfile)
                .delete("/{id}", handlers::deleteProfile)
                .post("/{id}/templates", handlers::addFaceTemplate)
                .delete("/{id}/templates/{templateId}", handlers::removeFaceTemplate))
            .mount(Routes.at("/authentication")
                .get("/", handlers::authenticationState)
                .post("/challenges", handlers::beginAuthentication)
                .post("/challenges/{challengeId}/complete", handlers::completeAuthentication)
                .get("/attempts", handlers::listAuthenticationAttempts))
            .mount(Routes.at("/sessions")
                .get("/active", handlers::activeSession)
                .post("/{id}/refresh", handlers::refreshSession)
                .delete("/{id}", handlers::closeSession))
            .mount(Routes.at("/interactions")
                .post("/gestures", interactions::gesture))
            .mount(Routes.at("/presence")
                .get("/face-quality.js", UiHandlers::faceQualityScript)
                .get("/sandbox", UiHandlers::sandbox)
                .get("/sandbox.css", UiHandlers::sandboxStyles)
                .get("/sandbox.js", UiHandlers::sandboxScript))
            .mount(Routes.at("/")
                .get("/", UiHandlers::index)
                .get("/errors.js", ErrorMessagesHandler::script)
                .get("/app.css", UiHandlers::styles)
                .get("/app.js", UiHandlers::script)
                .get("/docs", DocsHandlers::docs)
                .get("/openapi.json", DocsHandlers::openapi))
            .build();
    }

    private static void registerCommandHandlers(
        SimpleCommandBus commands,
        PresenceUnitOfWork unitOfWork,
        InMemoryLivenessChallengeRepository challenges,
        PresenceSettings settings,
        Clock clock,
        LogEntryRenderer renderer) {

        var templateIds = new UuidFaceTemplateIdGenerator();

        commands.register(EnrollProfileCommand.class, new LoggingCommandHandler<>(
            new EnrollProfileCommandHandler(unitOfWork, templateIds, clock, settings.maintenanceMode()), renderer));
        commands.register(AddFaceTemplateCommand.class, new LoggingCommandHandler<>(
            new AddFaceTemplateCommandHandler(unitOfWork, templateIds, clock), renderer));
        commands.register(RemoveFaceTemplateCommand.class, new LoggingCommandHandler<>(
            new RemoveFaceTemplateCommandHandler(unitOfWork, clock), renderer));
        commands.register(DeleteProfileCommand.class, new LoggingCommandHandler<>(
            new DeleteProfileCommandHandler(unitOfWork, clock), renderer));
        commands.register(BeginAuthenticationCommand.class, new LoggingCommandHandler<>(
            new BeginAuthenticationCommandHandler(
                unitOfWork, challenges, new LivenessPolicy(), new SecureRandom(), clock),
            renderer));
        commands.register(CompleteAuthenticationCommand.class, new LoggingCommandHandler<>(
            new CompleteAuthenticationCommandHandler(
                unitOfWork,
                challenges,
                new FaceMatcher(),
                settings.matchThreshold(),
                settings.sessionDuration(),
                clock),
            renderer));
        commands.register(RefreshSessionCommand.class, new LoggingCommandHandler<>(
            new RefreshSessionCommandHandler(unitOfWork, settings.sessionDuration(), clock), renderer));
        commands.register(CloseSessionCommand.class, new LoggingCommandHandler<>(
            new CloseSessionCommandHandler(unitOfWork, clock), renderer));
        commands.register(
            ExpireStaleSessionsCommand.class, new ExpireStaleSessionsCommandHandler(unitOfWork, clock));
    }

    private static void registerQueryHandlers(
        SimpleQueryBus queries,
        PresenceUnitOfWork unitOfWork,
        AuthenticationAttemptReadModel attempts,
        Clock clock,
        LogEntryRenderer renderer) {

        queries.register(GetProfileQuery.class, new LoggingQueryHandler<>(
            new GetProfileQueryHandler(unitOfWork.profiles()), renderer));
        queries.register(ListProfilesQuery.class, new LoggingQueryHandler<>(
            new ListProfilesQueryHandler(unitOfWork.profiles()), renderer));
        queries.register(GetActiveSessionQuery.class, new LoggingQueryHandler<>(
            new GetActiveSessionQueryHandler(unitOfWork.sessions(), clock), renderer));
        queries.register(GetAuthenticationStateQuery.class, new LoggingQueryHandler<>(
            new GetAuthenticationStateQueryHandler(
                unitOfWork.gate(), unitOfWork.profiles(), unitOfWork.sessions(), clock),
            renderer));
        queries.register(ListAuthenticationAttemptsQuery.class, new LoggingQueryHandler<>(
            new ListAuthenticationAttemptsQueryHandler(attempts), renderer));
    }
}

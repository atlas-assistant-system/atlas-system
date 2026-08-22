package atlas.app.appointments;

import atlas.application.appointments.commands.acknowledgereminder.AcknowledgeReminderCommand;
import atlas.application.appointments.commands.acknowledgereminder.AcknowledgeReminderCommandHandler;
import atlas.application.appointments.commands.addreminder.AddReminderCommand;
import atlas.application.appointments.commands.addreminder.AddReminderCommandHandler;
import atlas.application.appointments.commands.cancelappointment.CancelAppointmentCommand;
import atlas.application.appointments.commands.cancelappointment.CancelAppointmentCommandHandler;
import atlas.application.appointments.commands.changeappointmentdetails.ChangeAppointmentDetailsCommand;
import atlas.application.appointments.commands.changeappointmentdetails.ChangeAppointmentDetailsCommandHandler;
import atlas.application.appointments.commands.deleteappointment.DeleteAppointmentCommand;
import atlas.application.appointments.commands.deleteappointment.DeleteAppointmentCommandHandler;
import atlas.application.appointments.commands.removereminder.RemoveReminderCommand;
import atlas.application.appointments.commands.removereminder.RemoveReminderCommandHandler;
import atlas.application.appointments.commands.rescheduleappointment.RescheduleAppointmentCommand;
import atlas.application.appointments.commands.rescheduleappointment.RescheduleAppointmentCommandHandler;
import atlas.application.appointments.commands.restoreappointment.RestoreAppointmentCommand;
import atlas.application.appointments.commands.restoreappointment.RestoreAppointmentCommandHandler;
import atlas.application.appointments.commands.scheduleappointment.ScheduleAppointmentCommand;
import atlas.application.appointments.commands.scheduleappointment.ScheduleAppointmentCommandHandler;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.AppointmentRepository;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.appointments.ports.ReminderIdGenerator;
import atlas.application.appointments.queries.findfreeslots.FindFreeSlotsQuery;
import atlas.application.appointments.queries.findfreeslots.FindFreeSlotsQueryHandler;
import atlas.application.appointments.queries.findoverlappingappointments.FindOverlappingAppointmentsQuery;
import atlas.application.appointments.queries.findoverlappingappointments.FindOverlappingAppointmentsQueryHandler;
import atlas.application.appointments.queries.getappointment.GetAppointmentQuery;
import atlas.application.appointments.queries.getappointment.GetAppointmentQueryHandler;
import atlas.application.appointments.queries.getappointmentcountsbyday.GetAppointmentCountsByDayQuery;
import atlas.application.appointments.queries.getappointmentcountsbyday.GetAppointmentCountsByDayQueryHandler;
import atlas.application.appointments.queries.getappointmentcountsbymonth.GetAppointmentCountsByMonthQuery;
import atlas.application.appointments.queries.getappointmentcountsbymonth.GetAppointmentCountsByMonthQueryHandler;
import atlas.application.appointments.queries.getduereminders.GetDueRemindersQuery;
import atlas.application.appointments.queries.getduereminders.GetDueRemindersQueryHandler;
import atlas.application.appointments.queries.getupcomingappointments.GetUpcomingAppointmentsQuery;
import atlas.application.appointments.queries.getupcomingappointments.GetUpcomingAppointmentsQueryHandler;
import atlas.application.appointments.queries.listappointmentsbyperiod.ListAppointmentsByPeriodQuery;
import atlas.application.appointments.queries.listappointmentsbyperiod.ListAppointmentsByPeriodQueryHandler;
import atlas.application.sharedkernel.cqrs.SimpleCommandBus;
import atlas.application.sharedkernel.cqrs.SimpleQueryBus;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LoggingCommandHandler;
import atlas.application.sharedkernel.logging.LoggingQueryHandler;
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.infrastructure.appointments.persistence.SqliteAppointmentReadModel;
import atlas.infrastructure.appointments.persistence.SqliteAppointmentUnitOfWork;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.common.UuidReminderIdGenerator;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import atlas.presentation.appointments.handlers.AppointmentHandlers;
import atlas.presentation.appointments.sse.AppointmentEventsBroadcaster;
import atlas.presentation.appointments.sse.DueReminderPusher;
import atlas.presentation.appointments.web.DocsHandlers;
import atlas.presentation.common.web.SessionGuard;
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

public final class AppointmentsApplication {

    public static final String EVENT_STREAM_PATH = "/events";

    private final SimpleCommandBus commands;
    private final SimpleQueryBus queries;
    private final SimpleDomainEventPublisher events;
    private final SseHub hub;
    private final Router router;
    private final DueReminderPusher pusher;
    private final SessionGuard sessions;
    private final Connection connection;

    private WebServer server;

    private AppointmentsApplication(
        SimpleCommandBus commands,
        SimpleQueryBus queries,
        SimpleDomainEventPublisher events,
        SseHub hub,
        Router router,
        DueReminderPusher pusher,
        SessionGuard sessions,
        Connection connection) {
        this.commands = commands;
        this.queries = queries;
        this.events = events;
        this.hub = hub;
        this.router = router;
        this.pusher = pusher;
        this.sessions = sessions;
        this.connection = connection;
    }

    public static AppointmentsApplication wire(
        LogEntryRenderer renderer, Path databaseDirectory, Clock clock, BooleanSupplier hasActiveSession) {
        var connection = SqliteConnections.openForContext(databaseDirectory, "appointments");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            AppointmentsApplication.class, "/db-migrations/appointments",
            "V001__create_appointments.sql", "V002__create_appointment_reminders.sql",
            "V003__create_sequences.sql", "V004__index_appointments_window.sql",
            "V005__index_reminders_by_appointment.sql"));

        var events = new SimpleDomainEventPublisher();
        var commands = new SimpleCommandBus();
        var queries = new SimpleQueryBus();
        var hub = new SseHub();

        var unitOfWork = new SqliteAppointmentUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(events)),
            new SqliteSequenceGenerator(connection));
        var readModel = new SqliteAppointmentReadModel(connection);
        var availability = new AppointmentAvailability();
        var reminderIds = new UuidReminderIdGenerator();

        registerCommandHandlers(commands, unitOfWork, availability, reminderIds, clock, renderer);
        registerQueryHandlers(queries, readModel, unitOfWork.appointments(), availability, renderer);
        AppointmentEventsBroadcaster.subscribeAll(events, hub);

        var handlers = new AppointmentHandlers(commands, queries, clock);
        var pusher = new DueReminderPusher(queries, hub, clock);
        var sessions = new SessionGuard(hasActiveSession);

        return new AppointmentsApplication(commands, queries, events, hub, routes(handlers, sessions), pusher, sessions,
            connection);
    }

    public AppointmentsApplication start(int port) throws IOException {
        server = WebServer
            .onLoopback(port)
            .mount("/", router)
            .mount(EVENT_STREAM_PATH, sessions.protect(new SseEndpoint(hub)))
            .start();
        pusher.start();

        return this;
    }

    public SimpleCommandBus commands() {
        return commands;
    }

    public SimpleQueryBus queries() {
        return queries;
    }

    public SseHub hub() {
        return hub;
    }

    public HttpHandler eventStream() {
        return sessions.protect(new SseEndpoint(hub));
    }

    public void startBackgroundTasks() {
        pusher.start();
    }

    public Router router() {
        return router;
    }

    public void stop() {
        pusher.close();
        hub.closeAll();

        if (server != null) {
            server.close();
        }

        try {
            connection.close();
        } catch (SQLException e) {
            System.getLogger("appointments").log(System.Logger.Level.WARNING, "Failed to close the database.", e);
        }
    }

    public int port() {
        return server.port();
    }

    private static void registerCommandHandlers(
        SimpleCommandBus commands,
        AppointmentUnitOfWork unitOfWork,
        AppointmentAvailability availability,
        ReminderIdGenerator reminderIds,
        Clock clock,
        LogEntryRenderer renderer) {

        commands.register(ScheduleAppointmentCommand.class, new LoggingCommandHandler<>(
            new ScheduleAppointmentCommandHandler(unitOfWork, availability, reminderIds, clock), renderer));
        commands.register(RescheduleAppointmentCommand.class, new LoggingCommandHandler<>(
            new RescheduleAppointmentCommandHandler(unitOfWork, availability, clock), renderer));
        commands.register(CancelAppointmentCommand.class, new LoggingCommandHandler<>(
            new CancelAppointmentCommandHandler(unitOfWork, clock), renderer));
        commands.register(RestoreAppointmentCommand.class, new LoggingCommandHandler<>(
            new RestoreAppointmentCommandHandler(unitOfWork, availability, clock), renderer));
        commands.register(DeleteAppointmentCommand.class, new LoggingCommandHandler<>(
            new DeleteAppointmentCommandHandler(unitOfWork, clock), renderer));
        commands.register(ChangeAppointmentDetailsCommand.class, new LoggingCommandHandler<>(
            new ChangeAppointmentDetailsCommandHandler(unitOfWork, clock), renderer));
        commands.register(AddReminderCommand.class, new LoggingCommandHandler<>(
            new AddReminderCommandHandler(unitOfWork, reminderIds, clock), renderer));
        commands.register(RemoveReminderCommand.class, new LoggingCommandHandler<>(
            new RemoveReminderCommandHandler(unitOfWork, clock), renderer));
        commands.register(AcknowledgeReminderCommand.class, new LoggingCommandHandler<>(
            new AcknowledgeReminderCommandHandler(unitOfWork, clock), renderer));
    }

    private static void registerQueryHandlers(
        SimpleQueryBus queries,
        AppointmentReadModel readModel,
        AppointmentRepository repository,
        AppointmentAvailability availability,
        LogEntryRenderer renderer) {

        queries.register(GetAppointmentQuery.class, new LoggingQueryHandler<>(
            new GetAppointmentQueryHandler(repository), renderer));
        queries.register(ListAppointmentsByPeriodQuery.class, new LoggingQueryHandler<>(
            new ListAppointmentsByPeriodQueryHandler(readModel), renderer));
        queries.register(GetAppointmentCountsByMonthQuery.class, new LoggingQueryHandler<>(
            new GetAppointmentCountsByMonthQueryHandler(readModel), renderer));
        queries.register(GetAppointmentCountsByDayQuery.class, new LoggingQueryHandler<>(
            new GetAppointmentCountsByDayQueryHandler(readModel), renderer));
        queries.register(FindOverlappingAppointmentsQuery.class, new LoggingQueryHandler<>(
            new FindOverlappingAppointmentsQueryHandler(readModel), renderer));
        queries.register(FindFreeSlotsQuery.class, new LoggingQueryHandler<>(
            new FindFreeSlotsQueryHandler(readModel, availability), renderer));
        queries.register(GetUpcomingAppointmentsQuery.class, new LoggingQueryHandler<>(
            new GetUpcomingAppointmentsQueryHandler(readModel), renderer));
        queries.register(GetDueRemindersQuery.class, new LoggingQueryHandler<>(
            new GetDueRemindersQueryHandler(readModel), renderer));
    }

    private static Router routes(AppointmentHandlers handlers, SessionGuard sessions) {
        return Router.builder()
            .mount(Routes.at("/appointments")
                .post("/", sessions.protect(handlers::schedule))
                .get("/", sessions.protect(handlers::list))
                .get("/counts/by-month", sessions.protect(handlers::countsByMonth))
                .get("/counts/by-day", sessions.protect(handlers::countsByDay))
                .get("/overlapping", sessions.protect(handlers::overlapping))
                .get("/free-slots", sessions.protect(handlers::freeSlots))
                .get("/upcoming", sessions.protect(handlers::upcoming))
                .get("/{id}", sessions.protect(handlers::detail))
                .delete("/{id}", sessions.protect(handlers::delete))
                .post("/{id}/reschedule", sessions.protect(handlers::reschedule))
                .post("/{id}/cancel", sessions.protect(handlers::cancel))
                .post("/{id}/restore", sessions.protect(handlers::restore))
                .put("/{id}/details", sessions.protect(handlers::changeDetails))
                .post("/{id}/reminders", sessions.protect(handlers::addReminder))
                .delete("/{id}/reminders/{reminderId}", sessions.protect(handlers::removeReminder))
                .post("/{id}/reminders/{reminderId}/acknowledge", sessions.protect(handlers::acknowledgeReminder)))
            .mount(Routes.at("/reminders")
                .get("/due", sessions.protect(handlers::dueReminders)))
            .mount(Routes.at("/")
                .get("/docs", sessions.protect(DocsHandlers::docs))
                .get("/openapi.json", sessions.protect(DocsHandlers::openapi)))
            .build();
    }
}

package atlas.infrastructure.appointments.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.appointments.ports.AppointmentRepository;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.events.AppointmentScheduledEvent;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteAppointmentRepositoryIT {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final ReminderId FIRST_REMINDER = ReminderId.of(new UUID(0, 1));
    private static final ReminderId SECOND_REMINDER = ReminderId.of(new UUID(0, 2));

    private Connection connection;
    private SqliteAppointmentUnitOfWork unitOfWork;
    private AppointmentRepository appointments;
    private final List<DomainEvent> publishedEvents = new ArrayList<>();

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        connection.setAutoCommit(false);

        new SchemaMigrator(connection, Clock.fixed(NOW, ZoneOffset.UTC)).migrate(Migrations.load(
            SqliteAppointmentRepositoryIT.class, "/db-migrations/appointments",
            "V001__create_appointments.sql", "V002__create_appointment_reminders.sql",
            "V003__create_sequences.sql", "V004__index_appointments_window.sql",
            "V005__index_reminders_by_appointment.sql"));

        var publisher = new SimpleDomainEventPublisher();
        publisher.subscribe(AppointmentScheduledEvent.class, publishedEvents::add);

        unitOfWork = new SqliteAppointmentUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(publisher)),
            new SqliteSequenceGenerator(connection));
        appointments = unitOfWork.appointments();
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldPersistAndRehydrateAnAppointmentWithItsReminders() {
        var id = unitOfWork.execute(() -> {
            var appointment = scheduled(appointments.nextId(), 11, 12);
            appointment.addReminder(FIRST_REMINDER, leadTime(15), NOW_LOCAL, NOW);
            appointment.acknowledgeReminder(FIRST_REMINDER, NOW);
            appointment.addReminder(SECOND_REMINDER, leadTime(60), NOW_LOCAL, NOW);
            appointments.create(appointment);

            return appointment.id();
        });

        var loaded = appointments.get(id).orElseThrow();

        assertThat(loaded.title().value()).isEqualTo("Dentista");
        assertThat(loaded.description()).map(AppointmentDescription::value).contains("Llevar radiografia");
        assertThat(loaded.timeSlot()).isEqualTo(slot(11, 12));
        assertThat(loaded.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(loaded.reminders()).hasSize(2);
        assertThat(loaded.reminders().getFirst().id()).isEqualTo(FIRST_REMINDER);
        assertThat(loaded.reminders().getFirst().acknowledgedAt()).contains(NOW);
        assertThat(loaded.reminders().getLast().id()).isEqualTo(SECOND_REMINDER);
        assertThat(loaded.reminders().getLast().isAcknowledged()).isFalse();
    }

    @Test
    void shouldGenerateSequentialPrefixedIds() {
        var ids = unitOfWork.execute(() -> List.of(appointments.nextId(), appointments.nextId()));

        assertThat(ids).containsExactly(AppointmentId.of(1), AppointmentId.of(2));
    }

    @Test
    void shouldReplaceTheRemindersOnUpdate() {
        var id = unitOfWork.execute(() -> {
            var appointment = scheduled(appointments.nextId(), 11, 12);
            appointment.addReminder(FIRST_REMINDER, leadTime(15), NOW_LOCAL, NOW);
            appointments.create(appointment);

            return appointment.id();
        });

        unitOfWork.execute(() -> {
            var appointment = appointments.get(id).orElseThrow();
            appointment.acknowledgeReminder(FIRST_REMINDER, NOW);
            appointment.addReminder(SECOND_REMINDER, leadTime(60), NOW_LOCAL, NOW);
            appointments.update(appointment);

            return Result.success();
        });

        var loaded = appointments.get(id).orElseThrow();

        assertThat(loaded.reminders()).hasSize(2);
        assertThat(loaded.reminders().getFirst().acknowledgedAt()).contains(NOW);
    }

    @Test
    void shouldDeleteTheAppointmentTogetherWithItsReminders() throws SQLException {
        var id = unitOfWork.execute(() -> {
            var appointment = scheduled(appointments.nextId(), 11, 12);
            appointment.addReminder(FIRST_REMINDER, leadTime(15), NOW_LOCAL, NOW);
            appointments.create(appointment);

            return appointment.id();
        });

        unitOfWork.execute(() -> {
            appointments.delete(appointments.get(id).orElseThrow());

            return Result.success();
        });

        assertThat(appointments.get(id)).isEmpty();
        assertThat(countReminderRows()).isZero();
    }

    @Test
    void shouldFindOnlyActiveAppointmentsIntersectingTheWindow() {
        unitOfWork.execute(() -> {
            appointments.create(scheduled(appointments.nextId(), 11, 12));
            appointments.create(scheduled(appointments.nextId(), 13, 14));

            var cancelled = scheduled(appointments.nextId(), 11, 12);
            cancelled.cancel(NOW_LOCAL, NOW);
            appointments.create(cancelled);

            return Result.success();
        });

        var found = appointments.findActiveInWindow(slot(10, 13));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().id()).isEqualTo(AppointmentId.of(1));
    }

    @Test
    void shouldPublishPendingEventsOnlyAfterTheCommit() {
        unitOfWork.execute(() -> {
            appointments.create(scheduled(appointments.nextId(), 11, 12));
            assertThat(publishedEvents).isEmpty();

            return Result.success();
        });

        assertThat(publishedEvents).singleElement().isInstanceOf(AppointmentScheduledEvent.class);
    }

    @Test
    void shouldRollBackAndPublishNothingWhenTheWorkReturnsAFailure() {
        var result = unitOfWork.execute(() -> {
            appointments.create(scheduled(appointments.nextId(), 11, 12));

            return Result.failure(AppointmentErrors.TITLE_REQUIRED);
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(appointments.get(AppointmentId.of(1))).isEmpty();
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void shouldFailWhenUpdatingAnAppointmentThatWasNeverPersisted() {
        assertThatThrownBy(() -> unitOfWork.execute(() -> {
            appointments.update(scheduled(AppointmentId.of(9), 11, 12));

            return Result.success();
        })).isInstanceOf(PersistenceException.class);
    }

    private long countReminderRows() throws SQLException {
        try (var statement = connection.createStatement();
            var rows = statement.executeQuery("SELECT COUNT(*) FROM appointment_reminders")) {
            rows.next();

            return rows.getLong(1);
        }
    }

    private static Appointment scheduled(AppointmentId id, int startHour, int endHour) {
        return Appointment.schedule(
            id,
            AppointmentTitle.create("Dentista").value(),
            AppointmentDescription.create("Llevar radiografia").value(),
            slot(startHour, endHour),
            NOW_LOCAL,
            NOW).value();
    }

    private static TimeSlot slot(int startHour, int endHour) {
        return TimeSlot.of(LocalDateTime.of(2026, 8, 17, startHour, 0), LocalDateTime.of(2026, 8, 17, endHour, 0));
    }

    private static ReminderLeadTime leadTime(int minutes) {
        return ReminderLeadTime.create(minutes).value();
    }
}

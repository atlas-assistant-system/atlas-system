package atlas.infrastructure.appointments.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.AppointmentRepository;
import atlas.application.appointments.ports.AppointmentSummary;
import atlas.application.appointments.ports.DailyAppointmentCount;
import atlas.application.appointments.ports.MonthlyAppointmentCount;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.BookedSlot;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteAppointmentReadModelIT {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 1, 0, 0);

    private Connection connection;
    private SqliteAppointmentUnitOfWork unitOfWork;
    private AppointmentRepository appointments;
    private AppointmentReadModel readModel;
    private int reminderSeed;

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        connection.setAutoCommit(false);

        new SchemaMigrator(connection, Clock.fixed(NOW, ZoneOffset.UTC)).migrate(Migrations.load(
            SqliteAppointmentReadModelIT.class, "/db-migrations/appointments",
            "V001__create_appointments.sql", "V002__create_appointment_reminders.sql",
            "V003__create_sequences.sql", "V004__index_appointments_window.sql",
            "V005__index_reminders_by_appointment.sql"));

        unitOfWork = new SqliteAppointmentUnitOfWork(
            connection, new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            new SqliteSequenceGenerator(connection));
        appointments = unitOfWork.appointments();
        readModel = new SqliteAppointmentReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldListActiveAppointmentsInTheWindowInChronologicalOrder() {
        seed(at(17, 12, 0), at(17, 13, 0));
        seed(at(17, 10, 0), at(17, 11, 0));
        seedCancelled(at(17, 14, 0), at(17, 15, 0));
        seed(at(20, 10, 0), at(20, 11, 0));

        var page = readModel.findInWindow(window(17), false, PageRequest.first());

        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.items()).extracting(AppointmentSummary::slot)
            .containsExactly(slot(at(17, 10, 0), at(17, 11, 0)), slot(at(17, 12, 0), at(17, 13, 0)));
    }

    @Test
    void shouldIncludeCancelledAppointmentsOnlyWhenAskedTo() {
        seed(at(17, 10, 0), at(17, 11, 0));
        seedCancelled(at(17, 12, 0), at(17, 13, 0));

        assertThat(readModel.findInWindow(window(17), false, PageRequest.first()).totalCount()).isEqualTo(1);
        assertThat(readModel.findInWindow(window(17), true, PageRequest.first()).totalCount()).isEqualTo(2);
    }

    @Test
    void shouldPaginateKeepingTheTotalCount() {
        seed(at(17, 10, 0), at(17, 11, 0));
        seed(at(17, 12, 0), at(17, 13, 0));
        seed(at(17, 14, 0), at(17, 15, 0));

        var firstPage = readModel.findInWindow(window(17), false, PageRequest.of(1, 2));
        var secondPage = readModel.findInWindow(window(17), false, PageRequest.of(2, 2));

        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.totalCount()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.items()).hasSize(1);
    }

    @Test
    void shouldCountAnAppointmentSpanningTwoMonthsInBothBuckets() {
        seed(LocalDateTime.of(2026, 8, 31, 23, 0), LocalDateTime.of(2026, 9, 1, 1, 0));
        seed(at(17, 10, 0), at(17, 11, 0));

        var counts = readModel.countByMonth(yearWindow(), false);

        assertThat(counts).containsExactly(
            new MonthlyAppointmentCount(YearMonth.of(2026, 8), 2),
            new MonthlyAppointmentCount(YearMonth.of(2026, 9), 1));
    }

    @Test
    void shouldCountAnAppointmentSpanningTwoDaysInBothBuckets() {
        seed(LocalDateTime.of(2026, 8, 17, 23, 0), LocalDateTime.of(2026, 8, 18, 1, 0));
        seed(at(17, 10, 0), at(17, 11, 0));

        var counts = readModel.countByDay(monthWindow(), false);

        assertThat(counts).containsExactly(
            new DailyAppointmentCount(LocalDate.of(2026, 8, 17), 2),
            new DailyAppointmentCount(LocalDate.of(2026, 8, 18), 1));
    }

    @Test
    void shouldFindOverlappingAppointmentsExcludingTheGivenOne() {
        var first = seed(at(17, 10, 0), at(17, 12, 0));
        var second = seed(at(17, 11, 0), at(17, 13, 0));
        seedCancelled(at(17, 11, 0), at(17, 13, 0));

        var overlapping = readModel.findOverlapping(slot(at(17, 11, 0), at(17, 12, 0)), Optional.of(first));

        assertThat(overlapping).extracting(AppointmentSummary::id).containsExactly(second);
    }

    @Test
    void shouldReturnBookedSlotsWithoutTheExcludedAppointment() {
        var first = seed(at(17, 10, 0), at(17, 11, 0));
        var second = seed(at(17, 12, 0), at(17, 13, 0));

        var booked = readModel.findBookedSlots(window(17), Optional.of(first));

        assertThat(booked).containsExactly(BookedSlot.of(second, slot(at(17, 12, 0), at(17, 13, 0))));
    }

    @Test
    void shouldListUpcomingAppointmentsIncludingOneInProgress() {
        seed(at(17, 9, 0), at(17, 10, 0));
        var inProgress = seed(at(17, 11, 0), at(17, 12, 0));
        var future = seed(at(17, 15, 0), at(17, 16, 0));
        seedCancelled(at(17, 17, 0), at(17, 18, 0));

        var upcoming = readModel.findUpcoming(at(17, 11, 30), 10);

        assertThat(upcoming).extracting(AppointmentSummary::id).containsExactly(inProgress, future);
        assertThat(readModel.findUpcoming(at(17, 11, 30), 1)).hasSize(1);
    }

    @Test
    void shouldReturnOnlyUnacknowledgedRemindersWhoseTriggerTimeArrived() {
        var due = seedWithReminder(at(17, 11, 0), at(17, 12, 0), 15, false);
        seedWithReminder(at(17, 11, 0), at(17, 12, 0), 30, true);
        seedWithReminder(at(17, 15, 0), at(17, 16, 0), 15, false);

        var reminders = readModel.findDueReminders(at(17, 10, 50));

        assertThat(reminders).singleElement().satisfies(reminder -> {
            assertThat(reminder.appointmentId()).isEqualTo(due);
            assertThat(reminder.leadTime().value()).isEqualTo(15);
            assertThat(reminder.slot().start()).isEqualTo(at(17, 11, 0));
        });
    }

    private AppointmentId seed(LocalDateTime start, LocalDateTime end) {
        return unitOfWork.execute(() -> {
            var appointment = newAppointment(start, end);
            appointments.create(appointment);

            return appointment.id();
        });
    }

    private AppointmentId seedCancelled(LocalDateTime start, LocalDateTime end) {
        return unitOfWork.execute(() -> {
            var appointment = newAppointment(start, end);
            appointment.cancel(NOW_LOCAL, NOW);
            appointments.create(appointment);

            return appointment.id();
        });
    }

    private AppointmentId seedWithReminder(
        LocalDateTime start, LocalDateTime end, int leadTimeMinutes, boolean acknowledged) {
        return unitOfWork.execute(() -> {
            var appointment = newAppointment(start, end);
            var reminderId = ReminderId.of(new UUID(0, ++reminderSeed));
            appointment.addReminder(reminderId, ReminderLeadTime.create(leadTimeMinutes).value(), NOW_LOCAL, NOW);
            if (acknowledged) {
                appointment.acknowledgeReminder(reminderId, NOW);
            }
            appointments.create(appointment);

            return appointment.id();
        });
    }

    private Appointment newAppointment(LocalDateTime start, LocalDateTime end) {
        return Appointment.schedule(
            appointments.nextId(), AppointmentTitle.create("Dentista").value(), Optional.empty(),
            slot(start, end), NOW_LOCAL, NOW).value();
    }

    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 8, day, hour, minute);
    }

    private static TimeSlot slot(LocalDateTime start, LocalDateTime end) {
        return TimeSlot.of(start, end);
    }

    private static TimeSlot window(int day) {
        return TimeSlot.of(at(day, 0, 0), LocalDateTime.of(2026, 8, day + 1, 0, 0));
    }

    private static TimeSlot monthWindow() {
        return TimeSlot.of(LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0));
    }

    private static TimeSlot yearWindow() {
        return TimeSlot.of(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
    }
}

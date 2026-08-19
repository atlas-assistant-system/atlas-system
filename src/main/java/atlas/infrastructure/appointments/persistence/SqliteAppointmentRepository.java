package atlas.infrastructure.appointments.persistence;

import atlas.application.appointments.ports.AppointmentRepository;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.Reminder;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteAppointmentRepository extends AbstractSqlRepository<Appointment, AppointmentId>
    implements AppointmentRepository {

    private static final String SEQUENCE_NAME = "appointments";

    private final SequenceGenerator sequences;

    public SqliteAppointmentRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "appointments", "id");
        this.sequences = sequences;
    }

    @Override
    public AppointmentId nextId() {
        return AppointmentId.of(sequences.next(SEQUENCE_NAME));
    }

    @Override
    public List<Appointment> findActiveInWindow(TimeSlot window) {
        var sql = "SELECT id, title, description, starts_at, ends_at, status FROM appointments"
            + " WHERE status = ? AND starts_at < ? AND ends_at > ? ORDER BY starts_at, id";

        try (var statement = connection().prepareStatement(sql)) {
            statement.setString(1, AppointmentStatus.SCHEDULED.name());
            statement.setString(2, Timestamps.format(window.end()));
            statement.setString(3, Timestamps.format(window.start()));

            try (var rows = statement.executeQuery()) {
                var found = new ArrayList<Appointment>();
                while (rows.next()) {
                    found.add(mapRow(rows));
                }

                return List.copyOf(found);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load active appointments in window", e);
        }
    }

    @Override
    public void create(Appointment appointment) {
        super.create(appointment);
        insertReminders(appointment);
    }

    @Override
    public void update(Appointment appointment) {
        super.update(appointment);
        deleteRemindersOf(appointment.id());
        insertReminders(appointment);
    }

    @Override
    public void delete(Appointment appointment) {
        deleteRemindersOf(appointment.id());
        super.delete(appointment);
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "title", "description", "starts_at", "ends_at", "status");
    }

    @Override
    protected void bind(PreparedStatement statement, Appointment appointment) throws SQLException {
        statement.setLong(1, appointment.id().value());
        statement.setString(2, appointment.title().value());
        statement.setString(3, appointment.description().map(AppointmentDescription::value).orElse(null));
        statement.setString(4, Timestamps.format(appointment.timeSlot().start()));
        statement.setString(5, Timestamps.format(appointment.timeSlot().end()));
        statement.setString(6, appointment.status().name());
    }

    @Override
    protected Appointment mapRow(ResultSet row) throws SQLException {
        var id = AppointmentId.of(row.getLong("id"));

        return Appointment.rehydrate(
            id,
            new AppointmentTitle(row.getString("title")),
            Optional.ofNullable(row.getString("description")).map(AppointmentDescription::new),
            TimeSlot.of(Timestamps.parse(row.getString("starts_at")), Timestamps.parse(row.getString("ends_at"))),
            AppointmentStatus.valueOf(row.getString("status")),
            remindersOf(id));
    }

    @Override
    protected Object idValue(AppointmentId id) {
        return id.value();
    }

    private List<Reminder> remindersOf(AppointmentId id) throws SQLException {
        var sql = "SELECT id, lead_time_minutes, acknowledged_at FROM appointment_reminders"
            + " WHERE appointment_id = ? ORDER BY lead_time_minutes";

        try (var statement = connection().prepareStatement(sql)) {
            statement.setLong(1, id.value());

            try (var rows = statement.executeQuery()) {
                var reminders = new ArrayList<Reminder>();
                while (rows.next()) {
                    var acknowledgedAt = rows.getString("acknowledged_at");
                    reminders.add(Reminder.rehydrate(
                        ReminderId.of(UUID.fromString(rows.getString("id"))),
                        new ReminderLeadTime(rows.getInt("lead_time_minutes")),
                        acknowledgedAt == null ? null : Instant.parse(acknowledgedAt)));
                }

                return List.copyOf(reminders);
            }
        }
    }

    private void insertReminders(Appointment appointment) {
        var sql = "INSERT INTO appointment_reminders (id, appointment_id, lead_time_minutes, acknowledged_at)"
            + " VALUES (?, ?, ?, ?)";

        try (var statement = connection().prepareStatement(sql)) {
            for (var reminder : appointment.reminders()) {
                statement.setString(1, reminder.id().value().toString());
                statement.setLong(2, appointment.id().value());
                statement.setInt(3, reminder.leadTime().value());
                statement.setString(4, reminder.acknowledgedAt().map(Instant::toString).orElse(null));
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to persist the reminders of " + appointment.id(), e);
        }
    }

    private void deleteRemindersOf(AppointmentId id) {
        try (var statement =
            connection().prepareStatement("DELETE FROM appointment_reminders WHERE appointment_id = ?")) {
            statement.setLong(1, id.value());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete the reminders of " + id, e);
        }
    }
}

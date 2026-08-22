package atlas.infrastructure.appointments.persistence;

import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.AppointmentSummary;
import atlas.application.appointments.ports.DailyAppointmentCount;
import atlas.application.appointments.ports.DueReminder;
import atlas.application.appointments.ports.MonthlyAppointmentCount;
import atlas.application.sharedkernel.paging.Page;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.BookedSlot;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.infrastructure.sharedkernel.persistence.StatementBinder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteAppointmentReadModel implements AppointmentReadModel {

    private static final String SUMMARY_COLUMNS = "id, title, starts_at, ends_at, status";
    private static final String OVERLAPS_WINDOW = " starts_at < ? AND ends_at > ?";

    private final Connection connection;

    public SqliteAppointmentReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Page<AppointmentSummary> findInWindow(TimeSlot window, boolean includeCancelled, PageRequest page) {
        var filter = " WHERE" + OVERLAPS_WINDOW + statusFilter(includeCancelled);
        var items = querySummaries(
            "SELECT " + SUMMARY_COLUMNS + " FROM appointments" + filter + " ORDER BY starts_at, id LIMIT ? OFFSET ?",
            statement -> {
                bindWindow(statement, window);
                statement.setInt(3, page.pageSize());
                statement.setInt(4, page.offset());
            });
        var total = count(
            "SELECT COUNT(*) FROM appointments" + filter, statement -> bindWindow(statement, window));

        return Page.of(items, page, total);
    }

    @Override
    public List<MonthlyAppointmentCount> countByMonth(TimeSlot yearWindow, boolean includeCancelled) {
        var slots = slotsInWindow(yearWindow, includeCancelled);

        var counts = new ArrayList<MonthlyAppointmentCount>();
        var month = YearMonth.from(yearWindow.start());
        while (month.atDay(1).atStartOfDay().isBefore(yearWindow.end())) {
            var monthSlot = TimeSlot.of(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
            var count = slots.stream().filter(slot -> slot.overlaps(monthSlot)).count();
            if (count > 0) {
                counts.add(new MonthlyAppointmentCount(month, count));
            }

            month = month.plusMonths(1);
        }

        return List.copyOf(counts);
    }

    @Override
    public List<DailyAppointmentCount> countByDay(TimeSlot monthWindow, boolean includeCancelled) {
        var slots = slotsInWindow(monthWindow, includeCancelled);

        var counts = new ArrayList<DailyAppointmentCount>();
        var day = monthWindow.start().toLocalDate();
        while (day.atStartOfDay().isBefore(monthWindow.end())) {
            var daySlot = TimeSlot.of(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
            var count = slots.stream().filter(slot -> slot.overlaps(daySlot)).count();
            if (count > 0) {
                counts.add(new DailyAppointmentCount(day, count));
            }

            day = day.plusDays(1);
        }

        return List.copyOf(counts);
    }

    @Override
    public List<AppointmentSummary> findOverlapping(TimeSlot candidate, Optional<AppointmentId> exclude) {
        return querySummaries(
            "SELECT " + SUMMARY_COLUMNS + " FROM appointments WHERE status = ? AND" + OVERLAPS_WINDOW
                + " AND id <> ? ORDER BY starts_at, id",
            statement -> {
                statement.setString(1, AppointmentStatus.SCHEDULED.name());
                statement.setString(2, Timestamps.format(candidate.end()));
                statement.setString(3, Timestamps.format(candidate.start()));
                statement.setLong(4, exclude.map(AppointmentId::value).orElse(-1L));
            });
    }

    @Override
    public List<BookedSlot> findBookedSlots(TimeSlot window, Optional<AppointmentId> exclude) {
        var sql = "SELECT id, starts_at, ends_at FROM appointments WHERE status = ? AND" + OVERLAPS_WINDOW
            + " AND id <> ? ORDER BY starts_at, id";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppointmentStatus.SCHEDULED.name());
            statement.setString(2, Timestamps.format(window.end()));
            statement.setString(3, Timestamps.format(window.start()));
            statement.setLong(4, exclude.map(AppointmentId::value).orElse(-1L));

            try (var rows = statement.executeQuery()) {
                var booked = new ArrayList<BookedSlot>();
                while (rows.next()) {
                    booked.add(BookedSlot.of(AppointmentId.of(rows.getLong("id")), slotOf(rows)));
                }

                return List.copyOf(booked);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load booked slots", e);
        }
    }

    @Override
    public List<AppointmentSummary> findUpcoming(LocalDateTime now, int limit) {
        return querySummaries(
            "SELECT " + SUMMARY_COLUMNS + " FROM appointments WHERE status = ? AND ends_at > ?"
                + " ORDER BY starts_at, id LIMIT ?",
            statement -> {
                statement.setString(1, AppointmentStatus.SCHEDULED.name());
                statement.setString(2, Timestamps.format(now));
                statement.setInt(3, limit);
            });
    }

    @Override
    public List<DueReminder> findDueReminders(LocalDateTime now) {
        var sql = "SELECT r.id AS reminder_id, r.lead_time_minutes, a.id AS appointment_id, a.title,"
            + " a.starts_at, a.ends_at FROM appointment_reminders r"
            + " JOIN appointments a ON a.id = r.appointment_id"
            + " WHERE a.status = ? AND r.acknowledged_at IS NULL AND a.starts_at > ?"
            + " ORDER BY a.starts_at, r.lead_time_minutes";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppointmentStatus.SCHEDULED.name());
            statement.setString(2, Timestamps.format(now));

            try (var rows = statement.executeQuery()) {
                var due = new ArrayList<DueReminder>();
                while (rows.next()) {
                    var slot = slotOf(rows);
                    var leadTime = new ReminderLeadTime(rows.getInt("lead_time_minutes"));
                    if (!leadTime.triggerTimeFor(slot).isAfter(now)) {
                        due.add(new DueReminder(
                            AppointmentId.of(rows.getLong("appointment_id")),
                            ReminderId.of(UUID.fromString(rows.getString("reminder_id"))),
                            new AppointmentTitle(rows.getString("title")),
                            slot,
                            leadTime));
                    }
                }

                return List.copyOf(due);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load due reminders", e);
        }
    }

    private List<TimeSlot> slotsInWindow(TimeSlot window, boolean includeCancelled) {
        var sql = "SELECT starts_at, ends_at FROM appointments WHERE" + OVERLAPS_WINDOW
            + statusFilter(includeCancelled);

        try (var statement = connection.prepareStatement(sql)) {
            bindWindow(statement, window);

            try (var rows = statement.executeQuery()) {
                var slots = new ArrayList<TimeSlot>();
                while (rows.next()) {
                    slots.add(slotOf(rows));
                }

                return List.copyOf(slots);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load appointment slots in window", e);
        }
    }

    private List<AppointmentSummary> querySummaries(String sql, StatementBinder binder) {
        try (var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);

            try (var rows = statement.executeQuery()) {
                var summaries = new ArrayList<AppointmentSummary>();
                while (rows.next()) {
                    summaries.add(new AppointmentSummary(
                        AppointmentId.of(rows.getLong("id")),
                        new AppointmentTitle(rows.getString("title")),
                        slotOf(rows),
                        AppointmentStatus.valueOf(rows.getString("status"))));
                }

                return List.copyOf(summaries);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load appointment summaries", e);
        }
    }

    private long count(String sql, StatementBinder binder) {
        try (var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);

            try (var rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count appointments", e);
        }
    }

    private static void bindWindow(PreparedStatement statement, TimeSlot window) throws SQLException {
        statement.setString(1, Timestamps.format(window.end()));
        statement.setString(2, Timestamps.format(window.start()));
    }

    private static TimeSlot slotOf(ResultSet rows) throws SQLException {
        return TimeSlot.of(Timestamps.parse(rows.getString("starts_at")), Timestamps.parse(rows.getString("ends_at")));
    }

    private static String statusFilter(boolean includeCancelled) {
        return includeCancelled ? "" : " AND status = '" + AppointmentStatus.SCHEDULED.name() + "'";
    }
}

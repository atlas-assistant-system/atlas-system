package atlas.infrastructure.routines.persistence.mappers;

import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineEntryId;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.vos.RoutineDescription;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import atlas.domain.routines.vos.Unit;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RoutineRows {

    private RoutineRows() {}

    public static Routine toRoutine(ResultSet row) throws SQLException {
        var id = RoutineId.of(row.getLong("id"));
        var name = require(RoutineName.create(row.getString("name")), "routines.name");
        var description =
            require(RoutineDescription.create(row.getString("description")), "routines.description").orElse(null);
        var unit = require(Unit.create(row.getString("target_unit")), "routines.target_unit").orElse(null);
        var target = require(Target.create(new BigDecimal(row.getString("target_amount")), unit), "routines.target");
        var schedule = require(
            Schedule.create(
                RecurrencePeriod.valueOf(row.getString("period")),
                readDays(row.getString("active_days")),
                readDaysOfMonth(row.getString("days_of_month"))),
            "routines.schedule");

        return Routine.rehydrate(id, name, description, target, schedule, row.getBoolean("archived"));
    }

    public static void bind(PreparedStatement statement, Routine routine) throws SQLException {
        var schedule = routine.schedule();

        statement.setLong(1, routine.id().value());
        statement.setString(2, routine.name().value());
        statement.setString(3, routine.description().map(RoutineDescription::value).orElse(null));
        statement.setString(4, routine.target().amount().toPlainString());
        statement.setString(5, routine.target().unit().map(Unit::value).orElse(null));
        statement.setString(6, schedule.period().name());
        statement.setString(7,
            schedule.activeDays().stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(",")));
        statement.setString(8,
            schedule.daysOfMonth().stream().sorted().map(String::valueOf).collect(Collectors.joining(",")));
        statement.setBoolean(9, routine.isArchived());
    }

    public static RoutineEntry toEntry(ResultSet row) throws SQLException {
        return RoutineEntry.rehydrate(
            RoutineEntryId.of(UUID.fromString(row.getString("id"))),
            RoutineId.of(row.getLong("routine_id")),
            LocalDate.parse(row.getString("day")),
            new BigDecimal(row.getString("amount")));
    }

    public static void bind(PreparedStatement statement, RoutineEntry entry) throws SQLException {
        statement.setString(1, entry.id().value().toString());
        statement.setLong(2, entry.routineId().value());
        statement.setString(3, entry.day().toString());
        statement.setString(4, entry.amount().toPlainString());
    }

    private static Set<DayOfWeek> readDays(String stored) {
        return split(stored).map(DayOfWeek::valueOf).collect(Collectors.toSet());
    }

    private static Set<Integer> readDaysOfMonth(String stored) {
        return split(stored).map(Integer::valueOf).collect(Collectors.toSet());
    }

    private static Stream<String> split(String stored) {
        return stored == null || stored.isBlank()
            ? Stream.empty()
            : Arrays.stream(stored.split(",")).map(String::trim);
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException(
                "Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}

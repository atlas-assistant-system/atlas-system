package atlas.infrastructure.routines.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineEntryId;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.events.RoutineDefinedEvent;
import atlas.domain.routines.vos.RoutineDescription;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import atlas.domain.routines.vos.Unit;
import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.support.builders.RoutinesTestDatabase;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteRoutineRepositoryIT {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 2, 10);

    @TempDir
    private Path directory;

    private Connection connection;
    private SqliteRoutineUnitOfWork work;
    private java.util.List<DomainEvent> published;

    @BeforeEach
    void setUp() {
        connection = RoutinesTestDatabase.open(directory, CLOCK);
        published = new java.util.ArrayList<>();
        work = new SqliteRoutineUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(published::add)),
            new SqliteSequenceGenerator(connection));
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void shouldRoundTripARoutineWithEveryFieldSet() {
        var routine = work.execute(() -> {
            var defined = define(
                "Beber agua", "Dos litros al dia", new BigDecimal("1.5"), "L",
                Schedule.daily(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)).value());
            work.routines().create(defined);

            return defined;
        });

        var loaded = work.routines().get(routine.id()).orElseThrow();

        assertThat(loaded.name().value()).isEqualTo("Beber agua");
        assertThat(loaded.description()).map(RoutineDescription::value).contains("Dos litros al dia");
        assertThat(loaded.target().amount()).isEqualByComparingTo("1.5");
        assertThat(loaded.target().unit()).map(Unit::value).contains("L");
        assertThat(loaded.schedule().period()).isEqualTo(RecurrencePeriod.DAY);
        assertThat(loaded.schedule().activeDays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(loaded.isArchived()).isFalse();
    }

    @Test
    void shouldRoundTripARoutineWithoutDescriptionOrUnit() {
        var routine = givenRoutine();

        var loaded = work.routines().get(routine).orElseThrow();

        assertThat(loaded.description()).isEmpty();
        assertThat(loaded.target().unit()).isEmpty();
    }

    @Test
    void shouldRoundTripTheDaysOfMonthIncludingTheLastDayMarker() {
        var schedule = Schedule.create(RecurrencePeriod.MONTH, Set.of(), Set.of(1, Schedule.LAST_DAY)).value();
        var routine = work.execute(() -> {
            var defined = define("Cierre de mes", null, BigDecimal.ONE, null, schedule);
            work.routines().create(defined);

            return defined;
        });

        var loaded = work.routines().get(routine.id()).orElseThrow();

        assertThat(loaded.schedule().daysOfMonth()).containsExactlyInAnyOrder(0, 1);
        assertThat(loaded.occursOn(LocalDate.of(2026, 2, 28))).isTrue();
        assertThat(loaded.occursOn(LocalDate.of(2026, 2, 1))).isTrue();
        assertThat(loaded.occursOn(LocalDate.of(2026, 2, 15))).isFalse();
    }

    @Test
    void shouldNotConfuseAnEmptyDaySetWithTheLastDayMarker() {
        var routine = work.execute(() -> {
            var defined = define("Revision", null, BigDecimal.ONE, null, Schedule.over(RecurrencePeriod.MONTH).value());
            work.routines().create(defined);

            return defined;
        });

        assertThat(work.routines().get(routine.id()).orElseThrow().schedule().daysOfMonth()).isEmpty();
    }

    @Test
    void shouldKeepDecimalAmountsExact() {
        var routine = work.execute(() -> {
            var defined =
                define("Agua", null, new BigDecimal("0.1"), "L", Schedule.over(RecurrencePeriod.WEEK).value());
            work.routines().create(defined);

            return defined;
        });

        assertThat(work.routines().get(routine.id()).orElseThrow().target().amount())
            .isEqualTo(new BigDecimal("0.1"));
    }

    @Test
    void shouldPersistTheArchivedFlag() {
        var id = givenRoutine();
        work.run(() -> {
            var routine = work.routines().get(id).orElseThrow();
            routine.archive(NOW);
            work.routines().update(routine);
        });

        assertThat(work.routines().get(id).orElseThrow().isArchived()).isTrue();
    }

    @Test
    void shouldReturnEmptyForAnUnknownRoutine() {
        assertThat(work.routines().get(RoutineId.of(404))).isEmpty();
    }

    @Test
    void shouldHandOutConsecutiveIds() {
        var first = work.execute(() -> work.routines().nextId());
        var second = work.execute(() -> work.routines().nextId());

        assertThat(first).isEqualTo(RoutineId.of(1));
        assertThat(second).isEqualTo(RoutineId.of(2));
    }

    @Test
    void shouldReportZeroForASequenceNeverUsed() {
        assertThat(new SqliteSequenceGenerator(connection).current("nunca-usada")).isZero();
    }

    @Nested
    class Entries {

        @Test
        void shouldRoundTripAnEntry() {
            var id = givenRoutine();
            var entry = work.execute(() -> {
                var logged = entry(id, MON_09, "1.5");
                work.entries().create(logged);

                return logged;
            });

            var loaded = work.entries().find(id, MON_09).orElseThrow();

            assertThat(loaded.id()).isEqualTo(entry.id());
            assertThat(loaded.day()).isEqualTo(MON_09);
            assertThat(loaded.amount()).isEqualByComparingTo("1.5");
        }

        @Test
        void shouldReturnEmptyWhenThereIsNothingLoggedThatDay() {
            assertThat(work.entries().find(givenRoutine(), MON_09)).isEmpty();
        }

        @Test
        void shouldRejectASecondEntryForTheSameRoutineAndDay() {
            var id = givenRoutine();
            work.run(() -> work.entries().create(entry(id, MON_09, "1")));

            assertThatThrownBy(() -> work.run(() -> work.entries().create(entry(id, MON_09, "1"))))
                .isInstanceOf(RuntimeException.class);
        }

        @Test
        void shouldAllowTheSameDayInAnotherRoutine() {
            var first = givenRoutine();
            var second = givenRoutine();

            work.run(() -> work.entries().create(entry(first, MON_09, "1")));
            work.run(() -> work.entries().create(entry(second, MON_09, "1")));

            assertThat(work.entries().find(first, MON_09)).isPresent();
            assertThat(work.entries().find(second, MON_09)).isPresent();
        }

        @Test
        void shouldUpdateTheAmountOfAnExistingEntry() {
            var id = givenRoutine();
            var entry = work.execute(() -> {
                var logged = entry(id, MON_09, "1");
                work.entries().create(logged);

                return logged;
            });

            work.run(() -> {
                entry.add(new BigDecimal("2"), NOW);
                work.entries().update(entry);
            });

            assertThat(work.entries().find(id, MON_09).orElseThrow().amount()).isEqualByComparingTo("3");
        }

        @Test
        void shouldReadTheEntriesOfAHalfOpenWindow() {
            var id = givenRoutine();
            work.run(() -> {
                work.entries().create(entry(id, MON_09, "1"));
                work.entries().create(entry(id, TUE_10, "1"));
                work.entries().create(entry(id, LocalDate.of(2026, 2, 11), "1"));
            });

            var found = work.entries().findInWindow(id, MON_09, LocalDate.of(2026, 2, 11));

            assertThat(found).extracting(RoutineEntry::day).containsExactly(MON_09, TUE_10);
        }

        @Test
        void shouldWipeTheWholeHistoryOfOneRoutineOnly() {
            var kept = givenRoutine();
            var removed = givenRoutine();
            work.run(() -> {
                work.entries().create(entry(removed, MON_09, "1"));
                work.entries().create(entry(removed, TUE_10, "1"));
                work.entries().create(entry(kept, MON_09, "1"));
            });

            work.run(() -> work.entries().deleteAllOf(removed));

            assertThat(work.entries().findInWindow(removed, MON_09, TUE_10.plusDays(1))).isEmpty();
            assertThat(work.entries().findInWindow(kept, MON_09, TUE_10.plusDays(1))).hasSize(1);
        }
    }

    @Nested
    class Transactions {

        @Test
        void shouldPublishEventsAfterACommit() {
            givenRoutine();

            assertThat(published).hasExactlyElementsOfTypes(RoutineDefinedEvent.class);
        }

        @Test
        void shouldRollBackAndPublishNothingWhenTheWorkReturnsAFailure() {
            published.clear();

            Result<Void> result = work.execute(() -> {
                work.routines().create(define(
                    "No deberia quedar", null, BigDecimal.ONE, null, Schedule.over(RecurrencePeriod.WEEK).value()));

                return Result.failure(RoutineErrors.ROUTINE_IS_ARCHIVED);
            });

            assertThat(result.isFailure()).isTrue();
            assertThat(work.routines().getAll()).isEmpty();
            assertThat(published).isEmpty();
        }

        @Test
        void shouldRollBackAndRethrowWhenTheWorkBlowsUp() {
            published.clear();

            assertThatThrownBy(() -> work.run(() -> {
                work.routines().create(define(
                    "No deberia quedar", null, BigDecimal.ONE, null, Schedule.over(RecurrencePeriod.WEEK).value()));

                throw new IllegalStateException("boom");
            })).isInstanceOf(IllegalStateException.class);

            assertThat(work.routines().getAll()).isEmpty();
            assertThat(published).isEmpty();
        }
    }

    @Test
    void shouldBeSafeToOpenAnExistingDatabaseAgain() throws Exception {
        var id = givenRoutine();
        connection.close();

        connection = RoutinesTestDatabase.open(directory, CLOCK);
        work = new SqliteRoutineUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(published::add)),
            new SqliteSequenceGenerator(connection));

        assertThat(work.routines().get(id)).isPresent();
    }

    private RoutineId givenRoutine() {
        return work.execute(() -> {
            var routine = define("Rutina", null, BigDecimal.ONE, null, Schedule.over(RecurrencePeriod.WEEK).value());
            work.routines().create(routine);

            return routine.id();
        });
    }

    private Routine define(String name, String description, BigDecimal target, String unit, Schedule schedule) {
        return Routine
            .define(
                work.routines().nextId(),
                RoutineName.create(name).value(),
                RoutineDescription.create(description).value().orElse(null),
                Target.create(target, Unit.create(unit).value().orElse(null)).value(),
                schedule,
                NOW)
            .value();
    }

    private static RoutineEntry entry(RoutineId routineId, LocalDate day, String amount) {
        return RoutineEntry
            .log(RoutineEntryId.of(UUID.randomUUID()), routineId, day, new BigDecimal(amount), NOW)
            .value();
    }
}

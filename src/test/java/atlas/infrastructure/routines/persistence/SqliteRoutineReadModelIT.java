package atlas.infrastructure.routines.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineEntryId;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.routines.vos.Target;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.support.builders.RoutinesTestDatabase;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteRoutineReadModelIT {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);
    private static final LocalDate TUE_10 = LocalDate.of(2026, 2, 10);
    private static final LocalDate WED_11 = LocalDate.of(2026, 2, 11);

    @TempDir
    private Path directory;

    private Connection connection;
    private SqliteRoutineUnitOfWork work;
    private SqliteRoutineReadModel readModel;

    @BeforeEach
    void setUp() {
        connection = RoutinesTestDatabase.open(directory, CLOCK);
        work = new SqliteRoutineUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(event -> {})),
            new SqliteSequenceGenerator(connection));
        readModel = new SqliteRoutineReadModel(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void shouldFindARoutineById() {
        var id = givenRoutine("Correr");

        assertThat(readModel.find(id)).map(routine -> routine.name().value()).contains("Correr");
    }

    @Test
    void shouldReturnEmptyForAnUnknownRoutine() {
        assertThat(readModel.find(RoutineId.of(404))).isEmpty();
    }

    @Test
    void shouldHideArchivedRoutinesUnlessAskedFor() {
        var archived = givenRoutine("Archivada");
        givenRoutine("Viva");
        work.run(() -> {
            var routine = work.routines().get(archived).orElseThrow();
            routine.archive(NOW);
            work.routines().update(routine);
        });

        assertThat(readModel.findAll(false)).extracting(routine -> routine.name().value()).containsExactly("Viva");
        assertThat(readModel.findAll(true)).hasSize(2);
    }

    @Test
    void shouldSortRoutinesByName() {
        givenRoutine("Zumba");
        givenRoutine("Andar");

        assertThat(readModel.findAll(true))
            .extracting(routine -> routine.name().value())
            .containsExactly("Andar", "Zumba");
    }

    @Test
    void shouldFilterEntriesByAHalfOpenRange() {
        var id = givenRoutine("Correr");
        givenEntries(id, MON_09, TUE_10, WED_11);

        var found = readModel.findEntries(id, MON_09, WED_11);

        assertThat(found).extracting(RoutineEntry::day).containsExactly(MON_09, TUE_10);
    }

    @Test
    void shouldReadTheEntriesOfSeveralRoutinesAtOnce() {
        var first = givenRoutine("Primera");
        var second = givenRoutine("Segunda");
        var third = givenRoutine("Tercera");
        givenEntries(first, MON_09);
        givenEntries(second, TUE_10);
        givenEntries(third, WED_11);

        var found = readModel.findEntries(List.of(first, second), MON_09, WED_11.plusDays(1));

        assertThat(found).extracting(RoutineEntry::day).containsExactly(MON_09, TUE_10);
    }

    @Test
    void shouldReturnNothingWhenAskedForNoRoutines() {
        assertThat(readModel.findEntries(List.of(), MON_09, WED_11)).isEmpty();
    }

    @Test
    void shouldReadTheWholeHistoryOfARoutine() {
        var id = givenRoutine("Correr");
        givenEntries(id, MON_09, TUE_10, WED_11);

        assertThat(readModel.findAllEntries(id)).hasSize(3);
    }

    @Test
    void shouldSeeWhatTheOngoingTransactionHasWritten() {
        var id = givenRoutine("Correr");

        var seen = work.execute(() -> {
            work.entries().create(entry(id, MON_09));

            return readModel.findAllEntries(id);
        });

        assertThat(seen).hasSize(1);
    }

    private RoutineId givenRoutine(String name) {
        return work.execute(() -> {
            var routine = Routine
                .define(
                    work.routines().nextId(),
                    RoutineName.create(name).value(),
                    null,
                    Target.create(BigDecimal.ONE).value(),
                    Schedule.over(RecurrencePeriod.WEEK).value(),
                    NOW)
                .value();
            work.routines().create(routine);

            return routine.id();
        });
    }

    private void givenEntries(RoutineId routineId, LocalDate... days) {
        work.run(() -> {
            for (var day : days) {
                work.entries().create(entry(routineId, day));
            }
        });
    }

    private static RoutineEntry entry(RoutineId routineId, LocalDate day) {
        return RoutineEntry
            .log(RoutineEntryId.of(UUID.randomUUID()), routineId, day, BigDecimal.ONE, NOW)
            .value();
    }
}

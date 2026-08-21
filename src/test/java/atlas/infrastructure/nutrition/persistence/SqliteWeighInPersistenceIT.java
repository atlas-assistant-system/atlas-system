package atlas.infrastructure.nutrition.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import atlas.domain.nutrition.vos.Weight;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.support.builders.NutritionTestDatabase;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteWeighInPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-08-22T07:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate AUGUST_20 = LocalDate.of(2026, 8, 20);
    private static final LocalDate AUGUST_21 = LocalDate.of(2026, 8, 21);
    private static final LocalDate AUGUST_22 = LocalDate.of(2026, 8, 22);

    private Connection connection;
    private SqliteNutritionUnitOfWork unitOfWork;
    private SqliteWeighInReadModel readModel;

    @BeforeEach
    void openDatabase(@TempDir Path directory) {
        connection = NutritionTestDatabase.open(directory, CLOCK);
        unitOfWork = new SqliteNutritionUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            new SqliteSequenceGenerator(connection));
        readModel = new SqliteWeighInReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldReadBackEverythingItStored() {
        var id = record(82_400, AUGUST_22);

        var found = readModel.find(id).orElseThrow();

        assertThat(found.id()).isEqualTo(id);
        assertThat(found.weight()).isEqualTo(new Weight(82_400));
        assertThat(found.measuredOn()).isEqualTo(AUGUST_22);
        assertThat(found.recordedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldKeepTheGramExact() {
        var id = record(82_437, AUGUST_22);

        assertThat(readModel.find(id).orElseThrow().weight().toKilograms())
            .isEqualByComparingTo("82.437");
    }

    @Test
    void shouldReturnTheSeriesOldestFirst() {
        record(84_000, AUGUST_20);
        record(82_400, AUGUST_22);
        record(83_100, AUGUST_21);

        assertThat(readModel.findBetween(AUGUST_20, AUGUST_22))
            .extracting(WeighIn::measuredOn)
            .containsExactly(AUGUST_20, AUGUST_21, AUGUST_22);
    }

    @Test
    void shouldLeaveOutWhatFallsOutsideTheRange() {
        record(84_000, AUGUST_20);
        record(82_400, AUGUST_22);

        assertThat(readModel.findBetween(AUGUST_21, AUGUST_22)).hasSize(1);
    }

    @Test
    void shouldFindTheLatestReadingOfAll() {
        record(84_000, AUGUST_20);
        record(82_400, AUGUST_22);
        record(83_100, AUGUST_21);

        assertThat(readModel.findLatest().orElseThrow().measuredOn()).isEqualTo(AUGUST_22);
    }

    @Test
    void shouldFindNoLatestReadingBeforeAnyIsRecorded() {
        assertThat(readModel.findLatest()).isEmpty();
    }

    @Test
    void shouldFindTheReadingOfADayFromTheRepository() {
        record(82_400, AUGUST_22);

        assertThat(unitOfWork.execute(() -> unitOfWork.weighIns().findOn(AUGUST_22))).isPresent();
        assertThat(unitOfWork.execute(() -> unitOfWork.weighIns().findOn(AUGUST_21))).isEmpty();
    }

    @Test
    void shouldRefuseASecondReadingOnTheSameDay() {
        record(82_400, AUGUST_22);

        assertThatThrownBy(() -> record(82_100, AUGUST_22))
            .isInstanceOf(PersistenceException.class)
            .rootCause()
            .hasMessageContaining("UNIQUE constraint failed: weigh_ins.measured_on");
    }

    @Test
    void shouldPersistACorrection() {
        var id = record(82_400, AUGUST_22);

        unitOfWork.run(() -> {
            var weighIns = unitOfWork.weighIns();
            var weighIn = weighIns.get(id).orElseThrow();
            weighIn.correct(new Weight(82_100), NOW);
            weighIns.update(weighIn);
        });

        var found = readModel.find(id).orElseThrow();
        assertThat(found.weight()).isEqualTo(new Weight(82_100));
        assertThat(found.measuredOn()).isEqualTo(AUGUST_22);
    }

    @Test
    void shouldRemoveWhatItDeletes() {
        var id = record(82_400, AUGUST_22);

        unitOfWork.run(() -> {
            var weighIns = unitOfWork.weighIns();
            var weighIn = weighIns.get(id).orElseThrow();
            weighIn.delete(NOW);
            weighIns.delete(weighIn);
        });

        assertThat(readModel.find(id)).isEmpty();
    }

    private WeighInId record(int grams, LocalDate day) {
        return unitOfWork.execute(() -> {
            var weighIns = unitOfWork.weighIns();
            var weighIn = WeighIn.record(
                weighIns.nextId(), new Weight(grams), day, AUGUST_22, NOW).value();
            weighIns.create(weighIn);

            return weighIn.id();
        });
    }
}

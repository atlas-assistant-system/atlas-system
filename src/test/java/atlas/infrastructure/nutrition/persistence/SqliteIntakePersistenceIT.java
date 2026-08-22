package atlas.infrastructure.nutrition.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.nutrition.ports.DayConsumption;
import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.support.builders.NutritionTestDatabase;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteIntakePersistenceIT {

    private static final Instant NOW = Instant.parse("2026-08-22T13:45:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate AUGUST_20 = LocalDate.of(2026, 8, 20);
    private static final LocalDate AUGUST_21 = LocalDate.of(2026, 8, 21);
    private static final LocalDate AUGUST_22 = LocalDate.of(2026, 8, 22);

    private Connection connection;
    private SqliteNutritionUnitOfWork unitOfWork;
    private SqliteIntakeReadModel readModel;

    @BeforeEach
    void openDatabase(@TempDir Path directory) {
        connection = NutritionTestDatabase.open(directory, CLOCK);
        unitOfWork = new SqliteNutritionUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            new SqliteSequenceGenerator(connection));
        readModel = new SqliteIntakeReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldReadBackEverythingItStored() {
        var id = record(new Macros(30, 60, 10), "Tortilla y pan", AUGUST_22);

        var found = readModel.find(id).orElseThrow();

        assertThat(found.id()).isEqualTo(id);
        assertThat(found.macros()).isEqualTo(new Macros(30, 60, 10));
        assertThat(found.note()).map(IntakeNote::value).contains("Tortilla y pan");
        assertThat(found.consumedOn()).isEqualTo(AUGUST_22);
        assertThat(found.recordedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldKeepAnIntakeWithoutANote() {
        var id = record(new Macros(30, 60, 10), null, AUGUST_22);

        assertThat(readModel.find(id).orElseThrow().note()).isEmpty();
    }

    @Test
    void shouldReadBackTheCaloriesItWasGiven() {
        var id = record(new Macros(30, 60, 10), null, AUGUST_22);

        assertThat(readModel.find(id).orElseThrow().calories().kcal()).isEqualTo(450);
    }

    @Test
    void shouldFindOnlyTheIntakesOfTheDayAsked() {
        record(new Macros(30, 60, 10), "desayuno", AUGUST_22);
        record(new Macros(45, 80, 25), "comida", AUGUST_22);
        record(new Macros(20, 30, 5), "ayer", AUGUST_21);

        assertThat(readModel.findOn(AUGUST_22)).hasSize(2);
        assertThat(readModel.findOn(AUGUST_21)).hasSize(1);
        assertThat(readModel.findOn(AUGUST_20)).isEmpty();
    }

    @Test
    void shouldListTheRangeNewestFirst() {
        record(new Macros(20, 30, 5), null, AUGUST_20);
        record(new Macros(30, 60, 10), null, AUGUST_22);
        record(new Macros(45, 80, 25), null, AUGUST_21);

        var found = readModel.findBetween(AUGUST_20, AUGUST_22, 10);

        assertThat(found).extracting(Intake::consumedOn)
            .containsExactly(AUGUST_22, AUGUST_21, AUGUST_20);
    }

    @Test
    void shouldHonourTheLimitOfTheRange() {
        record(new Macros(20, 30, 5), null, AUGUST_20);
        record(new Macros(30, 60, 10), null, AUGUST_21);
        record(new Macros(45, 80, 25), null, AUGUST_22);

        assertThat(readModel.findBetween(AUGUST_20, AUGUST_22, 2)).hasSize(2);
    }

    @Test
    void shouldLeaveOutWhatFallsOutsideTheRange() {
        record(new Macros(20, 30, 5), null, AUGUST_20);
        record(new Macros(30, 60, 10), null, AUGUST_22);

        assertThat(readModel.findBetween(AUGUST_21, AUGUST_22, 10)).hasSize(1);
    }

    @Test
    void shouldAddUpEachDaySeparatelyInSql() {
        record(new Macros(30, 60, 10), null, AUGUST_22);
        record(new Macros(45, 80, 25), null, AUGUST_22);
        record(new Macros(20, 30, 5), null, AUGUST_21);

        var consumption = readModel.consumptionBetween(AUGUST_21, AUGUST_22);

        assertThat(consumption).hasSize(2);
        assertThat(macrosOn(consumption, AUGUST_22)).isEqualTo(new Macros(75, 140, 35));
        assertThat(macrosOn(consumption, AUGUST_21)).isEqualTo(new Macros(20, 30, 5));
    }

    @Test
    void shouldAddUpTheSameTotalsAsSummingTheRowsByHand() {
        record(new Macros(30, 60, 10), null, AUGUST_22);
        record(new Macros(45, 80, 25), null, AUGUST_22);
        record(new Macros(17, 3, 9), null, AUGUST_22);

        var byHand = readModel.findOn(AUGUST_22).stream()
            .map(Intake::calories)
            .reduce(Calories.NONE, Calories::plus);

        assertThat(caloriesOn(readModel.consumptionBetween(AUGUST_22, AUGUST_22), AUGUST_22))
            .isEqualTo(byHand);
    }

    @Test
    void shouldReportNoConsumptionForADayWithoutIntakes() {
        assertThat(readModel.consumptionBetween(AUGUST_20, AUGUST_22)).isEmpty();
    }

    @Test
    void shouldPersistACorrection() {
        var id = record(new Macros(30, 60, 10), "desayuno", AUGUST_22);

        unitOfWork.run(() -> {
            var intakes = unitOfWork.intakes();
            var intake = intakes.get(id).orElseThrow();
            intake.correct(new Calories(480), new Macros(35, 55, 12), Optional.empty(), NOW);
            intakes.update(intake);
        });

        var found = readModel.find(id).orElseThrow();
        assertThat(found.macros()).isEqualTo(new Macros(35, 55, 12));
        assertThat(found.note()).isEmpty();
        assertThat(found.consumedOn()).isEqualTo(AUGUST_22);
    }

    @Test
    void shouldRemoveWhatItDeletes() {
        var id = record(new Macros(30, 60, 10), null, AUGUST_22);

        unitOfWork.run(() -> {
            var intakes = unitOfWork.intakes();
            var intake = intakes.get(id).orElseThrow();
            intake.delete(NOW);
            intakes.delete(intake);
        });

        assertThat(readModel.find(id)).isEmpty();
    }

    private static Calories caloriesOn(List<DayConsumption> consumption, LocalDate date) {
        return consumption.stream()
            .filter(day -> day.date().equals(date))
            .map(DayConsumption::calories)
            .findFirst()
            .orElseThrow();
    }

    private static Macros macrosOn(List<DayConsumption> consumption, LocalDate date) {
        return consumption.stream()
            .filter(day -> day.date().equals(date))
            .map(DayConsumption::macros)
            .findFirst()
            .orElseThrow();
    }

    private IntakeId record(Macros macros, String note, LocalDate consumedOn) {
        return unitOfWork.execute(() -> {
            var intakes = unitOfWork.intakes();
            var intake = Intake.record(
                intakes.nextId(),
                new Calories(4 * macros.protein() + 4 * macros.carbs() + 9 * macros.fat()),
                macros, IntakeNote.create(note).value(), consumedOn, AUGUST_22, NOW)
                .value();
            intakes.create(intake);

            return intake.id();
        });
    }
}

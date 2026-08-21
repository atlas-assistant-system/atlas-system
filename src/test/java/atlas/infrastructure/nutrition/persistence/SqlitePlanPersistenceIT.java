package atlas.infrastructure.nutrition.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanId;
import atlas.domain.nutrition.enums.Goal;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.vos.Macros;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlitePlanPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-08-22T07:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate JULY_23 = LocalDate.of(2026, 7, 23);
    private static final LocalDate AUGUST_22 = LocalDate.of(2026, 8, 22);

    private Connection connection;
    private SqliteNutritionUnitOfWork unitOfWork;
    private SqlitePlanReadModel readModel;

    @BeforeEach
    void openDatabase(@TempDir Path directory) {
        connection = NutritionTestDatabase.open(directory, CLOCK);
        unitOfWork = new SqliteNutritionUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            new SqliteSequenceGenerator(connection));
        readModel = new SqlitePlanReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldReadBackEverythingItStored() {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);

        var found = readModel.findActive().orElseThrow();

        assertThat(found.startWeight()).isEqualTo(new Weight(84_000));
        assertThat(found.targetWeight()).isEqualTo(new Weight(78_000));
        assertThat(found.dailyMacros()).isEqualTo(new Macros(150, 200, 60));
        assertThat(found.status()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(found.startedOn()).isEqualTo(JULY_23);
        assertThat(found.definedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldDeriveTheGoalAndTheCaloriesOnTheWayBackOut() {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);

        var found = readModel.findActive().orElseThrow();

        assertThat(found.goal()).isEqualTo(Goal.LOSE);
        assertThat(found.dailyCalories().kcal()).isEqualTo(1_940);
    }

    @Test
    void shouldNotStoreTheGoalOrTheCalories() throws SQLException {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);

        assertThat(columnsOfPlans())
            .doesNotContain("goal", "calories", "daily_calories")
            .contains("protein_g", "carbs_g", "fat_g");
    }

    @Test
    void shouldFindNoActivePlanBeforeAnyIsDefined() {
        assertThat(readModel.findActive()).isEmpty();
    }

    @Test
    void shouldPersistAnAdjustment() {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);

        unitOfWork.run(() -> {
            var plan = unitOfWork.plans().findActive().orElseThrow();
            plan.adjust(new Macros(160, 180, 55), new Weight(76_000), NOW);
            unitOfWork.plans().update(plan);
        });

        var found = readModel.findActive().orElseThrow();
        assertThat(found.dailyMacros()).isEqualTo(new Macros(160, 180, 55));
        assertThat(found.targetWeight()).isEqualTo(new Weight(76_000));
        assertThat(found.startWeight()).isEqualTo(new Weight(84_000));
    }

    @Test
    void shouldLeaveNoActivePlanOnceItIsArchived() {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);

        archiveActive();

        assertThat(readModel.findActive()).isEmpty();
    }

    @Test
    void shouldKeepTheArchivedPlanAsHistory() {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);
        archiveActive();

        var archived = unitOfWork.execute(() -> unitOfWork.plans().get(PlanId.of(1)));

        assertThat(archived).isPresent();
        assertThat(archived.orElseThrow().status()).isEqualTo(PlanStatus.ARCHIVED);
    }

    @Test
    void shouldLetANewPlanBeDefinedAfterArchivingThePrevious() {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);
        archiveActive();

        define(78_000, 76_000, new Macros(160, 180, 55), AUGUST_22);

        assertThat(readModel.findActive().orElseThrow().startedOn()).isEqualTo(AUGUST_22);
    }

    @Test
    void shouldRefuseASecondActivePlan() {
        define(84_000, 78_000, new Macros(150, 200, 60), JULY_23);

        assertThatThrownBy(() -> define(78_000, 76_000, new Macros(160, 180, 55), AUGUST_22))
            .isInstanceOf(PersistenceException.class)
            .rootCause()
            .hasMessageContaining("UNIQUE constraint failed: plans.status");
    }

    private void define(int startGrams, int targetGrams, Macros macros, LocalDate startedOn) {
        unitOfWork.run(() -> {
            var plans = unitOfWork.plans();
            var plan = Plan.define(
                plans.nextId(), new Weight(startGrams), new Weight(targetGrams), macros,
                startedOn, AUGUST_22, NOW);
            plans.create(plan.value());
        });
    }

    private void archiveActive() {
        unitOfWork.run(() -> {
            var plan = unitOfWork.plans().findActive().orElseThrow();
            plan.archive(NOW);
            unitOfWork.plans().update(plan);
        });
    }

    private List<String> columnsOfPlans() throws SQLException {
        try (var statement = connection.prepareStatement("SELECT * FROM plans LIMIT 1");
            var rows = statement.executeQuery()) {
            var metaData = rows.getMetaData();
            var columns = new ArrayList<String>();

            for (var index = 1; index <= metaData.getColumnCount(); index++) {
                columns.add(metaData.getColumnName(index));
            }

            return columns;
        }
    }
}

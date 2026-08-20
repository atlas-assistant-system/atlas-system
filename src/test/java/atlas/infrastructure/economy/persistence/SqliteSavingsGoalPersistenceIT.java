package atlas.infrastructure.economy.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.economy.SavingsGoal;
import atlas.domain.economy.SavingsGoalId;
import atlas.domain.economy.vos.GoalName;
import atlas.domain.economy.vos.Money;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.support.builders.EconomyTestDatabase;
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

class SqliteSavingsGoalPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final LocalDate NEXT_FEBRUARY = LocalDate.of(2027, 2, 28);

    private Connection connection;
    private SqliteEconomyUnitOfWork unitOfWork;
    private SqliteSavingsGoalReadModel readModel;

    @BeforeEach
    void openDatabase(@TempDir Path directory) {
        connection = EconomyTestDatabase.open(directory, CLOCK);
        unitOfWork = new SqliteEconomyUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            new SqliteSequenceGenerator(connection));
        readModel = new SqliteSavingsGoalReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldReadBackTheGoalItStored() {
        var id = set("Viaje a Japon", 300000, NEXT_FEBRUARY);

        assertThat(readModel.findAll()).singleElement().satisfies(goal -> {
            assertThat(goal.id()).isEqualTo(id);
            assertThat(goal.name().value()).isEqualTo("Viaje a Japon");
            assertThat(goal.target()).isEqualTo(Money.ofCents(300000).value());
            assertThat(goal.deadline()).isEqualTo(NEXT_FEBRUARY);
        });
    }

    @Test
    void shouldHandOutIdsFromItsOwnSequence() {
        var first = set("Viaje", 300000, NEXT_FEBRUARY);
        var second = set("Coche", 900000, NEXT_FEBRUARY);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldReturnTheClosestDeadlineFirst() {
        set("Coche", 900000, LocalDate.of(2028, 1, 31));
        set("Viaje", 300000, NEXT_FEBRUARY);

        assertThat(readModel.findAll())
            .extracting(goal -> goal.name().value())
            .containsExactly("Viaje", "Coche");
    }

    @Test
    void shouldPersistAChange() {
        var id = set("Viaje a Japon", 300000, NEXT_FEBRUARY);
        var later = LocalDate.of(2027, 12, 31);

        unitOfWork.run(() -> {
            var goal = unitOfWork.goals().get(id).orElseThrow();
            goal.change(GoalName.create("Viaje a Corea").value(), Money.ofCents(500000).value(), later, TODAY, NOW);
            unitOfWork.goals().update(goal);
        });

        assertThat(readModel.findAll()).singleElement().satisfies(goal -> {
            assertThat(goal.name().value()).isEqualTo("Viaje a Corea");
            assertThat(goal.target()).isEqualTo(Money.ofCents(500000).value());
            assertThat(goal.deadline()).isEqualTo(later);
        });
    }

    @Test
    void shouldLeaveNothingBehindWhenAGoalIsAbandoned() {
        var id = set("Viaje a Japon", 300000, NEXT_FEBRUARY);

        unitOfWork.run(() -> {
            var goal = unitOfWork.goals().get(id).orElseThrow();
            goal.abandon(NOW);
            unitOfWork.goals().delete(goal);
        });

        assertThat(readModel.findAll()).isEmpty();
    }

    private SavingsGoalId set(String name, long targetCents, LocalDate deadline) {
        return unitOfWork.execute(() -> {
            var goals = unitOfWork.goals();
            var goal = SavingsGoal.set(
                goals.nextId(), GoalName.create(name).value(), Money.ofCents(targetCents).value(),
                deadline, TODAY, NOW).value();
            goals.create(goal);

            return goal.id();
        });
    }
}

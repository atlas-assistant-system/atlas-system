package atlas.infrastructure.economy.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.economy.Budget;
import atlas.domain.economy.BudgetId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.vos.Money;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.support.builders.EconomyTestDatabase;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteBudgetPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private Connection connection;
    private SqliteEconomyUnitOfWork unitOfWork;
    private SqliteBudgetReadModel readModel;

    @BeforeEach
    void openDatabase(@TempDir Path directory) {
        connection = EconomyTestDatabase.open(directory, CLOCK);
        unitOfWork = new SqliteEconomyUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            new SqliteSequenceGenerator(connection));
        readModel = new SqliteBudgetReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldReadBackTheBudgetItStored() {
        var id = define(Category.FOOD, 30000);

        var found = readModel.findAll();

        assertThat(found).singleElement().satisfies(budget -> {
            assertThat(budget.id()).isEqualTo(id);
            assertThat(budget.category()).isEqualTo(Category.FOOD);
            assertThat(budget.limit()).isEqualTo(Money.ofCents(30000).value());
        });
    }

    @Test
    void shouldHandOutIdsFromItsOwnSequence() {
        var first = define(Category.FOOD, 30000);
        var second = define(Category.LEISURE, 10000);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldPersistANewLimit() {
        var id = define(Category.FOOD, 30000);

        unitOfWork.run(() -> {
            var budget = unitOfWork.budgets().get(id).orElseThrow();
            budget.changeLimit(Money.ofCents(50000).value(), NOW);
            unitOfWork.budgets().update(budget);
        });

        assertThat(readModel.findAll().getFirst().limit()).isEqualTo(Money.ofCents(50000).value());
    }

    @Test
    void shouldLeaveNothingBehindWhenABudgetIsRemoved() {
        var id = define(Category.FOOD, 30000);

        unitOfWork.run(() -> {
            var budget = unitOfWork.budgets().get(id).orElseThrow();
            budget.remove(NOW);
            unitOfWork.budgets().delete(budget);
        });

        assertThat(readModel.findAll()).isEmpty();
    }

    @Test
    void shouldKnowWhetherACategoryIsAlreadyBudgeted() {
        define(Category.FOOD, 30000);

        assertThat(unitOfWork.budgets().existsFor(Category.FOOD)).isTrue();
        assertThat(unitOfWork.budgets().existsFor(Category.LEISURE)).isFalse();
    }

    @Test
    void shouldRefuseTwoBudgetsForTheSameCategory() {
        define(Category.FOOD, 30000);

        assertThatThrownBy(() -> define(Category.FOOD, 40000)).isInstanceOf(RuntimeException.class);
    }

    private BudgetId define(Category category, long limitCents) {
        return unitOfWork.execute(() -> {
            var budgets = unitOfWork.budgets();
            var budget = Budget.define(
                budgets.nextId(), category, Money.ofCents(limitCents).value(), NOW).value();
            budgets.create(budget);

            return budget.id();
        });
    }
}

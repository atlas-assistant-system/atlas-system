package atlas.infrastructure.economy.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.MovementNote;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.support.builders.EconomyTestDatabase;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteMovementPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate AUGUST_5 = LocalDate.of(2026, 8, 5);
    private static final LocalDate AUGUST_20 = LocalDate.of(2026, 8, 20);
    private static final LocalDate AUGUST_FIRST = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUGUST_LAST = LocalDate.of(2026, 8, 31);

    private Connection connection;
    private SqliteMovementUnitOfWork unitOfWork;
    private SqliteMovementReadModel readModel;

    @BeforeEach
    void openDatabase(@TempDir Path directory) {
        connection = EconomyTestDatabase.open(directory, CLOCK);
        unitOfWork = new SqliteMovementUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            new SqliteSequenceGenerator(connection));
        readModel = new SqliteMovementReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldReadBackEverythingItStored() {
        var id = record(MovementKind.EXPENSE, 1250, Category.FOOD, "cena con Ana", AUGUST_5);

        var found = readModel.find(id).orElseThrow();

        assertThat(found.id()).isEqualTo(id);
        assertThat(found.kind()).isEqualTo(MovementKind.EXPENSE);
        assertThat(found.amount()).isEqualTo(Money.ofCents(1250).value());
        assertThat(found.category()).isEqualTo(Category.FOOD);
        assertThat(found.note()).map(MovementNote::value).contains("cena con Ana");
        assertThat(found.occurredOn()).isEqualTo(AUGUST_5);
        assertThat(found.recordedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldStoreAnExpenseAsANegativeAmount() throws SQLException {
        var id = record(MovementKind.EXPENSE, 1250, Category.FOOD, null, AUGUST_5);

        assertThat(storedCentsOf(id)).isEqualTo(-1250);
    }

    @Test
    void shouldStoreAnIncomeAsAPositiveAmount() throws SQLException {
        var id = record(MovementKind.INCOME, 200000, Category.INCOME, null, AUGUST_5);

        assertThat(storedCentsOf(id)).isEqualTo(200000);
    }

    @Test
    void shouldKeepAMovementWithoutANote() {
        var id = record(MovementKind.EXPENSE, 1250, Category.FOOD, null, AUGUST_5);

        assertThat(readModel.find(id).orElseThrow().note()).isEmpty();
    }

    @Test
    void shouldPersistACorrection() {
        var id = record(MovementKind.EXPENSE, 1250, Category.FOOD, "cena", AUGUST_5);

        unitOfWork.run(() -> {
            var movement = unitOfWork.movements().get(id).orElseThrow();
            movement.correct(Money.ofCents(2000).value(), Optional.empty(), AUGUST_20, AUGUST_20, NOW);
            unitOfWork.movements().update(movement);
        });

        var found = readModel.find(id).orElseThrow();

        assertThat(found.amount()).isEqualTo(Money.ofCents(2000).value());
        assertThat(found.note()).isEmpty();
        assertThat(found.occurredOn()).isEqualTo(AUGUST_20);
    }

    @Test
    void shouldPersistARecategorization() {
        var id = record(MovementKind.EXPENSE, 1250, Category.FOOD, null, AUGUST_5);

        unitOfWork.run(() -> {
            var movement = unitOfWork.movements().get(id).orElseThrow();
            movement.recategorize(Category.LEISURE, NOW);
            unitOfWork.movements().update(movement);
        });

        assertThat(readModel.find(id).orElseThrow().category()).isEqualTo(Category.LEISURE);
    }

    @Test
    void shouldLeaveNothingBehindWhenAMovementIsDeleted() {
        var id = record(MovementKind.EXPENSE, 1250, Category.FOOD, null, AUGUST_5);

        unitOfWork.run(() -> {
            var movement = unitOfWork.movements().get(id).orElseThrow();
            movement.delete(NOW);
            unitOfWork.movements().delete(movement);
        });

        assertThat(readModel.find(id)).isEmpty();
    }

    @Test
    void shouldHandOutADifferentIdEachTime() {
        var first = record(MovementKind.EXPENSE, 100, Category.FOOD, null, AUGUST_5);
        var second = record(MovementKind.EXPENSE, 100, Category.FOOD, null, AUGUST_5);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldAddUpAmountsToTheExactCent() {
        record(MovementKind.INCOME, 200000, Category.INCOME, null, AUGUST_5);
        record(MovementKind.EXPENSE, 1013, Category.FOOD, null, AUGUST_5);
        record(MovementKind.EXPENSE, 2037, Category.LEISURE, null, AUGUST_20);
        record(MovementKind.EXPENSE, 1, Category.OTHER, null, AUGUST_20);

        var balance = readModel.balanceBetween(AUGUST_FIRST, AUGUST_LAST);

        assertThat(balance.incomeCents()).isEqualTo(200000);
        assertThat(balance.expenseCents()).isEqualTo(3051);
    }

    @Test
    void shouldLeaveTheBalanceAtZeroWhenIncomeAndSpendingMatch() {
        record(MovementKind.INCOME, 4999, Category.INCOME, null, AUGUST_5);
        record(MovementKind.EXPENSE, 4999, Category.FOOD, null, AUGUST_5);

        var balance = readModel.balanceBetween(AUGUST_FIRST, AUGUST_LAST);

        assertThat(balance.incomeCents() - balance.expenseCents()).isZero();
    }

    @Test
    void shouldReportZeroForAPeriodWithoutMovements() {
        var balance = readModel.balanceBetween(AUGUST_FIRST, AUGUST_LAST);

        assertThat(balance.incomeCents()).isZero();
        assertThat(balance.expenseCents()).isZero();
    }

    @Test
    void shouldLeaveOutMovementsOutsideThePeriod() {
        record(MovementKind.EXPENSE, 1000, Category.FOOD, null, LocalDate.of(2026, 7, 31));
        record(MovementKind.EXPENSE, 2000, Category.FOOD, null, AUGUST_FIRST);
        record(MovementKind.EXPENSE, 4000, Category.FOOD, null, AUGUST_LAST);
        record(MovementKind.EXPENSE, 8000, Category.FOOD, null, LocalDate.of(2026, 9, 1));

        assertThat(readModel.balanceBetween(AUGUST_FIRST, AUGUST_LAST).expenseCents()).isEqualTo(6000);
    }

    @Test
    void shouldGroupSpendingByCategory() {
        record(MovementKind.EXPENSE, 5000, Category.FOOD, null, AUGUST_5);
        record(MovementKind.EXPENSE, 2500, Category.FOOD, null, AUGUST_20);
        record(MovementKind.EXPENSE, 2500, Category.LEISURE, null, AUGUST_20);
        record(MovementKind.INCOME, 200000, Category.INCOME, null, AUGUST_5);

        var spending = readModel.spendingBetween(AUGUST_FIRST, AUGUST_LAST);

        assertThat(spending).hasSize(2);
        assertThat(spending)
            .filteredOn(spend -> spend.category() == Category.FOOD)
            .singleElement()
            .satisfies(spend -> assertThat(spend.cents()).isEqualTo(7500));
    }

    @Test
    void shouldListTheMostRecentMovementsFirst() {
        record(MovementKind.EXPENSE, 1000, Category.FOOD, null, AUGUST_5);
        record(MovementKind.EXPENSE, 2000, Category.FOOD, null, AUGUST_20);

        var latest = readModel.findLatest(null, null, null, 10);

        assertThat(latest).extracting(movement -> movement.occurredOn())
            .containsExactly(AUGUST_20, AUGUST_5);
    }

    @Test
    void shouldReturnNoMoreMovementsThanAskedFor() {
        record(MovementKind.EXPENSE, 1000, Category.FOOD, null, AUGUST_5);
        record(MovementKind.EXPENSE, 2000, Category.FOOD, null, AUGUST_20);

        assertThat(readModel.findLatest(null, null, null, 1)).hasSize(1);
    }

    @Test
    void shouldListOnlyTheCategoryItWasAskedFor() {
        record(MovementKind.EXPENSE, 1000, Category.FOOD, null, AUGUST_5);
        record(MovementKind.EXPENSE, 2000, Category.LEISURE, null, AUGUST_20);

        var latest = readModel.findLatest(null, null, Category.LEISURE, 10);

        assertThat(latest).singleElement()
            .satisfies(movement -> assertThat(movement.category()).isEqualTo(Category.LEISURE));
    }

    @Test
    void shouldListOnlyMovementsInsideThePeriod() {
        record(MovementKind.EXPENSE, 1000, Category.FOOD, null, LocalDate.of(2026, 7, 31));
        record(MovementKind.EXPENSE, 2000, Category.FOOD, null, AUGUST_5);

        assertThat(readModel.findLatest(AUGUST_FIRST, AUGUST_LAST, null, 10)).hasSize(1);
    }

    private MovementId record(MovementKind kind, long cents, Category category, String note, LocalDate day) {
        return unitOfWork.execute(() -> {
            var movements = unitOfWork.movements();
            var movement = Movement.record(
                movements.nextId(), kind, Money.ofCents(cents).value(), category,
                MovementNote.create(note).value(), day, day, NOW).value();
            movements.create(movement);

            return movement.id();
        });
    }

    private long storedCentsOf(MovementId id) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT amount_cents FROM movements WHERE id = ?")) {
            statement.setLong(1, id.value());

            try (var rows = statement.executeQuery()) {
                rows.next();

                return rows.getLong(1);
            }
        }
    }
}

package atlas.app.economy;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.economy.dto.BudgetStatusDto;
import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.dto.SavingsGoalStatusDto;
import atlas.application.economy.queries.getbalance.GetBalanceQuery;
import atlas.application.economy.queries.listbudgets.ListBudgetsQuery;
import atlas.application.economy.queries.listmovements.ListMovementsQuery;
import atlas.application.economy.queries.listmovements.ListMovementsQueryHandler;
import atlas.application.economy.queries.listsavingsgoals.ListSavingsGoalsQuery;
import atlas.infrastructure.sharedkernel.logging.LogEntryRenderers;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EconomySeederTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T09:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 21);

    @Test
    void shouldFillAnEmptyDatabaseWithSomethingToLookAt(@TempDir Path directory) {
        var summary = EconomySeeder.seed(directory, CLOCK);

        assertThat(summary.movements()).isPositive();
        assertThat(summary.budgets()).isPositive();
        assertThat(summary.goals()).isPositive();
        assertThat(summary.alreadyPopulated()).isFalse();
    }

    @Test
    void shouldStayUnderTheLimitOfASingleListing() {
        assertThat(EconomySeeder.MAX_MOVEMENTS).isLessThan(ListMovementsQueryHandler.MAX_LIMIT);
    }

    @Test
    void shouldReachBackFarEnoughForTheSavingsProjection(@TempDir Path directory) {
        EconomySeeder.seed(directory, CLOCK);

        var oldest = movements(directory).getLast().occurredOn();

        assertThat(oldest).isBeforeOrEqualTo(TODAY.withDayOfMonth(1).minusMonths(6));
    }

    @Test
    void shouldNeverDateAMovementInTheFuture(@TempDir Path directory) {
        EconomySeeder.seed(directory, CLOCK);

        assertThat(movements(directory))
            .allSatisfy(movement -> assertThat(movement.occurredOn()).isBeforeOrEqualTo(TODAY));
    }

    @Test
    void shouldLeaveMoreComingInThanGoingOut(@TempDir Path directory) {
        EconomySeeder.seed(directory, CLOCK);

        var balance = read(directory, application -> application.queries()
            .dispatch(new GetBalanceQuery(TODAY.minusMonths(8), TODAY)).value());

        assertThat(balance.net()).isPositive();
    }

    @Test
    void shouldGiveEveryBudgetSomeSpendingToWatch(@TempDir Path directory) {
        EconomySeeder.seed(directory, CLOCK);

        List<BudgetStatusDto> budgets = read(directory, application -> application.queries()
            .dispatch(new ListBudgetsQuery()).value());

        assertThat(budgets).isNotEmpty().allSatisfy(budget -> assertThat(budget.spent()).isPositive());
    }

    @Test
    void shouldSetAGoalThatIsStillAhead(@TempDir Path directory) {
        EconomySeeder.seed(directory, CLOCK);

        List<SavingsGoalStatusDto> goals = read(directory, application -> application.queries()
            .dispatch(new ListSavingsGoalsQuery()).value());

        assertThat(goals).isNotEmpty().allSatisfy(goal -> assertThat(goal.deadline()).isAfter(TODAY));
    }

    @Test
    void shouldProduceTheSameDataEveryRun(@TempDir Path first, @TempDir Path second) {
        EconomySeeder.seed(first, CLOCK);
        EconomySeeder.seed(second, CLOCK);

        assertThat(movements(first)).isEqualTo(movements(second));
    }

    @Test
    void shouldRefuseToSeedADatabaseThatAlreadyHasMovements(@TempDir Path directory) {
        var first = EconomySeeder.seed(directory, CLOCK);

        var second = EconomySeeder.seed(directory, CLOCK);

        assertThat(second.alreadyPopulated()).isTrue();
        assertThat(second.movements()).isZero();
        assertThat(movements(directory)).hasSize(first.movements());
    }

    private static List<MovementDto> movements(Path directory) {
        return read(directory, application -> application.queries()
            .dispatch(new ListMovementsQuery(null, null, null, ListMovementsQueryHandler.MAX_LIMIT))
            .value());
    }

    private static <T> T read(Path directory, Function<EconomyApplication, T> work) {
        var application = EconomyApplication.wire(
            LogEntryRenderers.forConsole(false, ZoneOffset.UTC), directory, CLOCK);
        try {
            return work.apply(application);
        } finally {
            application.stop();
        }
    }
}

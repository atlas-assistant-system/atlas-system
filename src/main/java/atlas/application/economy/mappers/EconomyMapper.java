package atlas.application.economy.mappers;

import atlas.application.economy.dto.BalanceDto;
import atlas.application.economy.dto.BudgetDto;
import atlas.application.economy.dto.BudgetStatusDto;
import atlas.application.economy.dto.CategorySpendDto;
import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.ports.MovementReadModel.Balance;
import atlas.application.economy.ports.MovementReadModel.CategorySpend;
import atlas.application.economy.queries.Period;
import atlas.domain.economy.Budget;
import atlas.domain.economy.Movement;
import atlas.domain.economy.vos.MovementNote;
import atlas.domain.economy.vos.Pace;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class EconomyMapper {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private static final int CENT_SCALE = 2;
    private static final int PERCENTAGE_SCALE = 1;

    private EconomyMapper() {}

    public static MovementDto toDto(Movement movement) {
        var category = movement.category();

        return new MovementDto(
            movement.id().toString(),
            movement.kind().name(),
            movement.amount().toEuros(),
            category.name(),
            category.label(),
            category.icon(),
            movement.note().map(MovementNote::value).orElse(null),
            movement.occurredOn(),
            movement.recordedAt());
    }

    public static List<MovementDto> toDtos(Collection<Movement> movements) {
        return movements.stream().map(EconomyMapper::toDto).toList();
    }

    public static BalanceDto toDto(Period period, Balance balance) {
        return new BalanceDto(
            period.from(),
            period.to(),
            euros(balance.incomeCents()),
            euros(balance.expenseCents()),
            euros(balance.incomeCents() - balance.expenseCents()));
    }

    public static List<CategorySpendDto> toBreakdown(Collection<CategorySpend> spending) {
        var total = spending.stream().mapToLong(CategorySpend::cents).sum();

        return spending.stream()
            .sorted(Comparator.comparingLong(CategorySpend::cents).reversed())
            .map(spend -> toDto(spend, total))
            .toList();
    }

    private static CategorySpendDto toDto(CategorySpend spend, long total) {
        var category = spend.category();

        return new CategorySpendDto(
            category.name(),
            category.label(),
            category.icon(),
            euros(spend.cents()),
            shareOf(spend.cents(), total));
    }

    private static BigDecimal shareOf(long cents, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE);
        }

        return BigDecimal.valueOf(cents)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(total), PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    public static BudgetDto toDto(Budget budget) {
        var category = budget.category();

        return new BudgetDto(
            budget.id().toString(),
            category.name(),
            category.label(),
            category.icon(),
            budget.limit().toEuros());
    }

    public static List<BudgetStatusDto> toBudgets(Collection<Budget> budgets, Function<Budget, Pace> paceOf) {
        return budgets.stream()
            .map(budget -> toStatus(budget, paceOf.apply(budget)))
            .sorted(Comparator.comparingDouble(EconomyMapper::commitment).reversed())
            .toList();
    }

    private static BudgetStatusDto toStatus(Budget budget, Pace pace) {
        var category = budget.category();

        return new BudgetStatusDto(
            budget.id().toString(),
            category.name(),
            category.label(),
            category.icon(),
            euros(pace.limitCents()),
            euros(pace.spentCents()),
            euros(pace.projectedCents()),
            pace.status().name());
    }

    private static double commitment(BudgetStatusDto budget) {
        return budget.spent().doubleValue() / budget.limit().doubleValue();
    }

    private static BigDecimal euros(long cents) {
        return BigDecimal.valueOf(cents, CENT_SCALE);
    }
}

package atlas.application.economy.mappers;

import atlas.application.economy.dto.BalanceDto;
import atlas.application.economy.dto.CategorySpendDto;
import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.ports.MovementReadModel.Balance;
import atlas.application.economy.ports.MovementReadModel.CategorySpend;
import atlas.application.economy.queries.Period;
import atlas.domain.economy.Movement;
import atlas.domain.economy.vos.MovementNote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

    private static BigDecimal euros(long cents) {
        return BigDecimal.valueOf(cents, CENT_SCALE);
    }
}

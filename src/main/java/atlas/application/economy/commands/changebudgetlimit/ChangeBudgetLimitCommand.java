package atlas.application.economy.commands.changebudgetlimit;

import atlas.application.economy.dto.BudgetDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.BudgetId;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;

public record ChangeBudgetLimitCommand(BudgetId budgetId, BigDecimal limit) implements Command<Result<BudgetDto>> {}

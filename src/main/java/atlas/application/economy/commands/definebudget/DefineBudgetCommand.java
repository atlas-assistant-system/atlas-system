package atlas.application.economy.commands.definebudget;

import atlas.application.economy.dto.BudgetDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.enums.Category;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;

public record DefineBudgetCommand(Category category, BigDecimal limit) implements Command<Result<BudgetDto>> {}

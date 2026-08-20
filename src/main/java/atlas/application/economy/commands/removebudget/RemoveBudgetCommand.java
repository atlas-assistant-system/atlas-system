package atlas.application.economy.commands.removebudget;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.BudgetId;
import atlas.domain.sharedkernel.results.Result;

public record RemoveBudgetCommand(BudgetId budgetId) implements Command<Result<Void>> {}

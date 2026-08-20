package atlas.application.economy.commands.abandonsavingsgoal;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.SavingsGoalId;
import atlas.domain.sharedkernel.results.Result;

public record AbandonSavingsGoalCommand(SavingsGoalId goalId) implements Command<Result<Void>> {}

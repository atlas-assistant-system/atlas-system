package atlas.application.economy.commands.changesavingsgoal;

import atlas.application.economy.dto.SavingsGoalDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.economy.SavingsGoalId;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ChangeSavingsGoalCommand(SavingsGoalId goalId, String name, BigDecimal target, LocalDate deadline)
    implements Command<Result<SavingsGoalDto>> {}

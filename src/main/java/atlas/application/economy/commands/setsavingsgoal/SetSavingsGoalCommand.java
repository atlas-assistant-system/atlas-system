package atlas.application.economy.commands.setsavingsgoal;

import atlas.application.economy.dto.SavingsGoalDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SetSavingsGoalCommand(String name, BigDecimal target, LocalDate deadline)
    implements Command<Result<SavingsGoalDto>> {}

package atlas.application.nutrition.commands.defineplan;

import atlas.application.nutrition.dto.PlanDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DefinePlanCommand(
    BigDecimal startWeight,
    BigDecimal targetWeight,
    int protein,
    int carbs,
    int fat,
    LocalDate startedOn) implements Command<Result<PlanDto>> {}

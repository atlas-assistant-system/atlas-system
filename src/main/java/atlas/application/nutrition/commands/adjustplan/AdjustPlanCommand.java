package atlas.application.nutrition.commands.adjustplan;

import atlas.application.nutrition.dto.PlanDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;

public record AdjustPlanCommand(
    BigDecimal targetWeight,
    int calories,
    int protein,
    int carbs,
    int fat) implements Command<Result<PlanDto>> {}

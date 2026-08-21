package atlas.application.nutrition.commands.correctweighin;

import atlas.application.nutrition.dto.WeighInDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.nutrition.WeighInId;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;

public record CorrectWeighInCommand(WeighInId weighInId, BigDecimal weight)
    implements Command<Result<WeighInDto>> {}

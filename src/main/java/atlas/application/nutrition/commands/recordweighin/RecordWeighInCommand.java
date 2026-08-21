package atlas.application.nutrition.commands.recordweighin;

import atlas.application.nutrition.dto.WeighInDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordWeighInCommand(BigDecimal weight, LocalDate measuredOn)
    implements Command<Result<WeighInDto>> {}

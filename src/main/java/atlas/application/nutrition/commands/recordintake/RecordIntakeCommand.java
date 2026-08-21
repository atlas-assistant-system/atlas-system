package atlas.application.nutrition.commands.recordintake;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;

public record RecordIntakeCommand(
    int protein,
    int carbs,
    int fat,
    String note,
    LocalDate consumedOn) implements Command<Result<IntakeDto>> {}

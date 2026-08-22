package atlas.application.training.commands.defineexercise;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.application.training.dto.ExerciseDto;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.enums.Metric;

public record DefineExerciseCommand(String name, Metric metric) implements Command<Result<ExerciseDto>> {}

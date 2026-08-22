package atlas.application.training.commands;

import atlas.domain.training.ExerciseId;

public record PlannedLineInput(ExerciseId exerciseId, int sets, EffortInput target) {}

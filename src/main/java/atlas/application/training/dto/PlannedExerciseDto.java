package atlas.application.training.dto;

public record PlannedExerciseDto(
    String id, String exerciseId, int position, int sets, EffortDto target) {}

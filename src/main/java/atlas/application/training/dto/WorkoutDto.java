package atlas.application.training.dto;

import java.util.List;

public record WorkoutDto(
    String id, String name, List<String> days, boolean archived, List<PlannedExerciseDto> plan) {}

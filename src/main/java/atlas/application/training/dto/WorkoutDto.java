package atlas.application.training.dto;

import java.util.List;

public record WorkoutDto(String id, String name, boolean archived, List<PlannedExerciseDto> plan) {}

package atlas.application.training.dto;

import java.util.List;

/** {@code days} son nombres de {@code DayOfWeek} en orden de lunes a domingo, vacío si no toca ningún día fijo. */
public record WorkoutDto(
    String id, String name, List<String> days, boolean archived, List<PlannedExerciseDto> plan) {}

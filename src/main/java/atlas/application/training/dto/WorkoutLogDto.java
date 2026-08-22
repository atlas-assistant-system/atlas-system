package atlas.application.training.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutLogDto(
    String id,
    String workoutId,
    LocalDate performedOn,
    Instant startedAt,
    List<SetLogDto> sets) {}

package atlas.presentation.training.responses;

import atlas.application.training.dto.EffortDto;
import atlas.application.training.dto.ExerciseDto;
import atlas.application.training.dto.PlannedExerciseDto;
import atlas.application.training.dto.SetLogDto;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.dto.WorkoutLogDto;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TrainingResponses {

    private TrainingResponses() {}

    public static Map<String, Object> exercise(ExerciseDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("name", dto.name());
        body.put("metric", dto.metric());
        body.put("metricLabel", dto.metricLabel());
        body.put("archived", dto.archived());

        return body;
    }

    public static List<Map<String, Object>> exercises(List<ExerciseDto> exercises) {
        return exercises.stream().map(TrainingResponses::exercise).toList();
    }

    public static Map<String, Object> workout(WorkoutDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("name", dto.name());
        body.put("days", dto.days());
        body.put("archived", dto.archived());
        body.put("plan", dto.plan().stream().map(TrainingResponses::line).toList());

        return body;
    }

    public static List<Map<String, Object>> workouts(List<WorkoutDto> workouts) {
        return workouts.stream().map(TrainingResponses::workout).toList();
    }

    public static Map<String, Object> log(WorkoutLogDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("workoutId", dto.workoutId());
        body.put("performedOn", dto.performedOn().toString());
        body.put("startedAt", dto.startedAt().toString());
        body.put("sets", dto.sets().stream().map(TrainingResponses::set).toList());

        return body;
    }

    public static List<Map<String, Object>> logs(List<WorkoutLogDto> logs) {
        return logs.stream().map(TrainingResponses::log).toList();
    }

    private static Map<String, Object> line(PlannedExerciseDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("exerciseId", dto.exerciseId());
        body.put("position", dto.position());
        body.put("sets", dto.sets());
        body.put("target", effort(dto.target()));

        return body;
    }

    private static Map<String, Object> set(SetLogDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("exerciseId", dto.exerciseId());
        body.put("position", dto.position());
        body.put("planned", effort(dto.planned()));
        body.put("actual", effort(dto.actual()));

        return body;
    }

    private static Map<String, Object> effort(EffortDto dto) {
        if (dto == null) {
            return null;
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("load", kilograms(dto.load()));
        body.put("reps", dto.reps());
        body.put("seconds", dto.seconds());
        body.put("meters", dto.meters());

        return body;
    }

    private static String kilograms(BigDecimal load) {
        return load.stripTrailingZeros().toPlainString();
    }
}

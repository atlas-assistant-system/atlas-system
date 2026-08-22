package atlas.presentation.training.requests;

import atlas.application.training.commands.EffortInput;
import atlas.application.training.commands.PlannedLineInput;
import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.training.ExerciseId;
import atlas.presentation.training.web.Values;
import java.util.List;
import java.util.Map;

/**
 * La traduccion de JSON a comandos. Un solo sitio para los seis cuerpos que acepta el
 * contexto: son variaciones de las mismas cuatro medidas y separarlos en seis records de
 * cuatro lineas seria mas ficheros para el mismo trabajo.
 */
public final class TrainingRequests {

    private TrainingRequests() {}

    public static EffortInput effort(Map<String, Object> body) {
        return new EffortInput(
            Values.kilograms(body, "load"),
            Values.count(body, "reps"),
            Values.count(body, "seconds"),
            Values.count(body, "meters"));
    }

    public static List<PlannedLineInput> plan(Map<String, Object> body) {
        return Values.rows(body, "plan").stream().map(TrainingRequests::line).toList();
    }

    private static PlannedLineInput line(Map<String, Object> row) {
        var exerciseId = Values.text(row, "exerciseId");
        if (exerciseId == null) {
            throw new FormatException("'exerciseId' is required for every line of the plan.");
        }

        return new PlannedLineInput(
            ExerciseId.parse(exerciseId), Values.count(row, "sets"), effort(target(row)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> target(Map<String, Object> row) {
        var value = row.get("target");
        if (value == null) {
            return Map.of();
        }

        if (!(value instanceof Map<?, ?>)) {
            throw new FormatException("'target' must be an object.");
        }

        return (Map<String, Object>) value;
    }
}

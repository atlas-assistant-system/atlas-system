package atlas.app;

import atlas.app.appointments.AppointmentsApplication;
import atlas.app.nutrition.NutritionApplication;
import atlas.app.routines.RoutinesApplication;
import atlas.app.training.TrainingApplication;
import atlas.application.appointments.commands.scheduleappointment.ScheduleAppointmentCommand;
import atlas.application.appointments.queries.getupcomingappointments.GetUpcomingAppointmentsQuery;
import atlas.application.nutrition.commands.defineplan.DefinePlanCommand;
import atlas.application.nutrition.queries.getactiveplan.GetActivePlanQuery;
import atlas.application.routines.commands.defineroutine.DefineRoutineCommand;
import atlas.application.routines.queries.listroutines.ListRoutinesQuery;
import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.training.commands.EffortInput;
import atlas.application.training.commands.PlannedLineInput;
import atlas.application.training.commands.defineexercise.DefineExerciseCommand;
import atlas.application.training.commands.defineworkout.DefineWorkoutCommand;
import atlas.application.training.commands.scheduleworkout.ScheduleWorkoutCommand;
import atlas.application.training.commands.setworkoutplan.SetWorkoutPlanCommand;
import atlas.application.training.queries.listexercises.ListExercisesQuery;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.enums.Metric;
import atlas.infrastructure.sharedkernel.logging.LogEntryRenderers;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deja el espejo con algo que enseñar recién instalado: cuatro ejercicios (uno por métrica),
 * una rutina con sus días, dos hábitos, un plan de nutrición y dos citas.
 *
 * <p>Cada contexto se siembra solo si está vacío, así que repetirlo no duplica nada.
 * {@code economy} tiene el suyo aparte, en {@code gradle seedEconomy}.
 */
public final class Seeder {

    private Seeder() {}

    public static void main(String[] args) {
        Logger.getLogger("sharedkernel.command").setLevel(Level.OFF);

        var directory = Path.of(args.length > 0 ? args[0] : "data");
        Summary summary;
        try {
            summary = seed(directory, Clock.systemDefaultZone());
        } catch (RuntimeException e) {
            if (!isDatabaseLocked(e)) {
                throw e;
            }

            System.err.println(lockedMessage(directory));
            System.exit(1);

            return;
        }

        System.out.println(summary.describe(directory));
    }

    public static Summary seed(Path dataDirectory, Clock clock) {
        return new Summary(
            seedTraining(dataDirectory, clock),
            seedRoutines(dataDirectory, clock),
            seedNutrition(dataDirectory, clock),
            seedAppointments(dataDirectory, clock));
    }

    private static boolean seedTraining(Path dataDirectory, Clock clock) {
        var application = TrainingApplication.wire(quietRenderer(), dataDirectory, clock);
        try {
            var commands = application.commands();
            if (!application.queries().dispatch(new ListExercisesQuery(true)).value().isEmpty()) {
                return false;
            }

            var bench = exercise(commands, "Press banca", Metric.LOAD);
            var pullUps = exercise(commands, "Dominadas", Metric.REPS);
            var plank = exercise(commands, "Plancha", Metric.TIME);
            exercise(commands, "Correr", Metric.DISTANCE);

            var push = WorkoutId.parse(
                value(commands.dispatch(new DefineWorkoutCommand("Empuje"))).id());
            commands.dispatch(new ScheduleWorkoutCommand(
                push, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)));
            commands.dispatch(new SetWorkoutPlanCommand(push, List.of(
                new PlannedLineInput(bench, 4, new EffortInput(new BigDecimal("70"), 12, 0, 0)),
                new PlannedLineInput(pullUps, 4, new EffortInput(BigDecimal.ZERO, 8, 0, 0)),
                new PlannedLineInput(plank, 3, new EffortInput(BigDecimal.ZERO, 0, 60, 0)))));

            return true;
        } finally {
            application.stop();
        }
    }

    private static ExerciseId exercise(CommandBus commands, String name, Metric metric) {
        return ExerciseId.parse(value(commands.dispatch(new DefineExerciseCommand(name, metric))).id());
    }

    private static boolean seedRoutines(Path dataDirectory, Clock clock) {
        var application = RoutinesApplication.wire(quietRenderer(), dataDirectory, clock);
        try {
            if (!application.queries().dispatch(new ListRoutinesQuery(true)).value().isEmpty()) {
                return false;
            }

            application.commands().dispatch(new DefineRoutineCommand(
                "Gimnasio", "Ir a entrenar", new BigDecimal("4"), "sesiones",
                RecurrencePeriod.WEEK, Set.of(), Set.of()));
            application.commands().dispatch(new DefineRoutineCommand(
                "Leer", "Antes de dormir", new BigDecimal("20"), "minutos",
                RecurrencePeriod.DAY, EnumSet.allOf(DayOfWeek.class), Set.of()));

            return true;
        } finally {
            application.stop();
        }
    }

    private static boolean seedNutrition(Path dataDirectory, Clock clock) {
        var application = NutritionApplication.wire(quietRenderer(), dataDirectory, clock);
        try {
            if (application.queries().dispatch(new GetActivePlanQuery()).isSuccess()) {
                return false;
            }

            application.commands().dispatch(new DefinePlanCommand(
                new BigDecimal("82"), new BigDecimal("76"), 2200, 160, 220, 70,
                LocalDate.now(clock)));

            return true;
        } finally {
            application.stop();
        }
    }

    private static boolean seedAppointments(Path dataDirectory, Clock clock) {
        var application = AppointmentsApplication.wire(quietRenderer(), dataDirectory, clock, () -> true);
        try {
            var now = LocalDateTime.now(clock);
            if (!application.queries().dispatch(new GetUpcomingAppointmentsQuery(now, 1)).value().isEmpty()) {
                return false;
            }

            var tomorrow = now.toLocalDate().plusDays(1);
            application.commands().dispatch(new ScheduleAppointmentCommand(
                "Fisioterapia", "Espalda", tomorrow.atTime(10, 0), tomorrow.atTime(11, 0),
                List.of(30), false));
            var weekend = now.toLocalDate().plusDays(3);
            application.commands().dispatch(new ScheduleAppointmentCommand(
                "Cena con Marta", "", weekend.atTime(21, 0), weekend.atTime(23, 0),
                List.of(60), false));

            return true;
        } finally {
            application.stop();
        }
    }

    private static <T> T value(Result<T> result) {
        if (result.isFailure()) {
            throw new IllegalStateException("El seed no pudo escribir: " + result.error().code());
        }

        return result.value();
    }

    private static LogEntryRenderer quietRenderer() {
        return LogEntryRenderers.forConsole(false, ZoneOffset.UTC);
    }

    private static boolean isDatabaseLocked(Throwable failure) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("database is locked")) {
                return true;
            }
        }

        return false;
    }

    private static String lockedMessage(Path directory) {
        return String.join(
            System.lineSeparator(),
            "Alguna base de " + directory + " esta en uso por otro proceso.",
            "SQLite admite un solo escritor: para Atlas, siembra, y vuelve a arrancarlo.");
    }

    public record Summary(boolean training, boolean routines, boolean nutrition, boolean appointments) {

        public String describe(Path directory) {
            var sown = new ArrayList<String>();
            if (training) {
                sown.add("training");
            }
            if (routines) {
                sown.add("routines");
            }
            if (nutrition) {
                sown.add("nutrition");
            }
            if (appointments) {
                sown.add("appointments");
            }

            if (sown.isEmpty()) {
                return "Todos los contextos tenian datos: no se ha tocado nada.";
            }

            return "Sembrados " + String.join(", ", sown) + " en " + directory
                + (sown.size() == 4 ? "." : " (los demas ya tenian datos).");
        }
    }
}

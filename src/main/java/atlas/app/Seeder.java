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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deja el espejo con algo que enseñar recién instalado: el catálogo de ejercicios con las tres
 * rutinas de la semana, cuatro hábitos, un plan de nutrición y la agenda de las próximas semanas.
 *
 * <p>
 * Cada contexto se siembra solo si está vacío, así que repetirlo no duplica nada. Con
 * {@code --reset} se tira lo que hubiera antes y se siembra de cero. {@code economy} y
 * {@code presence} no se tocan: el primero tiene su propio {@code gradle seedEconomy} y el
 * segundo guarda tu cara.
 */
public final class Seeder {

    private static final List<String> CONTEXTS = List.of("training", "routines", "nutrition", "appointments");

    /** Los ejercicios del catálogo, con la métrica que los define para siempre. */
    private static final List<Exercise> EXERCISES = List.of(
        new Exercise("Press banca", Metric.LOAD),
        new Exercise("Press militar", Metric.LOAD),
        new Exercise("Fondos", Metric.REPS),
        new Exercise("Dominadas", Metric.REPS),
        new Exercise("Remo con barra", Metric.LOAD),
        new Exercise("Curl de biceps", Metric.LOAD),
        new Exercise("Sentadilla", Metric.LOAD),
        new Exercise("Peso muerto", Metric.LOAD),
        new Exercise("Zancadas", Metric.LOAD),
        new Exercise("Gemelos", Metric.LOAD),
        new Exercise("Plancha", Metric.TIME),
        new Exercise("Correr", Metric.DISTANCE));

    /** Empuje, tiron y pierna, dos dias cada uno: la semana entera menos el domingo. */
    private static final List<Workout> WORKOUTS = List.of(
        new Workout("Empuje", Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), List.of(
            new Line("Press banca", 4, 70, 12, 0, 0),
            new Line("Press militar", 4, 40, 10, 0, 0),
            new Line("Fondos", 3, 0, 12, 0, 0),
            new Line("Plancha", 3, 0, 0, 60, 0))),
        new Workout("Tiron", Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY), List.of(
            new Line("Dominadas", 4, 0, 8, 0, 0),
            new Line("Remo con barra", 4, 60, 10, 0, 0),
            new Line("Curl de biceps", 3, 15, 12, 0, 0))),
        new Workout("Pierna", Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY), List.of(
            new Line("Sentadilla", 5, 90, 8, 0, 0),
            new Line("Peso muerto", 4, 110, 6, 0, 0),
            new Line("Zancadas", 3, 20, 12, 0, 0),
            new Line("Gemelos", 4, 40, 15, 0, 0))));

    private static final List<Habit> HABITS = List.of(
        new Habit("Gimnasio", "Ir a entrenar", 4, "sesiones", RecurrencePeriod.WEEK, Set.of()),
        new Habit("Leer", "Antes de dormir", 20, "minutos", RecurrencePeriod.DAY,
            EnumSet.allOf(DayOfWeek.class)),
        new Habit("Beber agua", "Dos litros", 2, "litros", RecurrencePeriod.DAY,
            EnumSet.allOf(DayOfWeek.class)),
        new Habit("Meditar", "Diez minutos al despertar", 5, "sesiones", RecurrencePeriod.WEEK, Set.of()),
        new Habit("Llamar a casa", "Sin excusas", 4, "llamadas", RecurrencePeriod.MONTH, Set.of()));

    /** Dias desde hoy, hora, duracion en minutos, aviso en minutos. */
    private static final List<Event> EVENTS = List.of(
        new Event(1, 10, 0, 60, "Fisioterapia", "Espalda", 30),
        new Event(1, 19, 30, 90, "Cena con Marta", "", 60),
        new Event(2, 9, 0, 45, "Revision del coche", "Taller de siempre", 60),
        new Event(3, 17, 0, 60, "Dentista", "Limpieza", 1440),
        new Event(5, 12, 0, 30, "Llamada con el banco", "Hipoteca", 10),
        new Event(8, 20, 0, 120, "Cumpleanos de Javi", "Llevar algo", 1440),
        new Event(10, 11, 0, 60, "Analitica", "En ayunas", 1440),
        new Event(14, 18, 0, 90, "Corte de pelo", "", 60));

    private Seeder() {}

    public static void main(String[] args) {
        Logger.getLogger("sharedkernel.command").setLevel(Level.OFF);

        var reset = List.of(args).contains("--reset");
        var directory = Path.of(List.of(args).stream()
            .filter(argument -> !argument.startsWith("--"))
            .findFirst()
            .orElse("data"));

        Summary summary;
        try {
            if (reset) {
                wipe(directory);
            }
            summary = seed(directory, Clock.systemDefaultZone());
        } catch (RuntimeException e) {
            if (!isDatabaseLocked(e)) {
                throw e;
            }

            System.err.println(lockedMessage(directory));
            System.exit(1);

            return;
        }

        System.out.println(summary.describe(directory, reset));
    }

    /** Borra las bases de los cuatro contextos; las migraciones las vuelven a crear vacias. */
    public static void wipe(Path dataDirectory) {
        for (var context : CONTEXTS) {
            for (var suffix : List.of(".db", ".db-wal", ".db-shm")) {
                var file = dataDirectory.resolve(context + suffix);
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(
                        "No se pudo borrar " + file + ": para Atlas antes de sembrar de cero.", e);
                }
            }
        }
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

            var catalogue = new LinkedHashMap<String, ExerciseId>();
            for (var exercise : EXERCISES) {
                catalogue.put(exercise.name(), ExerciseId.parse(
                    value(commands.dispatch(new DefineExerciseCommand(exercise.name(), exercise.metric()))).id()));
            }

            for (var workout : WORKOUTS) {
                define(commands, workout, catalogue);
            }

            return true;
        } finally {
            application.stop();
        }
    }

    private static void define(CommandBus commands, Workout workout, Map<String, ExerciseId> catalogue) {
        var id = WorkoutId.parse(value(commands.dispatch(new DefineWorkoutCommand(workout.name()))).id());
        commands.dispatch(new ScheduleWorkoutCommand(id, workout.days()));
        commands.dispatch(new SetWorkoutPlanCommand(id, workout.lines().stream()
            .map(line -> new PlannedLineInput(catalogue.get(line.exercise()), line.sets(),
                new EffortInput(BigDecimal.valueOf(line.load()), line.reps(), line.seconds(), line.meters())))
            .toList()));
    }

    private static boolean seedRoutines(Path dataDirectory, Clock clock) {
        var application = RoutinesApplication.wire(quietRenderer(), dataDirectory, clock);
        try {
            if (!application.queries().dispatch(new ListRoutinesQuery(true)).value().isEmpty()) {
                return false;
            }

            for (var habit : HABITS) {
                application.commands().dispatch(new DefineRoutineCommand(
                    habit.name(), habit.description(), BigDecimal.valueOf(habit.target()), habit.unit(),
                    habit.period(), habit.activeDays(), Set.of()));
            }

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
                new BigDecimal("82"), new BigDecimal("76"), 2200, 160, 220, 70, LocalDate.now(clock)));

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

            for (var event : EVENTS) {
                var start = now.toLocalDate().plusDays(event.inDays()).atTime(event.hour(), event.minute());
                application.commands().dispatch(new ScheduleAppointmentCommand(
                    event.title(), event.description(), start, start.plusMinutes(event.minutes()),
                    List.of(event.reminder()), false));
            }

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

    private record Exercise(String name, Metric metric) {}

    private record Line(String exercise, int sets, int load, int reps, int seconds, int meters) {}

    private record Workout(String name, Set<DayOfWeek> days, List<Line> lines) {}

    private record Habit(
        String name, String description, int target, String unit,
        RecurrencePeriod period, Set<DayOfWeek> activeDays) {}

    private record Event(
        int inDays, int hour, int minute, int minutes, String title, String description, int reminder) {}

    public record Summary(boolean training, boolean routines, boolean nutrition, boolean appointments) {

        public String describe(Path directory, boolean reset) {
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

            return (reset ? "Borrado y sembrado de cero " : "Sembrados ") + String.join(", ", sown)
                + " en " + directory + (sown.size() == CONTEXTS.size() ? "." : " (los demas ya tenian datos).");
        }
    }
}

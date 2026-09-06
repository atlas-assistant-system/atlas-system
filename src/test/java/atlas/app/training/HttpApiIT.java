package atlas.app.training;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.infrastructure.sharedkernel.logging.LogEntryRenderers;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpApiIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Clock CLOCK =
        Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @TempDir
    private Path databaseDirectory;

    private static TrainingApplication application;
    private static HttpClient client;
    private static String base;

    
    @BeforeEach
    void startServer() throws IOException {
        application = TrainingApplication
            .wire(LogEntryRenderers.forConsole(false, ZoneOffset.UTC), databaseDirectory, CLOCK)
            .start(0);
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + application.port();
    }

    @AfterEach
    void stopServer() {
        application.stop();
    }

    @Test
    void shouldWalkTheWholeLifeOfAnExercise() throws Exception {
        var created = send("POST", "/training/exercises",
            "{\"name\":\"Press banca\",\"metric\":\"LOAD\"}");
        assertThat(created.statusCode()).isEqualTo(201);

        var exercise = json(created);
        assertThat((String) exercise.get("id")).matches("E\\d{8}");
        assertThat(exercise.get("metric")).isEqualTo("LOAD");
        assertThat(exercise.get("metricLabel")).isEqualTo("Carga");
        assertThat(exercise.get("archived")).isEqualTo(false);

        var id = exercise.get("id");
        assertThat(json(send("PUT", "/training/exercises/" + id, "{\"name\":\"Press inclinado\"}"))
            .get("name")).isEqualTo("Press inclinado");

        assertThat(send("DELETE", "/training/exercises/" + id, null).statusCode()).isEqualTo(204);
        assertThat(jsonList(send("GET", "/training/exercises", null))).isEmpty();
        assertThat(jsonList(send("GET", "/training/exercises?archived=true", null))).hasSize(1);

        assertThat(send("POST", "/training/exercises/" + id + "/restore", null).statusCode())
            .isEqualTo(204);
        assertThat(jsonList(send("GET", "/training/exercises", null))).hasSize(1);
    }

    @Test
    void shouldRefuseASecondExerciseWithTheSameName() throws Exception {
        anExercise("Press banca", "LOAD");

        var repeated = send("POST", "/training/exercises",
            "{\"name\":\"Press banca\",\"metric\":\"LOAD\"}");

        assertThat(repeated.statusCode()).isEqualTo(409);
        assertThat(json(repeated).get("code")).isEqualTo("Exercise.NameAlreadyTaken");
    }

    @Test
    void shouldBringBackAnArchivedExerciseWhenItsNameIsDefinedAgain() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        send("DELETE", "/training/exercises/" + press, null);

        var repeated = json(send("POST", "/training/exercises",
            "{\"name\":\"Press banca\",\"metric\":\"LOAD\"}"));

        assertThat(repeated.get("id")).isEqualTo(press);
        assertThat(repeated.get("archived")).isEqualTo(false);
    }

    @Test
    void shouldRefuseToBringBackAnArchivedExerciseUnderAnotherMeasure() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        send("DELETE", "/training/exercises/" + press, null);

        var repeated = send("POST", "/training/exercises",
            "{\"name\":\"Press banca\",\"metric\":\"REPS\"}");

        assertThat(repeated.statusCode()).isEqualTo(409);
    }

    @Test
    void shouldRejectAMetricThatIsNotOneOfTheFour() throws Exception {
        var response = send("POST", "/training/exercises",
            "{\"name\":\"Algo\",\"metric\":\"VIBES\"}");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldExpandTheTemplateIntoOneSetPerRepetitionOfEachLine() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var workout = aWorkoutOf(press, 4, "70", 8);

        var started = send("POST", "/training/logs",
            "{\"workoutId\":\"" + workout + "\"}");

        assertThat(started.statusCode()).isEqualTo(201);
        var log = json(started);
        assertThat((String) log.get("id")).matches("T\\d{8}");
        assertThat(log.get("workoutId")).isEqualTo(workout);
        assertThat(log.get("performedOn")).isEqualTo("2026-08-22");
        assertThat(setsOf(log)).hasSize(4);
        assertThat(effort(setsOf(log).getFirst(), "planned").get("load")).isEqualTo("70");
        assertThat(effort(setsOf(log).getFirst(), "planned").get("reps")).isEqualTo(8);
        assertThat(setsOf(log).getFirst().get("actual")).isNull();
    }

    @Test
    void shouldKeepThePlanFrozenWhenTheTemplateChangesAfterwards() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var workout = aWorkoutOf(press, 1, "70", 8);
        var log = json(send("POST", "/training/logs", "{\"workoutId\":\"" + workout + "\"}"));

        send("PUT", "/training/workouts/" + workout + "/plan",
            "{\"plan\":[{\"exerciseId\":\"" + press + "\",\"sets\":5,"
                + "\"target\":{\"load\":\"90\",\"reps\":5}}]}");

        var reread = json(send("GET", "/training/logs/" + log.get("id"), null));

        assertThat(setsOf(reread)).hasSize(1);
        assertThat(effort(setsOf(reread).getFirst(), "planned").get("load")).isEqualTo("70");
        assertThat(effort(setsOf(reread).getFirst(), "planned").get("reps")).isEqualTo(8);
    }

    @Test
    void shouldRecordWhatWasActuallyLiftedWithoutTouchingThePlan() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var workout = aWorkoutOf(press, 2, "70", 8);
        var log = json(send("POST", "/training/logs", "{\"workoutId\":\"" + workout + "\"}"));
        var setId = setsOf(log).getFirst().get("id");

        var recorded = json(send("PUT",
            "/training/logs/" + log.get("id") + "/sets/" + setId,
            "{\"load\":\"72.5\",\"reps\":6}"));

        var first = setsOf(recorded).getFirst();
        assertThat(effort(first, "actual").get("load")).isEqualTo("72.5");
        assertThat(effort(first, "actual").get("reps")).isEqualTo(6);
        assertThat(effort(first, "planned").get("reps")).isEqualTo(8);
        assertThat(setsOf(recorded).getLast().get("actual")).isNull();
    }

    @Test
    void shouldRefuseASetThatMeasuresNothing() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var workout = aWorkoutOf(press, 1, "70", 8);
        var log = json(send("POST", "/training/logs", "{\"workoutId\":\"" + workout + "\"}"));

        var response = send("PUT",
            "/training/logs/" + log.get("id") + "/sets/" + setsOf(log).getFirst().get("id"),
            "{\"load\":\"0\",\"reps\":0}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("WorkoutLog.SetMeasuresNothing");
    }

    @Test
    void shouldAddAndRemoveASetOutsideTheScript() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var dips = anExercise("Fondos", "REPS");
        var workout = aWorkoutOf(press, 1, "70", 8);
        var log = json(send("POST", "/training/logs", "{\"workoutId\":\"" + workout + "\"}"));

        var withExtra = json(send("POST", "/training/logs/" + log.get("id") + "/sets",
            "{\"exerciseId\":\"" + dips + "\",\"reps\":12}"));

        assertThat(setsOf(withExtra)).hasSize(2);
        assertThat(setsOf(withExtra).getLast().get("planned")).isNull();
        assertThat(effort(setsOf(withExtra).getLast(), "actual").get("reps")).isEqualTo(12);

        var extraId = setsOf(withExtra).getLast().get("id");
        assertThat(send("DELETE", "/training/logs/" + log.get("id") + "/sets/" + extraId, null)
            .statusCode()).isEqualTo(204);
        assertThat(setsOf(json(send("GET", "/training/logs/" + log.get("id"), null)))).hasSize(1);
    }

    @Test
    void shouldStartAFreeWorkoutWithNoTemplate() throws Exception {
        var dips = anExercise("Fondos", "REPS");

        var log = json(send("POST", "/training/logs", "{}"));
        assertThat(log.get("workoutId")).isNull();
        assertThat(setsOf(log)).isEmpty();

        var withSet = json(send("POST", "/training/logs/" + log.get("id") + "/sets",
            "{\"exerciseId\":\"" + dips + "\",\"reps\":12}"));
        assertThat(setsOf(withSet)).hasSize(1);
    }

    @Test
    void shouldReplaceTheWholePlanInsteadOfMergingIt() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var dips = anExercise("Fondos", "REPS");
        var workout = aWorkoutOf(press, 4, "70", 8);

        var replaced = json(send("PUT", "/training/workouts/" + workout + "/plan",
            "{\"plan\":[{\"exerciseId\":\"" + dips + "\",\"sets\":3,\"target\":{\"reps\":10}}]}"));

        assertThat(planOf(replaced)).hasSize(1);
        assertThat(planOf(replaced).getFirst().get("exerciseId")).isEqualTo(dips);
        assertThat(planOf(replaced).getFirst().get("position")).isEqualTo(0);
    }

    @Test
    void shouldKeepTheOrderOfThePlanAsItWasGiven() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var dips = anExercise("Fondos", "REPS");
        var workout = json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}")).get("id");

        var plan = json(send("PUT", "/training/workouts/" + workout + "/plan",
            "{\"plan\":[{\"exerciseId\":\"" + dips + "\",\"sets\":3,\"target\":{\"reps\":10}},"
                + "{\"exerciseId\":\"" + press + "\",\"sets\":4,\"target\":{\"load\":\"70\",\"reps\":8}}]}"));

        assertThat(planOf(plan)).extracting(line -> line.get("exerciseId"))
            .containsExactly(dips, press);
    }

    @Test
    void shouldAssignAWorkoutToTheDaysOfTheWeekItIsTrainedOn() throws Exception {
        var workout = json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}")).get("id");

        var scheduled = json(send("PUT", "/training/workouts/" + workout + "/schedule",
            "{\"days\":[\"THURSDAY\",\"MONDAY\"]}"));

        assertThat(scheduled.get("days")).isEqualTo(List.of("MONDAY", "THURSDAY"));
    }

    @Test
    void shouldLeaveAWorkoutOffTheWeekWhenTheDaysAreCleared() throws Exception {
        var workout = json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}")).get("id");
        send("PUT", "/training/workouts/" + workout + "/schedule", "{\"days\":[\"MONDAY\"]}");

        var cleared = json(send("PUT", "/training/workouts/" + workout + "/schedule",
            "{\"days\":[]}"));

        assertThat(cleared.get("days")).isEqualTo(List.of());
    }

    @Test
    void shouldRejectSomethingThatIsNotAWeekday() throws Exception {
        var workout = json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}")).get("id");

        var response = send("PUT", "/training/workouts/" + workout + "/schedule",
            "{\"days\":[\"LUNES\"]}");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldRejectAPlanWithASetCountNobodyDoes() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var workout = json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}")).get("id");

        var response = send("PUT", "/training/workouts/" + workout + "/plan",
            "{\"plan\":[{\"exerciseId\":\"" + press + "\",\"sets\":48,"
                + "\"target\":{\"load\":\"70\",\"reps\":8}}]}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Workout.SetCountOutOfRange");
    }

    @Test
    void shouldRejectALoadNobodyLifts() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        var workout = json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}")).get("id");

        var response = send("PUT", "/training/workouts/" + workout + "/plan",
            "{\"plan\":[{\"exerciseId\":\"" + press + "\",\"sets\":4,"
                + "\"target\":{\"load\":\"725\",\"reps\":8}}]}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Training.LoadOutOfRange");
    }

    @Test
    void shouldRejectAnArchivedExerciseInANewPlan() throws Exception {
        var press = anExercise("Press banca", "LOAD");
        send("DELETE", "/training/exercises/" + press, null);
        var workout = json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}")).get("id");

        var response = send("PUT", "/training/workouts/" + workout + "/plan",
            "{\"plan\":[{\"exerciseId\":\"" + press + "\",\"sets\":4,"
                + "\"target\":{\"load\":\"70\",\"reps\":8}}]}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Workout.ArchivedExerciseNotAllowed");
    }

    @Test
    void shouldRejectAWorkoutDatedInTheFuture() throws Exception {
        var response = send("POST", "/training/logs", "{\"performedOn\":\"2026-08-23\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("WorkoutLog.CannotBeDatedInTheFuture");
    }

    @Test
    void shouldReturnEveryWorkoutOfTodayBecauseMorningAndEveningAreTwo() throws Exception {
        send("POST", "/training/logs", "{}");
        send("POST", "/training/logs", "{}");
        send("POST", "/training/logs", "{\"performedOn\":\"2026-08-21\"}");

        assertThat(jsonList(send("GET", "/training/today", null))).hasSize(2);
    }

    @Test
    void shouldListTheHistoryNewestFirst() throws Exception {
        send("POST", "/training/logs", "{\"performedOn\":\"2026-08-20\"}");
        send("POST", "/training/logs", "{\"performedOn\":\"2026-08-22\"}");
        send("POST", "/training/logs", "{\"performedOn\":\"2026-08-21\"}");

        var history = jsonList(send("GET", "/training/logs", null));

        assertThat(history).extracting(log -> log.get("performedOn"))
            .containsExactly("2026-08-22", "2026-08-21", "2026-08-20");
    }

    @Test
    void shouldFilterTheHistoryByPeriodAndLimit() throws Exception {
        send("POST", "/training/logs", "{\"performedOn\":\"2026-08-20\"}");
        send("POST", "/training/logs", "{\"performedOn\":\"2026-08-21\"}");
        send("POST", "/training/logs", "{\"performedOn\":\"2026-08-22\"}");

        assertThat(jsonList(send("GET", "/training/logs?from=2026-08-21&to=2026-08-22", null)))
            .hasSize(2);
        assertThat(jsonList(send("GET", "/training/logs?limit=1", null))).hasSize(1);
    }

    @Test
    void shouldRejectADayThatIsNotAnIsoDate() throws Exception {
        assertThat(send("GET", "/training/logs?from=ayer", null).statusCode()).isEqualTo(400);
    }

    @Test
    void shouldReportAnUnknownLogAsNotFound() throws Exception {
        var response = send("GET", "/training/logs/T00009999", null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json(response).get("code")).isEqualTo("WorkoutLog.NotFound");
    }

    @Test
    void shouldReportAnUnknownTemplateAsNotFoundWhenStarting() throws Exception {
        var response = send("POST", "/training/logs", "{\"workoutId\":\"W00009999\"}");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json(response).get("code")).isEqualTo("Workout.NotFound");
    }

    @Test
    void shouldDiscardTheWholeWorkout() throws Exception {
        var log = json(send("POST", "/training/logs", "{}"));

        assertThat(send("DELETE", "/training/logs/" + log.get("id"), null).statusCode())
            .isEqualTo(204);
        assertThat(send("GET", "/training/logs/" + log.get("id"), null).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldServeItsDocsBelowTheModulePath() throws Exception {
        assertThat(send("GET", "/training/docs", null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/training/openapi.json", null).statusCode()).isEqualTo(200);
    }

    private static String anExercise(String name, String metric) throws Exception {
        return (String) json(send("POST", "/training/exercises",
            "{\"name\":\"" + name + "\",\"metric\":\"" + metric + "\"}")).get("id");
    }

    private static String aWorkoutOf(String exerciseId, int sets, String load, int reps)
        throws Exception {

        var workout = (String) json(send("POST", "/training/workouts", "{\"name\":\"Empuje\"}"))
            .get("id");
        send("PUT", "/training/workouts/" + workout + "/plan",
            "{\"plan\":[{\"exerciseId\":\"" + exerciseId + "\",\"sets\":" + sets
                + ",\"target\":{\"load\":\"" + load + "\",\"reps\":" + reps + "}}]}");

        return workout;
    }

    private static HttpResponse<String> send(String method, String path, String body)
        throws Exception {

        var request = HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/json")
            .method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body))
            .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, Object> json(HttpResponse<String> response) throws IOException {
        return JSON.std.mapFrom(response.body());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> jsonList(HttpResponse<String> response)
        throws IOException {

        return (List<Map<String, Object>>) (List<?>) JSON.std.listFrom(response.body());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> setsOf(Map<String, Object> log) {
        return (List<Map<String, Object>>) log.get("sets");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> planOf(Map<String, Object> workout) {
        return (List<Map<String, Object>>) workout.get("plan");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> effort(Map<String, Object> set, String field) {
        return (Map<String, Object>) set.get(field);
    }
}

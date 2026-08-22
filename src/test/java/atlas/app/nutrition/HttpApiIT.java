package atlas.app.nutrition;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpApiIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @TempDir
    static Path databaseDirectory;

    private static NutritionApplication application;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        application = NutritionApplication
            .wire(LogEntryRenderers.forConsole(false, ZoneOffset.UTC), databaseDirectory, CLOCK)
            .start(0);
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + application.port();
    }

    @AfterAll
    static void stopServer() {
        application.stop();
    }

    @BeforeEach
    void emptyTheDay() throws Exception {
        send("DELETE", "/nutrition/plan", null);

        for (var intake : jsonList(send("GET", "/nutrition/intakes?from=2000-01-01", null))) {
            send("DELETE", "/nutrition/intakes/" + intake.get("id"), null);
        }
    }

    @Test
    void shouldWalkTheWholeLifeOfAPlan() throws Exception {
        var created = send("POST", "/nutrition/plan", """
            {"startWeight":"84.0","targetWeight":"78.0","calories":1940,"protein":150,"carbs":200,"fat":60}""");
        assertThat(created.statusCode()).isEqualTo(201);

        var plan = json(created);
        assertThat((String) plan.get("id")).matches("N\\d{8}");
        assertThat(plan.get("startWeight")).isEqualTo("84");
        assertThat(plan.get("targetWeight")).isEqualTo("78");
        assertThat(plan.get("goal")).isEqualTo("LOSE");
        assertThat(plan.get("goalLabel")).isEqualTo("Perder peso");
        assertThat(plan.get("status")).isEqualTo("ACTIVE");
        assertThat(plan.get("startedOn")).isEqualTo("2026-08-22");
        assertThat(plan.get("dailyCalories")).isEqualTo(1940);

        assertThat(json(send("GET", "/nutrition/plan", null)).get("goal")).isEqualTo("LOSE");

        var adjusted = json(send("PUT", "/nutrition/plan", """
            {"targetWeight":"76.0","calories":1855,"protein":160,"carbs":180,"fat":55}"""));
        assertThat(adjusted.get("targetWeight")).isEqualTo("76");
        assertThat(adjusted.get("startWeight")).isEqualTo("84");
        assertThat(adjusted.get("dailyCalories")).isEqualTo(1855);

        assertThat(send("DELETE", "/nutrition/plan", null).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/nutrition/plan", null).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldArchiveThePreviousPlanWhenANewOneIsDefined() throws Exception {
        var first = (String) json(definePlan()).get("id");

        var second = json(send("POST", "/nutrition/plan", """
            {"startWeight":"78.0","targetWeight":"82.0","calories":2350,"protein":180,"carbs":250,"fat":70}"""));

        assertThat(second.get("id")).isNotEqualTo(first);
        assertThat(second.get("goal")).isEqualTo("GAIN");
        assertThat(json(send("GET", "/nutrition/plan", null)).get("id")).isEqualTo(second.get("id"));
    }

    @Test
    void shouldWalkTheWholeLifeOfAnIntake() throws Exception {
        var created = send("POST", "/nutrition/intakes", """
            {"calories":450,"protein":30,"carbs":60,"fat":10,"note":"Tortilla y pan","consumedOn":"2026-08-21"}""");
        assertThat(created.statusCode()).isEqualTo(201);

        var intake = json(created);
        var id = (String) intake.get("id");
        assertThat(id).matches("I\\d{8}");
        assertThat(intake.get("note")).isEqualTo("Tortilla y pan");
        assertThat(intake.get("consumedOn")).isEqualTo("2026-08-21");
        assertThat(intake.get("calories")).isEqualTo(450);

        assertThat(json(send("GET", "/nutrition/intakes/" + id, null)).get("id")).isEqualTo(id);

        var corrected = json(send("PUT", "/nutrition/intakes/" + id, """
            {"calories":480,"protein":35,"carbs":55,"fat":12}"""));
        assertThat(macros(corrected, "macros")).containsEntry("protein", 35);
        assertThat(corrected.get("calories")).isEqualTo(480);
        assertThat(corrected.get("note")).isNull();
        assertThat(corrected.get("consumedOn")).isEqualTo("2026-08-21");

        assertThat(send("DELETE", "/nutrition/intakes/" + id, null).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/nutrition/intakes/" + id, null).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldAddUpTodayAgainstTheQuota() throws Exception {
        definePlan();
        recordIntake(30, 60, 10, null);
        recordIntake(45, 40, 20, null);

        var day = json(send("GET", "/nutrition/today", null));

        assertThat(day.get("date")).isEqualTo("2026-08-22");
        assertThat(macros(day, "consumedMacros")).containsEntry("protein", 75);
        assertThat(day.get("consumedCalories")).isEqualTo(970);
        assertThat(day.get("targetCalories")).isEqualTo(1940);
        assertThat(macros(day, "remainingMacros")).containsEntry("protein", 75);
        assertThat(day.get("remainingCalories")).isEqualTo(970);
        assertThat(day.get("caloriePercentage")).isEqualTo(50);
        assertThat(day.get("overBudget")).isEqualTo(false);
        assertThat(intakesOf(day)).hasSize(2);
    }

    @Test
    void shouldReportADayWithoutAQuotaWhenThereIsNoPlan() throws Exception {
        recordIntake(30, 60, 10, null);

        var day = json(send("GET", "/nutrition/today", null));

        assertThat(day.get("consumedCalories")).isEqualTo(450);
        assertThat(day.get("targetCalories")).isNull();
        assertThat(day.get("targetMacros")).isNull();
        assertThat(day.get("remainingCalories")).isNull();
        assertThat(day.get("caloriePercentage")).isEqualTo(0);
    }

    @Test
    void shouldFlagTheDayAsOverBudgetWithANegativeRemainder() throws Exception {
        definePlan();
        recordIntake(200, 250, 80, null);

        var day = json(send("GET", "/nutrition/days/2026-08-22", null));

        assertThat(day.get("overBudget")).isEqualTo(true);
        assertThat(macros(day, "remainingMacros")).containsEntry("protein", -50);
    }

    @Test
    void shouldSummariseTheLastDaysNewestFirst() throws Exception {
        definePlan();
        recordIntake(75, 100, 30, "2026-08-22");
        recordIntake(150, 200, 60, "2026-08-21");

        var days = jsonList(send("GET", "/nutrition/days", null));

        assertThat(days).hasSize(2);
        assertThat(days.getFirst().get("date")).isEqualTo("2026-08-22");
        assertThat(days.getFirst().get("caloriePercentage")).isEqualTo(50);
        assertThat(days.getLast().get("date")).isEqualTo("2026-08-21");
        assertThat(days.getLast().get("caloriePercentage")).isEqualTo(100);
    }

    @Test
    void shouldDateAnIntakeTodayWhenNoDateIsGiven() throws Exception {
        var intake = json(send("POST", "/nutrition/intakes", """
            {"calories":450,"protein":30,"carbs":60,"fat":10}"""));

        assertThat(intake.get("consumedOn")).isEqualTo("2026-08-22");
        assertThat(intake.get("note")).isNull();
    }

    @Test
    void shouldTreatAMissingMacroAsZero() throws Exception {
        var intake = json(send("POST", "/nutrition/intakes", """
            {"calories":100,"carbs":25}"""));

        assertThat(macros(intake, "macros"))
            .containsEntry("protein", 0)
            .containsEntry("carbs", 25)
            .containsEntry("fat", 0);
        assertThat(intake.get("calories")).isEqualTo(100);
    }

    @Test
    void shouldKeepTheCaloriesEvenWhenTheyDoNotMatchTheMacros() throws Exception {
        var intake = json(send("POST", "/nutrition/intakes", """
            {"calories":250,"protein":30,"carbs":60,"fat":10}"""));

        assertThat(intake.get("calories")).isEqualTo(250);
    }

    @Test
    void shouldAcceptCaloriesWithoutAnyMacro() throws Exception {
        var intake = json(send("POST", "/nutrition/intakes", """
            {"calories":150,"note":"Una cerveza"}"""));

        assertThat(intake.get("calories")).isEqualTo(150);
        assertThat(macros(intake, "macros")).containsEntry("protein", 0);
    }

    @Test
    void shouldRejectAnIntakeWithoutCalories() throws Exception {
        var response = send("POST", "/nutrition/intakes", """
            {"calories":0,"protein":30}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Intake.CaloriesRequired");
    }

    @Test
    void shouldRejectAnIntakeWithNoBodyAtAll() throws Exception {
        assertThat(send("POST", "/nutrition/intakes", "{}").statusCode()).isEqualTo(400);
    }

    @Test
    void shouldRejectAnIntakeDatedInTheFuture() throws Exception {
        var response = send("POST", "/nutrition/intakes", """
            {"calories":450,"protein":30,"consumedOn":"2026-08-23"}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Intake.CannotBeDatedInTheFuture");
    }

    @Test
    void shouldRejectAWeightThatIsNotBelievable() throws Exception {
        var response = send("POST", "/nutrition/plan", """
            {"startWeight":"4.0","targetWeight":"78.0","calories":1940,"protein":150}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Nutrition.WeightOutOfRange");
    }

    @Test
    void shouldRejectAPlanWithoutAQuota() throws Exception {
        var response = send("POST", "/nutrition/plan", """
            {"startWeight":"84.0","targetWeight":"78.0","calories":0}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Plan.CaloriesRequired");
    }

    @Test
    void shouldRejectAWeightThatIsNotANumber() throws Exception {
        var response = send("POST", "/nutrition/plan", """
            {"startWeight":"ochenta","targetWeight":"78.0","calories":1940,"protein":150}""");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldReportNoActivePlanWhenAdjusting() throws Exception {
        var response = send("PUT", "/nutrition/plan", """
            {"targetWeight":"76.0","calories":1855,"protein":160,"carbs":180,"fat":55}""");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json(response).get("code")).isEqualTo("Plan.NoneActive");
    }

    @Test
    void shouldReportAnUnknownIntakeAsNotFound() throws Exception {
        var response = send("GET", "/nutrition/intakes/I00009999", null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json(response).get("code")).isEqualTo("Intake.NotFound");
    }

    @Test
    void shouldRejectADayThatIsNotAnIsoDate() throws Exception {
        assertThat(send("GET", "/nutrition/days/ayer", null).statusCode()).isEqualTo(400);
    }

    @Test
    void shouldFilterTheListByPeriodAndLimit() throws Exception {
        recordIntake(30, 60, 10, "2026-08-21");
        recordIntake(45, 40, 20, "2026-08-22");

        assertThat(jsonList(send("GET", "/nutrition/intakes?from=2026-08-22", null))).hasSize(1);
        assertThat(jsonList(send("GET", "/nutrition/intakes?limit=1", null))).hasSize(1);
    }

    @Test
    void shouldServeItsDocsBelowTheModulePath() throws Exception {
        assertThat(send("GET", "/", null).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/nutrition/docs", null).body()).contains("swagger");
        assertThat(json(send("GET", "/nutrition/openapi.json", null))).containsKey("paths");
    }

    private static HttpResponse<String> definePlan() throws Exception {
        return send("POST", "/nutrition/plan", """
            {"startWeight":"84.0","targetWeight":"78.0","calories":1940,"protein":150,"carbs":200,"fat":60}""");
    }

    private static void recordIntake(int protein, int carbs, int fat, String day) throws Exception {
        var calories = 4 * protein + 4 * carbs + 9 * fat;

        send("POST", "/nutrition/intakes", day == null
            ? """
                {"calories":%d,"protein":%d,"carbs":%d,"fat":%d}"""
                .formatted(calories, protein, carbs, fat)
            : """
                {"calories":%d,"protein":%d,"carbs":%d,"fat":%d,"consumedOn":"%s"}"""
                .formatted(calories, protein, carbs, fat, day));
    }

    private static HttpResponse<String> send(String method, String path, String body) throws Exception {
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
    private static List<Map<String, Object>> jsonList(HttpResponse<String> response) throws IOException {
        return (List<Map<String, Object>>) (List<?>) JSON.std.listFrom(response.body());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> macros(Map<String, Object> body, String field) {
        return (Map<String, Object>) body.get(field);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> intakesOf(Map<String, Object> day) {
        return (List<Map<String, Object>>) day.get("intakes");
    }
}

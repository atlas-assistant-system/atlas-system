package atlas.app.routines;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sharedkernel.infrastructure.logging.LogEntryRenderers;

class HttpApiIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 2, 11);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @TempDir
    static Path databaseDirectory;

    private static RoutinesApplication application;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        application =
            RoutinesApplication.wire(LogEntryRenderers.forConsole(false, ZoneOffset.UTC), databaseDirectory, CLOCK)
                .start(0);
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + application.port();
    }

    @AfterAll
    static void stopServer() {
        application.stop();
    }

    @Test
    void shouldWalkTheWholeLifeOfARoutine() throws Exception {
        var created = send("POST", "/routines", """
            {"name":"Correr","target":3,"unit":"veces","period":"WEEK"}""");
        assertThat(created.statusCode()).isEqualTo(201);

        var routine = json(created);
        var id = (String) routine.get("id");
        assertThat(id).matches("R\\d{8}");
        assertThat(routine.get("name")).isEqualTo("Correr");
        assertThat(routine.get("period")).isEqualTo("WEEK");

        var todayList = jsonList(send("GET", "/routines/today", null));
        assertThat(todayList).hasSize(1);

        var logged = json(send("POST", "/routines/" + id + "/entries", "{}"));
        assertThat(logged.get("logged")).isEqualTo("1");
        assertThat(logged.get("target")).isEqualTo("3");
        assertThat(logged.get("met")).isEqualTo(false);
        assertThat(logged.get("periodStart")).isEqualTo("2026-02-09");
        assertThat(logged.get("periodEnd")).isEqualTo("2026-02-16");

        var again = json(send("POST", "/routines/" + id + "/entries", """
            {"day":"2026-02-09","amount":2}"""));
        assertThat(again.get("logged")).isEqualTo("3");
        assertThat(again.get("met")).isEqualTo(true);

        var streak = json(send("GET", "/routines/" + id + "/streak", null));
        assertThat(streak.get("current")).isEqualTo(1);

        var history = jsonList(send("GET", "/routines/" + id + "/history?from=2026-02-09&to=2026-02-11", null));
        assertThat(history).hasSize(3);
        assertThat(history.getFirst().get("logged")).isEqualTo("2");

        assertThat(send("DELETE", "/routines/" + id + "/entries/2026-02-09", null).statusCode()).isEqualTo(204);
        assertThat(json(send("GET", "/routines/" + id + "/progress", null)).get("logged")).isEqualTo("1");

        assertThat(send("POST", "/routines/" + id + "/archive", "").statusCode()).isEqualTo(204);
        assertThat(jsonList(send("GET", "/routines", null))).isEmpty();
        assertThat(jsonList(send("GET", "/routines?includeArchived=true", null))).hasSize(1);

        assertThat(send("POST", "/routines/" + id + "/unarchive", "").statusCode()).isEqualTo(204);
        assertThat(send("DELETE", "/routines/" + id, null).statusCode()).isEqualTo(204);
        assertThat(jsonList(send("GET", "/routines?includeArchived=true", null))).isEmpty();
    }

    @Test
    void shouldKeepTheLastDayOfMonthAndTheThirtyFirstApart() throws Exception {
        var lastDay = (String) json(send("POST", "/routines", """
            {"name":"Cierre de mes","target":1,"period":"MONTH","daysOfMonth":[0]}""")).get("id");
        var thirtyFirst = (String) json(send("POST", "/routines", """
            {"name":"El 31","target":1,"period":"MONTH","daysOfMonth":[31]}""")).get("id");

        assertThat(json(send("GET", "/routines/" + lastDay, null)).get("daysOfMonth")).isEqualTo(List.of(0));

        var onFebruary28 = send("POST", "/routines/" + lastDay + "/entries", """
            {"day":"2026-02-28"}""");
        assertThat(onFebruary28.statusCode()).isEqualTo(200);

        var refusedOn28 = send("POST", "/routines/" + thirtyFirst + "/entries", """
            {"day":"2026-02-28"}""");
        assertThat(refusedOn28.statusCode()).isEqualTo(400);
        assertThat(json(refusedOn28).get("code")).isEqualTo("Routine.DayNotScheduled");

        assertThat(send("POST", "/routines/" + thirtyFirst + "/entries", """
            {"day":"2026-01-31"}""").statusCode()).isEqualTo(200);

        send("DELETE", "/routines/" + lastDay, null);
        send("DELETE", "/routines/" + thirtyFirst, null);
    }

    @Test
    void shouldRejectAnIncoherentSchedule() throws Exception {
        var response = send("POST", "/routines", """
            {"name":"Sin dias","target":1,"period":"DAY"}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Routine.ActiveDaysRequiredForDailyRoutine");
    }

    @Test
    void shouldRejectAnInvalidName() throws Exception {
        var response = send("POST", "/routines", """
            {"name":"  ","target":1,"period":"WEEK"}""");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json(response).get("code")).isEqualTo("Routine.NameRequired");
    }

    @Test
    void shouldRejectAnUnknownPeriod() throws Exception {
        var response = send("POST", "/routines", """
            {"name":"Correr","target":1,"period":"FORTNIGHT"}""");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldReportAnUnknownRoutineAsNotFound() throws Exception {
        var response = send("GET", "/routines/R00009999", null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json(response).get("code")).isEqualTo("Routine.NotFound");
    }

    @Test
    void shouldServeTheUiAndTheDocs() throws Exception {
        assertThat(send("GET", "/", null).body()).contains("<title>Rutinas</title>");
        assertThat(send("GET", "/app.css", null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/app.js", null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/docs", null).body()).contains("swagger");
        assertThat(json(send("GET", "/openapi.json", null))).containsKey("paths");
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
}

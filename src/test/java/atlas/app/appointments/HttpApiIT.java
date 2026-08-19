package atlas.app.appointments;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpApiIT {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");

    @TempDir
    static Path databaseDirectory;

    private static AppointmentsApplication application;
    private static HttpClient client;
    private static String base;
    private static final AtomicBoolean AUTHENTICATED = new AtomicBoolean(true);

    @BeforeAll
    static void startServer() throws IOException {
        application = AppointmentsApplication
            .wire(
                LogEntryRenderers.forCurrentConsole(), databaseDirectory, Clock.fixed(NOW, ZoneOffset.UTC),
                AUTHENTICATED::get)
            .start(0);
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + application.port();
    }

    @AfterAll
    static void stopServer() {
        application.stop();
    }

    @AfterEach
    void restoreActiveSession() {
        AUTHENTICATED.set(true);
    }

    @Test
    void shouldScheduleAnAppointmentAndServeItBack() throws Exception {
        var created = post("/appointments", """
            {"title":"Dentista","description":"Llevar radiografia","start":"2026-08-20T11:00",
             "end":"2026-08-20T12:00","reminderLeadTimesMinutes":[15],"allowOverlap":false}""");

        assertThat(created.statusCode()).isEqualTo(201);
        var body = JSON.std.mapFrom(created.body());
        var id = (String) body.get("id");
        assertThat(id).matches("A[0-9]{8}");
        assertThat(created.headers().firstValue("Location")).contains("/appointments/" + id);

        var detail = get("/appointments/" + id);

        assertThat(detail.statusCode()).isEqualTo(200);
        var loaded = JSON.std.mapFrom(detail.body());
        assertThat(loaded.get("title")).isEqualTo("Dentista");
        assertThat(loaded.get("status")).isEqualTo("SCHEDULED");
        assertThat((java.util.List<?>) loaded.get("reminders")).hasSize(1);
    }

    @Test
    void shouldRejectAMalformedBodyWith400() throws Exception {
        var response = post("/appointments", "esto no es json");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("correlationId");
    }

    @Test
    void shouldRejectANonIsoDateWith400() throws Exception {
        var response = post("/appointments", """
            {"title":"Dentista","start":"manana","end":"2026-08-20T12:00"}""");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldReturn404ForAnUnknownAppointment() throws Exception {
        var response = get("/appointments/A09999999");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Appointment.NotFound");
    }

    @Test
    void shouldReturn400ForAMalformedAppointmentId() throws Exception {
        var response = get("/appointments/nope");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldRejectAnOverlapAndAcceptItWithTheExplicitOverride() throws Exception {
        var first = post("/appointments", appointment("2026-08-21T13:00", "2026-08-21T14:00"));
        assertThat(first.statusCode()).isEqualTo(201);

        var blocked = post("/appointments", appointment("2026-08-21T13:30", "2026-08-21T14:30"));

        assertThat(blocked.statusCode()).isEqualTo(409);
        assertThat(blocked.body()).contains("Appointment.Overlaps");

        var overridden = post("/appointments", """
            {"title":"Forzada","start":"2026-08-21T13:30","end":"2026-08-21T14:30","allowOverlap":true}""");

        assertThat(overridden.statusCode()).isEqualTo(201);
        var body = JSON.std.mapFrom(overridden.body());
        assertThat((java.util.List<?>) body.get("conflictingAppointmentIds")).isNotEmpty();
    }

    @Test
    void shouldCancelAnAppointmentAndKeepItReadable() throws Exception {
        var created = post("/appointments", appointment("2026-08-22T09:00", "2026-08-22T10:00"));
        var id = (String) JSON.std.mapFrom(created.body()).get("id");

        var cancelled = post("/appointments/" + id + "/cancel", "");

        assertThat(cancelled.statusCode()).isEqualTo(204);
        assertThat(JSON.std.mapFrom(get("/appointments/" + id).body()).get("status")).isEqualTo("CANCELLED");
    }

    @Test
    void shouldListTheAnchoredDayWithItsNavigationAnchors() throws Exception {
        post("/appointments", appointment("2026-08-23T09:00", "2026-08-23T10:00"));

        var response = get("/appointments?period=DAY&anchor=2026-08-23");

        assertThat(response.statusCode()).isEqualTo(200);
        var body = JSON.std.mapFrom(response.body());
        assertThat((java.util.List<?>) body.get("items")).isNotEmpty();
        assertThat(body.get("previousAnchor")).isEqualTo("2026-08-22");
        assertThat(body.get("nextAnchor")).isEqualTo("2026-08-24");
    }

    @Test
    void shouldOfferFreeSlotsAroundABooking() throws Exception {
        post("/appointments", appointment("2026-08-24T10:00", "2026-08-24T11:00"));

        var response = get("/appointments/free-slots?day=2026-08-24&from=09:00&to=18:00&minDurationMinutes=60");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("2026-08-24T09:00").contains("2026-08-24T11:00");
    }

    @Test
    void shouldDeleteAnAppointmentForGood() throws Exception {
        var created = post("/appointments", """
            {"title":"Borrable","start":"2026-08-30T09:00","end":"2026-08-30T10:00",
             "reminderLeadTimesMinutes":[15]}""");
        var id = (String) JSON.std.mapFrom(created.body()).get("id");

        var deleted = delete("/appointments/" + id);

        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(get("/appointments/" + id).statusCode()).isEqualTo(404);
        assertThat(delete("/appointments/" + id).statusCode()).isEqualTo(404);
        assertThat(get("/appointments?period=DAY&anchor=2026-08-30&includeCancelled=true").body())
            .doesNotContain("Borrable");
    }

    @Test
    void shouldRestoreACancelledAppointmentWithItsOriginalSlot() throws Exception {
        var created = post("/appointments", """
            {"title":"Fisio","start":"2026-08-28T09:00","end":"2026-08-28T10:00",
             "reminderLeadTimesMinutes":[30]}""");
        var id = (String) JSON.std.mapFrom(created.body()).get("id");
        post("/appointments/" + id + "/cancel", "");

        var restored = post("/appointments/" + id + "/restore", "");

        assertThat(restored.statusCode()).isEqualTo(200);
        var body = JSON.std.mapFrom(restored.body());
        assertThat(body.get("status")).isEqualTo("SCHEDULED");
        assertThat(body.get("start")).isEqualTo("2026-08-28T09:00");
        assertThat((java.util.List<?>) body.get("reminders")).hasSize(1);

        var again = post("/appointments/" + id + "/restore", "");

        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(again.body()).contains("Appointment.NotCancelled");
    }

    @Test
    void shouldRefuseToRestoreOntoATakenSlotUnlessOverridden() throws Exception {
        var created = post("/appointments", appointment("2026-08-29T09:00", "2026-08-29T10:00"));
        var id = (String) JSON.std.mapFrom(created.body()).get("id");
        post("/appointments/" + id + "/cancel", "");
        post("/appointments", appointment("2026-08-29T09:30", "2026-08-29T10:30"));

        var blocked = post("/appointments/" + id + "/restore", "");

        assertThat(blocked.statusCode()).isEqualTo(409);
        assertThat(blocked.body()).contains("Appointment.Overlaps");

        var forced = post("/appointments/" + id + "/restore", "{\"allowOverlap\":true}");

        assertThat(forced.statusCode()).isEqualTo(200);
        assertThat((java.util.List<?>) JSON.std.mapFrom(forced.body()).get("conflictingAppointmentIds")).isNotEmpty();
    }

    @Test
    void shouldRescheduleAnAppointmentAndChangeItsDetails() throws Exception {
        var created = post("/appointments", appointment("2026-08-25T09:00", "2026-08-25T10:00"));
        var id = (String) JSON.std.mapFrom(created.body()).get("id");

        var moved = post("/appointments/" + id + "/reschedule", """
            {"newStart":"2026-08-25T16:00","newEnd":"2026-08-25T17:00","allowOverlap":false}""");

        assertThat(moved.statusCode()).isEqualTo(200);
        assertThat(JSON.std.mapFrom(moved.body()).get("start")).isEqualTo("2026-08-25T16:00");

        var renamed = put("/appointments/" + id + "/details", """
            {"title":"Revision","description":"Con informe"}""");

        assertThat(renamed.statusCode()).isEqualTo(200);
        var body = JSON.std.mapFrom(renamed.body());
        assertThat(body.get("title")).isEqualTo("Revision");
        assertThat(body.get("description")).isEqualTo("Con informe");
    }

    @Test
    void shouldAddAcknowledgeAndRemoveAReminder() throws Exception {
        var created = post("/appointments", appointment("2026-08-26T09:00", "2026-08-26T10:00"));
        var id = (String) JSON.std.mapFrom(created.body()).get("id");

        var added = post("/appointments/" + id + "/reminders", "{\"leadTimeMinutes\":45}");

        assertThat(added.statusCode()).isEqualTo(200);
        var reminder = (Map<?, ?>) ((java.util.List<?>) JSON.std.mapFrom(added.body()).get("reminders")).get(0);
        var reminderId = (String) reminder.get("id");
        assertThat(reminder.get("acknowledgedAt")).isNull();

        var acknowledged = post("/appointments/" + id + "/reminders/" + reminderId + "/acknowledge", "");

        assertThat(acknowledged.statusCode()).isEqualTo(204);
        var reloaded = (Map<?, ?>) ((java.util.List<?>) JSON.std
            .mapFrom(get("/appointments/" + id).body())
            .get("reminders")).get(0);
        assertThat(reloaded.get("acknowledgedAt")).isNotNull();

        var removed = delete("/appointments/" + id + "/reminders/" + reminderId);

        assertThat(removed.statusCode()).isEqualTo(204);
        assertThat((java.util.List<?>) JSON.std.mapFrom(get("/appointments/" + id).body()).get("reminders")).isEmpty();
    }

    @Test
    void shouldReportOverlapsAndCountsForTheCalendar() throws Exception {
        post("/appointments", appointment("2026-08-27T09:00", "2026-08-27T10:00"));

        var overlapping = get("/appointments/overlapping?start=2026-08-27T09:30&end=2026-08-27T10:30");

        assertThat(overlapping.statusCode()).isEqualTo(200);
        assertThat(overlapping.body()).contains("Cita");

        var byDay = get("/appointments/counts/by-day?month=2026-08");

        assertThat(byDay.statusCode()).isEqualTo(200);
        assertThat(byDay.body()).contains("{\"day\":\"2026-08-27\",\"count\":1}");

        var byMonth = get("/appointments/counts/by-month?year=2026");

        assertThat(byMonth.statusCode()).isEqualTo(200);
        assertThat(byMonth.body()).contains("\"month\":\"2026-08\"");
    }

    @Test
    void shouldServeTheOpenApiSpec() throws Exception {
        var response = get("/openapi.json");

        assertThat(response.statusCode()).isEqualTo(200);
        var spec = JSON.std.mapFrom(response.body());
        assertThat(spec.get("openapi")).isEqualTo("3.0.3");
        assertThat(((Map<?, ?>) spec.get("paths")).containsKey("/appointments")).isTrue();
    }

    @Test
    void shouldKeepAppointmentAccessLockedWithoutAnActivePresenceSession() throws Exception {
        AUTHENTICATED.set(false);

        var appointments = get("/appointments/upcoming?limit=1");

        assertThat(appointments.statusCode()).isEqualTo(401);
        assertThat(appointments.body()).contains("Authentication.Required");
    }

    @Test
    void shouldKeepTheEventStreamLockedWithoutAnActiveSession() throws Exception {
        AUTHENTICATED.set(false);

        assertThat(get("/events").statusCode()).isEqualTo(401);
    }

    @Test
    void shouldServeTheSwaggerUiPage() throws Exception {
        var response = get("/docs");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/html");
        assertThat(response.body()).contains("SwaggerUIBundle").contains("/openapi.json");
    }

    private static String appointment(String start, String end) {
        return "{\"title\":\"Cita\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path)).DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> put(String path, String body) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }
}

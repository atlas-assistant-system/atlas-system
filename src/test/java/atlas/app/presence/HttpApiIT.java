package atlas.app.presence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.vos.MatchThreshold;
import atlas.domain.presence.vos.SessionDuration;
import atlas.infrastructure.sharedkernel.logging.LogEntryRenderers;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpApiIT {

    private static final Instant NOW = Instant.parse("2026-08-19T10:00:00Z");

    @TempDir
    static Path databaseDirectory;

    private static PresenceApplication application;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        var settings = new PresenceSettings(
            0,
            databaseDirectory,
            true,
            MatchThreshold.of(0.8),
            SessionDuration.of(Duration.ofMinutes(15)));
        application = PresenceApplication
            .wire(LogEntryRenderers.forCurrentConsole(), settings, Clock.fixed(NOW, ZoneOffset.UTC))
            .start(0);
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + application.port();
    }

    @AfterAll
    static void stopServer() {
        application.stop();
    }

    @Test
    void shouldEnrollManageAndReadAProfileWithoutExposingDescriptors() throws Exception {
        var created = enroll("Ada");

        assertThat(created.statusCode()).isEqualTo(201);
        var profile = json(created);
        var id = (String) profile.get("id");
        assertThat(id).matches("B[0-9]{8}");
        assertThat(created.headers().firstValue("Location")).contains("/profiles/" + id);
        assertThat(created.body()).doesNotContain("descriptor").doesNotContain("1.0");

        var added = post("/profiles/" + id + "/templates", """
            {"modelVersion":"face-v1","descriptor":[0.9,0.1]}""");
        assertThat(added.statusCode()).isEqualTo(200);
        var templates = (List<?>) json(added).get("templates");
        assertThat(templates).hasSize(2);

        var second = (String) ((Map<?, ?>) templates.get(1)).get("id");
        assertThat(delete("/profiles/" + id + "/templates/" + second).statusCode()).isEqualTo(204);

        var detail = get("/profiles/" + id);
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(json(detail).get("templateCount")).isEqualTo(1);
        assertThat(get("/profiles").body()).contains("Ada");
    }

    @Test
    void shouldOpenRefreshAndCloseASession() throws Exception {
        enroll("Grace");
        var challenge = json(post("/authentication/challenges", ""));

        var completed = post(
            "/authentication/challenges/" + challenge.get("challengeId") + "/complete",
            completion(challenge, "[1.0,0.0]"));

        assertThat(completed.statusCode()).isEqualTo(200);
        var session = json(completed);
        var id = (String) session.get("id");
        assertThat(id).matches("S[0-9]{8}");
        assertThat(json(get("/sessions/active")).get("id")).isEqualTo(id);
        assertThat(post("/sessions/" + id + "/refresh", "").statusCode()).isEqualTo(200);
        assertThat(delete("/sessions/" + id).statusCode()).isEqualTo(204);
        assertThat(get("/sessions/active").body()).isEqualTo("null");
    }

    @Test
    void shouldPersistARejectedAuthenticationInTheAudit() throws Exception {
        enroll("Linus");
        var challenge = json(post("/authentication/challenges", ""));

        var rejected = post(
            "/authentication/challenges/" + challenge.get("challengeId") + "/complete",
            completion(challenge, "[20.0,20.0]"));

        assertThat(rejected.statusCode()).isEqualTo(401);
        assertThat(rejected.body()).contains("Verification.NoMatch");
        var attempts = json(get("/authentication/attempts?page=1&pageSize=10"));
        assertThat(((Number) attempts.get("totalCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(attempts.toString()).contains("NO_MATCH");
        assertThat(json(get("/authentication")).get("failedAttempts")).isEqualTo(1);
    }

    @Test
    void shouldPublishHandGesturesAndCloseTheSessionWithFist() throws Exception {
        enroll("Gesture user");
        var challenge = json(post("/authentication/challenges", ""));
        var completed = post(
            "/authentication/challenges/" + challenge.get("challengeId") + "/complete",
            completion(challenge, "[1.0,0.0]"));
        var sessionId = (String) json(completed).get("id");
        var events = new ByteArrayOutputStream();
        application.hub().register(events);

        var accepted = post("/interactions/gestures", """
            {"type":"OPEN_PALM","handIndex":0,"confidence":0.94,
             "observedAt":"2026-08-19T10:00:00Z"}""");

        assertThat(accepted.statusCode()).isEqualTo(204);
        assertThat(events.toString(StandardCharsets.UTF_8))
            .contains("event: sessionRefreshed")
            .contains("event: gestureDetected")
            .contains("\"type\":\"OPEN_PALM\"")
            .contains("\"confidence\":0.94")
            .contains("\"sessionId\":\"" + sessionId + "\"");
        assertThat(post("/interactions/gestures", """
            {"type":"WAVE","handIndex":0,"confidence":0.9,
             "observedAt":"2026-08-19T10:00:00Z"}""").statusCode()).isEqualTo(400);
        assertThat(post("/interactions/gestures", """
            {"type":"PALM_LEFT","handIndex":0,"confidence":0.93,
             "observedAt":"2026-08-19T10:00:00Z"}""").statusCode()).isEqualTo(204);
        assertThat(events.toString(StandardCharsets.UTF_8)).contains("\"type\":\"PALM_LEFT\"");

        assertThat(post("/interactions/gestures", """
            {"type":"VICTORY","handIndex":0,"confidence":0.96,
             "observedAt":"2026-08-19T10:00:01Z"}""").statusCode()).isEqualTo(400);
        assertThat(post("/interactions/gestures", """
            {"type":"FIST","handIndex":0,"confidence":0.96,
             "observedAt":"2026-08-19T10:00:01Z"}""").statusCode()).isEqualTo(204);
        assertThat(events.toString(StandardCharsets.UTF_8))
            .contains("event: sessionClosed")
            .contains("event: gestureDetected")
            .contains("\"type\":\"FIST\"");
        assertThat(json(get("/sessions/active"))).isNull();

        assertThat(post("/interactions/gestures", """
            {"type":"OPEN_PALM","handIndex":0,"confidence":0.9,
             "observedAt":"2026-08-19T10:00:00Z"}""").statusCode()).isEqualTo(401);
    }

    @Test
    void shouldTranslateMalformedInputAndIdsToBadRequests() throws Exception {
        assertThat(post("/profiles", "not json").statusCode()).isEqualTo(400);
        assertThat(post("/profiles", "{\"displayName\":\"Ada\"}").statusCode()).isEqualTo(400);
        assertThat(post("/profiles", """
            {"displayName":"Ada","modelVersion":"face-v1","descriptor":[1e1000]}""").statusCode())
            .isEqualTo(400);
        assertThat(get("/profiles/nope").statusCode()).isEqualTo(400);
        assertThat(get("/authentication/attempts?page=wrong").statusCode()).isEqualTo(400);
    }

    @Test
    void shouldServeTheDashboardAndOpenApi() throws Exception {
        assertThat(get("/").body())
            .contains("Te reconoce y te entiende")
            .contains("id=\"camera\"")
            .contains("id=\"authenticate\"")
            .contains("/app.js");
        assertThat(get("/app.css").statusCode()).isEqualTo(200);
        assertThat(get("/app.js").statusCode()).isEqualTo(200);
        assertThat(get("/docs").body()).contains("SwaggerUIBundle").contains("/openapi.json");

        var specification = json(get("/openapi.json"));
        assertThat(specification.get("openapi")).isEqualTo("3.0.3");
        assertThat(((Map<?, ?>) specification.get("paths")).containsKey("/authentication/challenges")).isTrue();
        assertThat(json(get("/authentication")).get("maintenanceMode")).isEqualTo(true);
    }

    private static HttpResponse<String> enroll(String name) throws IOException, InterruptedException {
        return post("/profiles", "{\"displayName\":\"" + name
            + "\",\"modelVersion\":\"face-v1\",\"descriptor\":[1.0,0.0]}");
    }

    private static String completion(Map<String, Object> challenge, String descriptor) {
        return "{\"modelVersion\":\"face-v1\",\"descriptor\":" + descriptor
            + ",\"observedType\":\"" + challenge.get("type")
            + "\",\"nonce\":\"" + challenge.get("nonce")
            + "\",\"capturedAt\":\"" + NOW + "\"}";
    }

    private static Map<String, Object> json(HttpResponse<String> response) throws IOException {
        return JSON.std.mapFrom(response.body());
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path)).DELETE().build(), HttpResponse.BodyHandlers.ofString());
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

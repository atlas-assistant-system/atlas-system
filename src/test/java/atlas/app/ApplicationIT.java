package atlas.app;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.app.presence.PresenceSettings;
import atlas.application.presence.commands.beginauthentication.BeginAuthenticationCommand;
import atlas.application.presence.commands.closesession.CloseSessionCommand;
import atlas.application.presence.commands.completeauthentication.CompleteAuthenticationCommand;
import atlas.application.presence.commands.enrollprofile.EnrollProfileCommand;
import atlas.application.sharedkernel.logging.PlainLogEntryRenderer;
import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.SessionId;
import atlas.domain.presence.vos.MatchThreshold;
import atlas.domain.presence.vos.SessionDuration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationIT {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-19T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path dataDirectory;

    @Test
    void shouldConnectAppointmentsToPresenceInMemory() throws Exception {
        var settings = new PresenceSettings(
            0,
            dataDirectory,
            true,
            MatchThreshold.of(0.8),
            SessionDuration.of(Duration.ofMinutes(15)));
        var application = Application.wire(new PlainLogEntryRenderer(), settings, CLOCK).start(0);

        try {
            var client = HttpClient.newHttpClient();
            var base = "http://localhost:" + application.port();

            var core = get(client, base, "/");
            assertThat(core.statusCode()).isEqualTo(200);
            assertThat(core.body()).contains("<title>Atlas</title>")
                .contains("data-view=\"inicio\"")
                .contains("data-view=\"agenda\"")
                .contains("data-view=\"rutinas\"")
                .contains("id=\"view-rutinas\"")
                .contains("data-view=\"economia\"")
                .contains("id=\"view-economia\"");
            assertThat(get(client, base, "/assets/app.css").statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/assets/routines.css").statusCode()).isEqualTo(200);
            var coreScript = get(client, base, "/assets/app.js");
            assertThat(coreScript.statusCode()).isEqualTo(200);
            assertThat(coreScript.body()).contains("'/authentication'")
                .doesNotContain("'/presence/");
            assertThat(get(client, base, "/assets/routines.js").body())
                .contains("'/events/routines'");
            assertThat(get(client, base, "/assets/economy.css").statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/assets/economy.js").body())
                .contains("'/events/economy'");
            assertThat(get(client, base, "/assets/nutrition.css").statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/assets/nutrition.js").body())
                .contains("'/events/nutrition'");
            assertThat(get(client, base, "/assets/config.js").statusCode()).isEqualTo(404);

            assertThat(get(client, base, "/appointments/upcoming?limit=1").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/economy/balance").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/routines/today").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/nutrition/today").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/events").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/events/routines").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/events/economy").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/events/nutrition").statusCode()).isEqualTo(401);
            assertThat(get(client, base, "/authentication").statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/presence/authentication").statusCode()).isEqualTo(404);
            assertThat(get(client, base, "/presence/sandbox").body())
                .contains("Vision sandbox", "/presence/sandbox.js", "/presence/sandbox.css");
            assertThat(get(client, base, "/presence/sandbox.js").statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/presence/face-quality.js").statusCode()).isEqualTo(200);

            var commands = application.presence().commands();
            var profile = commands.dispatch(
                new EnrollProfileCommand("Ada", "face-v1", new float[]{1.0f, 0.0f})).value();
            var configured = put(client, base, "/home/profiles/" + profile.id(), """
                {
                  "locationName": "Las Palmas",
                  "latitude": 28.1235,
                  "longitude": -15.4363,
                  "timeZone": "Atlantic/Canary",
                  "newsCategories": ["AI", "DEVELOPMENT"]
                }
                """);
            assertThat(configured.statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/home/profiles/" + profile.id()).body())
                .contains("Las Palmas", "Atlantic/Canary", "AI", "DEVELOPMENT");
            var challenge = commands.dispatch(new BeginAuthenticationCommand()).value();
            var authenticated = commands.dispatch(new CompleteAuthenticationCommand(
                LivenessChallengeId.parse(challenge.challengeId()),
                "face-v1",
                new float[]{1.0f, 0.0f},
                challenge.type(),
                challenge.nonce(),
                CLOCK.instant()));

            assertThat(authenticated.isSuccess()).isTrue();
            assertThat(get(client, base, "/appointments/upcoming?limit=1").statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/economy/balance").statusCode()).isEqualTo(200);
            assertThat(get(client, base, "/nutrition/today").statusCode()).isEqualTo(200);

            var stream = client.sendAsync(
                HttpRequest.newBuilder(URI.create(base + "/events/economy")).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
            waitUntil(() -> application.economy().hub().connectedCount() == 1);
            assertThat(application.economy().hub().connectedCount()).isOne();

            var nutritionStream = client.sendAsync(
                HttpRequest.newBuilder(URI.create(base + "/events/nutrition")).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
            waitUntil(() -> application.nutrition().hub().connectedCount() == 1);

            commands.dispatch(new CloseSessionCommand(SessionId.parse(authenticated.value().id())));

            waitUntil(() -> application.economy().hub().connectedCount() == 0);
            assertThat(application.economy().hub().connectedCount()).isZero();
            assertThat(get(client, base, "/economy/balance").statusCode()).isEqualTo(401);

            waitUntil(() -> application.nutrition().hub().connectedCount() == 0);
            assertThat(application.nutrition().hub().connectedCount()).isZero();
            assertThat(get(client, base, "/nutrition/today").statusCode()).isEqualTo(401);

            stream.cancel(true);
            nutritionStream.cancel(true);
        } finally {
            application.stop();
        }
    }

    @Test
    void shouldServeEveryVisionAssetFromAtlasItself() throws Exception {
        var settings = new PresenceSettings(
            0,
            dataDirectory,
            true,
            MatchThreshold.of(0.8),
            SessionDuration.of(Duration.ofMinutes(15)));
        var application = Application.wire(new PlainLogEntryRenderer(), settings, CLOCK).start(0);

        try {
            var client = HttpClient.newHttpClient();
            var base = "http://localhost:" + application.port();

            for (var page : new String[]{"/", "/presence/sandbox"}) {
                assertThat(get(client, base, page).body())
                    .as("page %s", page)
                    .doesNotContain("cdn.jsdelivr.net")
                    .doesNotContain("storage.googleapis.com")
                    .contains("/vendor/human/human.js");
            }
            for (var script : new String[]{"/assets/app.js", "/presence/sandbox.js"}) {
                assertThat(get(client, base, script).body())
                    .as("script %s", script)
                    .doesNotContain("cdn.jsdelivr.net")
                    .doesNotContain("storage.googleapis.com")
                    .contains("/vendor/mediapipe/gesture_recognizer.task");
            }

            assertThat(head(client, base, "/vendor/human/human.js"))
                .satisfies(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(contentType(response)).startsWith("text/javascript");
                });
            assertThat(contentType(head(client, base, "/vendor/mediapipe/wasm/vision_wasm_internal.wasm")))
                .isEqualTo("application/wasm");
            assertThat(contentType(head(client, base, "/vendor/human/models/blazeface.json")))
                .startsWith("application/json");
            assertThat(head(client, base, "/vendor/mediapipe/gesture_recognizer.task").statusCode())
                .isEqualTo(200);
            var descriptorModel = head(client, base, "/vendor/human/models/faceres.bin");
            assertThat(descriptorModel.statusCode()).isEqualTo(200);
            assertThat(descriptorModel.body()).hasSizeGreaterThan(6_000_000);
            assertThat(head(client, base, "/vendor/human/models/nope.bin").statusCode()).isEqualTo(404);
            assertThat(head(client, base, "/vendor/../logging.properties").statusCode()).isEqualTo(404);
        } finally {
            application.stop();
        }
    }

    private static String contentType(HttpResponse<byte[]> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }

    private static HttpResponse<byte[]> head(HttpClient client, String base, String path) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        for (var attempt = 0; attempt < 100 && !condition.getAsBoolean(); attempt++) {
            Thread.sleep(20);
        }
    }

    private static HttpResponse<String> get(HttpClient client, String base, String path) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> put(HttpClient client, String base, String path, String body) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }
}

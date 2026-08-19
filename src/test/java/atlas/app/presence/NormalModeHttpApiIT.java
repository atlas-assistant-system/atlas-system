package atlas.app.presence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.presence.commands.enrollprofile.EnrollProfileCommand;
import atlas.application.sharedkernel.logging.PlainLogEntryRenderer;
import atlas.domain.presence.vos.MatchThreshold;
import atlas.domain.presence.vos.SessionDuration;
import com.fasterxml.jackson.jr.ob.JSON;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NormalModeHttpApiIT {

    private static final Instant NOW = Instant.parse("2026-08-19T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path databaseDirectory;

    @Test
    void shouldProtectManagementUntilAuthenticationAndAllowReEnrollmentAfterIt() throws Exception {
        var maintenance = PresenceApplication.wire(new PlainLogEntryRenderer(), settings(true), CLOCK);
        maintenance.commands().dispatch(new EnrollProfileCommand("Ada", "face-v1", new float[]{1.0f, 0.0f}));
        maintenance.stop();

        var normal = PresenceApplication.wire(new PlainLogEntryRenderer(), settings(false), CLOCK).start(0);
        try {
            var client = HttpClient.newHttpClient();
            var base = "http://localhost:" + normal.port();

            assertThat(send(client, base, "GET", "/profiles", "").statusCode()).isEqualTo(401);
            assertThat(send(client, base, "POST", "/profiles", profile("Grace")).statusCode()).isEqualTo(401);
            var challengeResponse = send(client, base, "POST", "/authentication/challenges", "");
            var challenge = JSON.std.mapFrom(challengeResponse.body());
            var completed = send(
                client,
                base,
                "POST",
                "/authentication/challenges/" + challenge.get("challengeId") + "/complete",
                completion(challenge));
            assertThat(completed.statusCode()).isEqualTo(200);

            assertThat(send(client, base, "GET", "/profiles", "").statusCode()).isEqualTo(200);
            assertThat(send(client, base, "POST", "/profiles", profile("Grace")).statusCode()).isEqualTo(201);
        } finally {
            normal.stop();
        }
    }

    private PresenceSettings settings(boolean maintenanceMode) {
        return new PresenceSettings(
            0,
            databaseDirectory,
            maintenanceMode,
            MatchThreshold.of(0.8),
            SessionDuration.of(Duration.ofMinutes(15)));
    }

    private static String profile(String name) {
        return "{\"displayName\":\"" + name
            + "\",\"modelVersion\":\"face-v1\",\"descriptor\":[1.0,0.0]}";
    }

    private static String completion(Map<String, Object> challenge) {
        return "{\"modelVersion\":\"face-v1\",\"descriptor\":[1.0,0.0]"
            + ",\"observedType\":\"" + challenge.get("type")
            + "\",\"nonce\":\"" + challenge.get("nonce")
            + "\",\"capturedAt\":\"" + NOW + "\"}";
    }

    private static HttpResponse<String> send(
        HttpClient client, String base, String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path));
        if ("GET".equals(method)) {
            request.GET();
        } else {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
